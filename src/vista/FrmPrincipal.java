package vista;

import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

// Contenedor único (SPA): la navegación entre pantallas cambia de "card" en vez de
// destruir (dispose()) y crear una ventana nueva por cada pantalla.
public class FrmPrincipal extends JFrame {
    public static final String CARD_LOGIN = "LOGIN";
    public static final String CARD_REGISTRO = "REGISTRO";
    public static final String CARD_CLIENTE = "CLIENTE";
    public static final String CARD_ADMIN = "ADMIN";

    private final CardLayout cardLayout;
    private final JPanel panelContenedor;
    private final FrmLogin vistaLogin;
    private final FrmRegistroCliente vistaRegistro;

    public FrmPrincipal() {
        setTitle("Sistema de Entradas");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        cardLayout = new CardLayout();
        panelContenedor = new JPanel(cardLayout);
        setContentPane(panelContenedor);

        vistaLogin = new FrmLogin();
        vistaRegistro = new FrmRegistroCliente();
        agregarCard(CARD_LOGIN, vistaLogin);
        agregarCard(CARD_REGISTRO, vistaRegistro);

        pack();
        setLocationRelativeTo(null);
    }

    public FrmLogin getVistaLogin() {
        return vistaLogin;
    }

    public FrmRegistroCliente getVistaRegistro() {
        return vistaRegistro;
    }

    // Evita la retención "fantasma" de datos entre sesiones: cada vez que se vuelve a login o
    // registro se limpian campos y etiquetas de error antes de mostrar la pantalla.
    public void mostrarLogin() {
        vistaLogin.limpiarFormulario();
        cardLayout.show(panelContenedor, CARD_LOGIN);
    }

    public void mostrarRegistro() {
        vistaRegistro.limpiarCampos();
        cardLayout.show(panelContenedor, CARD_REGISTRO);
    }

    // FrmCliente/FrmAdministrador se recrean en cada login (llevan estado de sesión),
    // así que reemplazamos la card anterior por la nueva antes de mostrarla.
    public void mostrarCliente(FrmCliente panel) {
        reemplazarCard(CARD_CLIENTE, panel);
    }

    public void mostrarAdministrador(FrmAdministrador panel) {
        reemplazarCard(CARD_ADMIN, panel);
    }

    private void reemplazarCard(String nombre, JPanel panel) {
        for (Component c : panelContenedor.getComponents()) {
            if (nombre.equals(c.getName())) {
                panelContenedor.remove(c);
                break;
            }
        }
        agregarCard(nombre, panel);
        cardLayout.show(panelContenedor, nombre);
    }

    private void agregarCard(String nombre, JPanel panel) {
        panel.setName(nombre);
        panelContenedor.add(panel, nombre);
    }

    // Indicador de carga global: se invoca al arrancar cualquier SwingWorker de red/BD para que
    // el usuario tenga feedback visual inmediato mientras la operación corre en background.
    public void iniciarCarga() {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        habilitarComponentes(panelContenedor, false);
    }

    public void finalizarCarga() {
        setCursor(Cursor.getDefaultCursor());
        habilitarComponentes(panelContenedor, true);
    }

    // JPanel.setEnabled() no deshabilita a sus hijos automáticamente en Swing, así que hay que
    // recorrer el árbol de componentes a mano.
    private static void habilitarComponentes(Container contenedor, boolean habilitado) {
        for (Component c : contenedor.getComponents()) {
            c.setEnabled(habilitado);
            if (c instanceof Container) {
                habilitarComponentes((Container) c, habilitado);
            }
        }
    }
}
