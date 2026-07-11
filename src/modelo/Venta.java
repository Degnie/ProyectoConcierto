package modelo;

import java.util.Date;
import java.util.UUID;

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
    private boolean aplicoPuntos;

    // montoBruto ya incluye el descuento por tipo de tarjeta (regla existente en Concierto), antes
    // de aplicar el canje de puntos. La validación del tope de redención vive acá: es la única
    // fuente de verdad de esta regla de negocio, ningún controlador la recalcula.
    public Venta(Date fecha, int montoBruto, Zona zona, Entrada[] entradas, String conciertoNombre, int puntosRedimidos)
            throws LimiteRedencionException {
        if (puntosRedimidos < 0) {
            throw new IllegalArgumentException("Los puntos a redimir no pueden ser negativos.");
        }
        int montoDescuentoPuntos = calcularDescuentoPorPuntos(puntosRedimidos);
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
        this.aplicoPuntos = puntosRedimidos > 0;
    }

    private Venta() {
        // Solo para reconstruirDesdeBaseDeDatos(...): una venta histórica ya pasó esta validación
        // el día que se creó de verdad; releerla desde Oracle no debe volver a exigirla.
    }

    // Reconstruye en memoria una venta que Oracle ya tiene registrada (hidratación del historial
    // del cliente, ver OracleVentaRepository). A propósito no pasa por el constructor público: no
    // tiene sentido re-verificar el tope de redención de un hecho histórico ya consumado, y hacerlo
    // introduciría una forma silenciosa de perder ventas viejas si la regla de negocio cambiara.
    public static Venta reconstruirDesdeBaseDeDatos(UUID id, Date fecha, int monto, Zona zona, Entrada[] entradas,
            String conciertoNombre, String paymentTransactionId, EstadoVenta estado,
            int puntosRedimidos, int puntosGanados) {
        Venta venta = new Venta();
        venta.id = id;
        venta.fecha = fecha;
        venta.monto = monto;
        venta.zona = zona;
        venta.entradas = entradas;
        venta.conciertoNombre = conciertoNombre;
        venta.paymentTransactionId = paymentTransactionId;
        venta.estado = estado;
        venta.puntosRedimidos = puntosRedimidos;
        venta.puntosGanados = puntosGanados;
        venta.aplicoPuntos = puntosRedimidos > 0;
        return venta;
    }

    // ===================== Calculadora reactiva (sin efectos secundarios, sin persistir nada) =====================
    // Estos métodos son los que usa la UI de checkout para mostrar un preview en caliente del total
    // a pagar mientras el cliente marca/desmarca "usar puntos", sin necesidad de intentar la compra
    // real. El constructor de arriba usa exactamente las mismas fórmulas (calcularDescuentoPorPuntos),
    // así que la regla del tope del 50% nunca puede quedar desincronizada entre el preview y la venta
    // real.

    // Descuento en soles/dólares que aporta el tipo de tarjeta sobre el precio de lista.
    public static int calcularDescuentoTarjeta(int precioUnitario, int cantidad, double porcentajeDescuentoTarjeta) {
        int precioLista = precioUnitario * cantidad;
        return precioLista - calcularMontoConDescuentoTarjeta(precioUnitario, cantidad, porcentajeDescuentoTarjeta);
    }

    // Monto ya neto del descuento por tarjeta (antes de aplicar puntos) — es el "montoBruto" que
    // recibe el constructor de Venta.
    public static int calcularMontoConDescuentoTarjeta(int precioUnitario, int cantidad, double porcentajeDescuentoTarjeta) {
        return (int) Math.round(precioUnitario * cantidad * (1 - porcentajeDescuentoTarjeta));
    }

    // Techo de puntos que el cliente puede aplicar a ESTA compra: el menor entre lo que tiene
    // acumulado y lo que representa el 50% del monto ya con descuento de tarjeta.
    public static int calcularMaximoPuntosRedimibles(int montoConDescuentoTarjeta, int puntosDisponiblesCliente) {
        int topeMonetario = (int) (montoConDescuentoTarjeta * TOPE_REDENCION);
        int puntosParaTope = topeMonetario * SOLES_POR_PUNTO_REDIMIDO;
        return Math.max(0, Math.min(puntosDisponiblesCliente, puntosParaTope));
    }

    public static int calcularDescuentoPorPuntos(int puntosARedimir) {
        return puntosARedimir / SOLES_POR_PUNTO_REDIMIDO;
    }

    // Total final a pagar: si aplicarPuntos es false, ignora puntosARedimir y devuelve el monto con
    // solo el descuento de tarjeta (el mismo comportamiento que un checkbox desmarcado en la UI).
    public static int calcularTotalFinal(int montoConDescuentoTarjeta, int puntosARedimir, boolean aplicarPuntos) {
        if (!aplicarPuntos || puntosARedimir <= 0) {
            return montoConDescuentoTarjeta;
        }
        return montoConDescuentoTarjeta - calcularDescuentoPorPuntos(puntosARedimir);
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

    public boolean isAplicoPuntos() {
        return aplicoPuntos;
    }
}
