package util;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.InputStream;

/**
 * Fuente Inter empaquetada en {@code /resources/fonts} (licencia SIL Open Font License, ver
 * {@code OFL.txt} junto a los .ttf), registrada una sola vez en el {@link GraphicsEnvironment} al
 * cargar esta clase. Los tres estilos que pide la guía visual (título/cuerpo/validación) se
 * derivan acá para que cada vista los reutilice sin releer el archivo .ttf.
 */
public final class Tipografia {

    public static final Font TITULO;
    public static final Font CUERPO;
    public static final Font VALIDACION;

    static {
        Font regular = cargar("/resources/fonts/Inter-Regular.ttf");
        Font italica = cargar("/resources/fonts/Inter-Italic.ttf");
        // La fuente Inter descargada es "variable" (un solo archivo con eje de peso); Java no
        // selecciona una instancia con peso Bold real a partir de ese eje, así que el título usa
        // negrita sintética vía deriveFont(BOLD) — el mismo mecanismo que Swing ya aplica de
        // memoria para cualquier fuente sin una cara Bold distinta.
        TITULO = regular.deriveFont(Font.BOLD, 18f);
        CUERPO = regular.deriveFont(Font.PLAIN, 14f);
        VALIDACION = italica.deriveFont(Font.ITALIC, 12f);
    }

    private Tipografia() {
    }

    private static Font cargar(String recurso) {
        try (InputStream in = Tipografia.class.getResourceAsStream(recurso)) {
            if (in == null) {
                throw new IOException("No se encontró el recurso " + recurso);
            }
            Font fuente = Font.createFont(Font.TRUETYPE_FONT, in);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(fuente);
            return fuente;
        } catch (Exception ex) {
            // ponytail: si el .ttf no carga (empaquetado roto), se cae a la fuente sans-serif del
            // sistema en vez de tumbar el arranque de la app por un detalle tipográfico.
            RegistradorErrores.registrar("Tipografia.cargar:" + recurso, ex);
            return new Font(Font.SANS_SERIF, Font.PLAIN, 14);
        }
    }
}
