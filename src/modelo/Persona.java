package modelo;

import java.util.UUID;

public abstract class Persona {
    private UUID id;
    private String nombres;
    private String apellidos;
    private String dni;
    private String contraseña; // Almacenará el hash SHA-256 de la contraseña
    private String correo;

    public Persona(String nombres, String apellidos, String dni, String contraseña, String correo) {
        this.id = UUID.randomUUID();
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.dni = dni;
        this.contraseña = contraseña;
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

    public String getContraseña() {
        return contraseña;
    }

    public String getCorreo() {
        return correo;
    }

    // Utilidad estática para encriptar contraseñas bajo el estándar SHA-256
    public static String hashPassword(String password) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(password.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception ex) {
            throw new RuntimeException("Error al encriptar contraseña", ex);
        }
    }
}
