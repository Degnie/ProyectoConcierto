package modelo;

import java.util.ArrayList;
import java.util.UUID;

public class Zona {
    private UUID id;
    private String nombre;
    private int capacidad;
    private int precio;
    private ArrayList<Entrada> entradas;

    // Zona nueva (el admin la crea desde la UI): genera de una vez las `capacidad` entradas,
    // todas disponibles. El bloqueo optimista real vive en la columna `version` de Oracle, no acá
    // — mantener un contador en memoria era redundante y quedaba desincronizado entre instancias.
    public Zona(String nombre, int capacidad, int precio) {
        this(UUID.randomUUID(), nombre, capacidad, precio);
        generarEntradas();
    }

    // Reconstrucción desde Oracle (ver OracleConciertoRepository): mismo id persistido, pero sin
    // generar entradas todavía — el repositorio genera el set completo (todas disponibles) y luego
    // llama a marcarEntradaVendida(...) por cada fila que ya constaba como vendida en la tabla
    // `entradas`, para que el estado en memoria refleje exactamente lo que hay en la base.
    public Zona(UUID id, String nombre, int capacidad, int precio) {
        this.id = id;
        this.nombre = nombre;
        this.capacidad = capacidad;
        this.precio = precio;
        this.entradas = new ArrayList<>();
    }

    public UUID getId() {
        return id;
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

    // Usado solo durante la hidratación al arrancar: pone en SOLD (con el id ya persistido) la
    // entrada de este número, para que no se vuelva a ofrecer como disponible tras un reinicio.
    public void marcarEntradaVendida(int numero, UUID idPersistido) {
        for (Entrada entrada : this.entradas) {
            if (entrada.getNumero() == numero) {
                entrada.marcarComoVendidaReconstruida(idPersistido);
                return;
            }
        }
    }

    public int getCantidadEntradasDisponibles() {
        int disponibles = 0;
        for (Entrada entrada : this.entradas) {
            if (entrada.getEstadoEnum() == Entrada.EstadoEntrada.AVAILABLE) {
                disponibles++;
            }
        }
        return disponibles;
    }

    public Entrada[] mostrarEntrada() {
        return this.entradas.toArray(new Entrada[0]);
    }

    // synchronized: la resta de capacidad y la generación de la Entrada son una sola operación
    // atómica. Es la única puerta de entrada para vender un asiento; Cliente.comprar() la llama
    // en un bucle para compras de varias entradas, revirtiendo (liberar()) lo ya vendido si una
    // iteración posterior se queda sin cupo.
    public synchronized Entrada comprarEntrada(Cliente cliente) throws ZonaAgotadaException {
        if (cliente == null) {
            throw new IllegalArgumentException("Se requiere un cliente para comprar una entrada.");
        }
        for (Entrada entrada : this.entradas) {
            if (entrada.getEstadoEnum() == Entrada.EstadoEntrada.AVAILABLE) {
                entrada.vender();
                return entrada;
            }
        }
        throw new ZonaAgotadaException("No quedan entradas disponibles en la zona \"" + nombre + "\".");
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
