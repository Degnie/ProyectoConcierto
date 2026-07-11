package controlador;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import modelo.Concierto;
import modelo.Zona;
import repositorio.ClienteRepository;
import repositorio.ConciertoRepository;
import repositorio.VentaRepository;
import vista.FrmAdministrador;
import vista.DlgNuevoConcierto;
import vista.DlgEditarZona;
import vista.FrmPrincipal;

public class ControladorAdministrador implements ActionListener {

    private final FrmPrincipal principal;
    private final FrmAdministrador vista;
    private final ClienteRepository clienteRepository;
    private final java.util.ArrayList<Concierto> listaConciertos;
    private final ConciertoRepository conciertoRepository;
    private final VentaRepository ventaRepository;

    public ControladorAdministrador(FrmPrincipal principal, FrmAdministrador vista, ClienteRepository clienteRepository,
                                     java.util.ArrayList<Concierto> listaConciertos, ConciertoRepository conciertoRepository,
                                     VentaRepository ventaRepository) {
        this.principal = principal;
        this.vista = vista;
        this.clienteRepository = clienteRepository;
        this.listaConciertos = listaConciertos;
        this.conciertoRepository = conciertoRepository;
        this.ventaRepository = ventaRepository;

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
            if (conciertoActual.getZonas() != null) {
                for (Zona z : conciertoActual.getZonas()) {
                    if (z != null) {
                        dtmZonas.addRow(new Object[]{z.getNombre(), z.getCapacidad(), z.getPrecio()});
                    }
                }
            }
        }
        vista.getTblZonas().setModel(dtmZonas);
        vista.getTblZonas().revalidate();
        vista.getTblZonas().repaint();

        // Lee directo de Oracle (JOIN, ver OracleVentaRepository.cargarResumenVentasParaAdmin):
        // Cliente.getVentas() solo está hidratado para el cliente con sesión activa, no sirve acá.
        DefaultTableModel dtmVentas = new DefaultTableModel(new Object[]{"cliente", "zona", "monto", "concierto"}, 0);
        if (ventaRepository != null) {
            for (Object[] fila : ventaRepository.cargarResumenVentasParaAdmin()) {
                dtmVentas.addRow(fila);
            }
        }
        vista.getTblVentas().setModel(dtmVentas);
        vista.getTblVentas().revalidate();
        vista.getTblVentas().repaint();
    }

    // Persistir en Oracle es I/O de red: se ejecuta en background para no congelar el EDT
    private void guardarConciertosEnSegundoPlano(Runnable alTerminar) {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                for (Concierto c : listaConciertos) {
                    conciertoRepository.save(c);
                }
                return null;
            }

            @Override
            protected void done() {
                alTerminar.run();
            }
        }.execute();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == vista.getCmbConciertos()) {
            actualizarTablas();
        }

        else if (e.getSource() == vista.getBtnNuevoConcierto()) {
            DlgNuevoConcierto dlg = new DlgNuevoConcierto(principal, true);

            dlg.getBtnGuardarConcierto().addActionListener(ev -> {
                String nombre = dlg.getNuevoNombre();
                String fechaStr = dlg.getNuevoFecha();

                if (nombre == null || nombre.isEmpty() || fechaStr == null || fechaStr.isEmpty()) {
                    JOptionPane.showMessageDialog(dlg, "Campos obligatorios vacíos.");
                    return;
                }
                try {
                    Date fecha = new SimpleDateFormat("dd/MM/yyyy").parse(fechaStr);
                    listaConciertos.add(new Concierto(nombre, fecha));

                    guardarConciertosEnSegundoPlano(() -> {
                        JOptionPane.showMessageDialog(vista, "Concierto guardado con éxito.");
                        inicializarComboBox();
                        actualizarTablas();
                    });
                    dlg.dispose();
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

                guardarConciertosEnSegundoPlano(() -> {
                    JOptionPane.showMessageDialog(vista, "Zona agregada con éxito.");
                    vista.limpiarFormularioZona();
                    actualizarTablas();
                });
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
            Zona zonaSel = conciertoSel.getZonas().get(zonIdx);

            DlgEditarZona dlg = new DlgEditarZona(principal, true);

            dlg.setNombreZona(zonaSel.getNombre());
            dlg.setPrecioZona(String.valueOf(zonaSel.getPrecio()));
            dlg.setCapacidadZona(String.valueOf(zonaSel.getCapacidad()));

            dlg.getBtnGuardarEdicion().addActionListener(ev -> {
                try {
                    String nuevoNom = dlg.getNombreZona();
                    int nuevoPre = Integer.parseInt(dlg.getPrecioZona());
                    int nuevoCap = Integer.parseInt(dlg.getCapacidadZona());

                    conciertoSel.getZonas().set(zonIdx, new Zona(nuevoNom, nuevoCap, nuevoPre));

                    guardarConciertosEnSegundoPlano(() -> {
                        JOptionPane.showMessageDialog(vista, "Zona modificada con éxito.");
                        actualizarTablas();
                    });
                    dlg.dispose();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(dlg, "Campos inválidos detectados.");
                }
            });

            dlg.getBtnCancelarEdicion().addActionListener(ev -> dlg.dispose());
            dlg.setVisible(true);
        }

        else if (e.getSource() == vista.getBtnCerrarSesion() || e.getSource() == vista.getBtnRegresar()) {
            principal.mostrarLogin();
        }
    }
}
