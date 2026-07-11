package servicio;

import conexion.ConfiguracionApp;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Servicio de mensajería SMTP, aislado del resto de la aplicación: es la única clase que conoce
 * Jakarta Mail. Los controladores solo ven {@link #enviarCodigoVerificacion(String, String)}, sin
 * enterarse de host/puerto/autenticación ni de la librería de correo usada.
 * <p>
 * Requiere agregar manualmente al classpath del proyecto (igual que ojdbc.jar) las librerías de
 * Jakarta Mail: {@code jakarta.mail-api.jar} + una implementación (p. ej. {@code angus-mail.jar}
 * o el clásico {@code javax.mail.jar} si se prefiere la API legada).
 * <p>
 * Los parámetros de conexión se leen de config.properties (mismo archivo externo que usa
 * {@link conexion.DatabaseConnection}, resuelto junto al .jar en ejecución):
 * <pre>
 *   mail.smtp.host=smtp.gmail.com
 *   mail.smtp.port=587
 *   mail.smtp.auth=true
 *   mail.smtp.starttls.enable=true
 *   mail.smtp.user=cuenta@dominio.com
 *   mail.smtp.password=token_de_aplicacion
 * </pre>
 */
public class EmailService {
    private static final String NOMBRE_ARCHIVO_CONFIG = "config.properties";

    private final Session session;
    private final String remitente;

    public EmailService() throws IOException {
        File archivoConfig = ConfiguracionApp.resolverArchivo(NOMBRE_ARCHIVO_CONFIG);
        Properties config = new Properties();
        try (FileInputStream in = new FileInputStream(archivoConfig)) {
            config.load(in);
        }

        this.remitente = requerido(config, "mail.smtp.user");
        String clave = requerido(config, "mail.smtp.password");

        Properties propsSmtp = new Properties();
        propsSmtp.put("mail.smtp.host", requerido(config, "mail.smtp.host"));
        propsSmtp.put("mail.smtp.port", requerido(config, "mail.smtp.port"));
        propsSmtp.put("mail.smtp.auth", config.getProperty("mail.smtp.auth", "true"));
        propsSmtp.put("mail.smtp.starttls.enable", config.getProperty("mail.smtp.starttls.enable", "true"));

        this.session = Session.getInstance(propsSmtp, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(remitente, clave);
            }
        });
    }

    private static String requerido(Properties config, String clave) throws IOException {
        String valor = config.getProperty(clave);
        if (valor == null || valor.isBlank()) {
            throw new IOException("Falta la propiedad '" + clave + "' en config.properties (sección SMTP).");
        }
        return valor;
    }

    /**
     * Envía el código de verificación de registro por correo real.
     *
     * @param correoDestino correo del cliente que se está registrando
     * @param codigo        código de 4 dígitos generado por el controlador
     * @throws Exception ante cualquier falla de red, autenticación o configuración SMTP;
     *                    el controlador la traduce a un mensaje amigable para el usuario.
     */
    public void enviarCodigoVerificacion(String correoDestino, String codigo) throws Exception {
        MimeMessage mensaje = new MimeMessage(session);
        mensaje.setFrom(new InternetAddress(remitente));
        mensaje.setRecipients(Message.RecipientType.TO, InternetAddress.parse(correoDestino));
        mensaje.setSubject("Código de verificación - Sistema de Entradas");
        mensaje.setText(
            "Tu código de verificación es: " + codigo
            + "\n\nSi no solicitaste este registro, ignora este mensaje."
        );
        Transport.send(mensaje);
    }
}
