package modelo;

public class Usuario extends Persona {
    private boolean estado;

    public Usuario(String nombres, String apellidos, String dni, String contraseña, String correo) {
        super(nombres, apellidos, dni, contraseña, correo);
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