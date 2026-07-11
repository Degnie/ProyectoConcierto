# Changelog

## [Sin publicar] - 2026-07-10 (iteración 2: SMTP real, rutas de despliegue, UX asíncrona)

Esta iteración retoma dos puntos que la entrega anterior había dejado pospuestos explícitamente
(envío real de correo y semilla del admin) y corrige dos riesgos detectados en la revisión
posterior: retención de contraseñas en memoria más allá de lo necesario, y una ruta de
configuración que solo funcionaba por casualidad al ejecutar el `.jar` empaquetado.

### Cambios implementados

**Envío real de correo (reemplaza la simulación de la iteración anterior)**
- `servicio/EmailService.java` (nuevo): servicio SMTP aislado, único punto del código que conoce Jakarta Mail. Expone un solo método público, `enviarCodigoVerificacion(String correoDestino, String codigo) throws Exception`; toda la configuración de sesión (`host`, `port`, `auth`, `starttls`, autenticación) queda encapsulada dentro de la clase.
- Los parámetros SMTP se leen de `config.properties` (mismo archivo externo que las credenciales Oracle), bajo las claves `mail.smtp.host`, `mail.smtp.port`, `mail.smtp.auth`, `mail.smtp.starttls.enable`, `mail.smtp.user` y `mail.smtp.password`. Se agregaron a `config.properties.example` con comentario explicando el caso Gmail (requiere "App Password", no la clave de la cuenta).
- Requiere agregar manualmente al classpath (mismo patrón que `ojdbc.jar`): `jakarta.mail-api.jar` + una implementación (`angus-mail.jar`, o `javax.mail.jar` si se prefiere la API clásica). Documentado en el `README.md`.
- `ControladorRegistro` ya no muestra el código en un `JOptionPane` "(SIMULADO)"; ahora lo envía de verdad y solo pide al usuario que lo ingrese una vez confirmado el envío.

**Resolución de rutas de despliegue**
- `conexion/ConfiguracionApp.java` (nuevo): utilidad compartida que ubica `config.properties` junto al `.jar` en ejecución, usando `ProtectionDomain#getCodeSource()` en vez de asumir que el directorio de trabajo del proceso es el correcto. Si no encuentra el archivo ahí (caso típico: ejecución sin empaquetar desde el IDE), hace *fallback* al directorio de trabajo actual para no romper el flujo de desarrollo en NetBeans.
- `conexion/DatabaseConnection.java` y `servicio/EmailService.java` usan ahora `ConfiguracionApp.resolverArchivo(...)` en vez de `new FileInputStream("config.properties")` directo.
- Verificado con una prueba manual: `.jar` empaquetado en `C:\tmp\jartest\deploy\`, ejecutado desde un directorio de trabajo distinto (`C:\tmp\unrelated_cwd\`) — `config.properties` se resolvió correctamente junto al `.jar`, no en el `cwd`.

**Concurrencia y flujo asíncrono (`ControladorRegistro`)**
- El registro ahora se estructura en dos fases con `SwingWorker` independientes, ninguna bloquea el EDT:
  1. **Fase correo**: genera el código, lo envía por SMTP en segundo plano; al terminar (`done()`), pide el código por diálogo.
  2. **Fase persistencia**: solo se dispara si el código es correcto; hashea la contraseña y guarda el cliente en Oracle en segundo plano.
- La excepción de dominio `CodigoVerificacionException` se preserva íntegra en el flujo (antes se evaluaba de forma síncrona; ahora se lanza/captura igual, solo que después de una fase async en vez de en línea).

**Seguridad en memoria (corrección)**
- `ControladorRegistro`: `Arrays.fill(contrasena, '0')` se ejecuta ahora *inmediatamente* después de `Persona.hashPassword(...)`, dentro de `doInBackground()` — antes se purgaba recién en `done()`, dejando la contraseña en claro viva en memoria durante todo el viaje de ida y vuelta al EDT.
- Se aplicó la misma corrección en `ControladorLogin.loginAdmin` (mismo patrón, mismo defecto detectado por extensión al revisar la clase hermana).

**UX asíncrona (`FrmPrincipal`)**
- `iniciarCarga()` / `finalizarCarga()`: cursor `Cursor.WAIT_CURSOR` global y deshabilitación recursiva de todos los componentes interactivos mientras corre un `SwingWorker` de red/BD; restaurados en `done()`. Conectado a los flujos de login (cliente y admin) y a las dos fases de registro.
- `mostrarLogin()` / `mostrarRegistro()`: invocan `limpiarFormulario()` / `limpiarCampos()` antes de mostrar la card, eliminando la retención "fantasma" de datos del usuario anterior al navegar por el `CardLayout`.
- `FrmLogin` y `FrmRegistroCliente`: Enter en el campo de contraseña dispara el botón principal (`doClick()`), aprovechando que `JTextField`/`JPasswordField` ya emiten `actionPerformed` al presionar Enter — sin `KeyListener` a medida.

### Decisiones alternativas evaluadas y descartadas en esta sesión

- **Cifrado del password SMTP en `config.properties`**: la tarea pedía la clave "de forma encriptada/externa". Se optó por dejarla externa (fuera del control de versiones, igual que la clave de Oracle) mediante `.gitignore`, y no agregar cifrado a nivel de campo. Justificación: introducir cifrado de un solo secreto (sin tocar el de la BD) crea una inconsistencia de tratamiento entre credenciales del mismo archivo; cifrar ambas requeriría diseñar gestión de claves (dónde vive la clave maestra) que excede el alcance de esta entrega docente. Queda documentado como mejora futura.
- **Reintentos automáticos / cola de reenvío si falla el SMTP**: se descartó por ahora; ante un fallo de envío se muestra el error y el usuario reintenta manualmente presionando "Registrar" de nuevo. Agregar reintentos con backoff es una mejora de robustez razonable pero no bloqueante para la entrega actual.
- **Indicador de carga con `JProgressBar`/spinner visual en vez de solo cursor**: se evaluó agregar una barra de progreso indeterminada superpuesta, pero se prefirió el cursor de espera global (`Cursor.WAIT_CURSOR`) más deshabilitado recursivo de componentes por ser la solución más simple que ya cumple el requisito pedido ("alternar el estado del cursor... deshabilitar los paneles interactivos"); no se justificaba el componente adicional para esta iteración.
- **Extender el binding de Enter a los diálogos modales de código de verificación**: el `JOptionPane.showInputDialog` que pide el código ya responde a Enter de forma nativa (comportamiento estándar de Swing para el botón por defecto del diálogo), así que no se agregó binding manual ahí.
- **Actualizar la iteración anterior del CHANGELOG para quitar la marca de "pendiente" en envío de correo**: se decidió no reescribir el bloque anterior — un changelog documenta decisiones en el momento en que se tomaron; este bloque nuevo deja constancia de que ese punto pendiente ya se resolvió, sin alterar el registro histórico.

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


