/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JPanel.java to edit this template
 */
package vista;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FocusTraversalPolicy;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import util.IconRegistry;
import util.Tipografia;

/**
 *
 * @author lopez
 */
public class FrmLogin extends javax.swing.JPanel {

    private static final Color COLOR_ACENTO = new Color(0x0F, 0x62, 0xFE);
    private static final Color COLOR_BORDE_TARJETA = new Color(0xE0, 0xE0, 0xE0);

    /**
     * Creates new form FrmLogin
     */
    public FrmLogin() {
        initComponents();
        // JTextField/JPasswordField disparan su propio actionPerformed al presionar Enter;
        // lo reenviamos al botón principal para no obligar al mouse (accesibilidad por teclado).
        txtContrasena.addActionListener(ev -> btnLoginCliente.doClick());
        aplicarEstiloVisual();
    }

    // Todo lo que sigue corre después de initComponents() (regenerado por el Form Editor de
    // NetBeans; no se toca) para no romper la sincronía con FrmLogin.form: reestiliza, recentra
    // y refuerza la accesibilidad por teclado sobre los mismos componentes ya construidos.
    private void aplicarEstiloVisual() {
        // Tarjeta centralizada: jPanel1 (todo el formulario) se saca del GroupLayout original y
        // se recentra dentro de FrmLogin, con fondo blanco y borde sutil de 1px.
        remove(jPanel1);
        setLayout(new GridBagLayout());

        // jLabel1 sigue viviendo dentro de jPanel1Layout (generado, no se toca), que le fija un
        // ancho HARDCODEADO de 143px (medido en diseño para el tamaño de fuente original chico).
        // Escalar su fuente ahí mismo trunca el texto ("SISTE..."), porque GroupLayout usa ese
        // literal de 143px sin importar cuánto necesite el nuevo tamaño. Se oculta (el hueco que
        // deja en jPanel1Layout no se nota, jPanel1 no tiene grillas visibles) y se reemplaza por
        // un título libre, sin esa restricción, ubicado arriba de la tarjeta.
        jLabel1.setVisible(false);
        JLabel titulo = new JLabel(jLabel1.getText(), SwingConstants.CENTER);
        titulo.setFont(Tipografia.TITULO.deriveFont(22f));
        titulo.setIcon(IconRegistry.get(IconRegistry.LOCK, 22));
        titulo.setIconTextGap(8);

        GridBagConstraints gbcTitulo = new GridBagConstraints();
        gbcTitulo.gridy = 0;
        gbcTitulo.insets = new java.awt.Insets(0, 0, 16, 0);
        add(titulo, gbcTitulo);

        GridBagConstraints gbcTarjeta = new GridBagConstraints();
        gbcTarjeta.gridy = 1;
        add(jPanel1, gbcTarjeta);

        jPanel1.setOpaque(true);
        jPanel1.setBackground(Color.WHITE);
        jPanel1.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDE_TARJETA, 1),
                BorderFactory.createEmptyBorder(24, 24, 24, 24)));

        for (JLabel etiqueta : new JLabel[]{Dni, lblContrasena}) {
            etiqueta.setFont(Tipografia.CUERPO);
        }
        Dni.setLabelFor(txtDni);
        lblContrasena.setLabelFor(txtContrasena);
        txtDni.setFont(Tipografia.CUERPO);
        txtContrasena.setFont(Tipografia.CUERPO);
        txtDni.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Ej. 12345678");

        // CTA principal: fondo sólido de acento, texto en negrita, cursor de mano.
        btnLoginCliente.setFont(Tipografia.CUERPO.deriveFont(Font.BOLD));
        btnLoginCliente.setMnemonic('C');
        btnLoginCliente.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnLoginCliente.putClientProperty(FlatClientProperties.STYLE,
                "background:#0F62FE;foreground:#FFFFFF;"
                + "hoverBackground:#0353E9;pressedBackground:#0043CE;"
                + "focusedBackground:#0353E9;borderWidth:0;");

        // Secundario discreto: solo borde y texto gris, sin relleno, para no competir con el CTA.
        btnLoginAdmin.setFont(Tipografia.CUERPO);
        btnLoginAdmin.setMnemonic('A');
        btnLoginAdmin.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnLoginAdmin.putClientProperty(FlatClientProperties.STYLE,
                "background:$Panel.background;foreground:#8D8D8D;"
                + "borderColor:#8D8D8D;focusedBorderColor:#8D8D8D;hoverBorderColor:#8D8D8D;");

        btnRegistrarse.setFont(Tipografia.CUERPO);
        btnRegistrarse.setMnemonic('R');
        btnRegistrarse.setForeground(COLOR_ACENTO);
        btnRegistrarse.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnRegistrarse.putClientProperty(FlatClientProperties.BUTTON_TYPE,
                FlatClientProperties.BUTTON_TYPE_BORDERLESS);

        txtDni.getAccessibleContext().setAccessibleName("DNI");
        txtDni.getAccessibleContext().setAccessibleDescription(
                "Documento Nacional de Identidad del cliente o administrador");
        txtContrasena.getAccessibleContext().setAccessibleName("Contraseña");
        txtContrasena.getAccessibleContext().setAccessibleDescription(
                "Contraseña asociada al DNI ingresado");
        btnLoginCliente.getAccessibleContext().setAccessibleDescription(
                "Inicia sesión como cliente con el DNI y contraseña ingresados");
        btnLoginAdmin.getAccessibleContext().setAccessibleDescription(
                "Inicia sesión como administrador con el DNI y contraseña ingresados");

        // Orden de tabulación explícito y predecible, independiente del orden en que
        // GroupLayout haya colocado los componentes en el .form.
        setFocusCycleRoot(true);
        setFocusTraversalPolicyProvider(true);
        setFocusTraversalPolicy(new OrdenFoco(new Component[]{
            txtDni, txtContrasena, btnLoginCliente, btnLoginAdmin, btnRegistrarse
        }));
    }

    // Política de foco simple por orden fijo de un arreglo, con ciclo (el último vuelve al
    // primero). Evita depender del orden de inserción en el layout generado por el Form Editor.
    private static final class OrdenFoco extends FocusTraversalPolicy {
        private final Component[] orden;

        OrdenFoco(Component[] orden) {
            this.orden = orden;
        }

        private int indiceDe(Component c) {
            for (int i = 0; i < orden.length; i++) {
                if (orden[i] == c) return i;
            }
            return -1;
        }

        @Override
        public Component getComponentAfter(Container focusCycleRoot, Component aComponent) {
            int i = indiceDe(aComponent);
            return i < 0 ? orden[0] : orden[(i + 1) % orden.length];
        }

        @Override
        public Component getComponentBefore(Container focusCycleRoot, Component aComponent) {
            int i = indiceDe(aComponent);
            return i < 0 ? orden[orden.length - 1] : orden[(i - 1 + orden.length) % orden.length];
        }

        @Override
        public Component getFirstComponent(Container focusCycleRoot) {
            return orden[0];
        }

        @Override
        public Component getLastComponent(Container focusCycleRoot) {
            return orden[orden.length - 1];
        }

        @Override
        public Component getDefaultComponent(Container focusCycleRoot) {
            return orden[0];
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        Dni = new javax.swing.JLabel();
        lblContrasena = new javax.swing.JLabel();
        txtDni = new javax.swing.JTextField();
        btnLoginCliente = new javax.swing.JButton();
        btnLoginAdmin = new javax.swing.JButton();
        txtContrasena = new javax.swing.JPasswordField();
        btnRegistrarse = new javax.swing.JButton();

        jLabel1.setText("SISTEMAS DE ENTRADAS");

        Dni.setText("DNI");

        lblContrasena.setText("Clave");

        btnLoginCliente.setText("Login Cliente");
        btnLoginCliente.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnLoginClienteActionPerformed(evt);
            }
        });

        btnLoginAdmin.setText("Login Admin");

        btnRegistrarse.setText("¿No tienes cuenta? Regístrate aquí");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(25, 25, 25)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lblContrasena, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel1Layout.createSequentialGroup()
                                    .addGap(9, 9, 9)
                                    .addComponent(btnLoginCliente)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 96, Short.MAX_VALUE)
                                    .addComponent(btnLoginAdmin))
                                .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel1Layout.createSequentialGroup()
                                    .addComponent(Dni, javax.swing.GroupLayout.PREFERRED_SIZE, 49, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGap(18, 18, 18)
                                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addComponent(txtDni, javax.swing.GroupLayout.DEFAULT_SIZE, 237, Short.MAX_VALUE)
                                        .addComponent(txtContrasena))))))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(105, 105, 105)
                        .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 143, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addGap(0, 84, Short.MAX_VALUE)
                .addComponent(btnRegistrarse)
                .addGap(61, 61, 61))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(23, 23, 23)
                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtDni, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(Dni))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblContrasena)
                    .addComponent(txtContrasena, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(27, 27, 27)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnLoginCliente)
                    .addComponent(btnLoginAdmin))
                .addGap(18, 18, 18)
                .addComponent(btnRegistrarse)
                .addGap(18, 18, 18))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(20, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void btnLoginClienteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLoginClienteActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_btnLoginClienteActionPerformed

    public javax.swing.JButton getBtnLoginCliente() { return btnLoginCliente; }
    public javax.swing.JButton getBtnLoginAdmin() { return btnLoginAdmin; }
    public javax.swing.JButton getBtnRegistrarse() { return btnRegistrarse; }
    public String getDni() {
        return txtDni.getText().trim();
    }

    // El llamador debe sobrescribir este arreglo con Arrays.fill(..., '0') apenas lo use
    public char[] getContrasenaChars() {
        return txtContrasena.getPassword();
    }

    public void limpiarContrasena() {
        txtContrasena.setText("");
    }

    // Para que Enter/Tab funcionen desde el primer instante al volver a esta pantalla, sin que el
    // usuario tenga que hacer clic en el campo primero.
    public void enfocarPrimerCampo() {
        txtDni.requestFocusInWindow();
    }

    // Limpieza profunda al volver a esta pantalla: sin esto, el DNI/clave del usuario anterior
    // quedaría visible "fantasma" al navegar de vuelta al login vía CardLayout.
    public void limpiarFormulario() {
        txtDni.setText("");
        txtContrasena.setText("");
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel Dni;
    private javax.swing.JButton btnLoginAdmin;
    private javax.swing.JButton btnLoginCliente;
    private javax.swing.JButton btnRegistrarse;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JLabel lblContrasena;
    private javax.swing.JPasswordField txtContrasena;
    private javax.swing.JTextField txtDni;
    // End of variables declaration//GEN-END:variables
}
