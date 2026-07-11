# Changelog

## [Sin publicar] - 2026-07-10

### Cambios implementados

**Persistencia (migración de `.txt` a Oracle)**
- `conexion/DatabaseConnection.java`: Singleton que abre conexiones Oracle vía `PreparedStatement`, credenciales leídas de `config.properties` (gitignorado).
- `repositorio/Oracle{Cliente,Concierto,Usuario}Repository.java`: implementan las interfaces `ClienteRepository`/`ConciertoRepository`/`UsuarioRepository` ya existentes.
- `schema.sql` y `config.properties.example` agregados como documentación/plantilla del esquema (`usuarios`, `conciertos`, `zonas`).
- Eliminados `ArchivoUsuarios.java`, `ArchivoConciertos.java`, `ClienteArreglo.java` (arreglo estático de 100 clientes).

**Seguridad**
- `Persona`: salt aleatorio por usuario (`generarSalt()`), `hashPassword(char[], salt)` sin crear nunca un `String` con la contraseña en claro.
- `ControladorLogin`: se eliminaron las credenciales de administrador hardcodeadas (`"admin"/"1234"`); el login de admin valida contra la tabla `usuarios` filtrando `rol = 'ADMIN'`.
- Campos de contraseña migrados a `JPasswordField`; los controladores extraen `char[]`, calculan el hash y lo sobrescriben con `Arrays.fill(...,'0')` inmediatamente después de usarlo.
- `Cliente.getVentas()` devuelve ahora una copia defensiva (`new ArrayList<>(ventas)`).

**Interfaz (SPA con CardLayout)**
- `vista/FrmPrincipal.java` (nuevo): único `JFrame` contenedor con `CardLayout`.
- `FrmLogin`, `FrmRegistroCliente`, `FrmCliente`, `FrmAdministrador` convertidos de `JFrame` a `JPanel` (`.form` actualizados para reflejar `JPanelFormInfo`).
- La navegación entre pantallas cambia de card en vez de `dispose()` + creación de una ventana nueva.
- I/O de base de datos (login, registro, compra, liberar entrada, guardar concierto/zona) movida a `SwingWorker`, fuera del Event Dispatch Thread.
- `FrmRegistroCliente`: `JLabel` de error en rojo debajo de cada campo, conectado a `DocumentListener`; el botón "Registrar" solo se habilita cuando el formulario completo es válido (reemplaza los `JOptionPane` de validación de formato).
- `ControladorRegistro`: se quitó la validación de comas en los campos (solo existía para no corromper el `.txt`, ya no aplica con Oracle).

**Sin cambios (lógica de negocio preservada, según instrucción explícita)**
- Algoritmo de Luhn y validación de dígitos por emisor (`Tarjeta.java`, `TipoTarjeta.java`).
- Tabla de descuentos por emisor de tarjeta (`Concierto.getDescuento`/`setDescuento`).
- Excepciones personalizadas (`TarjetaInvalidaException`, `EdadInvalidaException`, `CodigoVerificacionException`).

### Decisiones de arquitectura pospuestas / fuera de alcance de esta iteración

Estas opciones se evaluaron y se descartaron deliberadamente para esta entrega, por alcance o por riesgo. Quedan documentadas para retomarlas si el proyecto lo requiere más adelante:

- **Persistencia de Ventas, Entradas y Tarjeta en Oracle**: se mantienen en memoria por sesión, igual que en la versión anterior con `.txt` (que tampoco las persistía — solo guardaba identidad de cliente y datos de concierto/zona). Migrarlas a tablas propias (`ventas`, `entradas`, con relación a `zonas`) queda pendiente; implica diseñar el estado transaccional de una compra (reserva, pago, liberación) en base de datos en vez de en el objeto `Zona` en memoria.
- **Envío real de correo (SMTP/JavaMail)** para el código de verificación de registro: se mantuvo la simulación (`JOptionPane` mostrando el código) marcada con comentario `ponytail:` en `ControladorRegistro`, indicando que el reemplazo es acotado a esa única llamada.
- **Edición visual de `FrmRegistroCliente`/`FrmCliente`/`FrmAdministrador` en el GUI Builder de NetBeans**: como los `JLabel` de error y su estilo se agregan con un bucle dentro de `initComponents()` (no generable por el editor visual), estos tres formularios ya no se pueden reabrir de forma confiable en el diseñador de NetBeans. Se optó por mantenerlos como código plano en vez de reconstruir el layout completo a mano en XML `.form` para preservar compatibilidad total con el editor visual; se edita como código Java de ahora en más.
- **Migración a Maven/Gradle**: explícitamente fuera de alcance por instrucción del profesor; el proyecto se mantiene 100% Ant/NetBeans, con `ojdbc.jar` agregado manualmente al classpath (`Properties → Libraries → Compile → Classpath`, no `Modulepath`, ya que el proyecto no usa el sistema de módulos de Java).
- **Semilla automática de la cuenta admin en el arranque de la aplicación**: se descartó insertar un admin por defecto desde `Principal.main()` (habría reintroducido una credencial fija en el código). En su lugar, `schema.sql` documenta el `INSERT` de ejemplo y la cuenta se crea una sola vez a mano contra la base.


