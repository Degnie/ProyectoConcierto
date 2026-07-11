package modelo;

import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.Period;
import java.util.Arrays;
import java.util.UUID;
import java.util.regex.Pattern;

public abstract class Persona {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Pattern PATRON_DNI = Pattern.compile("\\d{8}");
    private static final Pattern PATRON_CORREO = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final int EDAD_MINIMA = 18;

    private UUID id;
    private String nombres;
    private String apellidos;
    private String dni;
    private String contrasenaHash; // SHA-256(password + salt)
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

    // Un salt distinto por usuario evita que contraseñas iguales produzcan el mismo hash (rainbow tables)
    public static String generarSalt() {
        byte[] saltBytes = new byte[16];
        RANDOM.nextBytes(saltBytes);
        return aHex(saltBytes);
    }

    // Hashea sin pasar por una String intermedia (que quedaría en el pool de Strings del heap)
    public static String hashPassword(char[] password, String salt) {
        byte[] passwordBytes = charsToUtf8Bytes(password);
        byte[] saltBytes = salt.getBytes(StandardCharsets.UTF_8);
        byte[] combined = new byte[passwordBytes.length + saltBytes.length];
        System.arraycopy(passwordBytes, 0, combined, 0, passwordBytes.length);
        System.arraycopy(saltBytes, 0, combined, passwordBytes.length, saltBytes.length);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(combined);
            return aHex(hashBytes);
        } catch (Exception ex) {
            throw new RuntimeException("Error al encriptar contraseña", ex);
        } finally {
            Arrays.fill(passwordBytes, (byte) 0);
            Arrays.fill(combined, (byte) 0);
        }
    }

    private static byte[] charsToUtf8Bytes(char[] chars) {
        CharBuffer charBuffer = CharBuffer.wrap(chars);
        java.nio.ByteBuffer byteBuffer = StandardCharsets.UTF_8.encode(charBuffer);
        byte[] bytes = new byte[byteBuffer.remaining()];
        byteBuffer.get(bytes);
        Arrays.fill(byteBuffer.array(), (byte) 0);
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
