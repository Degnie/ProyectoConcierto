package modelo;

import java.util.ArrayList;
import java.util.Date;

public class Cliente extends Persona {
    private int puntos;
    private Tarjeta tarjeta;
    private ArrayList<Venta> ventas;
    private String paymentToken;

    public Cliente(String nombres, String apellidos, String dni, String contraseña, String correo) {
        super(nombres, apellidos, dni, contraseña, correo);
        this.puntos = 0;
        this.tarjeta = null;
        this.ventas = new ArrayList<>();
        this.paymentToken = null;
    }

    public boolean ingresar(String usuario, String clave) {
        boolean result = false;
        String hashedClave = Persona.hashPassword(clave);
        if (this.getDni().equals(usuario) && this.getContraseña().equals(hashedClave)) {
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

    public ArrayList<Venta> getVentas() {
        return ventas;
    }

    public String getPaymentToken() {
        return paymentToken;
    }

    public void setPaymentToken(String paymentToken) {
        this.paymentToken = paymentToken;
    }
}
