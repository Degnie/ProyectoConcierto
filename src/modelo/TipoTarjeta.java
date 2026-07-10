package modelo;

public enum TipoTarjeta {
    VISA(0.05, 3, new int[]{13, 16, 19}),
    MASTERCARD(0.10, 3, new int[]{16}),
    DINERS(0.15, 3, new int[]{14}),
    AMEX(0.07, 4, new int[]{15}),
    DESCONOCIDA(0.0, 3, new int[]{});

    private final double descuentoPorDefecto;
    private final int digitosCvv;
    private final int[] digitosValidos;

    TipoTarjeta(double descuentoPorDefecto, int digitosCvv, int[] digitosValidos) {
        this.descuentoPorDefecto = descuentoPorDefecto;
        this.digitosCvv = digitosCvv;
        this.digitosValidos = digitosValidos;
    }

    public double getDescuentoPorDefecto() {
        return descuentoPorDefecto;
    }

    public int getDigitosCvv() {
        return digitosCvv;
    }

    public boolean digitosValidos(int cantidad) {
        for (int d : digitosValidos) {
            if (d == cantidad) return true;
        }
        return false;
    }

    // Texto de ayuda para mostrar en el formulario (ej. "16 dígitos, CVV de 3 dígitos")
    public String describirRequisitos() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < digitosValidos.length; i++) {
            if (i > 0) sb.append(i == digitosValidos.length - 1 ? " o " : ", ");
            sb.append(digitosValidos[i]);
        }
        sb.append(" dígitos, CVV de ").append(digitosCvv).append(" dígitos");
        return sb.toString();
    }

    // Detección por prefijo (rangos IIN/BIN estándar de la industria)
    public static TipoTarjeta detectar(String numero) {
        if (numero.matches("^4\\d*")) return VISA;
        if (numero.matches("^(5[1-5]\\d{2}|222[1-9]|22[3-9]\\d|2[3-6]\\d{2}|27[01]\\d|2720)\\d*")) return MASTERCARD;
        if (numero.matches("^(30[0-5]|36|38)\\d*")) return DINERS;
        if (numero.matches("^(34|37)\\d*")) return AMEX;
        return DESCONOCIDA;
    }
}
