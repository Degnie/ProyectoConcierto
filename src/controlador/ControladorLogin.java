package controlador;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JOptionPane;
import modelo.Cliente;
import modelo.ClienteArreglo;
import vista.FrmAdministrador;
import vista.FrmCliente;
import vista.FrmLogin;
import vista.FrmRegistroCliente;

public class ControladorLogin implements ActionListener {
    
    // Instancias de la Vista y los Almacenes de Datos (Modelo)
    private FrmLogin vista;
    private ClienteArreglo modeloClientes;
    
    // El constructor recibe la vista para controlarla y el almacén de datos existente
    public ControladorLogin(FrmLogin vista, ClienteArreglo modeloClientes) {
        this.vista = vista;
        this.modeloClientes = modeloClientes;
        
        // Ponemos al controlador a "escuchar" cada botón de la pantalla de Login
        this.vista.getBtnLoginCliente().addActionListener(this);
        this.vista.getBtnLoginAdmin().addActionListener(this);
        this.vista.getBtnRegistrarse().addActionListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // CAPTURA DE DATOS: Obtenemos lo que el usuario escribió mediante los Getters de la vista
        String dni = vista.getDni();
        String contrasena = vista.getContrasena();

        // CASO 1: Hizo clic en "Iniciar Sesión Cliente"
        if (e.getSource() == vista.getBtnLoginCliente()) {
            if (dni.isEmpty() || contrasena.isEmpty()) {
                JOptionPane.showMessageDialog(vista, "Por favor, complete todos los campos.");
                return;
            }
            
            // Buscamos al cliente en el repositorio profesional
            Cliente clienteEncontrado = proyectoentradas24200075.Principal.clienteRepository.findByDni(dni);
            
            // Validamos las credenciales usando el método interno del modelo Cliente (que ya realiza hash SHA-256)
            if (clienteEncontrado != null && clienteEncontrado.ingresar(dni, contrasena)) {
                JOptionPane.showMessageDialog(vista, "¡Bienvenido " + clienteEncontrado.getNombres() + "!");
                
                FrmCliente frmCliente = new FrmCliente();
                
                // CONEXIÓN VITAL: Conectamos la vista con su respectivo controlador de cliente
                new ControladorCliente(frmCliente, clienteEncontrado, this.modeloClientes, proyectoentradas24200075.Principal.listaConciertos);
                
                frmCliente.setVisible(true);
                vista.dispose(); // Cerramos el login
            } else {
                JOptionPane.showMessageDialog(vista, "DNI o contraseña incorrectos para Cliente.");
            }
        }
        
        // CASO 2: Hizo clic en "Iniciar Sesión Administrador"
        else if (e.getSource() == vista.getBtnLoginAdmin()) {
            if (dni.isEmpty() || contrasena.isEmpty()) {
                JOptionPane.showMessageDialog(vista, "Por favor, complete todos los campos.");
                return;
            }
            
            // Validación directa en base a un usuario Administrador por defecto
            if (dni.equals("admin") && contrasena.equals("1234")) {
                JOptionPane.showMessageDialog(vista, "Acceso concedido como Administrador.");
                
                FrmAdministrador frmAdmin = new FrmAdministrador();
                
                // CONEXIÓN VITAL: Instanciamos el ControladorAdministrador pasándole la vista y la lista global
                new ControladorAdministrador(frmAdmin, this.modeloClientes, proyectoentradas24200075.Principal.listaConciertos);
                
                frmAdmin.setVisible(true);
                vista.dispose();
            } else {
                JOptionPane.showMessageDialog(vista, "Credenciales de Administrador inválidas.");
            }
        }
        
        // CASO 3: Hizo clic en el botón "Registrarse"
        else if (e.getSource() == vista.getBtnRegistrarse()) {
            FrmRegistroCliente frmRegistro = new FrmRegistroCliente();
            ControladorRegistro controladorRegistro = new ControladorRegistro(frmRegistro, modeloClientes);
            frmRegistro.setVisible(true);
            vista.dispose(); 
        }
    }
}


