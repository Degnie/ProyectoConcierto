package controlador;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import javax.swing.JOptionPane;
import modelo.Cliente;
import modelo.ClienteArreglo;
import modelo.ArchivoUsuarios;
import modelo.CodigoVerificacionException;
import modelo.EdadInvalidaException;
import vista.FrmLogin;
import vista.FrmRegistroCliente;
import proyectoentradas24200075.Principal;

public class ControladorRegistro implements ActionListener {
    
    private FrmRegistroCliente vista;
    private ClienteArreglo modeloClientes;
    private static final SecureRandom RANDOM = new SecureRandom();

    public ControladorRegistro(FrmRegistroCliente vista, ClienteArreglo modeloClientes) {
        this.vista = vista;
        this.modeloClientes = modeloClientes;
        
        // Escuchamos los botones de la vista de registro
        this.vista.getBtnRegistrar().addActionListener(this);
        this.vista.getBtnRegresar().addActionListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // CASO 1: Clic en Registrar
        if (e.getSource() == vista.getBtnRegistrar()) {
            String dni = vista.getDni();
            String nombres = vista.getNombres();
            String apellidos = vista.getApellidos();
            String contrasena = vista.getContrasena();
            String fechaNacimientoTexto = vista.getFechaNacimiento();
            String correo = vista.getCorreo();

            if (dni.isEmpty() || nombres.isEmpty() || apellidos.isEmpty() || contrasena.isEmpty() || fechaNacimientoTexto.isEmpty() || correo.isEmpty()) {
                JOptionPane.showMessageDialog(vista, "Todos los campos son obligatorios.");
                return;
            }

            if (!correo.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                JOptionPane.showMessageDialog(vista, "Ingrese un correo válido (ej. nombre@dominio.com).");
                return;
            }

            // Validación de DNI: Numérico y 8 dígitos
            if (!dni.matches("\\d{8}")) {
                JOptionPane.showMessageDialog(vista, "El DNI debe tener exactamente 8 dígitos numéricos.");
                return;
            }

            // Validación de mayoría de edad (>= 18 años), con manejo de excepciones
            try {
                LocalDate fechaNacimiento = LocalDate.parse(fechaNacimientoTexto, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                validarMayoriaDeEdad(fechaNacimiento);
            } catch (DateTimeParseException ex) {
                JOptionPane.showMessageDialog(vista, "La fecha de nacimiento debe tener el formato dd/MM/aaaa.");
                return;
            } catch (EdadInvalidaException ex) {
                JOptionPane.showMessageDialog(vista, ex.getMessage());
                return;
            }
            
            // Validación de contraseña: Mínimo 4 caracteres
            if (contrasena.length() < 4) {
                JOptionPane.showMessageDialog(vista, "La contraseña debe tener al menos 4 caracteres.");
                return;
            }
            
            // Validación de comas para evitar corrupción del archivo txt
            if (dni.contains(",") || nombres.contains(",") || apellidos.contains(",") || contrasena.contains(",") || correo.contains(",")) {
                JOptionPane.showMessageDialog(vista, "Los datos no pueden contener comas (',').");
                return;
            }

            // Verificamos si ya existe el DNI
            if (modeloClientes.buscarPorDni(dni) != null) {
                JOptionPane.showMessageDialog(vista, "El DNI ingresado ya se encuentra registrado.");
                return;
            }

            // Envío (simulado) de código de verificación al correo, obligatorio para completar el registro
            try {
                verificarCorreo(correo);
            } catch (CodigoVerificacionException ex) {
                JOptionPane.showMessageDialog(vista, ex.getMessage());
                return;
            }

            // Hasheamos la contraseña de forma segura para producción
            String contrasenaHasheada = modelo.Persona.hashPassword(contrasena);
            Cliente nuevoCliente = new Cliente(nombres, apellidos, dni, contrasenaHasheada, correo);
            
            // Lo agregamos al arreglo estático y al repositorio profesional
            boolean exito = modeloClientes.agregar(nuevoCliente);
            
            if (exito) {
                proyectoentradas24200075.Principal.clienteRepository.save(nuevoCliente);
                // PERSISTENCIA: Guardamos la lista actualizada en el archivo de texto
                ArchivoUsuarios.guardarClientes(modeloClientes);
                
                JOptionPane.showMessageDialog(vista, "¡Cliente registrado con éxito!");
                regresarAlLogin();
            } else {
                JOptionPane.showMessageDialog(vista, "Error: El almacenamiento de clientes está lleno.");
            }
        }
        
        // CASO 2: Clic en Regresar
        else if (e.getSource() == vista.getBtnRegresar()) {
            regresarAlLogin();
        }
    }
    
    // ponytail: envío de correo simulado (código mostrado en pantalla en vez de enviarse por SMTP).
    // Para correo real, reemplazar solo el JOptionPane de "envío" por una llamada JavaMail/SMTP;
    // la generación y validación del código no cambian.
    private void verificarCorreo(String correo) throws CodigoVerificacionException {
        String codigo = String.format("%04d", RANDOM.nextInt(10000));

        JOptionPane.showMessageDialog(vista,
                "Se envió un código de verificación a " + correo + " (SIMULADO): " + codigo);

        String intento = JOptionPane.showInputDialog(vista, "Ingrese el código de 4 dígitos enviado a su correo:");

        if (intento == null || !intento.trim().equals(codigo)) {
            throw new CodigoVerificacionException("Código de verificación incorrecto. Registro cancelado.");
        }
    }

    private void validarMayoriaDeEdad(LocalDate fechaNacimiento) throws EdadInvalidaException {
        int edad = Period.between(fechaNacimiento, LocalDate.now()).getYears();
        if (edad < 18) {
            throw new EdadInvalidaException("Debe ser mayor de edad (18 años) para registrarse. Edad calculada: " + edad + ".");
        }
    }

    private void regresarAlLogin() {
        FrmLogin frmLogin = new FrmLogin();
        new ControladorLogin(frmLogin, modeloClientes);
        frmLogin.setVisible(true);
        vista.dispose();
    }
}