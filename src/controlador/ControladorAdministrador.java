package controlador;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import modelo.Concierto;
import modelo.Zona;
import modelo.ClienteArreglo;
import modelo.Cliente;
import modelo.Venta;
import modelo.ArchivoConciertos;
import vista.FrmAdministrador;
import vista.DlgNuevoConcierto;
import vista.DlgEditarZona; 
import vista.FrmLogin;

public class ControladorAdministrador implements ActionListener {

    private FrmAdministrador vista;
    private ClienteArreglo modeloClientes;
    private java.util.ArrayList<Concierto> listaConciertos; 

    public ControladorAdministrador(FrmAdministrador vista, ClienteArreglo modeloClientes, java.util.ArrayList<Concierto> listaConciertos) {
        this.vista = vista;
        this.modeloClientes = modeloClientes;
        this.listaConciertos = listaConciertos;

        // Registrar listeners de la vista
        this.vista.getBtnNuevoConcierto().addActionListener(this);
        this.vista.getBtnAgregarZona().addActionListener(this);
        this.vista.getBtnEditarSeleccion().addActionListener(this); 
        this.vista.getBtnCerrarSesion().addActionListener(this);
        this.vista.getBtnRegresar().addActionListener(this); 
        this.vista.getCmbConciertos().addActionListener(this);

        inicializarComboBox();
        actualizarTablas();
    }

    private void inicializarComboBox() {
        vista.getCmbConciertos().removeActionListener(this);
        vista.getCmbConciertos().removeAllItems();
        
        if (listaConciertos != null && !listaConciertos.isEmpty()) {
            for (Concierto c : listaConciertos) {
                vista.getCmbConciertos().addItem(c.getNombre());
            }
        }
        vista.getCmbConciertos().addActionListener(this);
    }

    private void actualizarTablas() {
        int selIdx = vista.getCmbConciertos().getSelectedIndex();
        DefaultTableModel dtmZonas = new DefaultTableModel(new Object[]{"nombre", "capacidad", "precio"}, 0);
        
        if (selIdx >= 0 && listaConciertos != null && selIdx < listaConciertos.size()) {
            Concierto conciertoActual = listaConciertos.get(selIdx);
            
            // CORRECCIÓN: getZonas() es un ArrayList<Zona>, lo recorremos con un for-each nativo
            if (conciertoActual.getZonas() != null) {
                for (Zona z : conciertoActual.getZonas()) {
                    if (z != null) {
                        dtmZonas.addRow(new Object[]{
                            z.getNombre(), 
                            z.getCapacidad(), 
                            z.getPrecio()
                        });
                    }
                }
            }
        }
        vista.getTblZonas().setModel(dtmZonas);
        vista.getTblZonas().revalidate();
        vista.getTblZonas().repaint();

        // Actualizar tabla global de ventas realizadas
        DefaultTableModel dtmVentas = new DefaultTableModel(new Object[]{"cliente", "zona", "monto", "concierto"}, 0);
        if (modeloClientes != null && modeloClientes.getClientes() != null) {
            for (Cliente c : modeloClientes.getClientes()) {
                if (c != null && c.getVentas() != null) {
                    for (Venta v : c.getVentas()) {
                        String nombreConcierto = (v.getConciertoNombre() != null) ? v.getConciertoNombre() : "Evento";
                        dtmVentas.addRow(new Object[]{
                            c.getNombres() + " " + c.getApellidos(), 
                            v.getZona().getNombre(), 
                            v.getMonto(), 
                            nombreConcierto
                        });
                    }
                }
            }
        }
        vista.getTblVentas().setModel(dtmVentas);
        vista.getTblVentas().revalidate();
        vista.getTblVentas().repaint();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == vista.getCmbConciertos()) {
            actualizarTablas();
        }
        
        else if (e.getSource() == vista.getBtnNuevoConcierto()) {
            DlgNuevoConcierto dlg = new DlgNuevoConcierto(vista, true);
            
            dlg.getBtnGuardarConcierto().addActionListener(ev -> {
                String nombre = dlg.getNuevoNombre();
                String fechaStr = dlg.getNuevoFecha();
                
                if(nombre == null || nombre.isEmpty() || fechaStr == null || fechaStr.isEmpty()) {
                    JOptionPane.showMessageDialog(dlg, "Campos obligatorios vacíos.");
                    return;
                }
                try {
                    Date fecha = new SimpleDateFormat("dd/MM/yyyy").parse(fechaStr);
                    listaConciertos.add(new Concierto(nombre, fecha));
                    
                    ArchivoConciertos.guardarConciertos(listaConciertos);
                    
                    JOptionPane.showMessageDialog(dlg, "Concierto guardado con éxito.");
                    inicializarComboBox();
                    dlg.dispose();
                    actualizarTablas();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(dlg, "Formato de fecha inválido (use dd/MM/yyyy).");
                }
            });
            
            dlg.getBtnCancelarConcierto().addActionListener(ev -> dlg.dispose());
            dlg.setVisible(true);
        }
        
        else if (e.getSource() == vista.getBtnAgregarZona()) {
            int selIdx = vista.getCmbConciertos().getSelectedIndex();
            if (selIdx < 0 || listaConciertos == null || selIdx >= listaConciertos.size()) {
                JOptionPane.showMessageDialog(vista, "Por favor, cree o seleccione un concierto primero.");
                return;
            }
            
            String nombre = vista.getZonaNombre();
            String precioStr = vista.getZonaPrecio();
            String capStr = vista.getZonaCapacidad(); 
            
            if (nombre.isEmpty() || precioStr.isEmpty() || capStr.isEmpty()) {
                JOptionPane.showMessageDialog(vista, "Complete todos los datos de la zona.");
                return;
            }
            
            try {
                int precio = Integer.parseInt(precioStr);
                int capacidad = Integer.parseInt(capStr);
                
                Concierto c = listaConciertos.get(selIdx);
                c.agregarZona(nombre, capacidad, precio);
                
                ArchivoConciertos.guardarConciertos(listaConciertos);
                
                JOptionPane.showMessageDialog(vista, "Zona agregada con éxito.");
                vista.limpiarFormularioZona();
                actualizarTablas(); 
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(vista, "Precio y Capacidad deben ser valores numéricos enteros.");
            }
        }

        else if (e.getSource() == vista.getBtnEditarSeleccion()) {
            int conIdx = vista.getCmbConciertos().getSelectedIndex();
            int zonIdx = vista.getTblZonas().getSelectedRow();
            
            if (conIdx < 0 || zonIdx < 0) {
                JOptionPane.showMessageDialog(vista, "Seleccione un concierto y una zona de la tabla para editar.");
                return;
            }
            
            Concierto conciertoSel = listaConciertos.get(conIdx);
            
            // CORRECCIÓN: Accedemos usando .get() de ArrayList de manera limpia
            Zona zonaSel = conciertoSel.getZonas().get(zonIdx);
            
            DlgEditarZona dlg = new DlgEditarZona(vista, true);
            
            dlg.setNombreZona(zonaSel.getNombre());
            dlg.setPrecioZona(String.valueOf(zonaSel.getPrecio()));
            dlg.setCapacidadZona(String.valueOf(zonaSel.getCapacidad()));
            
            dlg.getBtnGuardarEdicion().addActionListener(ev -> {
                try {
                    String nuevoNom = dlg.getNombreZona();
                    int nuevoPre = Integer.parseInt(dlg.getPrecioZona());
                    int nuevoCap = Integer.parseInt(dlg.getCapacidadZona());
                    
                    // CORRECCIÓN: Modificamos la posición usando .set() propio de los ArrayList
                    conciertoSel.getZonas().set(zonIdx, new Zona(nuevoNom, nuevoCap, nuevoPre));
                    
                    ArchivoConciertos.guardarConciertos(listaConciertos);
                    JOptionPane.showMessageDialog(dlg, "Zona modificada con éxito.");
                    dlg.dispose();
                    actualizarTablas();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(dlg, "Campos inválidos detectados.");
                }
            });
            
            dlg.getBtnCancelarEdicion().addActionListener(ev -> dlg.dispose());
            dlg.setVisible(true);
        }
        
        else if (e.getSource() == vista.getBtnCerrarSesion() || e.getSource() == vista.getBtnRegresar()) {
            FrmLogin login = new FrmLogin();
            new ControladorLogin(login, modeloClientes);
            login.setVisible(true);
            vista.dispose(); 
        }
    }
}