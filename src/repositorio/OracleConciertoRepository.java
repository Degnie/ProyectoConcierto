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

public class OracleConciertoRepository implements ConciertoRepository {

    @Override
    public boolean save(Concierto concierto) {
        if (concierto == null) return false;
        String upsertConcierto = "MERGE INTO conciertos c USING (SELECT ? id FROM dual) src ON (c.id = src.id) "
                + "WHEN MATCHED THEN UPDATE SET nombre = ?, fecha_ms = ? "
                + "WHEN NOT MATCHED THEN INSERT (id, nombre, fecha_ms) VALUES (?, ?, ?)";
        String borrarZonas = "DELETE FROM zonas WHERE concierto_id = ?";
        String insertarZona = "INSERT INTO zonas (id, concierto_id, nombre, capacidad, precio) VALUES (?, ?, ?, ?, ?)";

        try (Connection con = DatabaseConnection.getInstance().getConnection()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement ps = con.prepareStatement(upsertConcierto)) {
                    String id = concierto.getId().toString();
                    ps.setString(1, id);
                    ps.setString(2, concierto.getNombre());
                    ps.setLong(3, concierto.getFecha().getTime());
                    ps.setString(4, id);
                    ps.setString(5, concierto.getNombre());
                    ps.setLong(6, concierto.getFecha().getTime());
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = con.prepareStatement(borrarZonas)) {
                    ps.setString(1, concierto.getId().toString());
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = con.prepareStatement(insertarZona)) {
                    for (modelo.Zona zona : concierto.getZonas()) {
                        ps.setString(1, UUID.randomUUID().toString());
                        ps.setString(2, concierto.getId().toString());
                        ps.setString(3, zona.getNombre());
                        ps.setInt(4, zona.getCapacidad());
                        ps.setInt(5, zona.getPrecio());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                con.commit();
                return true;
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al guardar concierto en la base de datos", e);
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
        String sqlConciertos = "SELECT id, nombre, fecha_ms FROM conciertos";
        String sqlZonas = "SELECT nombre, capacidad, precio FROM zonas WHERE concierto_id = ?";
        List<Concierto> resultado = new ArrayList<>();
        try (Connection con = DatabaseConnection.getInstance().getConnection();
             PreparedStatement psConciertos = con.prepareStatement(sqlConciertos);
             ResultSet rsConciertos = psConciertos.executeQuery()) {
            while (rsConciertos.next()) {
                Concierto concierto = new Concierto(rsConciertos.getString("nombre"),
                        new Date(rsConciertos.getLong("fecha_ms")));
                try (PreparedStatement psZonas = con.prepareStatement(sqlZonas)) {
                    psZonas.setString(1, rsConciertos.getString("id"));
                    try (ResultSet rsZonas = psZonas.executeQuery()) {
                        while (rsZonas.next()) {
                            concierto.agregarZona(rsZonas.getString("nombre"),
                                    rsZonas.getInt("capacidad"), rsZonas.getInt("precio"));
                        }
                    }
                }
                resultado.add(concierto);
            }
            return resultado;
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar conciertos de la base de datos", e);
        }
    }

    @Override
    public boolean delete(UUID id) {
        String sql = "DELETE FROM conciertos WHERE id = ?";
        try (Connection con = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, id.toString());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error al eliminar concierto de la base de datos", e);
        }
    }
}
