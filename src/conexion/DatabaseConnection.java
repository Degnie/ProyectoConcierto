package conexion;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

// Singleton: unico punto de acceso a la conexion Oracle. Los repositorios (Oracle*Repository)
// son los unicos que deben usar esta clase; los controladores no conocen JDBC.
public final class DatabaseConnection {
    private static final String RUTA_CONFIG = "config.properties";
    private static DatabaseConnection instancia;

    private final String url;
    private final String usuario;
    private final String password;

    private DatabaseConnection() {
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(RUTA_CONFIG)) {
            props.load(in);
        } catch (IOException e) {
            throw new IllegalStateException(
                "No se pudo leer " + RUTA_CONFIG + ". Copie config.properties.example y complete sus credenciales de Oracle.", e);
        }
        this.url = props.getProperty("db.url");
        this.usuario = props.getProperty("db.user");
        this.password = props.getProperty("db.password");
        try {
            Class.forName("oracle.jdbc.OracleDriver");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Driver ojdbc.jar no encontrado en el classpath.", e);
        }
    }

    public static synchronized DatabaseConnection getInstance() {
        if (instancia == null) {
            instancia = new DatabaseConnection();
        }
        return instancia;
    }

    // Cada repositorio pide una conexion nueva y la cierra en su propio try-with-resources.
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, usuario, password);
    }
}
