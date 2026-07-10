package modelo;

import java.util.Date;

public class Venta {

    public enum EstadoVenta {
        PENDING, PAID, CANCELLED
    }

    private java.util.UUID id;
    private Date fecha;
    private int monto;
    private EstadoVenta estado;
    private Zona zona;
    private Entrada[] entradas;
    private String conciertoNombre;
    private String paymentTransactionId;

    public Venta(Date fecha, int monto, Zona zona, Entrada[] entradas, String conciertoNombre) {
        this.id = java.util.UUID.randomUUID();
        this.fecha = fecha;
        this.monto = monto;
        this.zona = zona;
        this.entradas = entradas;
        this.estado = EstadoVenta.PAID; // Pagado por defecto en la simulación
        this.conciertoNombre = conciertoNombre;
        this.paymentTransactionId = "TXN_" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public boolean anular() {
        boolean result = false;
        if (this.estado != EstadoVenta.CANCELLED) {
            for (Entrada entrada : this.entradas) {
                entrada.liberar();
            }
            this.estado = EstadoVenta.CANCELLED;
            result = true;
        }
        return result;
    }

    public Date getFecha() {
        return fecha;
    }

    public int getMonto() {
        return monto;
    }

    public boolean isAnulada() {
        return this.estado == EstadoVenta.CANCELLED;
    }

    public java.util.UUID getId() {
        return id;
    }

    public EstadoVenta getEstado() {
        return estado;
    }

    public String getPaymentTransactionId() {
        return paymentTransactionId;
    }

    public Zona getZona() {
        return zona;
    }

    public Entrada[] getEntradas() {
        return entradas;
    }

    public String getConciertoNombre() {
        return conciertoNombre;
    }
}