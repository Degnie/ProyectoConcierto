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
import servicio.EmailService;
import vista.FrmPrincipal;
import vista.FrmRegistroCliente;

/**
 * Controla el flujo de registro de clientes. El envío del código de verificación por correo y la
 * persistencia en Oracle corren en dos fases asíncronas separadas (dos {@link SwingWorker}), para
 * que ninguna operación de red/BD bloquee el Event Dispatch Thread (EDT):
 * <p>
 * Fase 1 (correo): genera el código, lo envía por SMTP real vía {@link EmailService} en segundo
 * plano, y al terminar pide el código por diálogo en el EDT.
 * <p>
 * Fase 2 (persistencia): solo se dispara si el código ingresado es correcto; hashea la contraseña
 * y guarda el cliente en Oracle en segundo plano.
 */
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
            iniciarFaseCorreo();
        } else if (e.getSource() == vista.getBtnRegresar()) {
            regresarAlLogin();
        }
    }

    // ===================== Fase 1: envío del código por correo real (SMTP) =====================

    private void iniciarFaseCorreo() {
        String correo = vista.getCorreo();
        String codigo = String.format("%04d", RANDOM.nextInt(10000));

        vista.getBtnRegistrar().setEnabled(false);
        principal.iniciarCarga();

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                // EmailService encapsula Jakarta Mail: si config.properties no tiene la sección
                // SMTP completa, o falla la conexión/autenticación, la excepción llega a done()
                // vía get() y se muestra como un único mensaje de error al usuario.
                new EmailService().enviarCodigoVerificacion(correo, codigo);
                return null;
            }

            @Override
            protected void done() {
                principal.finalizarCarga();
                try {
                    get();
                } catch (Exception ex) {
                    vista.getBtnRegistrar().setEnabled(true);
                    JOptionPane.showMessageDialog(vista,
                        "No se pudo enviar el correo de verificación: " + causaRaiz(ex).getMessage());
                    return;
                }
                pedirCodigoYContinuar(codigo);
            }
        }.execute();
    }

    // Corre en el EDT (justo después de done() de la fase de correo): bloquea con un diálogo
    // modal a propósito, es interacción de usuario, no I/O de red.
    private void pedirCodigoYContinuar(String codigoEnviado) {
        String intento = JOptionPane.showInputDialog(vista,
            "Se envió un código de verificación a " + vista.getCorreo() + ". Ingréselo para continuar:");

        try {
            validarCodigo(intento, codigoEnviado);
        } catch (CodigoVerificacionException ex) {
            vista.getBtnRegistrar().setEnabled(true);
            JOptionPane.showMessageDialog(vista, ex.getMessage());
            return;
        }

        iniciarFasePersistencia();
    }

    private void validarCodigo(String intento, String codigoEnviado) throws CodigoVerificacionException {
        if (intento == null || !intento.trim().equals(codigoEnviado)) {
            throw new CodigoVerificacionException("Código de verificación incorrecto. Registro cancelado.");
        }
    }

    // ===================== Fase 2: hash + persistencia en Oracle =====================

    private void iniciarFasePersistencia() {
        String dni = vista.getDni();
        String nombres = vista.getNombres();
        String apellidos = vista.getApellidos();
        String correo = vista.getCorreo();
        char[] contrasena = vista.getContrasenaChars();

        principal.iniciarCarga();
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                // El hash se calcula y el arreglo se purga en el mismo paso, dentro del hilo de
                // background: la contraseña en claro no sobrevive más de lo estrictamente
                // necesario, sin pasar nunca por una String intermedia.
                String salt = Persona.generarSalt();
                String hash = Persona.hashPassword(contrasena, salt);
                Arrays.fill(contrasena, '0');

                if (clienteRepository.findByDni(dni) != null) {
                    return false;
                }
                Cliente nuevoCliente = new Cliente(nombres, apellidos, dni, hash, salt, correo);
                return clienteRepository.save(nuevoCliente);
            }

            @Override
            protected void done() {
                principal.finalizarCarga();
                vista.getBtnRegistrar().setEnabled(true);
                boolean exito;
                try {
                    exito = get();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(vista, "Error al registrar: " + causaRaiz(ex).getMessage());
                    return;
                }
                if (exito) {
                    JOptionPane.showMessageDialog(vista, "¡Cliente registrado con éxito!");
                    regresarAlLogin();
                } else {
                    JOptionPane.showMessageDialog(vista, "El DNI ingresado ya se encuentra registrado.");
                }
            }
        }.execute();
    }

    private static Throwable causaRaiz(Throwable ex) {
        Throwable actual = ex;
        while (actual.getCause() != null) {
            actual = actual.getCause();
        }
        return actual;
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
