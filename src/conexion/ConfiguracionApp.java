package conexion;

import java.io.File;
import java.net.URI;

/**
 * Resuelve la ubicación real de los archivos de configuración externos (config.properties),
 * de forma que funcionen tanto empaquetados en el .jar de distribución como ejecutados
 * directamente desde el IDE.
 * <p>
 * Un {@code new File("config.properties")} depende del directorio de trabajo (cwd) del proceso
 * que lanza la JVM: funciona por casualidad cuando se ejecuta "java -jar" parado en la carpeta
 * correcta, pero falla en cuanto el .jar se invoca desde otra ubicación (acceso directo, tarea
 * programada, doble clic desde el explorador). Esta clase ubica el .jar en disco usando su
 * {@code ProtectionDomain} y busca el archivo de configuración junto a él.
 */
public final class ConfiguracionApp {

    private ConfiguracionApp() {
    }

    /**
     * Devuelve el archivo {@code nombreArchivo} ubicado junto al .jar en ejecución. Si no existe
     * ahí (típicamente porque se está corriendo desde el IDE sin empaquetar, donde el
     * "código fuente" en ejecución es un directorio de .class y no un .jar), se hace fallback al
     * directorio de trabajo actual para no romper el flujo de desarrollo habitual en NetBeans.
     */
    public static File resolverArchivo(String nombreArchivo) {
        File juntoAlJar = new File(directorioDeInstalacion(), nombreArchivo);
        if (juntoAlJar.exists()) {
            return juntoAlJar;
        }
        return new File(nombreArchivo);
    }

    private static File directorioDeInstalacion() {
        try {
            URI ubicacionCodigo = ConfiguracionApp.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI();
            File archivoOCarpeta = new File(ubicacionCodigo);
            // Empaquetado: la CodeSource apunta al .jar -> nos quedamos con su carpeta contenedora.
            // Sin empaquetar (NetBeans "Run"): apunta a build/classes -> se usa tal cual.
            return archivoOCarpeta.isFile() ? archivoOCarpeta.getParentFile() : archivoOCarpeta;
        } catch (Exception ex) {
            return new File(".");
        }
    }
}
