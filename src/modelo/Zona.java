package modelo;

import java.util.ArrayList;

public class Zona {
    private java.util.UUID id;
    private String nombre;
    private int capacidad;
    private int precio;
    private ArrayList<Entrada> entradas;
    private int version; // Bloqueo Optimista (Optimistic Locking)

    public Zona(String nombre, int capacidad, int precio) {
        this.id = java.util.UUID.randomUUID();
        this.nombre = nombre;
        this.capacidad = capacidad;
        this.precio = precio;
        this.entradas = new ArrayList<>();
        this.version = 0;
        generarEntradas();
    }

    public java.util.UUID getId() {
        return id;
    }

    public int getVersion() {
        return version;
    }

    public boolean generarEntradas() {
        boolean result = false;
        if (this.entradas.isEmpty()) {
            for (int i = 1; i <= this.capacidad; i++) {
                this.entradas.add(new Entrada(i));
            }
            result = true;
        }
        return result;
    }

    public int getCantidadEntradasDisponibles() {
        int disponibles = 0;
        for (Entrada entrada : this.entradas) {
            if (entrada.getEstado().equalsIgnoreCase("DISPONIBLE")) {
                disponibles++;
            }
        }
        return disponibles;
    }

    public Entrada[] mostrarEntrada() {
        return this.entradas.toArray(new Entrada[0]);
    }

    public Entrada[] venderEntrada(int numero) {
        this.version++; // Simula la modificación transaccional y el control de versión para bloqueo optimista
        Entrada[] result = null;
        ArrayList<Entrada> disponibles = new ArrayList<>();
        for (Entrada entrada : this.entradas) {
            if (entrada.getEstado().equalsIgnoreCase("DISPONIBLE")) {
                disponibles.add(entrada);
            }
            if (disponibles.size() == numero) {
                break;
            }
        }
        if (disponibles.size() == numero) {
            result = new Entrada[numero];
            for (int i = 0; i < numero; i++) {
                disponibles.get(i).vender();
                result[i] = disponibles.get(i);
            }
        }
        return result;
    }

    public String getNombre() {
        return nombre;
    }

    public int getCapacidad() {
        return capacidad;
    }

    public int getPrecio() {
        return precio;
    }

    public ArrayList<Entrada> getEntradas() {
        return entradas;
    }
}