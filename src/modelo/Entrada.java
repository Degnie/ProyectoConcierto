package modelo;

import java.time.LocalDateTime;
import java.util.UUID;

public class Entrada {

    public enum EstadoEntrada {
        AVAILABLE, RESERVED, SOLD, BLOCKED, VALIDATED
    }

    private UUID id;
    private int numero;
    private EstadoEntrada estado;
    private LocalDateTime reservedUntil;
    private String signedQrToken;

    public Entrada(int numero) {
        this.id = UUID.randomUUID();
        this.numero = numero;
        this.estado = EstadoEntrada.AVAILABLE;
        this.reservedUntil = null;
        this.signedQrToken = null;
    }

    // Reconstrucción desde Oracle con id/estado ya conocidos (historial de compras vía JOIN, ver
    // OracleVentaRepository) — a diferencia del constructor de arriba, que siempre nace disponible.
    public Entrada(UUID id, int numero, EstadoEntrada estado) {
        this.id = id;
        this.numero = numero;
        this.estado = estado;
        this.reservedUntil = null;
        this.signedQrToken = null;
    }

    public boolean vender() {
        boolean result = false;
        if (this.estado == EstadoEntrada.AVAILABLE || this.estado == EstadoEntrada.RESERVED) {
            this.estado = EstadoEntrada.SOLD;
            this.signedQrToken = UUID.randomUUID().toString(); // Simula token de firma digital para QR
            this.reservedUntil = null;
            result = true;
        }
        return result;
    }

    public boolean reservar(int minutos) {
        boolean result = false;
        if (this.estado == EstadoEntrada.AVAILABLE) {
            this.estado = EstadoEntrada.RESERVED;
            this.reservedUntil = LocalDateTime.now().plusMinutes(minutos);
            result = true;
        }
        return result;
    }

    public boolean liberar() {
        boolean result = false;
        if (this.estado == EstadoEntrada.SOLD || this.estado == EstadoEntrada.RESERVED) {
            this.estado = EstadoEntrada.AVAILABLE;
            this.reservedUntil = null;
            this.signedQrToken = null;
            result = true;
        }
        return result;
    }

    // Paquete-privado a propósito: solo Zona debe usarlo, y solo al reconstruir su estado desde
    // Oracle en el arranque (ver Zona#marcarEntradaVendida). No es una venta real (no pasa por
    // vender()); es forzar en memoria un hecho que la base de datos ya registró antes.
    void marcarComoVendidaReconstruida(UUID idPersistido) {
        this.id = idPersistido;
        this.estado = EstadoEntrada.SOLD;
    }

    public UUID getId() {
        return id;
    }

    public int getNumero() {
        return numero;
    }

    // Retorna el Enum para lógica profesional
    public EstadoEntrada getEstadoEnum() {
        return estado;
    }

    public LocalDateTime getReservedUntil() {
        return reservedUntil;
    }

    public String getSignedQrToken() {
        return signedQrToken;
    }

    // Mantiene compatibilidad de strings con el código existente
    public String getEstado() {
        switch (estado) {
            case AVAILABLE: return "DISPONIBLE";
            case RESERVED: return "RESERVADA";
            case SOLD: return "VENDIDA";
            case BLOCKED: return "BLOQUEADA";
            case VALIDATED: return "VALIDADA";
            default: return "DISPONIBLE";
        }
    }
}
