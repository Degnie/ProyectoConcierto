package modelo;

import java.time.LocalDate;

public class Usuario extends Persona {
    private boolean estado;

    public Usuario(String nombres, String apellidos, String dni, String contrasenaHash, String salt,
                    String correo, LocalDate fechaNacimiento)
            throws DniInvalidoException, CorreoInvalidoException, EdadInvalidaException {
        super(nombres, apellidos, dni, contrasenaHash, salt, correo, fechaNacimiento);
        this.estado = true;
    }

    public boolean registrarZonas(Concierto concierto, String nombre, int capacidad, int precio) {
        boolean result = false;
        if (concierto != null) {
            result = concierto.agregarZona(nombre, capacidad, precio);
        }
        return result;
    }

    public boolean isEstado() {
        return estado;
    }
}
