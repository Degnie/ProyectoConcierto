package controlador;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import modelo.Cliente;
import modelo.Concierto;
import modelo.CredencialAdmin;
import modelo.Persona;
import repositorio.ClienteRepository;
import repositorio.ConciertoRepository;
import repositorio.UsuarioRepository;
import vista.FrmAdministrador;
import vista.FrmCliente;
import vista.FrmLogin;
import vista.FrmPrincipal;

public class ControladorLogin implements ActionListener {

    private final FrmPrincipal principal;
    private final FrmLogin vista;
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final ArrayList<Concierto> listaConciertos;
    private final ConciertoRepository conciertoRepository;

    public ControladorLogin(FrmPrincipal principal, ClienteRepository clienteRepository, UsuarioRepository usuarioRepository,
                             ArrayList<Concierto> listaConciertos, ConciertoRepository conciertoRepository) {
        this.principal = principal;
        this.vista = principal.getVistaLogin();
        this.clienteRepository = clienteRepository;
        this.usuarioRepository = usuarioRepository;
        this.listaConciertos = listaConciertos;
        this.conciertoRepository = conciertoRepository;

        this.vista.getBtnLoginCliente().addActionListener(this);
        this.vista.getBtnLoginAdmin().addActionListener(this);
        this.vista.getBtnRegistrarse().addActionListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String dni = vista.getDni();
        char[] contrasena = vista.getContrasenaChars();

        if (e.getSource() == vista.getBtnLoginCliente()) {
            if (dni.isEmpty() || contrasena.length == 0) {
                Arrays.fill(contrasena, '0');
                JOptionPane.showMessageDialog(vista, "Por favor, complete todos los campos.");
                return;
            }
            loginCliente(dni, contrasena);
        } else if (e.getSource() == vista.getBtnLoginAdmin()) {
            if (dni.isEmpty() || contrasena.length == 0) {
                Arrays.fill(contrasena, '0');
                JOptionPane.showMessageDialog(vista, "Por favor, complete todos los campos.");
                return;
            }
            loginAdmin(dni, contrasena);
        } else if (e.getSource() == vista.getBtnRegistrarse()) {
            Arrays.fill(contrasena, '0');
            principal.mostrarRegistro();
        }
    }

    // La consulta a la BD y el hashing corren fuera del Event Dispatch Thread para no congelar la UI
    private void loginCliente(String dni, char[] contrasena) {
        setControlesHabilitados(false);
        principal.iniciarCarga();
        new SwingWorker<Cliente, Void>() {
            @Override
            protected Cliente doInBackground() {
                Cliente encontrado = clienteRepository.findByDni(dni);
                if (encontrado == null) {
                    Arrays.fill(contrasena, '0');
                    return null;
                }
                return encontrado.ingresar(dni, contrasena) ? encontrado : null;
            }

            @Override
            protected void done() {
                principal.finalizarCarga();
                setControlesHabilitados(true);
                vista.limpiarContrasena();
                Cliente clienteEncontrado = get_();
                if (clienteEncontrado != null) {
                    JOptionPane.showMessageDialog(vista, "¡Bienvenido " + clienteEncontrado.getNombres() + "!");
                    FrmCliente frmCliente = new FrmCliente();
                    new ControladorCliente(principal, frmCliente, clienteEncontrado, clienteRepository, listaConciertos, conciertoRepository);
                    principal.mostrarCliente(frmCliente);
                } else {
                    JOptionPane.showMessageDialog(vista, "DNI o contraseña incorrectos para Cliente.");
                }
            }

            private Cliente get_() {
                try {
                    return get();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(vista, "Error al validar credenciales: " + ex.getMessage());
                    return null;
                }
            }
        }.execute();
    }

    private void loginAdmin(String dni, char[] contrasena) {
        setControlesHabilitados(false);
        principal.iniciarCarga();
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                CredencialAdmin credencial = usuarioRepository.buscarAdminPorDni(dni);
                if (credencial == null) {
                    Arrays.fill(contrasena, '0');
                    return false;
                }
                // Purga inmediata tras hashear, dentro del mismo hilo de background: la
                // contraseña en claro no debe sobrevivir hasta el done() en el EDT.
                String hashIngresado = Persona.hashPassword(contrasena, credencial.getSalt());
                Arrays.fill(contrasena, '0');
                return hashIngresado.equals(credencial.getContrasenaHash());
            }

            @Override
            protected void done() {
                principal.finalizarCarga();
                setControlesHabilitados(true);
                vista.limpiarContrasena();
                boolean autenticado = get_();
                if (autenticado) {
                    JOptionPane.showMessageDialog(vista, "Acceso concedido como Administrador.");
                    FrmAdministrador frmAdmin = new FrmAdministrador();
                    new ControladorAdministrador(principal, frmAdmin, clienteRepository, listaConciertos, conciertoRepository);
                    principal.mostrarAdministrador(frmAdmin);
                } else {
                    JOptionPane.showMessageDialog(vista, "Credenciales de Administrador inválidas.");
                }
            }

            private boolean get_() {
                try {
                    return get();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(vista, "Error al validar credenciales: " + ex.getMessage());
                    return false;
                }
            }
        }.execute();
    }

    private void setControlesHabilitados(boolean habilitados) {
        vista.getBtnLoginCliente().setEnabled(habilitados);
        vista.getBtnLoginAdmin().setEnabled(habilitados);
    }
}
