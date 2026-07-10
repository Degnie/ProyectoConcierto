package proyectoentradas24200075;

import java.util.ArrayList;
import modelo.ClienteArreglo;
import modelo.Concierto;
import modelo.ArchivoUsuarios;
import modelo.ArchivoConciertos;
import vista.FrmLogin;
import controlador.ControladorLogin;
import repositorio.ClienteRepository;
import repositorio.ConciertoRepository;
import repositorio.MemoriaClienteRepository;
import repositorio.MemoriaConciertoRepository;

public class Principal {
    
    // Lista global estática para que todos los controladores compartan los mismos conciertos
    public static ArrayList<Concierto> listaConciertos = new ArrayList<>();

    // Repositorios Globales Profesionales (Desacoplados y listos para inyección/BD)
    public static ClienteRepository clienteRepository = new MemoriaClienteRepository();
    public static ConciertoRepository conciertoRepository = new MemoriaConciertoRepository(listaConciertos);

    public static void main(String[] args) {
        // 1. Inicializamos el almacén en memoria para clientes (con capacidad de 100)
        ClienteArreglo modeloClientes = new ClienteArreglo(100);
        
        // 2. LOGICA DE PERSISTENCIA: Cargamos de los archivos .txt apenas abre el programa
        ArchivoUsuarios.cargarUsuarios(modeloClientes);
        ArchivoConciertos.cargarConciertos(listaConciertos);

        // COMODÍN DE INICIALIZACIÓN: Si el archivo de conciertos estaba vacío, 
        // creamos un concierto por defecto para que el combo no inicie en blanco.
        if (listaConciertos.isEmpty()) {
            try {
                java.util.Date fechaDefecto = new java.text.SimpleDateFormat("dd/MM/yyyy").parse("20/11/2026");
                Concierto inicial = new Concierto("Concierto Inicial Rock", fechaDefecto);
                listaConciertos.add(inicial);
                // Lo guardamos de inmediato en el archivo txt
                ArchivoConciertos.guardarConciertos(listaConciertos);
            } catch (Exception e) {
                System.out.println("Error al generar concierto por defecto.");
            }
        }

        // 3. PERSISTENCIA A REPOSITORIOS: Sincronizamos los datos a los repositorios profesionales
        for (Concierto c : new ArrayList<>(listaConciertos)) {
            conciertoRepository.save(c);
        }
        for (modelo.Cliente c : modeloClientes.getClientes()) {
            clienteRepository.save(c);
        }

        // 4. Instanciamos la interfaz gráfica inicial
        FrmLogin vistaLogin = new FrmLogin();
        
        // 5. Creamos el controlador del Login inyectándole los datos recuperados
        ControladorLogin controlador = new ControladorLogin(vistaLogin, modeloClientes);
        
        // 6. Hacemos visible el sistema
        vistaLogin.setVisible(true);
    }
}