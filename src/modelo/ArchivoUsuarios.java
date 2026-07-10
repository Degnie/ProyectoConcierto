package modelo;

import java.io.*;
import java.util.ArrayList;

public class ArchivoUsuarios {
    private static final String RUTA_ARCHIVO = "usuarios.txt";

    // 1. Cargar usuarios desde el archivo al iniciar el programa
    public static void cargarUsuarios(ClienteArreglo clienteArreglo) {
        File archivo = new File(RUTA_ARCHIVO);
        
        // Si el archivo no existe, lo creamos e inyectamos la CUENTA BASE del Admin
        if (!archivo.exists()) {
            try (PrintWriter pw = new PrintWriter(new FileWriter(archivo))) {
                // Cuenta base por defecto requerida
                pw.println("ADMIN,admin,1234,Admin,General,0");
                System.out.println("Archivo de usuarios creado con cuenta base de administrador.");
            } catch (IOException e) {
                System.err.println("Error al crear el archivo base de usuarios: " + e.getMessage());
            }
            return;
        }

        // Si el archivo existe, lo leemos línea por línea aplicando Excepciones
        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                if (linea.trim().isEmpty()) continue;
                
                String[] datos = linea.split(",");
                String tipo = datos[0];
                String dni = datos[1];
                String contrasena = datos[2];
                String nombres = datos[3];
                String apellidos = datos[4];
                
                if (tipo.equals("CLIENTE")) {
                    int puntos = Integer.parseInt(datos[5]);
                    // datos[6] (correo) puede no existir en archivos guardados antes de esta mejora
                    String correo = datos.length > 6 ? datos[6] : "";
                    Cliente cliente = new Cliente(nombres, apellidos, dni, contrasena, correo);
                    // Si tu clase Cliente permite setear puntos, lo agregas, si no, se inicializa en 0
                    clienteArreglo.agregar(cliente);
                }
                // Los ADMIN no se guardan en el arreglo de clientes, se validan directo o en un arreglo de usuarios
            }
        } catch (IOException | ArrayIndexOutOfBoundsException | NumberFormatException e) {
            System.err.println("Excepción capturada al cargar usuarios: " + e.getMessage());
        }
    }

    // 2. Guardar todos los clientes del arreglo en el archivo (.txt)
    public static void guardarClientes(ClienteArreglo clienteArreglo) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(RUTA_ARCHIVO))) {
            // Volvemos a escribir la cuenta base para que no se pierda
            pw.println("ADMIN,admin,1234,Admin,General,0");
            
            // Recorremos el arreglo del modelo y lo escribimos en texto plano
            for (Cliente c : clienteArreglo.getClientes()) {
                pw.println("CLIENTE," + c.getDni() + "," + c.getContraseña() + ","
                           + c.getNombres() + "," + c.getApellidos() + "," + c.getPuntos() + "," + c.getCorreo());
            }
        } catch (IOException e) {
            System.err.println("Error grave al escribir en usuarios.txt: " + e.getMessage());
        }
    }
}
