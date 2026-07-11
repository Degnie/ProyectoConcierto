package util;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache singleton de {@link FlatSVGIcon}. Cada ícono SVG en {@code /resources/icons/} se
 * rasteriza una sola vez y se reutiliza la misma instancia en toda la UI (login, checkout,
 * tabla de compras, etc.) en vez de recrear el ícono cada vez que un panel se repinta o
 * reconstruye, lo que evitaría relecturas de disco y recodificaciones repetidas del SVG.
 */
public final class IconRegistry {

    public static final String LOCK = "lock";
    public static final String CARD = "card";
    public static final String CHECK_CIRCLE = "check-circle";
    public static final String ALERT_CIRCLE = "alert-circle";
    public static final String USER = "user";

    private static final Map<String, FlatSVGIcon> CACHE = new ConcurrentHashMap<>();

    private IconRegistry() {
    }

    /** Ícono en su tamaño nativo (24x24). */
    public static FlatSVGIcon get(String nombre) {
        return CACHE.computeIfAbsent(nombre, IconRegistry::cargar);
    }

    /** Mismo ícono cacheado, escalado a {@code tamano}px (FlatSVGIcon.derive no vuelve a leer el archivo). */
    public static FlatSVGIcon get(String nombre, int tamano) {
        return get(nombre).derive(tamano, tamano);
    }

    private static FlatSVGIcon cargar(String nombre) {
        // Sin "/" inicial: se resuelve vía ClassLoader.getResource, no Class.getResource.
        String ruta = "resources/icons/" + nombre + ".svg";
        ClassLoader classLoader = IconRegistry.class.getClassLoader();
        if (classLoader.getResource(ruta) == null) {
            throw new IllegalArgumentException("Ícono no encontrado: " + ruta);
        }
        return new FlatSVGIcon(ruta, classLoader);
    }
}
