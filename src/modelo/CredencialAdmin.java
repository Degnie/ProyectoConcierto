package modelo;

// Hash y salt almacenados de un administrador; separado de Usuario para no exponer
// más que lo necesario para validar el login.
public final class CredencialAdmin {
    private final String dni;
    private final String contrasenaHash;
    private final String salt;
    private final String nombres;

    public CredencialAdmin(String dni, String contrasenaHash, String salt, String nombres) {
        this.dni = dni;
        this.contrasenaHash = contrasenaHash;
        this.salt = salt;
        this.nombres = nombres;
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

    public String getNombres() {
        return nombres;
    }
}
