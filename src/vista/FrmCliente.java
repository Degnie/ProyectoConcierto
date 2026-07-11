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
import java.awt.event.KeyEvent;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import util.IconRegistry;
import util.Tipografia;

/**
 *
 * @author lopez
 */
public class FrmCliente extends javax.swing.JPanel {

    private static final Color COLOR_ACENTO = new Color(0x0F, 0x62, 0xFE);
    private static final Color COLOR_SECUNDARIO = new Color(0x8D, 0x8D, 0x8D);
    private static final Color COLOR_ERROR = new Color(0xDA, 0x1E, 0x28);
    private static final Color COLOR_BORDE_TARJETA = new Color(0xE0, 0xE0, 0xE0);
    private static final Color COLOR_FONDO = new Color(0xF4, 0xF4, 0xF4);

    // Primer ítem de cmbTipoTarjeta: no es un TipoTarjeta real, así que el controlador debe
    // tratarlo como "todavía no elegido nada" antes de llamar TipoTarjeta.valueOf(...).
    public static final String PLACEHOLDER_TIPO_TARJETA = "Elija el tipo de tarjeta";

    // Validación inline del formulario de tarjeta (reemplaza el JOptionPane de errores puntuales):
    // el componente vive acá, en vista/, listo para que el controlador lo use en vez de un diálogo
    // modal. Cablear procesarRegistroTarjeta() para que llame a mostrarErrorTarjeta(...) es un
    // cambio en controlador/, fuera del alcance permitido para esta pasada.
    private final JLabel lblErrorTarjeta = new JLabel(" ");

    // "Guardar tarjeta para futuras compras": si está marcado al registrar, el controlador
    // persiste tipo/enmascarado/fecha/token en Oracle para que la próxima sesión arranque con la
    // tarjeta ya activa (ver Cliente.hidratarTarjeta). Sin marcar, la tarjeta solo dura la sesión.
    private final JCheckBox chkGuardarTarjeta = new JCheckBox("Guardar esta tarjeta para futuras compras");

    // Indicador de que ya hay una tarjeta lista para pagar (registrada esta sesión, o recordada de
    // una anterior): así el cliente sabe que puede ir directo a "COMPRAR" sin volver a llenar el
    // formulario, y que llenarlo de nuevo simplemente reemplaza la tarjeta activa.
    private final JLabel lblTarjetaActiva = new JLabel(" ");

    /**
     * Creates new form FrmCliente
     */
    public FrmCliente() {
        initComponents();
        spnCantidadEntradas.setModel(new javax.swing.SpinnerNumberModel(1, 1, 4, 1));

        // Por defecto un JSpinner numérico solo confirma el valor con Enter o al perder el foco;
        // esto hace que cada tecla escrita dispare el ChangeListener de inmediato, para que el
        // total del checkout se recalcule en caliente sin que el usuario tenga que salir del campo.
        javax.swing.JSpinner.DefaultEditor editorCantidad = (javax.swing.JSpinner.DefaultEditor) spnCantidadEntradas.getEditor();
        javax.swing.JFormattedTextField campoCantidad = editorCantidad.getTextField();
        javax.swing.JFormattedTextField.AbstractFormatter formateador = campoCantidad.getFormatter();
        if (formateador instanceof javax.swing.text.DefaultFormatter) {
            ((javax.swing.text.DefaultFormatter) formateador).setCommitsOnValidEdit(true);
        }

        reestructurarLayout();
    }

    // Reorganiza los mismos componentes que initComponents() (regenerado por el Form Editor de
    // NetBeans; no se toca) ya construyó dentro de jPanel1, sacándolos de su GroupLayout original
    // hacia un JTabbedPane con dos contextos separados. Un Component en Swing solo puede tener un
    // padre a la vez, así que basta con volver a hacer contenedorNuevo.add(componente) para
    // "sacarlo" de jPanel1 — PERO si jPanel1 sigue siendo hijo de `this`, su GroupLayout se vuelve
    // a ejecutar en la próxima validate() (p. ej. al mostrar la ventana) y "reclama" de vuelta a
    // sus componentes originales, revirtiendo el reparenting silenciosamente. Por eso jPanel1 se
    // remueve de `this` explícitamente antes de reconstruir nada.
    private void reestructurarLayout() {
        remove(jPanel1);

        JTabbedPane pestañas = new JTabbedPane();
        pestañas.addTab("Nueva Compra", construirPestañaCompra());
        pestañas.addTab("Mis Compras", construirPestañaHistorial());
        pestañas.setMnemonicAt(0, KeyEvent.VK_1);
        pestañas.setMnemonicAt(1, KeyEvent.VK_2);
        pestañas.getAccessibleContext().setAccessibleDescription(
                "Alterna entre iniciar una compra nueva y ver el historial de compras");

        setLayout(new BorderLayout());
        add(construirEncabezado(), BorderLayout.NORTH);
        add(pestañas, BorderLayout.CENTER);

        aplicarAccesibilidadYCursores();
    }

    // Barra superior persistente (visible en ambas pestañas): identidad del cliente, puntos y
    // cierre de sesión, para no obligar a cambiar de pestaña solo para ver cuántos puntos tiene.
    private JPanel construirEncabezado() {
        jLabel1.setFont(Tipografia.TITULO.deriveFont(16f));
        jLabel1.setIcon(IconRegistry.get(IconRegistry.USER, 18));
        jLabel1.setIconTextGap(8);
        jLabel11.setFont(Tipografia.CUERPO);
        lblPuntos.setFont(Tipografia.CUERPO.deriveFont(Font.BOLD));
        lblPuntos.setForeground(COLOR_ACENTO);

        JPanel izquierda = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        izquierda.setOpaque(false);
        izquierda.add(jLabel1);
        izquierda.add(jLabel11);
        izquierda.add(lblPuntos);

        btnCerrarSesion.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        JPanel derecha = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 8));
        derecha.setOpaque(false);
        derecha.add(btnCerrarSesion);

        JPanel encabezado = new JPanel(new BorderLayout());
        encabezado.setOpaque(true);
        encabezado.setBackground(Color.WHITE);
        encabezado.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_BORDE_TARJETA));
        encabezado.add(izquierda, BorderLayout.WEST);
        encabezado.add(derecha, BorderLayout.EAST);
        return encabezado;
    }

    // Embudo reordenado de arriba hacia abajo: 1) qué comprar (concierto/zona), 2) cuánto y con
    // qué descuento (cantidad + puntos), 3) cómo pagar (tarjeta). Antes el formulario de tarjeta
    // aparecía primero, pidiendo datos de pago antes de que el usuario supiera siquiera qué zona
    // había elegido.
    private JScrollPane construirPestañaCompra() {
        JPanel columna = new JPanel();
        columna.setLayout(new BoxLayout(columna, BoxLayout.Y_AXIS));
        columna.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        columna.setBackground(COLOR_FONDO);
        columna.setOpaque(true);

        columna.add(seccionZonas());
        columna.add(Box.createVerticalStrut(16));
        columna.add(seccionResumenCompra());
        columna.add(Box.createVerticalStrut(16));
        columna.add(seccionTarjetaYPago());

        JScrollPane scroll = new JScrollPane(columna);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(COLOR_FONDO);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    private JPanel seccionZonas() {
        JPanel tarjeta = crearTarjeta();
        tarjeta.add(tituloSeccion("Concierto y zona", IconRegistry.CARD));
        tarjeta.add(Box.createVerticalStrut(8));
        tarjeta.add(fila(jLabel10, cmbConciertosCliente));
        tarjeta.add(Box.createVerticalStrut(16));

        jLabel7.setFont(Tipografia.CUERPO.deriveFont(Font.BOLD));
        alinearIzquierda(jLabel7);
        tarjeta.add(jLabel7);
        tarjeta.add(Box.createVerticalStrut(8));

        // JTable.getPreferredScrollableViewportSize() devuelve un fijo (450,400) sin importar la
        // cantidad real de filas; sin fijar el preferredSize acá, ese valor domina el BoxLayout y
        // la tabla se come toda la columna, dejando "Tarjeta y pago" fuera de la vista inicial.
        jScrollPane1.setAlignmentX(Component.LEFT_ALIGNMENT);
        jScrollPane1.setPreferredSize(new Dimension(600, 140));
        jScrollPane1.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));
        tblZonasDisponibles.getAccessibleContext().setAccessibleName("Zonas disponibles");
        tblZonasDisponibles.getAccessibleContext().setAccessibleDescription(
                "Tabla de zonas del concierto seleccionado, con precio y entradas disponibles");
        tarjeta.add(jScrollPane1);
        return tarjeta;
    }

    private JPanel seccionResumenCompra() {
        JPanel tarjeta = crearTarjeta();
        tarjeta.add(tituloSeccion("Cantidad y puntos", null));
        tarjeta.add(Box.createVerticalStrut(8));
        tarjeta.add(fila(jLabel8, spnCantidadEntradas));
        tarjeta.add(Box.createVerticalStrut(8));

        alinearIzquierda(chkAplicarPuntos);
        chkAplicarPuntos.setFont(Tipografia.CUERPO);
        chkAplicarPuntos.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        tarjeta.add(chkAplicarPuntos);
        tarjeta.add(Box.createVerticalStrut(8));

        JPanel totales = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        totales.setOpaque(false);
        alinearIzquierda(totales);
        lblDescuentoPuntos.setFont(Tipografia.CUERPO);
        lblTotal.setFont(Tipografia.CUERPO.deriveFont(Font.BOLD, 16f));
        totales.add(lblDescuentoPuntos);
        totales.add(lblTotal);
        tarjeta.add(totales);
        return tarjeta;
    }

    private JPanel seccionTarjetaYPago() {
        JPanel tarjeta = crearTarjeta();
        tarjeta.add(tituloSeccion("Tarjeta y pago", IconRegistry.LOCK));
        tarjeta.add(Box.createVerticalStrut(8));

        lblTarjetaActiva.setFont(Tipografia.CUERPO.deriveFont(Font.BOLD));
        lblTarjetaActiva.setForeground(COLOR_ACENTO);
        lblTarjetaActiva.setIcon(IconRegistry.get(IconRegistry.CHECK_CIRCLE, 16));
        lblTarjetaActiva.setIconTextGap(8);
        lblTarjetaActiva.setVisible(false);
        alinearIzquierda(lblTarjetaActiva);
        tarjeta.add(lblTarjetaActiva);
        tarjeta.add(Box.createVerticalStrut(8));

        // Placeholder inicial: sin esto el combo arranca en "VISA" y el descuento de VISA ya
        // queda aplicado en el total antes de que el cliente elija nada, lo que parece un
        // descuento "de regalo" no pedido. Con el placeholder seleccionado no se aplica descuento
        // (precio de lista) hasta que el cliente elige un tipo real.
        if (cmbTipoTarjeta.getItemCount() == 0 || !PLACEHOLDER_TIPO_TARJETA.equals(cmbTipoTarjeta.getItemAt(0))) {
            cmbTipoTarjeta.insertItemAt(PLACEHOLDER_TIPO_TARJETA, 0);
        }
        cmbTipoTarjeta.setSelectedIndex(0);
        tarjeta.add(fila(jLabelTipoTarjeta, cmbTipoTarjeta));
        lblRequisitosTarjeta.setFont(Tipografia.VALIDACION);
        lblRequisitosTarjeta.setForeground(COLOR_SECUNDARIO);
        alinearIzquierda(lblRequisitosTarjeta);
        tarjeta.add(lblRequisitosTarjeta);
        tarjeta.add(Box.createVerticalStrut(8));

        tarjeta.add(fila(jLabel3, txtTarjNumero));
        tarjeta.add(Box.createVerticalStrut(8));
        tarjeta.add(fila(jLabel5, txtTarjFecha));
        tarjeta.add(Box.createVerticalStrut(8));
        tarjeta.add(fila(jLabel6, txtTarjCvv));
        tarjeta.add(Box.createVerticalStrut(8));

        lblErrorTarjeta.setFont(Tipografia.VALIDACION);
        lblErrorTarjeta.setForeground(COLOR_ERROR);
        lblErrorTarjeta.setVisible(false);
        alinearIzquierda(lblErrorTarjeta);
        tarjeta.add(lblErrorTarjeta);
        tarjeta.add(Box.createVerticalStrut(8));

        chkGuardarTarjeta.setFont(Tipografia.CUERPO);
        chkGuardarTarjeta.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        alinearIzquierda(chkGuardarTarjeta);
        tarjeta.add(chkGuardarTarjeta);
        tarjeta.add(Box.createVerticalStrut(16));

        // Sistema de compra al instante: no tiene sentido un paso separado de "registrar tarjeta"
        // antes de comprar (¿quién completaría ese trámite solo para guardar una tarjeta sin
        // comprar nada?). btnRegistrarTarjeta (generado por el Form Editor, no se toca) queda sin
        // usar y oculto; "Comprar" tokeniza la tarjeta del formulario si hace falta y compra en el
        // mismo clic (ver ControladorCliente.asegurarTarjetaActiva).
        btnRegistrarTarjeta.setVisible(false);

        btnComprarEntrada.setFont(Tipografia.CUERPO.deriveFont(Font.BOLD));
        btnComprarEntrada.putClientProperty(FlatClientProperties.STYLE,
                "background:#0F62FE;foreground:#FFFFFF;"
                + "hoverBackground:#0353E9;pressedBackground:#0043CE;borderWidth:0;");
        alinearIzquierda(btnComprarEntrada);
        tarjeta.add(btnComprarEntrada);
        return tarjeta;
    }

    // Contexto separado del checkout: el historial no compite visualmente con el formulario de
    // pago ni obliga a hacer scroll más allá de lo necesario para llegar a "Comprar".
    private JPanel construirPestañaHistorial() {
        JPanel columna = new JPanel();
        columna.setLayout(new BoxLayout(columna, BoxLayout.Y_AXIS));
        columna.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        columna.setBackground(COLOR_FONDO);
        columna.setOpaque(true);

        JPanel tarjeta = crearTarjeta();
        tarjeta.add(tituloSeccion("Mis compras", IconRegistry.CHECK_CIRCLE));
        tarjeta.add(Box.createVerticalStrut(8));

        jScrollPane3.setAlignmentX(Component.LEFT_ALIGNMENT);
        jScrollPane3.setPreferredSize(new Dimension(600, 220));
        jScrollPane3.setMaximumSize(new Dimension(Integer.MAX_VALUE, 220));
        // El controlador reemplaza el modelo de la tabla en cada refresh (setModel(dtm)), lo que
        // recrea las columnas y borra cualquier renderer puesto directamente sobre ellas. Por eso
        // el renderer centrado se reaplica cada vez que cambia el modelo, en vez de una sola vez.
        tblMisCompras.addPropertyChangeListener("model", ev -> centrarColumnasMisCompras());
        centrarColumnasMisCompras();
        tblMisCompras.getAccessibleContext().setAccessibleName("Historial de compras");
        tblMisCompras.getAccessibleContext().setAccessibleDescription(
                "Tabla con tus compras anteriores: concierto, zona, cantidad, monto y estado");
        tarjeta.add(jScrollPane3);
        tarjeta.add(Box.createVerticalStrut(16));

        // "liberar" (texto generado por el Form Editor) no comunica qué hace sobre una compra ya
        // pagada; el propio dominio ya llama a esta operación "anular" (Venta.anular(),
        // Cliente.anularVenta(), VentaRepository.anularVentaPersistida), así que el botón usa el
        // mismo término en vez de inventar uno nuevo ("devolución" implicaría un reembolso que
        // este sistema no modela).
        btnLiberarEntrada.setText("Anular Compra");
        btnLiberarEntrada.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnLiberarEntrada.putClientProperty(FlatClientProperties.STYLE,
                "background:$Panel.background;foreground:#DA1E28;borderColor:#DA1E28;focusedBorderColor:#DA1E28;");
        alinearIzquierda(btnLiberarEntrada);
        tarjeta.add(btnLiberarEntrada);

        columna.add(tarjeta);
        return columna;
    }

    // Columna 0 (Concierto) queda a la izquierda como el resto de las tablas de la app; Zona,
    // Cantidad, Monto Total y Estado se centran para no dejar todo el texto/números pegados al
    // borde con un espacio en blanco enorme a la derecha de cada celda.
    private void centrarColumnasMisCompras() {
        DefaultTableCellRenderer centrado = new DefaultTableCellRenderer();
        centrado.setHorizontalAlignment(SwingConstants.CENTER);
        var columnas = tblMisCompras.getColumnModel();
        for (int i = 1; i < columnas.getColumnCount(); i++) {
            columnas.getColumn(i).setCellRenderer(centrado);
        }
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

    // Fila estándar de formulario: etiqueta arriba, 8px de separación, campo abajo (sistema de
    // grilla de 8pt). Los campos de texto/spinner se estiran al ancho de la tarjeta; los combo
    // quedan a su ancho natural, que se ve mejor sin estirar de borde a borde.
    private JPanel fila(JLabel etiqueta, JComponent campo) {
        etiqueta.setFont(Tipografia.CUERPO);
        alinearIzquierda(etiqueta);
        alinearIzquierda(campo);
        if (campo instanceof JTextField || campo instanceof JSpinner) {
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

    // Nombre/descripción accesible (lector de pantalla) y cursor de mano en cada control
    // interactivo. Los estilos de color/tipografía puntuales ya quedaron aplicados en cada
    // sección de arriba; acá solo lo transversal a todos los controles.
    private void aplicarAccesibilidadYCursores() {
        cmbConciertosCliente.getAccessibleContext().setAccessibleName("Concierto");
        cmbConciertosCliente.getAccessibleContext().setAccessibleDescription(
                "Selecciona el concierto para ver sus zonas disponibles");
        spnCantidadEntradas.getAccessibleContext().setAccessibleName("Cantidad de entradas");
        spnCantidadEntradas.getAccessibleContext().setAccessibleDescription(
                "Cantidad de entradas a comprar, de 1 a 4");
        chkAplicarPuntos.getAccessibleContext().setAccessibleDescription(
                "Aplica el descuento por puntos de fidelidad acumulados a esta compra");
        cmbTipoTarjeta.getAccessibleContext().setAccessibleName("Tipo de tarjeta");
        cmbTipoTarjeta.getAccessibleContext().setAccessibleDescription(
                "Marca de la tarjeta a registrar: VISA, Mastercard, Diners o Amex");
        txtTarjNumero.getAccessibleContext().setAccessibleName("Número de tarjeta");
        txtTarjFecha.getAccessibleContext().setAccessibleName("Fecha de vencimiento");
        txtTarjFecha.getAccessibleContext().setAccessibleDescription("Formato MM/AA");
        txtTarjCvv.getAccessibleContext().setAccessibleName("CVV");
        btnComprarEntrada.getAccessibleContext().setAccessibleDescription(
                "Tokeniza la tarjeta si hace falta y confirma la compra de las entradas seleccionadas");
        btnLiberarEntrada.getAccessibleContext().setAccessibleDescription(
                "Anula la compra seleccionada en la tabla y libera sus entradas");

        for (AbstractButton boton : new AbstractButton[]{
            btnComprarEntrada, btnLiberarEntrada
        }) {
            boton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
    }

    // Ganchos de validación inline para el formulario de tarjeta (ver comentario del campo
    // lblErrorTarjeta más arriba): quedan listos para que controlador/ los invoque.
    public void mostrarErrorTarjeta(String mensaje) {
        lblErrorTarjeta.setText(mensaje);
        lblErrorTarjeta.setVisible(true);
    }

    public void ocultarErrorTarjeta() {
        lblErrorTarjeta.setText(" ");
        lblErrorTarjeta.setVisible(false);
    }

    public boolean isGuardarTarjetaSeleccionado() {
        return chkGuardarTarjeta.isSelected();
    }

    public void limpiarGuardarTarjeta() {
        chkGuardarTarjeta.setSelected(false);
    }

    // Se muestra apenas hay una tarjeta lista para pagar (recordada de una sesión anterior, o
    // recién registrada en esta) para que el cliente sepa que puede ir directo a "COMPRAR".
    public void mostrarTarjetaActiva(String descripcion) {
        lblTarjetaActiva.setText(descripcion);
        lblTarjetaActiva.setVisible(true);
    }

    public void ocultarTarjetaActiva() {
        lblTarjetaActiva.setText(" ");
        lblTarjetaActiva.setVisible(false);
    }

    // "CLIENTE" (genérico) reemplazado por un saludo personalizado; jLabel1 es el mismo JLabel de
    // siempre, solo cambia el texto.
    public void setNombreCliente(String nombres) {
        jLabel1.setText("Hola, " + nombres);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane2 = new javax.swing.JScrollPane();
        jTable2 = new javax.swing.JTable();
        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabelTipoTarjeta = new javax.swing.JLabel();
        cmbTipoTarjeta = new javax.swing.JComboBox<>();
        lblRequisitosTarjeta = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        txtTarjNumero = new javax.swing.JTextField();
        txtTarjFecha = new javax.swing.JTextField();
        txtTarjCvv = new javax.swing.JTextField();
        btnRegistrarTarjeta = new javax.swing.JButton();
        jLabel7 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblZonasDisponibles = new javax.swing.JTable();
        jLabel8 = new javax.swing.JLabel();
        btnComprarEntrada = new javax.swing.JButton();
        jLabel9 = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        tblMisCompras = new javax.swing.JTable();
        btnLiberarEntrada = new javax.swing.JButton();
        spnCantidadEntradas = new javax.swing.JSpinner();
        cmbConciertosCliente = new javax.swing.JComboBox<>();
        jLabel10 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        btnCerrarSesion = new javax.swing.JButton();
        lblPuntos = new javax.swing.JLabel();
        chkAplicarPuntos = new javax.swing.JCheckBox();
        lblDescuentoPuntos = new javax.swing.JLabel();
        lblTotal = new javax.swing.JLabel();

        jTable2.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane2.setViewportView(jTable2);

        jLabel1.setText("CLIENTE");

        jLabel2.setText("TARJETA");

        jLabelTipoTarjeta.setText("Tipo de Tarjeta");

        cmbTipoTarjeta.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "VISA", "MASTERCARD", "DINERS", "AMEX" }));

        lblRequisitosTarjeta.setText("16 dígitos, CVV de 3 dígitos");

        jLabel3.setText("Número de Tarjeta");

        jLabel5.setText("Fecha Vencimiento");

        jLabel6.setText("CVV");

        btnRegistrarTarjeta.setText("REGISTRAR TARJETA");

        jLabel7.setText("ZONAS DISPONIBLES");

        tblZonasDisponibles.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null},
                {null, null, null},
                {null, null, null},
                {null, null, null}
            },
            new String [] {
                "zona", "precio", "disponibles"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Integer.class, java.lang.Integer.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tblZonasDisponibles.setColumnSelectionAllowed(true);
        jScrollPane1.setViewportView(tblZonasDisponibles);
        tblZonasDisponibles.getColumnModel().getSelectionModel().setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);

        jLabel8.setText("Cantidad de entradas");

        btnComprarEntrada.setText("COMPRAR");

        jLabel9.setText("MIS COMPRAS");

        tblMisCompras.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null}
            },
            new String [] {
                "Concierto", "Zona", "Cantidad", "Monto Total", "Estado"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.Integer.class, java.lang.Double.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, true, true
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tblMisCompras.setColumnSelectionAllowed(true);
        jScrollPane3.setViewportView(tblMisCompras);
        tblMisCompras.getColumnModel().getSelectionModel().setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);

        btnLiberarEntrada.setText("liberar");

        cmbConciertosCliente.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        jLabel10.setText("Seleccione Concierto:");

        jLabel11.setText("Puntos Acumulados: ");

        btnCerrarSesion.setText("Cerrar Sesión");

        lblPuntos.setText("0");

        chkAplicarPuntos.setText("Aplicar puntos de fidelidad (Disponibles: 0)");
        chkAplicarPuntos.setEnabled(false);

        lblDescuentoPuntos.setText(" ");
        lblTotal.setText("Total: —");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 462, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(59, 59, 59))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(48, 48, 48)
                                .addComponent(jLabel10)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(cmbConciertosCliente, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(49, 49, 49)
                                .addComponent(jLabelTipoTarjeta)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(cmbTipoTarjeta, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(lblRequisitosTarjeta))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jPanel1Layout.createSequentialGroup()
                                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addGroup(jPanel1Layout.createSequentialGroup()
                                                .addGap(39, 39, 39)
                                                .addComponent(jLabel2))
                                            .addGroup(jPanel1Layout.createSequentialGroup()
                                                .addGap(51, 51, 51)
                                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                    .addComponent(jLabel9)
                                                    .addGroup(jPanel1Layout.createSequentialGroup()
                                                        .addComponent(jLabel8)
                                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                        .addComponent(spnCantidadEntradas, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                                            .addGroup(jPanel1Layout.createSequentialGroup()
                                                .addGap(49, 49, 49)
                                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                                    .addGroup(jPanel1Layout.createSequentialGroup()
                                                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                                            .addGroup(jPanel1Layout.createSequentialGroup()
                                                                .addComponent(jLabel3)
                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED))
                                                            .addGroup(jPanel1Layout.createSequentialGroup()
                                                                .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addGap(39, 39, 39)))
                                                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                                            .addComponent(txtTarjNumero, javax.swing.GroupLayout.DEFAULT_SIZE, 172, Short.MAX_VALUE)
                                                            .addComponent(txtTarjCvv))
                                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                        .addComponent(jLabel5))
                                                    .addGroup(jPanel1Layout.createSequentialGroup()
                                                        .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, 121, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                        .addGap(24, 24, 24)))))
                                        .addGap(38, 38, 38))
                                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                        .addComponent(btnRegistrarTarjeta)
                                        .addGap(50, 50, 50)))
                                .addComponent(txtTarjFecha, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(300, 300, 300)
                .addComponent(btnComprarEntrada)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(313, 313, 313)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel11)
                .addGap(18, 18, 18)
                .addComponent(lblPuntos, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addGap(0, 119, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(118, 118, 118))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addComponent(btnLiberarEntrada)
                        .addGap(305, 305, 305))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addComponent(btnCerrarSesion)
                        .addGap(65, 65, 65))))
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(51, 51, 51)
                .addComponent(chkAplicarPuntos))
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(51, 51, 51)
                .addComponent(lblDescuentoPuntos)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(lblTotal))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jLabel11)
                    .addComponent(lblPuntos))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabelTipoTarjeta)
                    .addComponent(cmbTipoTarjeta, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblRequisitosTarjeta))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel3)
                        .addComponent(txtTarjNumero, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel5)
                        .addComponent(txtTarjFecha, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtTarjCvv, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel6))
                .addGap(22, 22, 22)
                .addComponent(btnRegistrarTarjeta)
                .addGap(18, 18, 18)
                .addComponent(jLabel7)
                .addGap(15, 15, 15)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel10)
                    .addComponent(cmbConciertosCliente, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(11, 11, 11)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 89, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel8)
                    .addComponent(spnCantidadEntradas, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(8, 8, 8)
                .addComponent(chkAplicarPuntos)
                .addGap(4, 4, 4)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblDescuentoPuntos)
                    .addComponent(lblTotal))
                .addGap(14, 14, 14)
                .addComponent(btnComprarEntrada)
                .addGap(17, 17, 17)
                .addComponent(jLabel9)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 94, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(btnLiberarEntrada)
                .addGap(18, 18, 18)
                .addComponent(btnCerrarSesion)
                .addContainerGap(59, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 86, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

// 1. Getters para que el Controlador escuche las acciones (Botones)
// btnRegistrarTarjeta ya no tiene getter: queda oculto (ver seccionTarjetaYPago) y sin
// controlador que lo escuche, "Comprar" es el único gatillo de tokenización.
public javax.swing.JButton getBtnComprarEntrada() { return btnComprarEntrada; }
public javax.swing.JButton getBtnLiberarEntrada() { return btnLiberarEntrada; }

// 2. Getters para los componentes de datos (Tablas y ComboBox)
public javax.swing.JComboBox<String> getCmbConciertosCliente() { return cmbConciertosCliente; }
public javax.swing.JTable getTblZonasDisponibles() { return tblZonasDisponibles; }
public javax.swing.JTable getTblMisCompras() { return tblMisCompras; }

// 3. Getters para leer lo que ingresa el cliente
public String getTarjNumero() { return txtTarjNumero.getText().trim(); }
public String getTarjFecha() { return txtTarjFecha.getText().trim(); }
public String getTarjCvv() { return txtTarjCvv.getText().trim(); }

// Tipo de tarjeta elegido primero por el usuario, y el label de ayuda con los dígitos requeridos
public javax.swing.JComboBox<String> getCmbTipoTarjeta() { return cmbTipoTarjeta; }
public void setRequisitosTarjeta(String texto) { lblRequisitosTarjeta.setText(texto); }

// Lee el valor numérico del JSpinner de forma segura
public int getCantidadEntradas() {
    return (int) spnCantidadEntradas.getValue();
}

public javax.swing.JSpinner getSpnCantidadEntradas() { return spnCantidadEntradas; }

// 4. Setters útiles para que el Controlador modifique la pantalla desde fuera
public void setPuntosAcumulados(int puntos) {
    lblPuntos.setText(String.valueOf(puntos));
}

public void limpiarFormularioTarjeta() {
    txtTarjNumero.setText("");
    txtTarjFecha.setText("");
    txtTarjCvv.setText("");
}
    
public javax.swing.JButton getBtnCerrarSesion() {
    return btnCerrarSesion;
}

// 5. Checkout con puntos de fidelidad
public javax.swing.JCheckBox getChkAplicarPuntos() { return chkAplicarPuntos; }

public void setCheckPuntosHabilitado(boolean habilitado, int puntosDisponibles, String motivoSiDeshabilitado) {
    chkAplicarPuntos.setText("Aplicar puntos de fidelidad (Disponibles: " + puntosDisponibles + ")");
    chkAplicarPuntos.setEnabled(habilitado);
    // Con el checkbox deshabilitado, el tooltip es la única forma de que el usuario sepa POR QUÉ
    // (sin puntos vs. puntos insuficientes para esta compra puntual), en vez de un bloqueo mudo.
    chkAplicarPuntos.setToolTipText(habilitado ? null : motivoSiDeshabilitado);
    if (!habilitado) {
        chkAplicarPuntos.setSelected(false);
    }
}

public boolean isAplicarPuntosSeleccionado() {
    return chkAplicarPuntos.isSelected();
}

public void setResumenCompra(String textoDescuentoPuntos, String textoTotal) {
    lblDescuentoPuntos.setText(textoDescuentoPuntos);
    lblTotal.setText(textoTotal);
}

public void limpiarResumenCompra() {
    setCheckPuntosHabilitado(false, 0, "Seleccione una zona y cantidad válidas primero.");
    lblDescuentoPuntos.setText(" ");
    lblTotal.setText("Total: —");
}
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnCerrarSesion;
    private javax.swing.JButton btnComprarEntrada;
    private javax.swing.JButton btnLiberarEntrada;
    private javax.swing.JButton btnRegistrarTarjeta;
    private javax.swing.JCheckBox chkAplicarPuntos;
    private javax.swing.JComboBox<String> cmbConciertosCliente;
    private javax.swing.JComboBox<String> cmbTipoTarjeta;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JLabel jLabelTipoTarjeta;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JTable jTable2;
    private javax.swing.JLabel lblDescuentoPuntos;
    private javax.swing.JLabel lblPuntos;
    private javax.swing.JLabel lblRequisitosTarjeta;
    private javax.swing.JLabel lblTotal;
    private javax.swing.JSpinner spnCantidadEntradas;
    private javax.swing.JTable tblMisCompras;
    private javax.swing.JTable tblZonasDisponibles;
    private javax.swing.JTextField txtTarjCvv;
    private javax.swing.JTextField txtTarjFecha;
    private javax.swing.JTextField txtTarjNumero;
    // End of variables declaration//GEN-END:variables
}
