package modelo;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.Period;
import java.util.Arrays;
import java.util.UUID;
import java.util.regex.Pattern;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public abstract class Persona {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Pattern PATRON_DNI = Pattern.compile("\\d{8}");
    private static final Pattern PATRON_CORREO = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final int EDAD_MINIMA = 18;

    // PBKDF2WithHmacSHA256 (OWASP A02): API estándar de Java, sin dependencias externas. 65536
    // iteraciones es el mínimo recomendado por OWASP para esta combinación de algoritmo/hash — hace
    // que cada intento de fuerza bruta sea deliberadamente costoso de computar, a diferencia de un
    // SHA-256 simple (rapidísimo de ejecutar en GPU, pensado para integridad, no para contraseñas).
    private static final String ALGORITMO_PBKDF2 = "PBKDF2WithHmacSHA256";
    private static final int PBKDF2_ITERACIONES = 65536;
    private static final int PBKDF2_LONGITUD_CLAVE_BITS = 256;

    private UUID id;
    private String nombres;
    private String apellidos;
    private String dni;
    private String contrasenaHash; // PBKDF2WithHmacSHA256(password, salt, 65536 iteraciones)
    private String salt;
    private String correo;
    private LocalDate fechaNacimiento;

    // Invariantes de identidad blindadas acá: cualquier Persona (Cliente o Usuario) que llegue a
    // existir en memoria ya es, por construcción, mayor de edad y tiene DNI/correo con formato
    // válido. Los controladores pueden precalcular estas mismas reglas para dar feedback rápido
    // en la UI, pero la autoridad real está acá, no ahí.
    public Persona(String nombres, String apellidos, String dni, String contrasenaHash, String salt,
                    String correo, LocalDate fechaNacimiento)
            throws DniInvalidoException, CorreoInvalidoException, EdadInvalidaException {
        if (dni == null || !PATRON_DNI.matcher(dni).matches()) {
            throw new DniInvalidoException("El DNI debe tener 8 dígitos numéricos.");
        }
        if (correo == null || !PATRON_CORREO.matcher(correo).matches()) {
            throw new CorreoInvalidoException("Correo inválido (ej. nombre@dominio.com).");
        }
        if (fechaNacimiento == null) {
            throw new EdadInvalidaException("La fecha de nacimiento es obligatoria.");
        }
        int edad = Period.between(fechaNacimiento, LocalDate.now()).getYears();
        if (edad < EDAD_MINIMA) {
            throw new EdadInvalidaException("Debe ser mayor de edad (18 años). Edad calculada: " + edad + ".");
        }

        this.id = UUID.randomUUID();
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.dni = dni;
        this.contrasenaHash = contrasenaHash;
        this.salt = salt;
        this.correo = correo;
        this.fechaNacimiento = fechaNacimiento;
    }

    public UUID getId() {
        return id;
    }

    public String getNombres() {
        return nombres;
    }

    public String getApellidos() {
        return apellidos;
    }

    public String getDni() {
        return dni;
    }

    public String getContrasenaHash() {
        return contrasenaHash;
    }

    public String getSalt() {
        return salt;
    }

    public String getCorreo() {
        return correo;
    }

    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }

    // Un salt distinto por usuario evita que contraseñas iguales produzcan la misma derivación
    // (rainbow tables). Se guarda como hex para poder persistirlo en una columna VARCHAR2.
    public static String generarSalt() {
        byte[] saltBytes = new byte[16];
        RANDOM.nextBytes(saltBytes);
        return aHex(saltBytes);
    }

    // Deriva la clave con PBKDF2WithHmacSHA256. No pasa nunca por una String intermedia con la
    // contraseña en claro; el PBEKeySpec y los bytes derivados se purgan en el finally.
    public static String hashPassword(char[] password, String salt) {
        byte[] saltBytes = fromHex(salt);
        PBEKeySpec especificacion = new PBEKeySpec(password, saltBytes, PBKDF2_ITERACIONES, PBKDF2_LONGITUD_CLAVE_BITS);
        byte[] claveDerivada = null;
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITMO_PBKDF2);
            claveDerivada = factory.generateSecret(especificacion).getEncoded();
            return aHex(claveDerivada);
        } catch (Exception ex) {
            throw new RuntimeException("Error al derivar la clave con PBKDF2", ex);
        } finally {
            especificacion.clearPassword();
            Arrays.fill(saltBytes, (byte) 0);
            if (claveDerivada != null) {
                Arrays.fill(claveDerivada, (byte) 0);
            }
        }
    }

    private static byte[] fromHex(String hex) {
        int longitud = hex.length();
        byte[] bytes = new byte[longitud / 2];
        for (int i = 0; i < longitud; i += 2) {
            bytes[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4) + Character.digit(hex.charAt(i + 1), 16));
        }
        return bytes;
    }

    private static String aHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
