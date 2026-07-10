package modelo;

public class Tarjeta {
    private TipoTarjeta tipo;
    private String numeroEnmascarado;
    private String fecha;

    public Tarjeta(String numeroCompleto, String cvv, String fecha) throws TarjetaInvalidaException {
        this.tipo = TipoTarjeta.detectar(numeroCompleto);
        if (this.tipo == TipoTarjeta.DESCONOCIDA) {
            throw new TarjetaInvalidaException("Emisor no reconocido. Solo se aceptan VISA, MASTERCARD, DINERS o AMERICAN EXPRESS.");
        }
        if (!this.tipo.digitosValidos(numeroCompleto.length())) {
            throw new TarjetaInvalidaException(this.tipo + " no admite un número de " + numeroCompleto.length() + " dígitos.");
        }
        if (!pasaLuhn(numeroCompleto)) {
            throw new TarjetaInvalidaException("El número de tarjeta no pasa la validación de Luhn.");
        }
        if (cvv.length() != this.tipo.getDigitosCvv()) {
            throw new TarjetaInvalidaException(this.tipo + " requiere un CVV de " + this.tipo.getDigitosCvv() + " dígitos.");
        }

        this.fecha = fecha;
        this.numeroEnmascarado = "**** **** **** " + numeroCompleto.substring(numeroCompleto.length() - 4);
    }

    // Algoritmo de Luhn (mod-10), el mismo checksum que usan las redes de tarjetas reales
    private static boolean pasaLuhn(String numero) {
        int suma = 0;
        boolean alternar = false;
        for (int i = numero.length() - 1; i >= 0; i--) {
            int digito = numero.charAt(i) - '0';
            if (alternar) {
                digito *= 2;
                if (digito > 9) digito -= 9;
            }
            suma += digito;
            alternar = !alternar;
        }
        return suma % 10 == 0;
    }

    public TipoTarjeta getTipo() {
        return tipo;
    }

    public String getNumeroEnmascarado() {
        return numeroEnmascarado;
    }

    public String getFecha() {
        return fecha;
    }
}
