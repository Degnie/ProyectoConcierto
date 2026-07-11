package proyectoentradas24200075;

import java.util.ArrayList;
import modelo.Concierto;
import vista.FrmPrincipal;
import controlador.ControladorLogin;
import controlador.ControladorRegistro;
import repositorio.ClienteRepository;
import repositorio.ConciertoRepository;
import repositorio.UsuarioRepository;
import repositorio.OracleClienteRepository;
import repositorio.OracleConciertoRepository;
import repositorio.OracleUsuarioRepository;

public class Principal {

    // Repositorios contra Oracle. Las credenciales de conexión se leen de config.properties
    // (ver config.properties.example y schema.sql en la raíz del proyecto).
    public static ClienteRepository clienteRepository = new OracleClienteRepository();
    public static ConciertoRepository conciertoRepository = new OracleConciertoRepository();
    public static UsuarioRepository usuarioRepository = new OracleUsuarioRepository();

    public static void main(String[] args) {
        ArrayList<Concierto> listaConciertos = new ArrayList<>(conciertoRepository.findAll());

        FrmPrincipal principal = new FrmPrincipal();
        new ControladorRegistro(principal, clienteRepository);
        new ControladorLogin(principal, clienteRepository, usuarioRepository, listaConciertos, conciertoRepository);

        principal.mostrarLogin();
        principal.setVisible(true);
    }
}
