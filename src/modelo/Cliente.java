package modelo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class Cliente extends Persona {
    private int puntos;
    private Tarjeta tarjeta;
    private ArrayList<Venta> ventas;
    private String paymentToken;

    public Cliente(String nombres, String apellidos, String dni, String contrasenaHash, String salt, String correo) {
        super(nombres, apellidos, dni, contrasenaHash, salt, correo);
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

    public boolean anularVenta(Venta venta) {
        boolean result = false;
        if (this.ventas.contains(venta)) {
            int cantidadEntradas = (venta.getEntradas() != null) ? venta.getEntradas().length : 0;
            result = venta.anular();
            if (result) {
                this.puntos = Math.max(0, this.puntos - (cantidadEntradas * 10));
            }
        }
        return result;
    }

    public boolean comprar(Zona zona, int cantidad, Concierto concierto) {
        boolean result = false;
        if (this.tarjeta != null && cantidad >= 1 && cantidad <= 4) {
            Entrada[] entradas = zona.venderEntrada(cantidad);
            if (entradas != null && entradas.length == cantidad) {
                double descuento = concierto.getDescuento(this.tarjeta.getTipo());
                int monto = (int) Math.round(zona.getPrecio() * cantidad * (1 - descuento));
                Venta venta = new Venta(new Date(), monto, zona, entradas, concierto.getNombre());
                this.ventas.add(venta);
                this.puntos += cantidad * 10;
                result = true;
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
