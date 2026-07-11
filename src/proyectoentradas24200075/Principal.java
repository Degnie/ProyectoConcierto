package proyectoentradas24200075;

import conexion.ConfiguracionApp;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;
import javax.swing.JOptionPane;
import modelo.Concierto;
import vista.FrmPrincipal;
import controlador.ControladorLogin;
import controlador.ControladorRegistro;
import repositorio.ClienteRepository;
import repositorio.ConciertoRepository;
import repositorio.UsuarioRepository;
import repositorio.VentaRepository;
import repositorio.OracleClienteRepository;
import repositorio.OracleConciertoRepository;
import repositorio.OracleUsuarioRepository;
import repositorio.OracleVentaRepository;

public class Principal {

    private static final String[] CLAVES_CRITICAS = {
        "db.url", "db.user", "db.password",
        "mail.smtp.host", "mail.smtp.port", "mail.smtp.user", "mail.smtp.password"
    };

    // Repositorios contra Oracle. Las credenciales de conexión se leen de config.properties
    // (ver config.properties.example y schema.sql en la raíz del proyecto).
    public static ClienteRepository clienteRepository = new OracleClienteRepository();
    public static ConciertoRepository conciertoRepository = new OracleConciertoRepository();
    public static UsuarioRepository usuarioRepository = new OracleUsuarioRepository();
    public static VentaRepository ventaRepository = new OracleVentaRepository();

    public static void main(String[] args) {
        // Fail-fast: si falta config.properties o alguna clave crítica, se avisa y se aborta acá
        // mismo, antes de intentar levantar la UI o abrir una conexión JDBC que fallaría más
        // adelante con un stacktrace menos claro para quien esté instalando el sistema.
        String errorConfig = validarConfiguracion();
        if (errorConfig != null) {
            JOptionPane.showMessageDialog(null, errorConfig, "Configuración incompleta", JOptionPane.ERROR_MESSAGE);
            return;
        }

        ArrayList<Concierto> listaConciertos = new ArrayList<>(conciertoRepository.findAll());

        FrmPrincipal principal = new FrmPrincipal();
        new ControladorRegistro(principal, clienteRepository);
        new ControladorLogin(principal, clienteRepository, usuarioRepository, listaConciertos, conciertoRepository, ventaRepository);

        principal.mostrarLogin();
        principal.setVisible(true);
    }

    // Devuelve un mensaje describiendo el problema, o null si la configuración está completa.
    private static String validarConfiguracion() {
        File archivoConfig = ConfiguracionApp.resolverArchivo("config.properties");
        if (!archivoConfig.exists()) {
            return "No se encontró config.properties junto al programa.\n"
                    + "Copie config.properties.example, complete sus datos y vuelva a intentarlo.\n"
                    + "Ruta esperada: " + archivoConfig.getAbsolutePath();
        }

        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(archivoConfig)) {
            props.load(in);
        } catch (IOException ex) {
            return "No se pudo leer config.properties: " + ex.getMessage();
        }

        StringBuilder faltantes = new StringBuilder();
        for (String clave : CLAVES_CRITICAS) {
            String valor = props.getProperty(clave);
            if (valor == null || valor.isBlank()) {
                if (faltantes.length() > 0) faltantes.append(", ");
                faltantes.append(clave);
            }
        }
        if (faltantes.length() > 0) {
            return "config.properties está incompleto. Faltan las siguientes claves:\n" + faltantes
                    + "\n\nRevise config.properties.example para ver el formato esperado.";
        }
        return null;
    }
}
