package modelo;

import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.UUID;

public abstract class Persona {
    private static final SecureRandom RANDOM = new SecureRandom();

    private UUID id;
    private String nombres;
    private String apellidos;
    private String dni;
    private String contrasenaHash; // SHA-256(password + salt)
    private String salt;
    private String correo;

    public Persona(String nombres, String apellidos, String dni, String contrasenaHash, String salt, String correo) {
        this.id = UUID.randomUUID();
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.dni = dni;
        this.contrasenaHash = contrasenaHash;
        this.salt = salt;
        this.correo = correo;
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
