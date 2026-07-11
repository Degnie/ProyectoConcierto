package controlador;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
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
import repositorio.VentaRepository;
import util.RegistradorErrores;
import vista.FrmCliente;
import vista.FrmPrincipal;

public class ControladorCliente implements ActionListener {

    private static final int CANTIDAD_MAXIMA_POR_COMPRA = 4;
    // SimpleDateFormat no es thread-safe, pero acá solo se usa desde el EDT (nunca dentro de un
    // SwingWorker de fondo), así que una única instancia compartida es segura.
    private static final SimpleDateFormat FORMATO_FECHA = new SimpleDateFormat("dd/MM/yyyy");

    private final FrmPrincipal principal;
    private final FrmCliente vista;
    private final Cliente clienteLogueado;
    private final ClienteRepository clienteRepository;
    private final java.util.ArrayList<Concierto> listaConciertos;
    private final ConciertoRepository conciertoRepository;
    private final VentaRepository ventaRepository;

    public ControladorCliente(FrmPrincipal principal, FrmCliente vista, Cliente clienteLogueado,
                               ClienteRepository clienteRepository, java.util.ArrayList<Concierto> listaConciertos,
                               ConciertoRepository conciertoRepository, VentaRepository ventaRepository) {
        this.principal = principal;
        this.vista = vista;
        this.clienteLogueado = clienteLogueado;
        this.clienteRepository = clienteRepository;
        this.listaConciertos = listaConciertos;
        this.conciertoRepository = conciertoRepository;
        this.ventaRepository = ventaRepository;

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

    // Refleja si ya hay una tarjeta lista para pagar (recordada de una sesión anterior guardada en
    // Oracle, o registrada en esta sesión): el cliente no necesita volver a llenar el formulario
    // para comprar, salvo que quiera cambiar de tarjeta.
    private void actualizarIndicadorTarjeta() {
        Tarjeta tarjeta = clienteLogueado.getTarjeta();
        if (tarjeta != null) {
            vista.mostrarTarjetaActiva("Tarjeta activa: " + tarjeta.getTipo() + " " + tarjeta.getNumeroEnmascarado());
        } else {
            vista.ocultarTarjetaActiva();
        }
    }

    // El primer ítem del combo es un placeholder ("Elija el tipo de tarjeta"), no un TipoTarjeta
    // real -- sin esto, el combo arrancaría en VISA y su descuento ya se aplicaría al total antes
    // de que el cliente eligiera nada, como si fuera un regalo no pedido. Cualquier lugar que lea
    // el combo pasa por acá en vez de TipoTarjeta.valueOf(...) directo.
    private TipoTarjeta obtenerTipoTarjetaSeleccionado() {
        String seleccion = (String) vista.getCmbTipoTarjeta().getSelectedItem();
        if (seleccion == null || FrmCliente.PLACEHOLDER_TIPO_TARJETA.equals(seleccion)) {
            return null;
        }
        return TipoTarjeta.valueOf(seleccion);
    }

    // Muestra cuántos dígitos exige la marca elegida en el combo (VISA/MASTERCARD/DINERS/AMEX)
    private void actualizarRequisitosTarjeta() {
        TipoTarjeta tipo = obtenerTipoTarjetaSeleccionado();
        vista.setRequisitosTarjeta(tipo != null ? tipo.describirRequisitos() : " ");
    }

    private void inicializarFormulario() {
        vista.setNombreCliente(clienteLogueado.getNombres());
        vista.setPuntosAcumulados(clienteLogueado.getPuntos());
        vista.limpiarResumenCompra();
        actualizarIndicadorTarjeta();

        // El combo de tipo de tarjeta es la única fuente de verdad para el descuento en preview
        // (ver actualizarResumenCompra); si ya hay una tarjeta activa (recordada de una sesión
        // anterior), el combo arranca alineado con ella para que el preview y el cobro real
        // coincidan desde el primer momento, en vez de mostrar el descuento del primer ítem
        // (VISA) mientras la tarjeta real es de otro tipo.
        if (clienteLogueado.getTarjeta() != null) {
            vista.getCmbTipoTarjeta().setSelectedItem(clienteLogueado.getTarjeta().getTipo().name());
        }

        vista.getCmbConciertosCliente().removeActionListener(this);
        vista.getCmbConciertosCliente().removeAllItems();
        if (listaConciertos != null && !listaConciertos.isEmpty()) {
            for (Concierto c : listaConciertos) {
                // Con el mismo tour repetido varias veces (ej. 3 fechas de un mismo artista), el
                // nombre solo no alcanza para distinguir cuál es cuál en el combo.
                vista.getCmbConciertosCliente().addItem(c.getNombre() + " — " + FORMATO_FECHA.format(c.getFecha()));
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

    // Preview del checkout: sin zona/concierto seleccionados, o con una cantidad fuera del rango
    // permitido (1-4), no hay nada realizable que mostrar. El descuento de tarjeta siempre sigue
    // al combo (cmbTipoTarjeta) — es la única fuente de verdad, tanto para el preview como para el
    // cobro real: actionPerformed invalida la tarjeta activa apenas el combo deja de coincidir con
    // ella (ver la rama de cmbTipoTarjeta), así el preview de acá nunca puede prometer un total
    // que después el cobro real no respete. Con datos válidos, calcula el descuento de tarjeta, el
    // techo de puntos redimibles para esta compra puntual, y el total final según si el checkbox
    // está marcado o no. Todo esto vive como fórmulas puras en Venta
    // (calcularDescuentoTarjeta/calcularMaximoPuntosRedimibles/calcularTotalFinal) — acá solo se
    // leen los datos de la UI y se pintan los resultados.
    private void actualizarResumenCompra() {
        int conIdx = vista.getCmbConciertosCliente().getSelectedIndex();
        int zonIdx = vista.getTblZonasDisponibles().getSelectedRow();
        int cantidad = vista.getCantidadEntradas();

        if (conIdx < 0 || conIdx >= listaConciertos.size() || zonIdx < 0
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

        // Sin tipo de tarjeta elegido todavía (placeholder), se muestra el precio de lista (0%
        // descuento) en vez de nada — así el total aparece apenas hay zona+cantidad, tal como
        // pide el embudo, sin prometer un descuento que el cliente todavía no eligió.
        TipoTarjeta tipoParaDescuento = obtenerTipoTarjetaSeleccionado();
        double descuentoTarjeta = tipoParaDescuento != null ? conciertoSel.getDescuento(tipoParaDescuento) : 0.0;
        int montoConDescuentoTarjeta = Venta.calcularMontoConDescuentoTarjeta(zonaSel.getPrecio(), cantidad, descuentoTarjeta);
        int maxPuntosRedimibles = Venta.calcularMaximoPuntosRedimibles(montoConDescuentoTarjeta, clienteLogueado.getPuntos());

        // Si al recalcular ya no alcanza el tope (cambió cantidad/zona), se deshabilita y desmarca
        // solo; el usuario nunca ve un checkbox marcado prometiendo un descuento que ya no aplica.
        // El tooltip explica el motivo puntual, no solo "está deshabilitado".
        String motivoDeshabilitado = clienteLogueado.getPuntos() <= 0
            ? "Todavía no acumulaste puntos de fidelidad."
            : "Tus puntos no alcanzan para esta compra (saldo insuficiente).";
        vista.setCheckPuntosHabilitado(maxPuntosRedimibles > 0, clienteLogueado.getPuntos(), motivoDeshabilitado);

        boolean aplicarPuntos = vista.isAplicarPuntosSeleccionado() && maxPuntosRedimibles > 0;
        int puntosARedimir = aplicarPuntos ? maxPuntosRedimibles : 0;
        int total = Venta.calcularTotalFinal(montoConDescuentoTarjeta, puntosARedimir, aplicarPuntos);

        String textoDescuento = aplicarPuntos
            ? "Descuento por puntos: -S/ " + Venta.calcularDescuentoPorPuntos(puntosARedimir) + " (" + puntosARedimir + " pts)"
            : " ";
        vista.setResumenCompra(textoDescuento, "Total: S/ " + total);
    }

    // Toda escritura a la BD corre fuera del EDT para no congelar la UI; los errores de la
    // transacción quedan registrados (System.err + error.log) además del diálogo al usuario.
    // iniciarCarga()/finalizarCarga() envuelven todo el ciclo (cursor de espera + botones
    // deshabilitados) para mitigar el doble clic mientras la operación está en vuelo.
    private void guardarEnSegundoPlano(String contexto, Runnable trabajoDeBd, Runnable alTerminar) {
        guardarEnSegundoPlano(contexto, trabajoDeBd, alTerminar, null);
    }

    // Variante con compensación: si trabajoDeBd falla, alFallar corre antes del diálogo de error,
    // para deshacer en memoria (ej. puntos de fidelidad) lo que comprar() ya había aplicado
    // optimistamente y que Oracle nunca llegó a persistir.
    private void guardarEnSegundoPlano(String contexto, Runnable trabajoDeBd, Runnable alTerminar, Runnable alFallar) {
        principal.iniciarCarga();
        new SwingWorker<Void, Void>() {
            private Exception fallo;

            @Override
            protected Void doInBackground() {
                try {
                    trabajoDeBd.run();
                } catch (Exception ex) {
                    fallo = ex;
                }
                return null;
            }

            @Override
            protected void done() {
                principal.finalizarCarga();
                if (fallo != null) {
                    RegistradorErrores.registrar(contexto, fallo);
                    if (alFallar != null) alFallar.run();
                    JOptionPane.showMessageDialog(vista, "Ocurrió un error al guardar en la base de datos: " + fallo.getMessage());
                    return;
                }
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
            // Si ya había una tarjeta activa de OTRO tipo, cambiar el combo significa "quiero
            // pagar con un tipo distinto": se invalida (no se borra el guardado en Oracle, solo la
            // activa en memoria) para que la próxima compra tokenice la nueva en vez de cobrar en
            // silencio con el descuento de una tarjeta que ya dejó de ser la seleccionada.
            Tarjeta tarjetaActiva = clienteLogueado.getTarjeta();
            TipoTarjeta tipoElegido = obtenerTipoTarjetaSeleccionado();
            if (tarjetaActiva != null && tipoElegido != null && tarjetaActiva.getTipo() != tipoElegido) {
                clienteLogueado.eliminarTarjeta();
                clienteLogueado.setPaymentToken(null);
                actualizarIndicadorTarjeta();
            }
            actualizarRequisitosTarjeta();
            actualizarResumenCompra();
        } else if (e.getSource() == vista.getBtnComprarEntrada()) {
            procesarCompraEntrada();
        } else if (e.getSource() == vista.getBtnLiberarEntrada()) {
            procesarLiberacionEntrada();
        } else if (e.getSource() == vista.getBtnCerrarSesion()) {
            principal.mostrarLogin();
        }
    }

    // Punto único que garantiza una tarjeta lista para pagar antes de confirmar la compra. No hay
    // botón separado de "Registrar Tarjeta" (sistema de compra al instante: no tiene sentido un
    // trámite aparte solo para guardar una tarjeta sin comprar nada) — "Comprar" es el único
    // gatillo de tokenización:
    //  - Si el cliente escribió algo en el número de tarjeta, quiere pagar con una tarjeta nueva o
    //    distinta a la activa (si había una): se tokeniza esa, sin importar si ya había otra.
    //  - Si el campo está vacío y ya hay una tarjeta activa (de esta sesión o recordada de una
    //    anterior), se usa esa tal cual.
    //  - Si el campo está vacío y no hay ninguna activa, intentarRegistrarTarjeta() dispara el
    //    mensaje de "complete los datos de la tarjeta".
    private boolean asegurarTarjetaActiva() {
        boolean quiereOtraTarjeta = !vista.getTarjNumero().isEmpty();
        if (!quiereOtraTarjeta && clienteLogueado.getPaymentToken() != null) {
            return true;
        }
        return intentarRegistrarTarjeta();
    }

    // Devuelve true si al terminar hay una tarjeta tokenizada y activa.
    private boolean intentarRegistrarTarjeta() {
        TipoTarjeta tipoElegido = obtenerTipoTarjetaSeleccionado();
        if (tipoElegido == null) {
            JOptionPane.showMessageDialog(vista, "Elija el tipo de tarjeta antes de continuar.");
            return false;
        }

        String nroTarjeta = vista.getTarjNumero();
        String fechaVenc = vista.getTarjFecha();
        String cvv = vista.getTarjCvv();

        if (nroTarjeta.isEmpty() || fechaVenc.isEmpty() || cvv.isEmpty()) {
            JOptionPane.showMessageDialog(vista, "Complete los datos de la tarjeta para pagar.");
            return false;
        }

        if (!nroTarjeta.matches("\\d+") || !cvv.matches("\\d+")) {
            JOptionPane.showMessageDialog(vista, "El número de tarjeta y el CVV deben ser numéricos.");
            return false;
        }

        if (!fechaVenc.matches("\\d{2}/\\d{2}")) {
            JOptionPane.showMessageDialog(vista, "La fecha de vencimiento debe tener el formato MM/AA (ej. 12/28).");
            return false;
        }

        TipoTarjeta tipoDetectado = TipoTarjeta.detectar(nroTarjeta);
        if (tipoDetectado != tipoElegido) {
            JOptionPane.showMessageDialog(vista, "Seleccionaste " + tipoElegido + " pero el número ingresado corresponde a "
                    + (tipoDetectado == TipoTarjeta.DESCONOCIDA ? "un emisor no reconocido" : tipoDetectado) + ".");
            return false;
        }

        try {
            Tarjeta tarjetaSegura = new Tarjeta(nroTarjeta, cvv, fechaVenc);
            String tokenPagoSimulado = "tok_" + java.util.UUID.randomUUID().toString().substring(0, 16);
            clienteLogueado.setPaymentToken(tokenPagoSimulado);
            clienteLogueado.registrarTarjeta(tarjetaSegura);

            JOptionPane.showMessageDialog(vista, "Tarjeta " + tarjetaSegura.getTipo() + " tokenizada con éxito (Token: " + tokenPagoSimulado + ").");
            vista.limpiarFormularioTarjeta();
            actualizarIndicadorTarjeta();
            actualizarResumenCompra();

            // "Guardar para futuras compras": persiste tipo/enmascarado/fecha/token en Oracle para
            // que la próxima sesión ya arranque con esta tarjeta activa (ver mapear() en
            // OracleClienteRepository). Sin marcar el check, la tarjeta solo vive en esta sesión,
            // igual que antes.
            if (vista.isGuardarTarjetaSeleccionado()) {
                guardarEnSegundoPlano("ControladorCliente.procesarRegistroTarjeta.guardarTarjeta",
                        () -> clienteRepository.guardarTarjeta(clienteLogueado.getDni(), tarjetaSegura, tokenPagoSimulado),
                        () -> {});
            }
            return true;
        } catch (TarjetaInvalidaException ex) {
            JOptionPane.showMessageDialog(vista, ex.getMessage());
            return false;
        }
    }

    private void procesarCompraEntrada() {
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

        // Última validación, justo antes de calcular el monto: si todavía no hay una tarjeta
        // activa, se intenta tokenizar con lo que haya en el formulario (mismo tipo elegido en el
        // combo que ya se usó para el preview del total) — el cliente no necesita un clic previo
        // en "Registrar Tarjeta" para el caso común de pagar con una tarjeta nueva.
        if (!asegurarTarjetaActiva()) {
            return;
        }

        // Mismo cálculo que usó el preview reactivo (Venta.calcularMaximoPuntosRedimibles):
        // si el checkbox está marcado se redime el máximo permitido para esta compra puntual.
        double descuentoTarjeta = conciertoSel.getDescuento(clienteLogueado.getTarjeta().getTipo());
        int montoConDescuentoTarjeta = Venta.calcularMontoConDescuentoTarjeta(zonaSel.getPrecio(), cantidad, descuentoTarjeta);
        int maxPuntosRedimibles = Venta.calcularMaximoPuntosRedimibles(montoConDescuentoTarjeta, clienteLogueado.getPuntos());
        int puntosARedimir = vista.isAplicarPuntosSeleccionado() ? maxPuntosRedimibles : 0;

        // La reserva de asientos y el tope de redención de puntos son invariantes del modelo
        // (Zona/Venta); el controlador solo traduce sus excepciones a un mensaje. comprar() ya
        // reservó las entradas en memoria (synchronized) y devuelve la Venta lista para persistir.
        Venta ventaCreada;
        try {
            ventaCreada = clienteLogueado.comprar(zonaSel, cantidad, conciertoSel, puntosARedimir);
        } catch (ZonaAgotadaException | LimiteRedencionException ex) {
            JOptionPane.showMessageDialog(vista, ex.getMessage());
            return;
        }

        if (ventaCreada != null) {
            guardarEnSegundoPlano("ControladorCliente.procesarCompraEntrada",
                () -> ventaRepository.guardarCompraCompleta(clienteLogueado, conciertoSel, ventaCreada),
                () -> {
                    JOptionPane.showMessageDialog(vista, "¡Compra efectuada con éxito!");
                    vista.setPuntosAcumulados(clienteLogueado.getPuntos());
                    actualizarTablaZonas();
                    actualizarTablaCompras();
                },
                // Compensación en RAM: comprar() ya descontó puntos y reservó entradas en la Zona
                // compartida antes de intentar persistir. Si Oracle no confirmó la venta, anularla
                // en memoria evita que la UI (puntos, disponibilidad) quede desincronizada de la BD.
                () -> {
                    clienteLogueado.anularVenta(ventaCreada);
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
            JOptionPane.showMessageDialog(vista, "Seleccione una compra de su lista para anularla.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(vista,
            "¿Está seguro de que desea anular esta compra? Se le restarán los puntos correspondientes.",
            "Confirmar anulación", JOptionPane.YES_NO_OPTION);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            Venta ventaALiberar = clienteLogueado.getVentas().get(filaSel);
            if (clienteLogueado.anularVenta(ventaALiberar)) {
                guardarEnSegundoPlano("ControladorCliente.procesarLiberacionEntrada",
                    () -> ventaRepository.anularVentaPersistida(clienteLogueado, ventaALiberar),
                    () -> {
                        JOptionPane.showMessageDialog(vista, "Compra anulada correctamente.");
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
}
