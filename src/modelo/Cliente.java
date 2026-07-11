package modelo;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class Cliente extends Persona {
    private int puntos;
    private Tarjeta tarjeta;
    private ArrayList<Venta> ventas;
    private String paymentToken;

    public Cliente(String nombres, String apellidos, String dni, String contrasenaHash, String salt,
                    String correo, LocalDate fechaNacimiento)
            throws DniInvalidoException, CorreoInvalidoException, EdadInvalidaException {
        super(nombres, apellidos, dni, contrasenaHash, salt, correo, fechaNacimiento);
        this.puntos = 0;
        this.tarjeta = null;
        this.ventas = new ArrayList<>();
        this.paymentToken = null;
    }

    // clave se sobrescribe con ceros apenas se calcula el hash, no queda en memoria más de lo necesario
    public boolean ingresar(String usuario, char[] clave) {
        boolean result = false;
        String hashIngresado = Persona.hashPassword(clave, this.getSalt());
        Arrays.fill(clave, '0');
        if (this.getDni().equals(usuario) && this.getContrasenaHash().equals(hashIngresado)) {
            result = true;
        }
        return result;
    }

    public boolean registrarTarjeta(Tarjeta tarjeta) {
        this.tarjeta = tarjeta;
        return true;
    }

    public boolean eliminarTarjeta() {
        boolean result = false;
        if (this.tarjeta != null) {
            this.tarjeta = null;
            result = true;
        }
        return result;
    }

    // Reversa exacta de comprar(): devuelve los puntos gastados en la redención y retira los que
    // esa compra había generado. Los montos vienen de la propia Venta (única fuente de verdad),
    // no se recalculan acá.
    public boolean anularVenta(Venta venta) {
        boolean result = false;
        if (this.ventas.contains(venta)) {
            result = venta.anular();
            if (result) {
                this.puntos = Math.max(0, this.puntos - venta.getPuntosGanados() + venta.getPuntosRedimidos());
            }
        }
        return result;
    }

    public boolean comprar(Zona zona, int cantidad, Concierto concierto) throws ZonaAgotadaException, LimiteRedencionException {
        return comprar(zona, cantidad, concierto, 0);
    }

    // puntosARedimir: canje de fidelidad (10 puntos = 1 sol de descuento), tope 50% del valor de
    // la compra validado dentro de Venta. La reserva de entradas y la venta se tratan como una
    // sola unidad: si algo falla a mitad de camino, se liberan las entradas ya reservadas.
    public boolean comprar(Zona zona, int cantidad, Concierto concierto, int puntosARedimir)
            throws ZonaAgotadaException, LimiteRedencionException {
        boolean result = false;
        if (this.tarjeta != null && cantidad >= 1 && cantidad <= 4
                && puntosARedimir >= 0 && puntosARedimir <= this.puntos) {
            Entrada[] entradas = new Entrada[cantidad];
            int reservadas = 0;
            try {
                for (int i = 0; i < cantidad; i++) {
                    entradas[i] = zona.comprarEntrada(this);
                    reservadas++;
                }
                double descuentoTarjeta = concierto.getDescuento(this.tarjeta.getTipo());
                int montoBruto = (int) Math.round(zona.getPrecio() * cantidad * (1 - descuentoTarjeta));
                Venta venta = new Venta(new Date(), montoBruto, zona, entradas, concierto.getNombre(), puntosARedimir);
                this.ventas.add(venta);
                this.puntos = this.puntos - venta.getPuntosRedimidos() + venta.getPuntosGanados();
                result = true;
            } catch (ZonaAgotadaException | LimiteRedencionException ex) {
                for (int i = 0; i < reservadas; i++) {
                    entradas[i].liberar();
                }
                throw ex;
            }
        }
        return result;
    }

    public int getPuntos() {
        return puntos;
    }

    public void setPuntos(int puntos) {
        this.puntos = puntos;
    }

    public Tarjeta getTarjeta() {
        return tarjeta;
    }

    public List<Venta> getVentas() {
        return new ArrayList<>(ventas);
    }

    public String getPaymentToken() {
        return paymentToken;
    }

    public void setPaymentToken(String paymentToken) {
        this.paymentToken = paymentToken;
    }
}
