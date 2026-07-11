package modelo;

// Se lanza cuando un intento de redención de puntos de fidelidad excede el límite de negocio
// (nunca más del 50% del valor de la compra: un cliente jamás puede llevarse una entrada gratis).
public class LimiteRedencionException extends Exception {
    public LimiteRedencionException(String message) {
        super(message);
    }
}
