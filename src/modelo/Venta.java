package modelo;

import java.util.Date;

public class Venta {

    public enum EstadoVenta {
        PENDING, PAID, CANCELLED
    }

    private static final int SOLES_POR_PUNTO_REDIMIDO = 10; // 10 puntos = 1 sol/dólar de descuento
    private static final int SOLES_POR_PUNTO_GANADO = 10;   // 1 punto por cada 10 soles/dólares netos pagados
    private static final double TOPE_REDENCION = 0.5;       // nunca más del 50% del valor de la compra

    private java.util.UUID id;
    private Date fecha;
    private int monto; // monto neto ya con el descuento por puntos aplicado
    private EstadoVenta estado;
    private Zona zona;
    private Entrada[] entradas;
    private String conciertoNombre;
    private String paymentTransactionId;
    private int puntosRedimidos;
    private int puntosGanados;

    // montoBruto ya incluye el descuento por tipo de tarjeta (regla existente en Concierto), antes
    // de aplicar el canje de puntos. La validación del tope de redención vive acá: es la única
    // fuente de verdad de esta regla de negocio, ningún controlador la recalcula.
    public Venta(Date fecha, int montoBruto, Zona zona, Entrada[] entradas, String conciertoNombre, int puntosRedimidos)
            throws LimiteRedencionException {
        if (puntosRedimidos < 0) {
            throw new IllegalArgumentException("Los puntos a redimir no pueden ser negativos.");
        }
        int montoDescuentoPuntos = puntosRedimidos / SOLES_POR_PUNTO_REDIMIDO;
        if (montoDescuentoPuntos > montoBruto * TOPE_REDENCION) {
            throw new LimiteRedencionException(
                "El descuento por puntos no puede superar el " + (int) (TOPE_REDENCION * 100)
                + "% del valor de la compra (una entrada nunca puede salir gratis).");
        }

        this.id = java.util.UUID.randomUUID();
        this.fecha = fecha;
        this.monto = montoBruto - montoDescuentoPuntos;
        this.zona = zona;
        this.entradas = entradas;
        this.estado = EstadoVenta.PAID; // Pagado por defecto en la simulación
        this.conciertoNombre = conciertoNombre;
        this.paymentTransactionId = "TXN_" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.puntosRedimidos = puntosRedimidos;
        this.puntosGanados = this.monto / SOLES_POR_PUNTO_GANADO;
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

    public int getPuntosRedimidos() {
        return puntosRedimidos;
    }

    public int getPuntosGanados() {
        return puntosGanados;
    }
}
