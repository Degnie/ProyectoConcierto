package controlador;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import modelo.Cliente;
import modelo.ClienteArreglo;
import modelo.Concierto;
import modelo.Zona;
import modelo.Venta;
import modelo.Tarjeta;
import modelo.TarjetaInvalidaException;
import modelo.TipoTarjeta;
import modelo.ArchivoConciertos;
import vista.FrmCliente;
import vista.FrmLogin;

public class ControladorCliente implements ActionListener {

    private FrmCliente vista;
    private Cliente clienteLogueado;
    private ClienteArreglo modeloClientes;
    private java.util.ArrayList<Concierto> listaConciertos;

    public ControladorCliente(FrmCliente vista, Cliente clienteLogueado, ClienteArreglo modeloClientes, java.util.ArrayList<Concierto> listaConciertos) {
        this.vista = vista;
        this.clienteLogueado = clienteLogueado;
        this.modeloClientes = modeloClientes;
        this.listaConciertos = listaConciertos;

        this.vista.getBtnRegistrarTarjeta().addActionListener(this);
        this.vista.getBtnComprarEntrada().addActionListener(this);
        this.vista.getBtnLiberarEntrada().addActionListener(this);
        this.vista.getBtnCerrarSesion().addActionListener(this);
        this.vista.getCmbConciertosCliente().addActionListener(this);
        this.vista.getCmbTipoTarjeta().addActionListener(this);

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
    }

    private void actualizarTablaCompras() {
        DefaultTableModel dtm = new DefaultTableModel(new Object[]{"Concierto", "Zona", "Cantidad", "Monto Total", "Estado"}, 0);
        
        if (clienteLogueado != null && clienteLogueado.getVentas() != null) {
            for (Venta v : clienteLogueado.getVentas()) {
                if (v != null) {
                    String nomZona = (v.getZona() != null) ? v.getZona().getNombre() : "Ubicación";
                    int montoTotal = v.getMonto();
                    int cantEntradas = (v.getEntradas() != null) ? v.getEntradas().length : 0;

                    // Mapeo nativo exitoso usando tu método isAnulada() real
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

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == vista.getCmbConciertosCliente()) {
            actualizarTablaZonas();
        }

        else if (e.getSource() == vista.getCmbTipoTarjeta()) {
            actualizarRequisitosTarjeta();
        }

        else if (e.getSource() == vista.getBtnRegistrarTarjeta()) {
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

            // El emisor elegido en el combo debe coincidir con el que se detecta por el número
            TipoTarjeta tipoDetectado = TipoTarjeta.detectar(nroTarjeta);
            if (tipoDetectado != tipoElegido) {
                JOptionPane.showMessageDialog(vista, "Seleccionaste " + tipoElegido + " pero el número ingresado corresponde a "
                        + (tipoDetectado == TipoTarjeta.DESCONOCIDA ? "un emisor no reconocido" : tipoDetectado) + ".");
                return;
            }

            try {
                // El constructor detecta el emisor (VISA/MASTERCARD/DINERS/AMEX), valida dígitos y Luhn
                Tarjeta tarjetaSegura = new Tarjeta(nroTarjeta, cvv, fechaVenc);

                // Simulación de tokenización PCI-DSS: el número completo y el CVV nunca se guardan
                String tokenPagoSimulado = "tok_" + java.util.UUID.randomUUID().toString().substring(0, 16);
                clienteLogueado.setPaymentToken(tokenPagoSimulado);
                clienteLogueado.registrarTarjeta(tarjetaSegura);

                JOptionPane.showMessageDialog(vista, "Tarjeta " + tarjetaSegura.getTipo() + " tokenizada con éxito (Token: " + tokenPagoSimulado + ").");
                vista.limpiarFormularioTarjeta();
            } catch (TarjetaInvalidaException ex) {
                JOptionPane.showMessageDialog(vista, ex.getMessage());
            }
        }

        else if (e.getSource() == vista.getBtnComprarEntrada()) {
            // Requerir explícitamente tarjeta tokenizada segura (PCI-DSS)
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
            if (cantidad < 1 || cantidad > 4) {
                JOptionPane.showMessageDialog(vista, "Permitido de 1 a 4 entradas por compra.");
                return;
            }

            Concierto conciertoSel = listaConciertos.get(conIdx);
            Zona zonaSel = conciertoSel.getZonas().get(zonIdx);

            if (zonaSel.getCantidadEntradasDisponibles() < cantidad) {
                JOptionPane.showMessageDialog(vista, "No quedan suficientes entradas disponibles en esta zona.");
                return;
            }

            // Llamada nativa al modelo de clases (el descuento por emisor se aplica dentro de comprar())
            boolean compraExitosa = clienteLogueado.comprar(zonaSel, cantidad, conciertoSel);

            if (compraExitosa) {
                try { 
                    ArchivoConciertos.guardarConciertos(listaConciertos); 
                } catch(Exception ex){}
                
                JOptionPane.showMessageDialog(vista, "¡Compra efectuada con éxito!");
                
                vista.setPuntosAcumulados(clienteLogueado.getPuntos());
                actualizarTablaZonas();
                actualizarTablaCompras();
            } else {
                JOptionPane.showMessageDialog(vista, "No se pudo procesar la compra de entradas.");
            }
        }

        else if (e.getSource() == vista.getBtnLiberarEntrada()) {
            int filaSel = vista.getTblMisCompras().getSelectedRow();
            if (filaSel < 0) {
                JOptionPane.showMessageDialog(vista, "Seleccione una compra de su lista para liberarla.");
                return;
            }
            
            int confirm = JOptionPane.showConfirmDialog(vista, 
                "¿Está seguro de que desea liberar esta entrada? Se le restarán los puntos correspondientes.", 
                "Confirmar liberación", JOptionPane.YES_NO_OPTION);
            
            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    Venta ventaALiberar = clienteLogueado.getVentas().get(filaSel);
                    if (clienteLogueado.anularVenta(ventaALiberar)) {
                        try { 
                            ArchivoConciertos.guardarConciertos(listaConciertos); 
                        } catch(Exception ex){}
                        
                        JOptionPane.showMessageDialog(vista, "Operación de liberación procesada correctamente.");
                        
                        vista.setPuntosAcumulados(clienteLogueado.getPuntos());
                        actualizarTablaZonas();
                        actualizarTablaCompras();
                    } else {
                        JOptionPane.showMessageDialog(vista, "La venta ya se encuentra anulada.");
                    }
                } catch(Exception ex) {
                    JOptionPane.showMessageDialog(vista, "No se pudo anular la venta.");
                }
            }
        }

        else if (e.getSource() == vista.getBtnCerrarSesion()) {
            FrmLogin login = new FrmLogin();
            new ControladorLogin(login, modeloClientes);
            login.setVisible(true);
            vista.dispose();
        }
    }
}