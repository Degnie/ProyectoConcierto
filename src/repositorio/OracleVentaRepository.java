package repositorio;

import conexion.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import modelo.Cliente;
import modelo.Concierto;
import modelo.Entrada;
import modelo.Venta;
import modelo.Zona;
import util.RegistradorErrores;

public class OracleVentaRepository implements VentaRepository {
    private static final int TIMEOUT_SEGUNDOS = 10;

    @Override
    public boolean guardarCompraCompleta(Cliente cliente, Concierto concierto, Venta venta) {
        if (cliente == null || concierto == null || venta == null) return false;
        String idZona = venta.getZona().getId().toString();

        String sqlSelectVersion = "SELECT version FROM zonas WHERE id = ?";
        String sqlUpdateZonaVersion = "UPDATE zonas SET version = version + 1 WHERE id = ? AND version = ?";
        String sqlInsertVenta = "INSERT INTO ventas "
                + "(id_venta, dni_cliente, id_concierto, id_zona, fecha_hora, monto_neto, puntos_redimidos, puntos_ganados, estado, payment_txn_id) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String sqlInsertEntrada = "INSERT INTO entradas (id_entrada, id_venta, id_zona, numero, estado) VALUES (?, ?, ?, ?, 'SOLD')";
        String sqlUpdatePuntos = "UPDATE usuarios SET puntos = ? WHERE dni = ?";

        try (Connection con = DatabaseConnection.getInstance().getConnection()) {
            con.setAutoCommit(false);
            try {
                // Concurrencia optimista pura: se lee la versión sin lock de fila y se actualiza
                // condicionada a que nadie la haya cambiado entretanto. Si otra transacción
                // concurrente ya vendió el último cupo, el UPDATE afecta 0 filas y se aborta como
                // conflicto — la sobreventa queda cortada acá, no solo por el "synchronized" en
                // memoria de Zona (que no protege contra una segunda instancia de la app apuntando
                // a la misma base). Sin FOR UPDATE no se serializan compradores de zonas distintas
                // esperando un lock que no necesitan; el propio UPDATE condicionado sigue siendo
                // atómico en Oracle.
                int versionActual;
                try (PreparedStatement ps = con.prepareStatement(sqlSelectVersion)) {
                    ps.setQueryTimeout(TIMEOUT_SEGUNDOS);
                    ps.setString(1, idZona);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException("La zona " + idZona + " no existe en la base de datos.");
                        }
                        versionActual = rs.getInt("version");
                    }
                }
                try (PreparedStatement ps = con.prepareStatement(sqlUpdateZonaVersion)) {
                    ps.setQueryTimeout(TIMEOUT_SEGUNDOS);
                    ps.setString(1, idZona);
                    ps.setInt(2, versionActual);
                    if (ps.executeUpdate() == 0) {
                        throw new SQLException("Conflicto de concurrencia en la zona " + idZona + " (version cambió entre lectura y escritura).");
                    }
                }

                // 2. Venta
                try (PreparedStatement ps = con.prepareStatement(sqlInsertVenta)) {
                    ps.setQueryTimeout(TIMEOUT_SEGUNDOS);
                    ps.setString(1, venta.getId().toString());
                    ps.setString(2, cliente.getDni());
                    ps.setString(3, concierto.getId().toString());
                    ps.setString(4, idZona);
                    ps.setTimestamp(5, new Timestamp(venta.getFecha().getTime()));
                    ps.setInt(6, venta.getMonto());
                    ps.setInt(7, venta.getPuntosRedimidos());
                    ps.setInt(8, venta.getPuntosGanados());
                    ps.setString(9, venta.getEstado().name());
                    ps.setString(10, venta.getPaymentTransactionId());
                    ps.executeUpdate();
                }

                // 3. Entradas, en un solo viaje de red (batch)
                try (PreparedStatement ps = con.prepareStatement(sqlInsertEntrada)) {
                    ps.setQueryTimeout(TIMEOUT_SEGUNDOS);
                    for (Entrada entrada : venta.getEntradas()) {
                        ps.setString(1, entrada.getId().toString());
                        ps.setString(2, venta.getId().toString());
                        ps.setString(3, idZona);
                        ps.setInt(4, entrada.getNumero());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                // 4. Saldo de puntos del cliente (ya recalculado en memoria por Cliente.comprar)
                try (PreparedStatement ps = con.prepareStatement(sqlUpdatePuntos)) {
                    ps.setQueryTimeout(TIMEOUT_SEGUNDOS);
                    ps.setInt(1, cliente.getPuntos());
                    ps.setString(2, cliente.getDni());
                    ps.executeUpdate();
                }

                con.commit();
                return true;
            } catch (SQLException e) {
                con.rollback();
                RegistradorErrores.registrar("OracleVentaRepository.guardarCompraCompleta", e);
                throw new RuntimeException("El servicio no pudo procesar la transacción.");
            }
        } catch (SQLException e) {
            RegistradorErrores.registrar("OracleVentaRepository.guardarCompraCompleta", e);
            throw new RuntimeException("El servicio no pudo procesar la transacción.");
        }
    }

    @Override
    public List<Venta> cargarHistorialPorCliente(String dniCliente, List<Concierto> conciertosEnMemoria) {
        // Un solo JOIN trae venta + todas sus entradas de una pasada — evita el N+1 de preguntar
        // "dame las entradas" venta por venta.
        String sql = "SELECT v.id_venta, v.id_concierto, v.id_zona, v.fecha_hora, v.monto_neto, "
                + "v.puntos_redimidos, v.puntos_ganados, v.estado, v.payment_txn_id, "
                + "e.id_entrada, e.numero "
                + "FROM ventas v JOIN entradas e ON e.id_venta = v.id_venta "
                + "WHERE v.dni_cliente = ? "
                + "ORDER BY v.fecha_hora, v.id_venta";

        Map<String, VentaEnConstruccion> porVenta = new LinkedHashMap<>();
        // Cache por Zona de sus entradas indexadas por número: evita recorrer linealmente
        // zona.getEntradas() (buscarEntradaEnZona) en cada una de las N filas del historial, lo
        // que degeneraba en O(N*tamañoZona) para historiales largos. Se construye una sola vez
        // por zona, la primera vez que aparece.
        Map<Zona, Map<Integer, Entrada>> entradasPorZona = new java.util.HashMap<>();
        try (Connection con = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setQueryTimeout(TIMEOUT_SEGUNDOS);
            ps.setString(1, dniCliente);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String idVenta = rs.getString("id_venta");
                    VentaEnConstruccion acumulado = porVenta.get(idVenta);
                    if (acumulado == null) {
                        String idConcierto = rs.getString("id_concierto");
                        String idZona = rs.getString("id_zona");
                        Zona zona = buscarZona(conciertosEnMemoria, idConcierto, idZona);
                        acumulado = new VentaEnConstruccion();
                        acumulado.id = UUID.fromString(idVenta);
                        acumulado.fecha = rs.getTimestamp("fecha_hora");
                        acumulado.monto = rs.getInt("monto_neto");
                        acumulado.puntosRedimidos = rs.getInt("puntos_redimidos");
                        acumulado.puntosGanados = rs.getInt("puntos_ganados");
                        acumulado.estado = Venta.EstadoVenta.valueOf(rs.getString("estado"));
                        acumulado.paymentTransactionId = rs.getString("payment_txn_id");
                        acumulado.zona = zona;
                        acumulado.conciertoNombre = buscarNombreConcierto(conciertosEnMemoria, idConcierto);
                        porVenta.put(idVenta, acumulado);
                    }
                    int numero = rs.getInt("numero");
                    Map<Integer, Entrada> indiceZona = acumulado.zona == null ? null
                            : entradasPorZona.computeIfAbsent(acumulado.zona, OracleVentaRepository::indexarEntradasPorNumero);
                    Entrada entradaViva = indiceZona == null ? null : indiceZona.get(numero);
                    if (entradaViva != null) {
                        acumulado.entradas.add(entradaViva); // misma instancia que ya vive en la Zona compartida
                    } else {
                        UUID idEntrada = UUID.fromString(rs.getString("id_entrada"));
                        acumulado.entradas.add(new Entrada(idEntrada, numero, Entrada.EstadoEntrada.SOLD));
                    }
                }
            }
        } catch (SQLException e) {
            RegistradorErrores.registrar("OracleVentaRepository.cargarHistorialPorCliente", e);
            throw new RuntimeException("El servicio no pudo procesar la transacción.");
        }

        List<Venta> resultado = new ArrayList<>();
        for (VentaEnConstruccion v : porVenta.values()) {
            resultado.add(Venta.reconstruirDesdeBaseDeDatos(v.id, v.fecha, v.monto, v.zona,
                    v.entradas.toArray(new Entrada[0]), v.conciertoNombre, v.paymentTransactionId,
                    v.estado, v.puntosRedimidos, v.puntosGanados));
        }
        return resultado;
    }

    @Override
    public boolean anularVentaPersistida(Cliente cliente, Venta venta) {
        if (cliente == null || venta == null) return false;
        String sqlUpdateEntradas = "UPDATE entradas SET estado = 'CANCELLED' WHERE id_venta = ?";
        String sqlUpdateVenta = "UPDATE ventas SET estado = 'CANCELLED' WHERE id_venta = ?";
        String sqlUpdatePuntos = "UPDATE usuarios SET puntos = ? WHERE dni = ?";

        try (Connection con = DatabaseConnection.getInstance().getConnection()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement ps = con.prepareStatement(sqlUpdateEntradas)) {
                    ps.setQueryTimeout(TIMEOUT_SEGUNDOS);
                    ps.setString(1, venta.getId().toString());
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = con.prepareStatement(sqlUpdateVenta)) {
                    ps.setQueryTimeout(TIMEOUT_SEGUNDOS);
                    ps.setString(1, venta.getId().toString());
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = con.prepareStatement(sqlUpdatePuntos)) {
                    ps.setQueryTimeout(TIMEOUT_SEGUNDOS);
                    ps.setInt(1, cliente.getPuntos());
                    ps.setString(2, cliente.getDni());
                    ps.executeUpdate();
                }
                con.commit();
                return true;
            } catch (SQLException e) {
                con.rollback();
                RegistradorErrores.registrar("OracleVentaRepository.anularVentaPersistida", e);
                throw new RuntimeException("El servicio no pudo procesar la transacción.");
            }
        } catch (SQLException e) {
            RegistradorErrores.registrar("OracleVentaRepository.anularVentaPersistida", e);
            throw new RuntimeException("El servicio no pudo procesar la transacción.");
        }
    }

    @Override
    public List<Object[]> cargarResumenVentasParaAdmin() {
        String sql = "SELECT u.nombres || ' ' || u.apellidos AS cliente, z.nombre AS zona, "
                + "v.monto_neto, c.nombre AS concierto "
                + "FROM ventas v "
                + "JOIN usuarios u ON u.dni = v.dni_cliente "
                + "JOIN zonas z ON z.id = v.id_zona "
                + "JOIN conciertos c ON c.id = v.id_concierto "
                + "WHERE v.estado = 'PAID' "
                + "ORDER BY v.fecha_hora";
        List<Object[]> filas = new ArrayList<>();
        try (Connection con = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setQueryTimeout(TIMEOUT_SEGUNDOS);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    filas.add(new Object[]{
                        rs.getString("cliente"),
                        rs.getString("zona"),
                        rs.getInt("monto_neto"),
                        rs.getString("concierto")
                    });
                }
            }
            return filas;
        } catch (SQLException e) {
            RegistradorErrores.registrar("OracleVentaRepository.cargarResumenVentasParaAdmin", e);
            throw new RuntimeException("El servicio no pudo procesar la transacción.");
        }
    }

    private static Zona buscarZona(List<Concierto> conciertos, String idConcierto, String idZona) {
        for (Concierto c : conciertos) {
            if (c.getId().toString().equals(idConcierto)) {
                for (Zona z : c.getZonas()) {
                    if (z.getId().toString().equals(idZona)) return z;
                }
            }
        }
        return null;
    }

    private static Map<Integer, Entrada> indexarEntradasPorNumero(Zona zona) {
        Map<Integer, Entrada> indice = new java.util.HashMap<>();
        for (Entrada e : zona.getEntradas()) {
            indice.put(e.getNumero(), e);
        }
        return indice;
    }

    private static String buscarNombreConcierto(List<Concierto> conciertos, String idConcierto) {
        for (Concierto c : conciertos) {
            if (c.getId().toString().equals(idConcierto)) return c.getNombre();
        }
        return "Evento";
    }

    // Acumulador mutable interno mientras se recorre el ResultSet del JOIN; una vez completo se
    // convierte a un Venta inmutable real vía Venta.reconstruirDesdeBaseDeDatos(...).
    private static final class VentaEnConstruccion {
        UUID id;
        java.util.Date fecha;
        int monto;
        int puntosRedimidos;
        int puntosGanados;
        Venta.EstadoVenta estado;
        String paymentTransactionId;
        String conciertoNombre;
        Zona zona;
        List<Entrada> entradas = new ArrayList<>();
    }
}
