package controlador;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import modelo.Cliente;
import modelo.CodigoVerificacionException;
import modelo.EdadInvalidaException;
import modelo.Persona;
import repositorio.ClienteRepository;
import vista.FrmPrincipal;
import vista.FrmRegistroCliente;

public class ControladorRegistro implements ActionListener {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final FrmPrincipal principal;
    private final FrmRegistroCliente vista;
    private final ClienteRepository clienteRepository;

    public ControladorRegistro(FrmPrincipal principal, ClienteRepository clienteRepository) {
        this.principal = principal;
        this.vista = principal.getVistaRegistro();
        this.clienteRepository = clienteRepository;

        this.vista.getBtnRegistrar().addActionListener(this);
        this.vista.getBtnRegresar().addActionListener(this);

        DocumentListener revalidar = new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { validarFormulario(); }
            @Override public void removeUpdate(DocumentEvent e) { validarFormulario(); }
            @Override public void changedUpdate(DocumentEvent e) { validarFormulario(); }
        };
        this.vista.getTxtDni().getDocument().addDocumentListener(revalidar);
        this.vista.getTxtNombres().getDocument().addDocumentListener(revalidar);
        this.vista.getTxtApellidos().getDocument().addDocumentListener(revalidar);
        this.vista.getTxtContrasena().getDocument().addDocumentListener(revalidar);
        this.vista.getTxtFechaNacimiento().getDocument().addDocumentListener(revalidar);
        this.vista.getTxtCorreo().getDocument().addDocumentListener(revalidar);
        validarFormulario();
    }

    // Revalida cada campo en vivo: pinta el JLabel de error correspondiente y solo habilita
    // "Registrar" cuando todos los campos son válidos (reemplaza los JOptionPane de formato).
    private boolean validarFormulario() {
        boolean valido = true;

        if (vista.getDni().isEmpty()) {
            vista.setErrorDni(null);
        } else if (!vista.getDni().matches("\\d{8}")) {
            vista.setErrorDni("El DNI debe tener 8 dígitos numéricos.");
            valido = false;
        } else {
            vista.setErrorDni(null);
        }

        if (vista.getNombres().isEmpty()) {
            vista.setErrorNombres(null);
            valido = false;
        } else {
            vista.setErrorNombres(null);
        }

        if (vista.getApellidos().isEmpty()) {
            vista.setErrorApellidos("Requerido.");
            valido = false;
        } else {
            vista.setErrorApellidos(null);
        }

        int largoContrasena = vista.getTxtContrasena().getPassword().length;
        if (largoContrasena == 0) {
            vista.setErrorContrasena(null);
            valido = false;
        } else if (largoContrasena < 4) {
            vista.setErrorContrasena("Mínimo 4 caracteres.");
            valido = false;
        } else {
            vista.setErrorContrasena(null);
        }

        String fechaTexto = vista.getFechaNacimiento();
        if (fechaTexto.isEmpty()) {
            vista.setErrorFecha(null);
            valido = false;
        } else {
            try {
                validarMayoriaDeEdad(LocalDate.parse(fechaTexto, FORMATO_FECHA));
                vista.setErrorFecha(null);
            } catch (DateTimeParseException ex) {
                vista.setErrorFecha("Formato esperado: dd/MM/aaaa.");
                valido = false;
            } catch (EdadInvalidaException ex) {
                vista.setErrorFecha(ex.getMessage());
                valido = false;
            }
        }

        String correo = vista.getCorreo();
        if (correo.isEmpty()) {
            vista.setErrorCorreo(null);
            valido = false;
        } else if (!correo.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            vista.setErrorCorreo("Correo inválido (ej. nombre@dominio.com).");
            valido = false;
        } else {
            vista.setErrorCorreo(null);
        }

        if (vista.getDni().isEmpty() || vista.getNombres().isEmpty() || vista.getApellidos().isEmpty()
                || largoContrasena == 0 || fechaTexto.isEmpty() || correo.isEmpty()) {
            valido = false;
        }

        vista.getBtnRegistrar().setEnabled(valido);
        return valido;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == vista.getBtnRegistrar()) {
            if (!validarFormulario()) {
                return;
            }
            registrar();
        } else if (e.getSource() == vista.getBtnRegresar()) {
            regresarAlLogin();
        }
    }

    private void registrar() {
        String dni = vista.getDni();
        String nombres = vista.getNombres();
        String apellidos = vista.getApellidos();
        char[] contrasena = vista.getContrasenaChars();
        String correo = vista.getCorreo();

        try {
            verificarCorreo(correo);
        } catch (CodigoVerificacionException ex) {
            Arrays.fill(contrasena, '0');
            JOptionPane.showMessageDialog(vista, ex.getMessage());
            return;
        }

        vista.getBtnRegistrar().setEnabled(false);
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                if (clienteRepository.findByDni(dni) != null) {
                    return false;
                }
                String salt = Persona.generarSalt();
                String hash = Persona.hashPassword(contrasena, salt);
                Cliente nuevoCliente = new Cliente(nombres, apellidos, dni, hash, salt, correo);
                return clienteRepository.save(nuevoCliente);
            }

            @Override
            protected void done() {
                Arrays.fill(contrasena, '0');
                vista.getBtnRegistrar().setEnabled(true);
                boolean exito;
                try {
                    exito = get();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(vista, "Error al registrar: " + ex.getMessage());
                    return;
                }
                if (exito) {
                    JOptionPane.showMessageDialog(vista, "¡Cliente registrado con éxito!");
                    vista.limpiarCampos();
                    regresarAlLogin();
                } else {
                    JOptionPane.showMessageDialog(vista, "El DNI ingresado ya se encuentra registrado.");
                }
            }
        }.execute();
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
            throw new EdadInvalidaException("Debe ser mayor de edad (18 años). Edad calculada: " + edad + ".");
        }
    }

    private void regresarAlLogin() {
        principal.mostrarLogin();
    }
}
