package modelo;

import java.io.*;
import java.util.ArrayList;
import java.util.Date;

public class ArchivoConciertos {
    private static final String RUTA_ARCHIVO = "conciertos.txt";

    public static void cargarConciertos(ArrayList<Concierto> listaConciertos) {
        File archivo = new File(RUTA_ARCHIVO);
        if (!archivo.exists()) return;

        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
            String linea;
            Concierto conciertoActual = null;

            while ((linea = br.readLine()) != null) {
                if (linea.trim().isEmpty()) continue;
                String[] datos = linea.split(",");
                
                if (datos[0].equals("CONCIERTO")) {
                    String nombre = datos[1];
                    long timestamp = Long.parseLong(datos[2]);
                    conciertoActual = new Concierto(nombre, new Date(timestamp));
                    listaConciertos.add(conciertoActual);
                } else if (datos[0].equals("ZONA") && conciertoActual != null) {
                    String nombreZona = datos[1];
                    int capacidad = Integer.parseInt(datos[2]);
                    int precio = Integer.parseInt(datos[3]);
                    conciertoActual.agregarZona(nombreZona, capacidad, precio);
                }
            }
        } catch (IOException | NumberFormatException e) {
            System.err.println("Excepción al cargar conciertos/zonas: " + e.getMessage());
        }
    }

    public static void guardarConciertos(ArrayList<Concierto> listaConciertos) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(RUTA_ARCHIVO))) {
            for (Concierto c : listaConciertos) {
                pw.println("CONCIERTO," + c.getNombre() + "," + c.getFecha().getTime());
                for (Zona z : c.getZonas()) {
                    pw.println("ZONA," + z.getNombre() + "," + z.getCapacidad() + "," + z.getPrecio());
                }
            }
        } catch (IOException e) {
            System.err.println("Error al guardar conciertos: " + e.getMessage());
        }
    }
}