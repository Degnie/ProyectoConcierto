package vista;

import java.awt.CardLayout;
import java.awt.Component;
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

    public void mostrarLogin() {
        cardLayout.show(panelContenedor, CARD_LOGIN);
    }

    public void mostrarRegistro() {
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
}
