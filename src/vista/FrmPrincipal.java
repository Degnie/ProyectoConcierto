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

    // Referencia explícita a la card de sesión actualmente montada (cliente o admin). Se recrea
    // en cada login, así que hay que soltar la anterior a mano antes de montar la nueva: de lo
    // contrario el panel viejo (y su controlador, y los listeners que registró) queda huérfano
    // pero alcanzable desde el árbol de Swing, y nunca se libera.
    private JPanel cardClienteActual;
    private JPanel cardAdminActual;

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
        cardClienteActual = reemplazarCard(CARD_CLIENTE, cardClienteActual, panel);
    }

    public void mostrarAdministrador(FrmAdministrador panel) {
        cardAdminActual = reemplazarCard(CARD_ADMIN, cardAdminActual, panel);
    }

    // Elimina explícitamente el panel anterior (si había uno) del contenedor antes de acoplar el
    // nuevo, en vez de barrer panelContenedor.getComponents() buscando por nombre: la referencia
    // directa es inequívoca y no depende de que ningún otro componente comparta el mismo name().
    private JPanel reemplazarCard(String nombre, JPanel anterior, JPanel nuevo) {
        if (anterior != null) {
            panelContenedor.remove(anterior);
        }
        agregarCard(nombre, nuevo);
        cardLayout.show(panelContenedor, nombre);
        return nuevo;
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
