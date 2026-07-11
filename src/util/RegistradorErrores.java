package util;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Punto único de trazabilidad para excepciones capturadas en operaciones asíncronas
 * (SwingWorker). El usuario ve un JOptionPane amigable; esta clase además deja un rastro en
 * System.err y en un archivo físico ({@value #NOMBRE_ARCHIVO_LOG}, en el directorio de trabajo
 * actual), para poder auditar fallas en producción sin depender de una consola visible — si la
 * app se ejecuta con javaw.exe (sin consola), System.err es efectivamente invisible.
 */
public final class RegistradorErrores {
    private static final String NOMBRE_ARCHIVO_LOG = "errores_app.log";
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private RegistradorErrores() {
    }

    public static void registrar(String contexto, Throwable ex) {
        System.err.println("[" + contexto + "] " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
        ex.printStackTrace();
        registrarEnArchivo(contexto, ex);
    }

    // Un fallo al escribir el log no debe tumbar la app ni ocultar el error original: se traga la
    // IOException acá mismo (ya se avisó por System.err arriba) en vez de propagarla.
    private static void registrarEnArchivo(String contexto, Throwable ex) {
        try (FileWriter fw = new FileWriter(NOMBRE_ARCHIVO_LOG, true);
             PrintWriter pw = new PrintWriter(fw)) {
            pw.println("[" + LocalDateTime.now().format(FORMATO_FECHA) + "] [" + contexto + "] "
                    + ex.getClass().getSimpleName() + ": " + ex.getMessage());
            ex.printStackTrace(pw);
            pw.println();
        } catch (IOException escritura) {
            System.err.println("No se pudo escribir en " + NOMBRE_ARCHIVO_LOG + ": " + escritura.getMessage());
        }
    }
}
