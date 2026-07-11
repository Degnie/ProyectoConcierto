package repositorio;

import conexion.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import modelo.Concierto;
import modelo.Zona;
import util.RegistradorErrores;

public class OracleConciertoRepository implements ConciertoRepository {
    private static final int TIMEOUT_SEGUNDOS = 10;

    @Override
    public boolean save(Concierto concierto) {
        if (concierto == null) return false;
        String upsertConcierto = "MERGE INTO conciertos c USING (SELECT ? id FROM dual) src ON (c.id = src.id) "
                + "WHEN MATCHED THEN UPDATE SET nombre = ?, fecha_ms = ? "
                + "WHEN NOT MATCHED THEN INSERT (id, nombre, fecha_ms) VALUES (?, ?, ?)";
        // Las zonas se actualizan por su propio id, nunca se borran y recrean: entradas y ventas ya
        // persistidas las referencian como foreign key (id_zona), y recrearlas con un id nuevo
        // rompería esa relación (quedarían huérfanas o el conteo de disponibles se desincronizaría).
        String upsertZona = "MERGE INTO zonas z USING (SELECT ? id FROM dual) src ON (z.id = src.id) "
                + "WHEN MATCHED THEN UPDATE SET nombre = ?, capacidad = ?, precio = ? "
                + "WHEN NOT MATCHED THEN INSERT (id, concierto_id, nombre, capacidad, precio, version) VALUES (?, ?, ?, ?, ?, 0)";

        try (Connection con = DatabaseConnection.getInstance().getConnection()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement ps = con.prepareStatement(upsertConcierto)) {
                    ps.setQueryTimeout(TIMEOUT_SEGUNDOS);
                    String id = concierto.getId().toString();
                    ps.setString(1, id);
                    ps.setString(2, concierto.getNombre());
                    ps.setLong(3, concierto.getFecha().getTime());
                    ps.setString(4, id);
                    ps.setString(5, concierto.getNombre());
                    ps.setLong(6, concierto.getFecha().getTime());
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = con.prepareStatement(upsertZona)) {
                    ps.setQueryTimeout(TIMEOUT_SEGUNDOS);
                    for (Zona zona : concierto.getZonas()) {
                        String idZona = zona.getId().toString();
                        ps.setString(1, idZona);
                        ps.setString(2, zona.getNombre());
                        ps.setInt(3, zona.getCapacidad());
                        ps.setInt(4, zona.getPrecio());
                        ps.setString(5, idZona);
                        ps.setString(6, concierto.getId().toString());
                        ps.setString(7, zona.getNombre());
                        ps.setInt(8, zona.getCapacidad());
                        ps.setInt(9, zona.getPrecio());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                con.commit();
                return true;
            } catch (SQLException e) {
                con.rollback();
                RegistradorErrores.registrar("OracleConciertoRepository.save", e);
                throw new RuntimeException("El servicio no pudo procesar la transacción.");
            }
        } catch (SQLException e) {
            RegistradorErrores.registrar("OracleConciertoRepository.save", e);
            throw new RuntimeException("El servicio no pudo procesar la transacción.");
        }
    }

    @Override
    public Concierto findById(UUID id) {
        for (Concierto c : findAll()) {
            if (c.getId().equals(id)) return c;
        }
        return null;
    }

    @Override
    public Concierto findByNombre(String nombre) {
        for (Concierto c : findAll()) {
            if (c.getNombre().equalsIgnoreCase(nombre)) return c;
        }
        return null;
    }

    @Override
    public List<Concierto> findAll() {
        // Orden alfabético por nombre y, dentro del mismo tour, por fecha: con varias fechas de un
        // mismo artista, aparecen en orden cronológico entre sí en vez de en orden arbitrario.
        String sqlConciertos = "SELECT id, nombre, fecha_ms FROM conciertos ORDER BY nombre, fecha_ms";
        String sqlZonas = "SELECT id, nombre, capacidad, precio FROM zonas WHERE concierto_id = ?";
        // Solo se persisten las entradas realmente vendidas (ver schema.sql); reconstruir la zona
        // significa generar el set completo de entradas disponibles y marcar como vendidas
        // exactamente las que aparecen acá, para que el estado sobreviva a un reinicio de la app.
        String sqlEntradasVendidas = "SELECT id_entrada, numero FROM entradas WHERE id_zona = ? AND estado = 'SOLD'";
        List<Concierto> resultado = new ArrayList<>();
        try (Connection con = DatabaseConnection.getInstance().getConnection();
             PreparedStatement psConciertos = con.prepareStatement(sqlConciertos)) {
            psConciertos.setQueryTimeout(TIMEOUT_SEGUNDOS);
            try (ResultSet rsConciertos = psConciertos.executeQuery()) {
                while (rsConciertos.next()) {
                    Concierto concierto = new Concierto(UUID.fromString(rsConciertos.getString("id")),
                            rsConciertos.getString("nombre"), new Date(rsConciertos.getLong("fecha_ms")));
                    try (PreparedStatement psZonas = con.prepareStatement(sqlZonas)) {
                        psZonas.setQueryTimeout(TIMEOUT_SEGUNDOS);
                        psZonas.setString(1, rsConciertos.getString("id"));
                        try (ResultSet rsZonas = psZonas.executeQuery()) {
                            while (rsZonas.next()) {
                                UUID idZona = UUID.fromString(rsZonas.getString("id"));
                                Zona zona = new Zona(idZona, rsZonas.getString("nombre"),
                                        rsZonas.getInt("capacidad"), rsZonas.getInt("precio"));
                                zona.generarEntradas();
                                try (PreparedStatement psEntradas = con.prepareStatement(sqlEntradasVendidas)) {
                                    psEntradas.setQueryTimeout(TIMEOUT_SEGUNDOS);
                                    psEntradas.setString(1, idZona.toString());
                                    try (ResultSet rsEntradas = psEntradas.executeQuery()) {
                                        while (rsEntradas.next()) {
                                            zona.marcarEntradaVendida(rsEntradas.getInt("numero"),
                                                    UUID.fromString(rsEntradas.getString("id_entrada")));
                                        }
                                    }
                                }
                                concierto.agregarZonaExistente(zona);
                            }
                        }
                    }
                    resultado.add(concierto);
                }
            }
            return resultado;
        } catch (SQLException e) {
            RegistradorErrores.registrar("OracleConciertoRepository.findAll", e);
            throw new RuntimeException("El servicio no pudo procesar la transacción.");
        }
    }

    @Override
    public boolean delete(UUID id) {
        String sql = "DELETE FROM conciertos WHERE id = ?";
        try (Connection con = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setQueryTimeout(TIMEOUT_SEGUNDOS);
            ps.setString(1, id.toString());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            RegistradorErrores.registrar("OracleConciertoRepository.delete", e);
            throw new RuntimeException("El servicio no pudo procesar la transacción.");
        }
    }
}
