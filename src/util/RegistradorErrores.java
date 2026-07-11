package util;

/**
 * Punto único de trazabilidad para excepciones capturadas en operaciones asíncronas
 * (SwingWorker). El usuario ve un JOptionPane amigable; esta clase deja además un rastro en
 * System.err (consola/log del proceso) para poder auditar fallas en producción sin depender
 * únicamente de que alguien haya visto el diálogo interactivo en el momento del error.
 */
public final class RegistradorErrores {

    private RegistradorErrores() {
    }

    public static void registrar(String contexto, Throwable ex) {
        System.err.println("[" + contexto + "] " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
        ex.printStackTrace();
    }
}
