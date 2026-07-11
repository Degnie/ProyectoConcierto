package conexion;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Singleton: único punto de acceso a la conexión Oracle. Los repositorios (Oracle*Repository)
 * son los únicos que deben usar esta clase; los controladores no conocen JDBC ni la ubicación
 * física de config.properties.
 */
public final class DatabaseConnection {
    private static final String NOMBRE_ARCHIVO_CONFIG = "config.properties";
    private static DatabaseConnection instancia;

    private final String url;
    private final String usuario;
    private final String password;

    private DatabaseConnection() {
        // La ruta ya no es relativa al directorio de trabajo del proceso: se resuelve junto al
        // .jar en ejecución (ver ConfiguracionApp), para que el despliegue sea portable.
        File archivoConfig = ConfiguracionApp.resolverArchivo(NOMBRE_ARCHIVO_CONFIG);

        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(archivoConfig)) {
            props.load(in);
        } catch (IOException e) {
            throw new IllegalStateException(
                "No se pudo leer " + archivoConfig.getAbsolutePath()
                    + ". Copie config.properties.example junto al .jar (o a la raíz del proyecto en modo desarrollo) y complete sus credenciales de Oracle.",
                e);
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

    // Cada repositorio pide una conexión nueva y la cierra en su propio try-with-resources.
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, usuario, password);
    }
}
