package controlador;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import modelo.Cliente;
import modelo.Concierto;
import modelo.Zona;
import modelo.Venta;
import modelo.Tarjeta;
import modelo.TarjetaInvalidaException;
import modelo.TipoTarjeta;
import modelo.ZonaAgotadaException;
import modelo.LimiteRedencionException;
import repositorio.ClienteRepository;
import repositorio.ConciertoRepository;
import vista.FrmCliente;
import vista.FrmPrincipal;

public class ControladorCliente implements ActionListener {

    private static final int CANTIDAD_MAXIMA_POR_COMPRA = 4;

    private final FrmPrincipal principal;
    private final FrmCliente vista;
    private final Cliente clienteLogueado;
    private final ClienteRepository clienteRepository;
    private final java.util.ArrayList<Concierto> listaConciertos;
    private final ConciertoRepository conciertoRepository;

    public ControladorCliente(FrmPrincipal principal, FrmCliente vista, Cliente clienteLogueado,
                               ClienteRepository clienteRepository, java.util.ArrayList<Concierto> listaConciertos,
                               ConciertoRepository conciertoRepository) {
        this.principal = principal;
        this.vista = vista;
        this.clienteLogueado = clienteLogueado;
        this.clienteRepository = clienteRepository;
        this.listaConciertos = listaConciertos;
        this.conciertoRepository = conciertoRepository;

        this.vista.getBtnRegistrarTarjeta().addActionListener(this);
        this.vista.getBtnComprarEntrada().addActionListener(this);
        this.vista.getBtnLiberarEntrada().addActionListener(this);
        this.vista.getBtnCerrarSesion().addActionListener(this);
        this.vista.getCmbConciertosCliente().addActionListener(this);
        this.vista.getCmbTipoTarjeta().addActionListener(this);

        // Todo lo que sigue es cálculo puro en memoria (Venta.calcular...), así que se recalcula
        // de forma síncrona en el EDT ante cualquier cambio de selección: no amerita SwingWorker,
        // eso se reserva para la persistencia real en Oracle al confirmar la compra.
        this.vista.getTblZonasDisponibles().getSelectionModel().addListSelectionListener(ev -> {
            if (!ev.getValueIsAdjusting()) actualizarResumenCompra();
        });
        this.vista.getSpnCantidadEntradas().addChangeListener(ev -> actualizarResumenCompra());
        this.vista.getChkAplicarPuntos().addItemListener(ev -> actualizarResumenCompra());

        inicializarFormulario();
        actualizarRequisitosTarjeta();
    }

    // Muestra cuántos dígitos exige la marca elegida en el combo (VISA/MASTERCARD/DINERS/AMEX)
    private void actualizarRequisitosTarjeta() {
        String seleccion = (String) vista.getCmbTipoTarjeta().getSelectedItem();
        TipoTarjeta tipo = TipoTarjeta.valueOf(seleccion);
        vista.setRequisitosTarjeta(tipo.describirRequisitos());
    }

    private void inicializarFormulario() {
        vista.setPuntosAcumulados(clienteLogueado.getPuntos());
        vista.limpiarResumenCompra();

        vista.getCmbConciertosCliente().removeActionListener(this);
        vista.getCmbConciertosCliente().removeAllItems();
        if (listaConciertos != null && !listaConciertos.isEmpty()) {
            for (Concierto c : listaConciertos) {
                vista.getCmbConciertosCliente().addItem(c.getNombre());
            }
        }
        vista.getCmbConciertosCliente().addActionListener(this);

        actualizarTablaZonas();
        actualizarTablaCompras();
    }

    private void actualizarTablaZonas() {
        int selIdx = vista.getCmbConciertosCliente().getSelectedIndex();
        DefaultTableModel dtm = new DefaultTableModel(new Object[]{"Zona", "Precio", "Disponibles"}, 0);

        if (selIdx >= 0 && listaConciertos != null && selIdx < listaConciertos.size()) {
            Concierto conciertoActual = listaConciertos.get(selIdx);
            if (conciertoActual.getZonas() != null) {
                for (Zona z : conciertoActual.getZonas()) {
                    if (z != null) {
                        dtm.addRow(new Object[]{
                            z.getNombre(),
                            z.getPrecio(),
                            z.getCantidadEntradasDisponibles()
                        });
                    }
                }
            }
        }
        vista.getTblZonasDisponibles().setModel(dtm);
        actualizarResumenCompra();
    }

    private void actualizarTablaCompras() {
        DefaultTableModel dtm = new DefaultTableModel(new Object[]{"Concierto", "Zona", "Cantidad", "Monto Total", "Estado"}, 0);

        if (clienteLogueado != null && clienteLogueado.getVentas() != null) {
            for (Venta v : clienteLogueado.getVentas()) {
                if (v != null) {
                    String nomZona = (v.getZona() != null) ? v.getZona().getNombre() : "Ubicación";
                    int montoTotal = v.getMonto();
                    int cantEntradas = (v.getEntradas() != null) ? v.getEntradas().length : 0;
                    String estadoVenta = v.isAnulada() ? "Anulado" : "Pagado";

                    dtm.addRow(new Object[]{
                        v.getConciertoNombre() != null ? v.getConciertoNombre() : "Evento",
                        nomZona,
                        cantEntradas,
                        montoTotal,
                        estadoVenta
                    });
                }
            }
        }
        vista.getTblMisCompras().setModel(dtm);
    }

    // Preview del checkout: sin tarjeta registrada, sin zona/concierto seleccionados, o con una
    // cantidad fuera del rango permitido (1-4), no hay nada realizable que mostrar. Con datos
    // válidos, calcula el descuento de tarjeta, el techo de puntos redimibles para esta compra
    // puntual, y el total final según si el checkbox está marcado o no. Todo esto vive como
    // fórmulas puras en Venta (calcularDescuentoTarjeta/calcularMaximoPuntosRedimibles/
    // calcularTotalFinal) — acá solo se leen los datos de la UI y se pintan los resultados.
    private void actualizarResumenCompra() {
        Tarjeta tarjeta = clienteLogueado.getTarjeta();
        int conIdx = vista.getCmbConciertosCliente().getSelectedIndex();
        int zonIdx = vista.getTblZonasDisponibles().getSelectedRow();
        int cantidad = vista.getCantidadEntradas();

        if (tarjeta == null || conIdx < 0 || conIdx >= listaConciertos.size() || zonIdx < 0
                || cantidad < 1 || cantidad > CANTIDAD_MAXIMA_POR_COMPRA) {
            vista.limpiarResumenCompra();
            return;
        }
        Concierto conciertoSel = listaConciertos.get(conIdx);
        if (zonIdx >= conciertoSel.getZonas().size()) {
            vista.limpiarResumenCompra();
            return;
        }
        Zona zonaSel = conciertoSel.getZonas().get(zonIdx);

        double descuentoTarjeta = conciertoSel.getDescuento(tarjeta.getTipo());
        int montoConDescuentoTarjeta = Venta.calcularMontoConDescuentoTarjeta(zonaSel.getPrecio(), cantidad, descuentoTarjeta);
        int maxPuntosRedimibles = Venta.calcularMaximoPuntosRedimibles(montoConDescuentoTarjeta, clienteLogueado.getPuntos());

        // Si al recalcular ya no alcanza el tope (cambió cantidad/zona), se deshabilita y desmarca
        // solo; el usuario nunca ve un checkbox marcado prometiendo un descuento que ya no aplica.
        vista.setCheckPuntosHabilitado(maxPuntosRedimibles > 0, clienteLogueado.getPuntos());

        boolean aplicarPuntos = vista.isAplicarPuntosSeleccionado() && maxPuntosRedimibles > 0;
        int puntosARedimir = aplicarPuntos ? maxPuntosRedimibles : 0;
        int total = Venta.calcularTotalFinal(montoConDescuentoTarjeta, puntosARedimir, aplicarPuntos);

        String textoDescuento = aplicarPuntos
            ? "Descuento por puntos: -S/ " + Venta.calcularDescuentoPorPuntos(puntosARedimir) + " (" + puntosARedimir + " pts)"
            : " ";
        vista.setResumenCompra(textoDescuento, "Total: S/ " + total);
    }

    // Toda escritura a la BD (cliente y/o concierto) corre fuera del EDT para no congelar la UI
    private void guardarEnSegundoPlano(Runnable trabajoDeBd, Runnable alTerminar) {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                trabajoDeBd.run();
                return null;
            }

            @Override
            protected void done() {
                alTerminar.run();
            }
        }.execute();
    }

    // actionPerformed queda como simple enrutador: cada rama delega a un método con nombre
    // semántico, sin lógica propia acá más allá de decidir a quién le toca.
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == vista.getCmbConciertosCliente()) {
            actualizarTablaZonas();
        } else if (e.getSource() == vista.getCmbTipoTarjeta()) {
            actualizarRequisitosTarjeta();
        } else if (e.getSource() == vista.getBtnRegistrarTarjeta()) {
            procesarRegistroTarjeta();
        } else if (e.getSource() == vista.getBtnComprarEntrada()) {
            procesarCompraEntrada();
        } else if (e.getSource() == vista.getBtnLiberarEntrada()) {
            procesarLiberacionEntrada();
        } else if (e.getSource() == vista.getBtnCerrarSesion()) {
            principal.mostrarLogin();
        }
    }

    private void procesarRegistroTarjeta() {
        String nroTarjeta = vista.getTarjNumero();
        String fechaVenc = vista.getTarjFecha();
        String cvv = vista.getTarjCvv();
        TipoTarjeta tipoElegido = TipoTarjeta.valueOf((String) vista.getCmbTipoTarjeta().getSelectedItem());

        if (nroTarjeta.isEmpty() || fechaVenc.isEmpty() || cvv.isEmpty()) {
            JOptionPane.showMessageDialog(vista, "Por favor, complete todos los campos de la tarjeta.");
            return;
        }

        if (!nroTarjeta.matches("\\d+") || !cvv.matches("\\d+")) {
            JOptionPane.showMessageDialog(vista, "El número de tarjeta y el CVV deben ser numéricos.");
            return;
        }

        if (!fechaVenc.matches("\\d{2}/\\d{2}")) {
            JOptionPane.showMessageDialog(vista, "La fecha de vencimiento debe tener el formato MM/AA (ej. 12/28).");
            return;
        }

        TipoTarjeta tipoDetectado = TipoTarjeta.detectar(nroTarjeta);
        if (tipoDetectado != tipoElegido) {
            JOptionPane.showMessageDialog(vista, "Seleccionaste " + tipoElegido + " pero el número ingresado corresponde a "
                    + (tipoDetectado == TipoTarjeta.DESCONOCIDA ? "un emisor no reconocido" : tipoDetectado) + ".");
            return;
        }

        try {
            Tarjeta tarjetaSegura = new Tarjeta(nroTarjeta, cvv, fechaVenc);
            String tokenPagoSimulado = "tok_" + java.util.UUID.randomUUID().toString().substring(0, 16);
            clienteLogueado.setPaymentToken(tokenPagoSimulado);
            clienteLogueado.registrarTarjeta(tarjetaSegura);

            JOptionPane.showMessageDialog(vista, "Tarjeta " + tarjetaSegura.getTipo() + " tokenizada con éxito (Token: " + tokenPagoSimulado + ").");
            vista.limpiarFormularioTarjeta();
            actualizarResumenCompra();
        } catch (TarjetaInvalidaException ex) {
            JOptionPane.showMessageDialog(vista, ex.getMessage());
        }
    }

    private void procesarCompraEntrada() {
        if (clienteLogueado.getPaymentToken() == null) {
            JOptionPane.showMessageDialog(vista, "Para realizar una compra, primero debe asociar una tarjeta de pago tokenizada.");
            return;
        }

        int conIdx = vista.getCmbConciertosCliente().getSelectedIndex();
        int zonIdx = vista.getTblZonasDisponibles().getSelectedRow();

        if (conIdx < 0 || zonIdx < 0) {
            JOptionPane.showMessageDialog(vista, "Seleccione un concierto y una zona de la lista.");
            return;
        }

        int cantidad = vista.getCantidadEntradas();
        if (cantidad < 1 || cantidad > CANTIDAD_MAXIMA_POR_COMPRA) {
            JOptionPane.showMessageDialog(vista, "Permitido de 1 a " + CANTIDAD_MAXIMA_POR_COMPRA + " entradas por compra.");
            return;
        }

        Concierto conciertoSel = listaConciertos.get(conIdx);
        Zona zonaSel = conciertoSel.getZonas().get(zonIdx);

        if (zonaSel.getCantidadEntradasDisponibles() < cantidad) {
            JOptionPane.showMessageDialog(vista, "No quedan suficientes entradas disponibles en esta zona.");
            return;
        }

        // Mismo cálculo que usó el preview reactivo (Venta.calcularMaximoPuntosRedimibles):
        // si el checkbox está marcado se redime el máximo permitido para esta compra puntual.
        double descuentoTarjeta = conciertoSel.getDescuento(clienteLogueado.getTarjeta().getTipo());
        int montoConDescuentoTarjeta = Venta.calcularMontoConDescuentoTarjeta(zonaSel.getPrecio(), cantidad, descuentoTarjeta);
        int maxPuntosRedimibles = Venta.calcularMaximoPuntosRedimibles(montoConDescuentoTarjeta, clienteLogueado.getPuntos());
        int puntosARedimir = vista.isAplicarPuntosSeleccionado() ? maxPuntosRedimibles : 0;

        // La reserva de asientos y el tope de redención de puntos son invariantes del modelo
        // (Zona/Venta); el controlador solo traduce sus excepciones a un mensaje.
        boolean compraExitosa;
        try {
            compraExitosa = clienteLogueado.comprar(zonaSel, cantidad, conciertoSel, puntosARedimir);
        } catch (ZonaAgotadaException | LimiteRedencionException ex) {
            JOptionPane.showMessageDialog(vista, ex.getMessage());
            return;
        }

        if (compraExitosa) {
            guardarEnSegundoPlano(() -> {
                clienteRepository.save(clienteLogueado);
                conciertoRepository.save(conciertoSel);
            }, () -> {
                JOptionPane.showMessageDialog(vista, "¡Compra efectuada con éxito!");
                vista.setPuntosAcumulados(clienteLogueado.getPuntos());
                actualizarTablaZonas();
                actualizarTablaCompras();
            });
        } else {
            JOptionPane.showMessageDialog(vista, "No se pudo procesar la compra de entradas.");
        }
    }

    private void procesarLiberacionEntrada() {
        int filaSel = vista.getTblMisCompras().getSelectedRow();
        if (filaSel < 0) {
            JOptionPane.showMessageDialog(vista, "Seleccione una compra de su lista para liberarla.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(vista,
            "¿Está seguro de que desea liberar esta entrada? Se le restarán los puntos correspondientes.",
            "Confirmar liberación", JOptionPane.YES_NO_OPTION);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            Venta ventaALiberar = clienteLogueado.getVentas().get(filaSel);
            if (clienteLogueado.anularVenta(ventaALiberar)) {
                Concierto conciertoDeVenta = buscarConciertoDeZona(ventaALiberar.getZona());
                guardarEnSegundoPlano(() -> {
                    clienteRepository.save(clienteLogueado);
                    if (conciertoDeVenta != null) {
                        conciertoRepository.save(conciertoDeVenta);
                    }
                }, () -> {
                    JOptionPane.showMessageDialog(vista, "Operación de liberación procesada correctamente.");
                    vista.setPuntosAcumulados(clienteLogueado.getPuntos());
                    actualizarTablaZonas();
                    actualizarTablaCompras();
                });
            } else {
                JOptionPane.showMessageDialog(vista, "La venta ya se encuentra anulada.");
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(vista, "No se pudo anular la venta.");
        }
    }

    private Concierto buscarConciertoDeZona(Zona zona) {
        for (Concierto c : listaConciertos) {
            if (c.getZonas().contains(zona)) {
                return c;
            }
        }
        return null;
    }
}
