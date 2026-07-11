package repositorio;

import conexion.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import modelo.CredencialAdmin;

public class OracleUsuarioRepository implements UsuarioRepository {
    private static final int TIMEOUT_SEGUNDOS = 10;

    @Override
    public CredencialAdmin buscarAdminPorDni(String dni) {
        String sql = "SELECT dni, contrasena_hash, salt, nombres FROM usuarios WHERE dni = ? AND rol = 'ADMIN'";
        try (Connection con = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setQueryTimeout(TIMEOUT_SEGUNDOS);
            ps.setString(1, dni);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new CredencialAdmin(rs.getString("dni"), rs.getString("contrasena_hash"),
                            rs.getString("salt"), rs.getString("nombres"));
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar administrador en la base de datos", e);
        }
    }
}
