package repositorio;

import conexion.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import modelo.Cliente;

public class OracleClienteRepository implements ClienteRepository {

    @Override
    public boolean save(Cliente cliente) {
        if (cliente == null) return false;
        // La cláusula WHERE tras el UPDATE SET es la guarda de seguridad: si el DNI ya existe pero
        // pertenece a un ADMIN, la fila queda fuera tanto del UPDATE (falla el WHERE) como del
        // INSERT (ya hizo match por DNI), así que no se toca y executeUpdate() devuelve 0 filas.
        // Sin esto, un registro de cliente con el DNI de un admin sobrescribiría su hash/salt.
        String sql = "MERGE INTO usuarios u USING (SELECT ? dni FROM dual) src ON (u.dni = src.dni) "
                + "WHEN MATCHED THEN UPDATE SET nombres = ?, apellidos = ?, correo = ?, "
                + "contrasena_hash = ?, salt = ?, puntos = ?, fecha_nacimiento = ? "
                + "WHERE u.rol = 'CLIENTE' "
                + "WHEN NOT MATCHED THEN INSERT (dni, nombres, apellidos, correo, contrasena_hash, salt, rol, puntos, fecha_nacimiento) "
                + "VALUES (?, ?, ?, ?, ?, ?, 'CLIENTE', ?, ?)";
        try (Connection con = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, cliente.getDni());
            ps.setString(2, cliente.getNombres());
            ps.setString(3, cliente.getApellidos());
            ps.setString(4, cliente.getCorreo());
            ps.setString(5, cliente.getContrasenaHash());
            ps.setString(6, cliente.getSalt());
            ps.setInt(7, cliente.getPuntos());
            ps.setDate(8, java.sql.Date.valueOf(cliente.getFechaNacimiento()));
            ps.setString(9, cliente.getDni());
            ps.setString(10, cliente.getNombres());
            ps.setString(11, cliente.getApellidos());
            ps.setString(12, cliente.getCorreo());
            ps.setString(13, cliente.getContrasenaHash());
            ps.setString(14, cliente.getSalt());
            ps.setInt(15, cliente.getPuntos());
            ps.setDate(16, java.sql.Date.valueOf(cliente.getFechaNacimiento()));
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error al guardar cliente en la base de datos", e);
        }
    }

    @Override
    public Cliente findByDni(String dni) {
        String sql = "SELECT dni, nombres, apellidos, correo, contrasena_hash, salt, puntos, fecha_nacimiento "
                + "FROM usuarios WHERE dni = ? AND rol = 'CLIENTE'";
        try (Connection con = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, dni);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar cliente en la base de datos", e);
        }
    }

    @Override
    public List<Cliente> findAll() {
        String sql = "SELECT dni, nombres, apellidos, correo, contrasena_hash, salt, puntos, fecha_nacimiento "
                + "FROM usuarios WHERE rol = 'CLIENTE'";
        List<Cliente> resultado = new ArrayList<>();
        try (Connection con = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                resultado.add(mapear(rs));
            }
            return resultado;
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar clientes de la base de datos", e);
        }
    }

    private Cliente mapear(ResultSet rs) throws SQLException {
        try {
            LocalDate fechaNacimiento = rs.getDate("fecha_nacimiento").toLocalDate();
            Cliente cliente = new Cliente(rs.getString("nombres"), rs.getString("apellidos"),
                    rs.getString("dni"), rs.getString("contrasena_hash"), rs.getString("salt"),
                    rs.getString("correo"), fechaNacimiento);
            cliente.setPuntos(rs.getInt("puntos"));
            return cliente;
        } catch (Exception ex) {
            // Los datos en Oracle ya pasaron esta validación al guardarse; si de todos modos
            // fallara (fila corrupta o migrada a mano), es un error de integridad, no de negocio.
            throw new RuntimeException("Registro de cliente inconsistente en la base de datos: dni="
                    + rs.getString("dni") + " - " + ex.getMessage(), ex);
        }
    }
}
