package controlador;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.security.SecureRandom;
import java.time.LocalDate;
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
import util.RegistradorErrores;
import vista.FrmPrincipal;
import vista.FrmRegistroCliente;

/**
 * Controla el flujo de registro de clientes, en tres fases asíncronas separadas (tres
 * {@link SwingWorker}), para que ninguna operación de red/BD bloquee el Event Dispatch Thread:
 * <p>
 * Fase 0 (verificación de DNI): confirma que el DNI no esté ya registrado <b>antes</b> de gastar
 * una cuota de envío SMTP en un registro que de todos modos va a fallar.
 * <p>
 * Fase 1 (correo): genera el código, lo envía por SMTP real vía {@link EmailService} en segundo
 * plano, y al terminar muestra el paso de verificación integrado en el mismo panel (sin diálogos
 * modales — paradigma SPA).
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

    // Código generado en la Fase 1, pendiente de que el usuario lo confirme en el paso de
    // verificación integrado (ver mostrarPasoVerificacion). Se limpia al cancelar o al concluir.
    private String codigoPendiente;

    public ControladorRegistro(FrmPrincipal principal, ClienteRepository clienteRepository) {
        this.principal = principal;
        this.vista = principal.getVistaRegistro();
        this.clienteRepository = clienteRepository;

        this.vista.getBtnRegistrar().addActionListener(this);
        this.vista.getBtnRegresar().addActionListener(this);
        this.vista.getBtnConfirmarCodigo().addActionListener(this);
        this.vista.getBtnCancelarCodigo().addActionListener(this);

        // Mientras el usuario escribe: solo se recalcula si "Registrar" debe habilitarse, sin
        // pintar ningún JLabel rojo (evita el feedback hostil de marcar error a mitad de tipeo).
        DocumentListener revalidarSilencioso = new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { actualizarBotonRegistrar(); }
            @Override public void removeUpdate(DocumentEvent e) { actualizarBotonRegistrar(); }
            @Override public void changedUpdate(DocumentEvent e) { actualizarBotonRegistrar(); }
        };
        this.vista.getTxtDni().getDocument().addDocumentListener(revalidarSilencioso);
        this.vista.getTxtNombres().getDocument().addDocumentListener(revalidarSilencioso);
        this.vista.getTxtApellidos().getDocument().addDocumentListener(revalidarSilencioso);
        this.vista.getTxtContrasena().getDocument().addDocumentListener(revalidarSilencioso);
        this.vista.getTxtFechaNacimiento().getDocument().addDocumentListener(revalidarSilencioso);
        this.vista.getTxtCorreo().getDocument().addDocumentListener(revalidarSilencioso);

        // Al perder el foco: recién ahí se revela el error (si lo hay) del campo específico.
        FocusAdapter revelarAlPerderFoco = new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                revelarError(e.getComponent());
            }
        };
        this.vista.getTxtDni().addFocusListener(revelarAlPerderFoco);
        this.vista.getTxtApellidos().addFocusListener(revelarAlPerderFoco);
        this.vista.getTxtContrasena().addFocusListener(revelarAlPerderFoco);
        this.vista.getTxtFechaNacimiento().addFocusListener(revelarAlPerderFoco);
        this.vista.getTxtCorreo().addFocusListener(revelarAlPerderFoco);

        // Enter en el campo de clave ya dispara btnRegistrar.doClick() desde la vista (no hace
        // nada si está deshabilitado); esto cubre el caso "intento de sumisión con el formulario
        // inválido" revelando todos los errores de una vez, ya que doClick() no genera feedback
        // por sí solo cuando el botón está deshabilitado.
        this.vista.getTxtContrasena().addActionListener(ev -> {
            if (!formularioValido()) {
                revelarTodosLosErrores();
            }
        });

        actualizarBotonRegistrar();
    }

    // ===================== Validación por campo (sin efectos visuales) =====================

    private String errorDni() {
        String dni = vista.getDni();
        if (dni.isEmpty()) return null;
        if (!dni.matches("\\d{8}")) return "El DNI debe tener 8 dígitos numéricos.";
        return null;
    }

    private String errorApellidos() {
        return vista.getApellidos().isEmpty() ? "Requerido." : null;
    }

    private String errorContrasena() {
        int largo = vista.getTxtContrasena().getPassword().length;
        if (largo == 0) return null;
        if (largo < 4) return "Mínimo 4 caracteres.";
        return null;
    }

    private String errorFecha() {
        String fechaTexto = vista.getFechaNacimiento();
        if (fechaTexto.isEmpty()) return null;
        try {
            // Persona.validarMayoriaDeEdad es la única fuente de verdad de esta regla (antes vivía
            // duplicada acá y de nuevo dentro del constructor de Persona).
            Persona.validarMayoriaDeEdad(LocalDate.parse(fechaTexto, FORMATO_FECHA));
            return null;
        } catch (DateTimeParseException ex) {
            return "Formato esperado: dd/MM/aaaa.";
        } catch (EdadInvalidaException ex) {
            return ex.getMessage();
        }
    }

    private String errorCorreo() {
        String correo = vista.getCorreo();
        if (correo.isEmpty()) return null;
        if (!correo.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) return "Correo inválido (ej. nombre@dominio.com).";
        return null;
    }

    // Único responsable de habilitar/deshabilitar "Registrar"; no toca ningún JLabel.
    private boolean formularioValido() {
        boolean camposCompletos = !vista.getDni().isEmpty() && !vista.getNombres().isEmpty()
                && !vista.getApellidos().isEmpty() && vista.getTxtContrasena().getPassword().length > 0
                && !vista.getFechaNacimiento().isEmpty() && !vista.getCorreo().isEmpty();

        boolean sinErroresDeFormato = errorDni() == null && errorContrasena() == null
                && errorFecha() == null && errorCorreo() == null;

        return camposCompletos && sinErroresDeFormato;
    }

    private void actualizarBotonRegistrar() {
        vista.getBtnRegistrar().setEnabled(formularioValido());
    }

    // ===================== Feedback visual (solo al perder foco o intentar someter) =====================

    private void revelarError(Component campo) {
        if (campo == vista.getTxtDni()) {
            vista.setErrorDni(errorDni());
        } else if (campo == vista.getTxtApellidos()) {
            vista.setErrorApellidos(errorApellidos());
        } else if (campo == vista.getTxtContrasena()) {
            vista.setErrorContrasena(errorContrasena());
        } else if (campo == vista.getTxtFechaNacimiento()) {
            vista.setErrorFecha(errorFecha());
        } else if (campo == vista.getTxtCorreo()) {
            vista.setErrorCorreo(errorCorreo());
        }
    }

    private void revelarTodosLosErrores() {
        vista.setErrorDni(errorDni());
        vista.setErrorApellidos(errorApellidos());
        vista.setErrorContrasena(errorContrasena());
        vista.setErrorFecha(errorFecha());
        vista.setErrorCorreo(errorCorreo());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == vista.getBtnRegistrar()) {
            if (!formularioValido()) {
                revelarTodosLosErrores();
                return;
            }
            iniciarFaseVerificacionDni();
        } else if (e.getSource() == vista.getBtnRegresar()) {
            regresarAlLogin();
        } else if (e.getSource() == vista.getBtnConfirmarCodigo()) {
            confirmarCodigo();
        } else if (e.getSource() == vista.getBtnCancelarCodigo()) {
            cancelarVerificacion();
        }
    }

    // ===================== Fase 0: DNI no debe existir ya (antes de gastar cuota SMTP) =====================

    private void iniciarFaseVerificacionDni() {
        String dni = vista.getDni();

        vista.getBtnRegistrar().setEnabled(false);
        principal.iniciarCarga();

        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                return clienteRepository.findByDni(dni) != null;
            }

            @Override
            protected void done() {
                principal.finalizarCarga();
                boolean yaExiste;
                try {
                    yaExiste = get();
                } catch (Exception ex) {
                    RegistradorErrores.registrar("ControladorRegistro.verificacionDni", ex);
                    vista.getBtnRegistrar().setEnabled(true);
                    JOptionPane.showMessageDialog(vista, "Error al verificar el DNI: " + causaRaiz(ex).getMessage());
                    return;
                }
                if (yaExiste) {
                    vista.getBtnRegistrar().setEnabled(true);
                    JOptionPane.showMessageDialog(vista, "El DNI ingresado ya se encuentra registrado.");
                    return;
                }
                iniciarFaseCorreo();
            }
        }.execute();
    }

    // ===================== Fase 1: envío del código por correo real (SMTP) =====================

    private void iniciarFaseCorreo() {
        String correo = vista.getCorreo();
        String codigo = String.format("%04d", RANDOM.nextInt(10000));

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
                    RegistradorErrores.registrar("ControladorRegistro.faseCorreo", ex);
                    vista.getBtnRegistrar().setEnabled(true);
                    JOptionPane.showMessageDialog(vista,
                        "No se pudo enviar el correo de verificación: " + causaRaiz(ex).getMessage());
                    return;
                }
                // Paso de verificación integrado en el mismo panel: reemplaza al antiguo
                // JOptionPane.showInputDialog para no salir del paradigma SPA.
                codigoPendiente = codigo;
                vista.mostrarPasoVerificacion(correo);
            }
        }.execute();
    }

    // Corre en el EDT, disparado por btnConfirmarCodigo: es interacción de usuario sobre datos ya
    // en memoria, no I/O de red, así que no necesita SwingWorker.
    private void confirmarCodigo() {
        try {
            validarCodigo(vista.getCodigoIngresado(), codigoPendiente);
        } catch (CodigoVerificacionException ex) {
            JOptionPane.showMessageDialog(vista, ex.getMessage());
            return;
        }
        iniciarFasePersistencia();
    }

    private void cancelarVerificacion() {
        codigoPendiente = null;
        vista.mostrarPasoDatos();
        vista.getBtnRegistrar().setEnabled(true);
    }

    private void validarCodigo(String intento, String codigoEnviado) throws CodigoVerificacionException {
        if (intento == null || codigoEnviado == null || !intento.equals(codigoEnviado)) {
            throw new CodigoVerificacionException("Código de verificación incorrecto.");
        }
    }

    // ===================== Fase 2: hash + persistencia en Oracle =====================

    private void iniciarFasePersistencia() {
        String dni = vista.getDni();
        String nombres = vista.getNombres();
        String apellidos = vista.getApellidos();
        String correo = vista.getCorreo();
        LocalDate fechaNacimiento = LocalDate.parse(vista.getFechaNacimiento(), FORMATO_FECHA);
        char[] contrasena = vista.getContrasenaChars();

        principal.iniciarCarga();
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                // try/finally: la contraseña en claro se sobrescribe apenas se calcula el hash,
                // incluso si Persona.hashPassword() lanzara una excepción inesperada. El resto de
                // la persistencia (verificación de unicidad + guardado) ya no toca el arreglo.
                String salt;
                String hash;
                try {
                    salt = Persona.generarSalt();
                    hash = Persona.hashPassword(contrasena, salt);
                } finally {
                    Arrays.fill(contrasena, '0');
                }

                // Defensa en profundidad: la Fase 0 ya validó que el DNI no existía, pero puede
                // haberse registrado otro cliente con el mismo DNI mientras el usuario escribía
                // el código de verificación (condición de carrera).
                if (clienteRepository.findByDni(dni) != null) {
                    return false;
                }
                // El constructor de Cliente es la autoridad real: revalida DNI/correo/mayoría de
                // edad y lanza sus propias excepciones de dominio si algo no cuadra, sin importar
                // que el formulario ya haya pasado los chequeos "de cortesía" del controlador.
                Cliente nuevoCliente = new Cliente(nombres, apellidos, dni, hash, salt, correo, fechaNacimiento);
                return clienteRepository.save(nuevoCliente);
            }

            @Override
            protected void done() {
                principal.finalizarCarga();
                codigoPendiente = null;
                vista.getBtnRegistrar().setEnabled(true);
                boolean exito;
                try {
                    exito = get();
                } catch (Exception ex) {
                    RegistradorErrores.registrar("ControladorRegistro.fasePersistencia", ex);
                    vista.mostrarPasoDatos();
                    JOptionPane.showMessageDialog(vista, "Error al registrar: " + causaRaiz(ex).getMessage());
                    return;
                }
                if (exito) {
                    JOptionPane.showMessageDialog(vista, "¡Cliente registrado con éxito!");
                    regresarAlLogin();
                } else {
                    vista.mostrarPasoDatos();
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

    private void regresarAlLogin() {
        principal.mostrarLogin();
    }
}
