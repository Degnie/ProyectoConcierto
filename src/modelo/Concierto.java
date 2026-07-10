package modelo;

import java.util.ArrayList;
import java.util.Date;
import java.util.EnumMap;
import java.util.Map;

public class Concierto {
    private java.util.UUID id;
    private String nombre;
    private Date fecha;
    private ArrayList<Zona> zonas;
    private Map<TipoTarjeta, Double> descuentos;

    public Concierto(String nombre, Date fecha) {
        this.id = java.util.UUID.randomUUID();
        this.nombre = nombre;
        this.fecha = fecha;
        this.zonas = new ArrayList<>();
        this.descuentos = new EnumMap<>(TipoTarjeta.class);
        for (TipoTarjeta tipo : TipoTarjeta.values()) {
            this.descuentos.put(tipo, tipo.getDescuentoPorDefecto());
        }
    }

    // Porcentaje de descuento (0.05 = 5%) aplicado a compras pagadas con ese emisor
    public double getDescuento(TipoTarjeta tipo) {
        return descuentos.getOrDefault(tipo, 0.0);
    }

    // Permite que el admin reconfigure el descuento por concierto
    public void setDescuento(TipoTarjeta tipo, double porcentaje) {
        descuentos.put(tipo, porcentaje);
    }

    public java.util.UUID getId() {
        return id;
    }

    public boolean agregarZona(String nombre, int capacidad, int precio) {
        boolean result = false;
        Zona zona = new Zona(nombre, capacidad, precio);
        result = this.zonas.add(zona);
        return result;
    }

    public boolean eliminarZona(String nombre) {
        boolean result = false;
        for (Zona zona : this.zonas) {
            if (zona.getNombre().equalsIgnoreCase(nombre)) {
                result = this.zonas.remove(zona);
                break;
            }
        }
        return result;
    }

    public String getNombre() {
        return nombre;
    }

    public Date getFecha() {
        return fecha;
    }

    public ArrayList<Zona> getZonas() {
        return zonas;
    }
}
