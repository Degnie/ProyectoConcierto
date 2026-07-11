/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package vista;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import util.IconRegistry;
import util.Tipografia;

/**
 *
 * @author lopez
 */
public class FrmAdministrador extends javax.swing.JPanel {

    private static final Color COLOR_BORDE_TARJETA = new Color(0xE0, 0xE0, 0xE0);
    private static final Color COLOR_FONDO = new Color(0xF4, 0xF4, 0xF4);

    /**
     * Creates new form FrmAdministrador
     */
    public FrmAdministrador() {
        initComponents();
        normalizarYCentrar();
        reestructurarLayout();
    }

    // Igual que en FrmCliente/FrmLogin: jPanel1 (todo el formulario generado por el Form Editor,
    // initComponents() no se toca) se remueve de `this` y sus componentes se reparentan a un
    // layout nuevo en tarjetas. Removerlo de `this` es imprescindible -- si quedara colgando como
    // hijo huérfano, su GroupLayout se re-ejecutaría en el próximo validate() y "reclamaría" de
    // vuelta a los componentes ya reparentados (bug real que se encontró y corrigió en FrmCliente).
    private void reestructurarLayout() {
        remove(jPanel1);
        setLayout(new BorderLayout());
        add(construirEncabezado(), BorderLayout.NORTH);
        add(construirContenido(), BorderLayout.CENTER);
    }

    private JPanel construirEncabezado() {
        jLabel1.setFont(Tipografia.TITULO.deriveFont(18f));
        jLabel1.setIcon(IconRegistry.get(IconRegistry.LOCK, 20));
        jLabel1.setIconTextGap(8);

        JPanel izquierda = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        izquierda.setOpaque(false);
        izquierda.add(jLabel1);

        btnRegresar.setText("Regresar");
        btnRegresar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnRegresar.putClientProperty(FlatClientProperties.STYLE,
                "background:$Panel.background;foreground:#8D8D8D;borderColor:#8D8D8D;focusedBorderColor:#8D8D8D;");
        btnCerrarSesion.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnCerrarSesion.putClientProperty(FlatClientProperties.STYLE,
                "background:$Panel.background;foreground:#8D8D8D;borderColor:#8D8D8D;focusedBorderColor:#8D8D8D;");

        JPanel derecha = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        derecha.setOpaque(false);
        derecha.add(btnRegresar);
        derecha.add(btnCerrarSesion);

        JPanel encabezado = new JPanel(new BorderLayout());
        encabezado.setOpaque(true);
        encabezado.setBackground(Color.WHITE);
        encabezado.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_BORDE_TARJETA));
        encabezado.add(izquierda, BorderLayout.WEST);
        encabezado.add(derecha, BorderLayout.EAST);
        return encabezado;
    }

    // Tres secciones apiladas, mismo patrón de tarjetas que FrmCliente: 1) elegir/crear concierto,
    // 2) crear zonas para el concierto elegido (con su tabla debajo), 3) ventas realizadas.
    private JScrollPane construirContenido() {
        JPanel columna = new JPanel();
        columna.setLayout(new BoxLayout(columna, BoxLayout.Y_AXIS));
        columna.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        columna.setBackground(COLOR_FONDO);
        columna.setOpaque(true);

        columna.add(seccionConcierto());
        columna.add(Box.createVerticalStrut(16));
        columna.add(seccionZonas());
        columna.add(Box.createVerticalStrut(16));
        columna.add(seccionVentas());

        JScrollPane scroll = new JScrollPane(columna);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(COLOR_FONDO);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    private JPanel seccionConcierto() {
        JPanel tarjeta = crearTarjeta();
        tarjeta.add(tituloSeccion("Concierto", IconRegistry.CARD));
        tarjeta.add(Box.createVerticalStrut(8));
        tarjeta.add(fila(jLabel2, cmbConciertos));
        tarjeta.add(Box.createVerticalStrut(16));

        btnNuevoConcierto.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnNuevoConcierto.putClientProperty(FlatClientProperties.STYLE,
                "background:$Panel.background;foreground:#0F62FE;borderColor:#0F62FE;focusedBorderColor:#0F62FE;");
        alinearIzquierda(btnNuevoConcierto);
        tarjeta.add(btnNuevoConcierto);
        return tarjeta;
    }

    private JPanel seccionZonas() {
        JPanel tarjeta = crearTarjeta();
        tarjeta.add(tituloSeccion("Zonas del concierto", null));
        tarjeta.add(Box.createVerticalStrut(8));

        tarjeta.add(fila(jLabel3, txtZonaNombre));
        tarjeta.add(Box.createVerticalStrut(8));
        tarjeta.add(fila(jLabel4, txtZonaCapacidad));
        tarjeta.add(Box.createVerticalStrut(8));
        tarjeta.add(fila(jLabel5, txtZonaPrecio));
        tarjeta.add(Box.createVerticalStrut(16));

        btnAgregarZona.setFont(Tipografia.CUERPO.deriveFont(Font.BOLD));
        btnAgregarZona.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnAgregarZona.putClientProperty(FlatClientProperties.STYLE,
                "background:#0F62FE;foreground:#FFFFFF;"
                + "hoverBackground:#0353E9;pressedBackground:#0043CE;borderWidth:0;");
        alinearIzquierda(btnAgregarZona);
        tarjeta.add(btnAgregarZona);
        tarjeta.add(Box.createVerticalStrut(16));

        jScrollPane2.setAlignmentX(Component.LEFT_ALIGNMENT);
        jScrollPane2.setPreferredSize(new Dimension(600, 140));
        jScrollPane2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));
        tblZonas.getAccessibleContext().setAccessibleName("Zonas del concierto");
        tarjeta.add(jScrollPane2);
        tarjeta.add(Box.createVerticalStrut(8));

        btnEditarSeleccion.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnEditarSeleccion.putClientProperty(FlatClientProperties.STYLE,
                "background:$Panel.background;foreground:#0F62FE;borderColor:#0F62FE;focusedBorderColor:#0F62FE;");
        alinearIzquierda(btnEditarSeleccion);
        tarjeta.add(btnEditarSeleccion);
        return tarjeta;
    }

    private JPanel seccionVentas() {
        JPanel tarjeta = crearTarjeta();
        jLabel6.setText("Ventas realizadas");
        tarjeta.add(tituloSeccion(jLabel6.getText(), IconRegistry.CHECK_CIRCLE));
        tarjeta.add(Box.createVerticalStrut(8));

        jScrollPane3.setAlignmentX(Component.LEFT_ALIGNMENT);
        jScrollPane3.setPreferredSize(new Dimension(600, 180));
        jScrollPane3.setMaximumSize(new Dimension(Integer.MAX_VALUE, 180));
        tblVentas.getAccessibleContext().setAccessibleName("Ventas realizadas");
        tarjeta.add(jScrollPane3);
        return tarjeta;
    }

    private JLabel tituloSeccion(String texto, String icono) {
        JLabel titulo = new JLabel(texto);
        titulo.setFont(Tipografia.CUERPO.deriveFont(Font.BOLD, 16f));
        if (icono != null) {
            titulo.setIcon(IconRegistry.get(icono, 18));
            titulo.setIconTextGap(8);
        }
        alinearIzquierda(titulo);
        return titulo;
    }

    // Fila estándar de formulario: etiqueta arriba, 8px de separación, campo abajo (mismo patrón
    // de FrmCliente). Los campos de texto se estiran al ancho de la tarjeta; el combo queda a su
    // ancho natural.
    private JPanel fila(JLabel etiqueta, JComponent campo) {
        etiqueta.setFont(Tipografia.CUERPO);
        alinearIzquierda(etiqueta);
        alinearIzquierda(campo);
        if (campo instanceof JTextField) {
            campo.setMaximumSize(new Dimension(Integer.MAX_VALUE, campo.getPreferredSize().height));
        }

        JPanel fila = new JPanel();
        fila.setLayout(new BoxLayout(fila, BoxLayout.Y_AXIS));
        fila.setOpaque(false);
        alinearIzquierda(fila);
        fila.add(etiqueta);
        fila.add(Box.createVerticalStrut(8));
        fila.add(campo);
        return fila;
    }

    private JPanel crearTarjeta() {
        JPanel tarjeta = new JPanel();
        tarjeta.setLayout(new BoxLayout(tarjeta, BoxLayout.Y_AXIS));
        tarjeta.setOpaque(true);
        tarjeta.setBackground(Color.WHITE);
        tarjeta.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDE_TARJETA, 1),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)));
        alinearIzquierda(tarjeta);
        return tarjeta;
    }

    private void alinearIzquierda(JComponent c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    // jLabel4/jLabel5 quedan en minúscula en el Form Editor (initComponents(), no se toca) mientras
    // el resto de las etiquetas de esta misma pantalla usan mayúscula inicial (jLabel2, jLabel3) —
    // se normaliza acá en código plano. También se centran las columnas de tblZonas/tblVentas
    // (salvo la primera, que identifica la fila) reaplicando el renderer cada vez que el
    // controlador reemplaza el modelo (setModel), igual que en FrmCliente.
    private void normalizarYCentrar() {
        jLabel4.setText("Capacidad");
        jLabel5.setText("Precio");

        tblZonas.addPropertyChangeListener("model", ev -> centrarColumnasDesde(tblZonas, 1));
        centrarColumnasDesde(tblZonas, 1);
        tblVentas.addPropertyChangeListener("model", ev -> centrarColumnasDesde(tblVentas, 1));
        centrarColumnasDesde(tblVentas, 1);
    }

    private static void centrarColumnasDesde(JTable tabla, int primeraColumnaACentrar) {
        DefaultTableCellRenderer centrado = new DefaultTableCellRenderer();
        centrado.setHorizontalAlignment(SwingConstants.CENTER);
        var columnas = tabla.getColumnModel();
        for (int i = primeraColumnaACentrar; i < columnas.getColumnCount(); i++) {
            columnas.getColumn(i).setCellRenderer(centrado);
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

        jScrollPane1 = new javax.swing.JScrollPane();
        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        txtZonaNombre = new javax.swing.JTextField();
        txtZonaCapacidad = new javax.swing.JTextField();
        txtZonaPrecio = new javax.swing.JTextField();
        btnNuevoConcierto = new javax.swing.JButton();
        jLabel6 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        tblZonas = new javax.swing.JTable();
        jScrollPane3 = new javax.swing.JScrollPane();
        tblVentas = new javax.swing.JTable();
        cmbConciertos = new javax.swing.JComboBox<>();
        btnCerrarSesion = new javax.swing.JButton();
        btnRegresar = new javax.swing.JButton();
        btnAgregarZona = new javax.swing.JButton();
        btnEditarSeleccion = new javax.swing.JButton();

        jLabel1.setText("ADMINISTRADOR");

        jLabel2.setText("Selección del Concierto:");

        jLabel3.setText("Nombre de la Zona:");

        jLabel4.setText("capacidad");

        jLabel5.setText("precio");

        txtZonaNombre.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtZonaNombreActionPerformed(evt);
            }
        });

        btnNuevoConcierto.setText("Nuevo Concierto");

        jLabel6.setText("VENTAS REALIZADAS");

        tblZonas.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null},
                {null, null, null},
                {null, null, null},
                {null, null, null}
            },
            new String [] {
                "nombre", "capacidad", "precio"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Integer.class, java.lang.Integer.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        tblZonas.setColumnSelectionAllowed(true);
        jScrollPane2.setViewportView(tblZonas);
        tblZonas.getColumnModel().getSelectionModel().setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);

        tblVentas.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "cliente", "zona", "monto", "concierto"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                true, false, false, true
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tblVentas.setColumnSelectionAllowed(true);
        jScrollPane3.setViewportView(tblVentas);
        tblVentas.getColumnModel().getSelectionModel().setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);

        btnCerrarSesion.setText("Cerrar Sesión");

        btnRegresar.setText("Regresar");

        btnAgregarZona.setText("Agregar Zona");

        btnEditarSeleccion.setText("Editar Selección");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(26, 26, 26)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel6)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 125, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtZonaNombre, javax.swing.GroupLayout.PREFERRED_SIZE, 176, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(244, 244, 244)
                                .addComponent(jLabel1))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(147, 147, 147)
                                .addComponent(btnCerrarSesion)
                                .addGap(127, 127, 127)
                                .addComponent(btnRegresar))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(60, 60, 60)
                                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(btnEditarSeleccion))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(235, 235, 235)
                                .addComponent(cmbConciertos, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 136, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btnNuevoConcierto)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel4))
                                .addGap(77, 77, 77)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(txtZonaCapacidad, javax.swing.GroupLayout.DEFAULT_SIZE, 175, Short.MAX_VALUE)
                                    .addComponent(txtZonaPrecio))
                                .addGap(76, 76, 76)
                                .addComponent(btnAgregarZona)))
                        .addContainerGap(72, Short.MAX_VALUE))))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addGap(27, 27, 27)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(cmbConciertos, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(btnNuevoConcierto)
                .addGap(21, 21, 21)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabel3)
                                    .addComponent(txtZonaNombre, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabel5)
                                    .addComponent(txtZonaPrecio, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(btnAgregarZona))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel4)
                                .addGap(6, 6, 6))
                            .addComponent(txtZonaCapacidad, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 111, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(btnEditarSeleccion))
                .addGap(24, 24, 24)
                .addComponent(jLabel6)
                .addGap(18, 18, 18)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 96, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 25, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnCerrarSesion)
                    .addComponent(btnRegresar))
                .addGap(12, 12, 12))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void txtZonaNombreActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtZonaNombreActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtZonaNombreActionPerformed

    // ======== MÉTODOS GETTER PARA EL CONTROLADOR ========
public javax.swing.JComboBox<String> getCmbConciertos() { return cmbConciertos; }
public javax.swing.JButton getBtnNuevoConcierto() { return btnNuevoConcierto; }
public javax.swing.JButton getBtnAgregarZona() { return btnAgregarZona; }
public javax.swing.JButton getBtnEditarSeleccion() { return btnEditarSeleccion; }
public javax.swing.JButton getBtnCerrarSesion() { return btnCerrarSesion; }
public javax.swing.JButton getBtnRegresar() { return btnRegresar; }

public javax.swing.JTable getTblZonas() { return tblZonas; }
public javax.swing.JTable getTblVentas() { return tblVentas; }

public String getZonaNombre() { return txtZonaNombre.getText().trim(); }
public String getZonaPrecio() { return txtZonaPrecio.getText().trim(); }
public String getZonaCapacidad() { return txtZonaCapacidad.getText().trim(); }

// Método para limpiar el formulario de zonas desde el controlador
public void limpiarFormularioZona() {
    txtZonaNombre.setText("");
    txtZonaPrecio.setText("");
    txtZonaCapacidad.setText("");
}
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAgregarZona;
    private javax.swing.JButton btnCerrarSesion;
    private javax.swing.JButton btnEditarSeleccion;
    private javax.swing.JButton btnNuevoConcierto;
    private javax.swing.JButton btnRegresar;
    private javax.swing.JComboBox<String> cmbConciertos;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JTable tblVentas;
    private javax.swing.JTable tblZonas;
    private javax.swing.JTextField txtZonaCapacidad;
    private javax.swing.JTextField txtZonaNombre;
    private javax.swing.JTextField txtZonaPrecio;
    // End of variables declaration//GEN-END:variables
}
