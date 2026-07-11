# Changelog

## [Sin publicar] - 2026-07-11 (iteración 7: transacciones ACID, N+1 y SPA sin modales)

Elimina la persistencia híbrida: hasta esta iteración, `Cliente.ventas` solo vivía en memoria por
sesión (un relogin arrancaba con "Mis Compras" vacío aunque hubiera compras reales) y el estado de
entradas vendidas no sobrevivía un reinicio de la app. Ahora ventas y entradas se persisten en
Oracle dentro de transacciones ACID manuales, con bloqueo optimista real en BD.

### Cambios implementados

**Esquema (`schema.sql`)**
- `usuarios.correo` ahora `UNIQUE`.
- Tabla `ventas` (id_venta, dni_cliente, id_concierto, id_zona, fecha_hora, monto_neto, puntos_redimidos, puntos_ganados, estado, payment_txn_id).
- Tabla `entradas` (id_entrada, id_venta, id_zona, numero, estado) — **solo se persisten las entradas vendidas**, no todo el mapa de asientos: una zona de 25 000 con 40 vendidas tiene 40 filas, no 25 000.
- `zonas.version NUMBER` para bloqueo optimista real en BD (reemplaza el `private int version` que vivía, sin ningún efecto real, en `Zona.java`).
- Migración aplicada contra la base de desarrollo real: constraint UNIQUE, columna `version`, y ambas tablas nuevas creadas y verificadas.

**Transacciones ACID (`VentaRepository`/`OracleVentaRepository`, nuevos)**
- `guardarCompraCompleta(Cliente, Concierto, Venta)`: una sola transacción manual (`setAutoCommit(false)`) que hace `SELECT ... FOR UPDATE` de `zonas.version`, `UPDATE` condicionado a esa versión (si otra transacción concurrente la cambió, el `UPDATE` afecta 0 filas y se aborta como conflicto — sobreventa cortada a nivel de BD, no solo por el `synchronized` en memoria de `Zona`), `INSERT` de la venta, `INSERT` por lotes de las entradas, y `UPDATE` del saldo de puntos. Cualquier fallo dispara `rollback()` de todo el bloque.
- `anularVentaPersistida(Cliente, Venta)`: transacción equivalente para la reversa (marca venta y entradas como `CANCELLED`, guarda el nuevo saldo de puntos).
- Todo `PreparedStatement`, nuevo o existente (`OracleClienteRepository`, `OracleConciertoRepository`, `OracleUsuarioRepository`, `OracleVentaRepository`), ahora tiene `setQueryTimeout(10)` para no dejar hilos colgados si la red o la base se cuelgan.

**Prevención de N+1 (`cargarHistorialPorCliente`)**
- Un solo `JOIN` entre `ventas` y `entradas` (por `dni_cliente`) hidrata el historial completo de un cliente, en vez de una consulta de entradas por cada venta. Se llama justo después de un login exitoso, dentro del mismo hilo de background que ya consulta la BD (`ControladorLogin.loginCliente`), y reemplaza el `ArrayList` vacío del `Cliente` recién reconstruido vía `Cliente.hidratarVentas(...)` (nuevo).
- Las entradas hidratadas reutilizan, cuando existen, la MISMA instancia que ya vive en la `Zona` compartida en memoria (`listaConciertos`) — así `anularVenta()` sobre una compra "vieja" (de una sesión anterior) libera el asiento real, no una copia desconectada.
- El panel de administrador ya no lee `Cliente.getVentas()` (solo está hidratado para el cliente con sesión activa): `VentaRepository.cargarResumenVentasParaAdmin()` trae todas las ventas pagadas de todos los clientes en un `JOIN` (ventas+usuarios+zonas+conciertos), también de una sola pasada.

**Modelo (`Zona`, `Entrada`, `Concierto`, `Persona`)**
- `Zona`: se eliminó `private int version` (bloqueo optimista real y único en la columna de Oracle). Toda comparación de estado ahora usa `Entrada.EstadoEntrada` (enum) en vez de `entrada.getEstado().equalsIgnoreCase("DISPONIBLE")` (string).
- `Zona`/`Entrada`: nuevos constructores/métodos de reconstrucción (`Zona(UUID, ...)`, `Entrada(UUID, int, EstadoEntrada)`, `Zona.marcarEntradaVendida(...)`) para que el estado de "vendida" sobreviva a un reinicio: al cargar, se generan las `capacidad` entradas disponibles de siempre y se marcan como vendidas, con su id real, exactamente las que constan en la tabla `entradas`.
- **Bug preexistente corregido de paso**: `Concierto` generaba un `UUID.randomUUID()` nuevo cada vez que `findAll()` reconstruía la lista (no importaba mientras nada dependiera de ese id como foreign key). Con `ventas.id_concierto` como FK real, esto rompía la integridad referencial (`ORA-02291`) — se agregó `Concierto(UUID id, String nombre, Date fecha)` para reconstrucción con id estable, detectado y corregido durante las pruebas de integración de esta misma iteración.
- `Persona`: `validarMayoriaDeEdad(LocalDate)` extraído a método estático público — antes existía duplicado (una copia privada en `ControladorRegistro`, y la misma regla otra vez inline en el constructor de `Persona`). Ahora hay una sola fuente de verdad; `ControladorRegistro` la llama para el chequeo en vivo del formulario.

**SPA sin modales (`FrmRegistroCliente`, `ControladorRegistro`)**
- Se eliminó el `JOptionPane.showInputDialog` que pedía el código OTP. En su lugar, `FrmRegistroCliente` tiene un paso de verificación integrado en el mismo panel (`lblInstruccionOtp`, `txtCodigoOtp`, `btnConfirmarCodigo`, `btnCancelarCodigo`, agregados en código Java plano dentro de `initComponents()`, sin tocar el `.form`): `mostrarPasoVerificacion(correo)` oculta los campos de datos y muestra el paso de código; `mostrarPasoDatos()` hace lo inverso. Enter en el campo de código dispara "Confirmar", igual que ya pasaba con la contraseña y "Registrar".

**Corrección de "amnesia" y fuga de foco (`FrmPrincipal`)**
- `iniciarCarga()`/`finalizarCarga()` ya no hacen `setEnabled(true)` ciego y recursivo al terminar: el estado de cada componente se guarda en un `IdentityHashMap` antes de deshabilitar, y se restaura con fidelidad. Un componente que ya estaba deshabilitado por una regla de negocio (ej. `chkAplicarPuntos` sin puntos suficientes) sigue deshabilitado después de una carga, en vez de reactivarse a la fuerza.
- `mostrarLogin()`/`mostrarRegistro()` piden foco explícito (`requestFocusInWindow()`) para el primer campo de texto tras limpiar el formulario, así Enter/Tab funcionan sin que el usuario tenga que hacer clic primero.

**Usabilidad del checkout (`FrmCliente`/`ControladorCliente`)**
- Cuando `chkAplicarPuntos` se deshabilita, su `ToolTipText` explica el motivo puntual ("Todavía no acumulaste puntos de fidelidad" vs. "Tus puntos no alcanzan para esta compra") en vez de un bloqueo mudo.

### Verificación

- Prueba de integración completa contra Oracle real (no versionada): concierto+zona de prueba con capacidad 3; compra de 2 entradas vía `guardarCompraCompleta` (monto 190 con descuento VISA); tras un `findAll()` fresco (simulando reinicio) la zona muestra 1 disponible; tras "relogin" simulado (`Cliente` reconstruido + `hidratarVentas` vía el `JOIN`), el historial trae 1 venta con sus 2 entradas reales; la venta aparece en el resumen de administrador; `anularVentaPersistida` revierte todo — la zona vuelve a mostrar 3 disponibles.
- Prueba de UI (fuera del repo): `iniciarCarga()`/`finalizarCarga()` preserva el estado `false` de un componente ya deshabilitado y restaura `true` en uno que sí estaba habilitado; el paso OTP oculta/muestra los componentes correctos al alternar.
- Compilación completa del proyecto sin errores.

### Recomendación rechazada en esta sesión

- **Migrar la lectura de contraseñas de `config.properties` a variables de entorno del sistema operativo**: rechazado explícitamente. Motivo: necesidad operativa de compartir fácilmente el proyecto (código + configuración) entre computadoras de laboratorio en un entorno universitario, donde configurar variables de entorno por máquina es más fricción que copiar un archivo. `config.properties` se mantiene como texto plano, fuera de control de versiones (`.gitignore`).

## [Sin publicar] - 2026-07-11 (iteración 6: refactorización, criptografía y robustez)

### Cambios implementados

**Criptografía (`Persona.java`, OWASP A02)**
- `hashPassword` migró de SHA-256 simple a `PBKDF2WithHmacSHA256` (API estándar de `javax.crypto`, sin dependencias externas), con 65 536 iteraciones y clave derivada de 256 bits. El salt (ya se guardaba como hex de 16 bytes) ahora se decodifica de vuelta a bytes crudos para la derivación, en vez de tratarse como texto.
- El `PBEKeySpec` se purga con `clearPassword()` en el `finally`, y los bytes de la clave derivada y del salt decodificado se sobrescriben con `Arrays.fill(..., 0)` en el mismo bloque — nada de material criptográfico intermedio sobrevive más allá de la llamada.
- **Compatibilidad**: la longitud del hash resultante sigue siendo 64 caracteres hex (32 bytes), igual que con SHA-256, así que `contrasena_hash VARCHAR2(64)` en `usuarios` no necesitó cambio de esquema. Los hashes ya existentes en la base (generados con el algoritmo viejo) dejan de ser válidos para el login — se regeneró a mano el hash del admin de desarrollo (`ResetAdminHash`, no versionado) contra la base real y se confirmó el login con el nuevo algoritmo end-to-end. Cualquier cliente registrado antes de esta iteración deberá re-registrarse o pedir un reseteo manual de contraseña (no había ningún flujo de "olvidé mi contraseña" antes de esto tampoco).

**Clean Code (`ControladorCliente.java`)**
- `actionPerformed` pasó de un bloque monolítico de `if/else if` a un enrutador puro: cada rama delega a `procesarRegistroTarjeta()`, `procesarCompraEntrada()` o `procesarLiberacionEntrada()`, con la lógica completa (antes inline) movida tal cual a cada método. Reduce la complejidad ciclomática del método principal sin cambiar ningún comportamiento.

**Logs físicos (`util/RegistradorErrores.java`)**
- `registrar(...)` ahora, además de `System.err`, hace *append* a `errores_app.log` (directorio de trabajo actual) con fecha/hora, contexto, tipo de excepción, mensaje y stacktrace completo. Un fallo al escribir el archivo no interrumpe el flujo ni oculta el error original — se traga la `IOException` y avisa por `System.err`. Verificado con una prueba que confirma la creación del archivo y su contenido.

**Fail-fast en el arranque (`Principal.java`)**
- Antes de instanciar los repositorios Oracle o levantar `FrmPrincipal`, `main()` llama a `validarConfiguracion()`: verifica que `config.properties` exista (vía `ConfiguracionApp`) y que tenga las 7 claves críticas (`db.url`, `db.user`, `db.password`, `mail.smtp.host`, `mail.smtp.port`, `mail.smtp.user`, `mail.smtp.password`). Si falta el archivo o alguna clave, muestra un `JOptionPane` con el detalle y aborta el arranque limpiamente, en vez de fallar más adelante con una `IllegalStateException` de JDBC menos clara para quien esté desplegando el sistema.

**Reactividad del checkout (`ControladorCliente.actualizarResumenCompra`)**
- Ahora valida explícitamente `cantidad > 4` (además del `cantidad < 1` ya cubierto) y limpia el resumen como si no hubiera selección válida — evita mostrar un preview de un descuento que la compra real rechazaría igual.

**Commit en vivo del `JSpinner` (`FrmCliente.java`)**
- En el constructor, tras fijar el `SpinnerNumberModel`, se obtiene el `JFormattedTextField` del editor por defecto y se llama `setCommitsOnValidEdit(true)` sobre su `DefaultFormatter`. El `ChangeListener` ya cableado en `ControladorCliente` (iteración 5) ahora dispara con cada tecla válida, no solo al perder foco o presionar Enter.

### Verificación

- PBKDF2: hash de 64 hex chars, determinista (mismo salt+clave), distinto ante clave o salt distintos; ~190ms por derivación (costo esperado y deliberado de 65 536 iteraciones).
- Login de admin end-to-end contra Oracle real con el hash regenerado: credencial correcta aceptada, credencial incorrecta rechazada.
- `RegistradorErrores`: archivo `errores_app.log` creado con el contexto y mensaje esperados.
- `validarConfiguracion()` (vía reflexión, sin tocar el `config.properties` real): devuelve `null` con la configuración actual, completa.
- `FrmCliente`: `DefaultFormatter.getCommitsOnValidEdit()` devuelve `true` tras construir el panel.
- Compilación completa del proyecto sin errores.

### Recomendaciones rechazadas en esta sesión

- **Pool de conexiones (HikariCP)** para evitar la latencia de renegociación JDBC en los guardados múltiples: rechazado por la restricción de alcance manual — el proyecto se mantiene en JDBC puro con `try-with-resources`, sin librerías de terceros para pooling.
- **`TransactionManager`** para evitar inconsistencias en escrituras múltiples: se mantuvo el `save()` secuencial (cliente y luego concierto, cada uno en su propia conexión/transacción) por las limitaciones de alcance ya impuestas al proyecto.
- **Data Binding explícito** para desacoplar las lecturas manuales de tablas del controlador: se mantiene el llenado manual de `DefaultTableModel` en `ControladorCliente`/`ControladorAdministrador`, consistente con el resto del código Java plano del proyecto.
- **`GlassPane`** en vez de `Cursor.WAIT_CURSOR` global para el feedback visual asíncrono: se mantiene el mecanismo ya implementado en `FrmPrincipal.iniciarCarga()/finalizarCarga()` (iteración 2).

## [Sin publicar] - 2026-07-11 (iteración 5: checkout interactivo con puntos de fidelidad)

Antes de tocar código se auditó el pedido contra el estado actual del repo. La mayor parte del
backend de puntos/seguridad ya estaba resuelto en la iteración 4 (equivalencia 10 puntos = 1 sol,
tope del 50% en `Venta`, `EntradaArreglo`/`VentaArreglo` eliminados, `MERGE` protegido contra
colisión con `ADMIN`, `try/finally` en la purga de `char[]`, remoción explícita de cards en
`FrmPrincipal`) — se dejó todo eso intacto, sin reescribir nada que ya funcionara. Lo nuevo real de
esta iteración es la **interfaz de checkout reactiva**: hasta ahora el sistema de puntos existía
en el modelo pero no había forma de usarlo desde la UI.

### Cambios implementados

**Calculadora reactiva en el modelo (`Venta.java`)**
- Se agregaron métodos estáticos, sin efectos secundarios, para que la UI pueda previsualizar el total en caliente sin intentar una compra real: `calcularDescuentoTarjeta(...)`, `calcularMontoConDescuentoTarjeta(...)`, `calcularMaximoPuntosRedimibles(...)`, `calcularDescuentoPorPuntos(...)`, `calcularTotalFinal(...)`.
- El constructor de `Venta` (la compra real) ahora llama a `calcularDescuentoPorPuntos(...)` en vez de reimplementar la división — el preview y la venta comprometida usan exactamente la misma fórmula, no pueden desincronizarse.
- Nuevo campo `aplicoPuntos` (booleano, fijado una sola vez en el constructor a partir de `puntosRedimidos > 0`) con su getter `isAplicoPuntos()`, para dejar constancia en el propio registro de venta de si esa compra puntual usó canje de puntos.

**Checkout interactivo (`FrmCliente.java`, código Java plano, sin editor visual)**
- `JCheckBox chkAplicarPuntos`: etiqueta dinámica `"Aplicar puntos de fidelidad (Disponibles: X)"`; deshabilitado y desmarcado por defecto.
- `JLabel lblDescuentoPuntos` / `JLabel lblTotal`: desglose del descuento por puntos y total a pagar.
- Getters/setters nuevos: `getChkAplicarPuntos()`, `setCheckPuntosHabilitado(habilitado, puntosDisponibles)`, `isAplicarPuntosSeleccionado()`, `setResumenCompra(...)`, `limpiarResumenCompra()`, `getSpnCantidadEntradas()` (para poder engancharle un `ChangeListener` desde el controlador).

**Reactividad (`ControladorCliente.java`)**
- Se agregaron listeners a la selección de zona (`ListSelectionListener`), a la cantidad de entradas (`ChangeListener` del `JSpinner`) y al checkbox (`ItemListener`), todos disparando `actualizarResumenCompra()`.
- `actualizarResumenCompra()` corre **síncrono en el EDT** (sin `SwingWorker`): lee tarjeta/zona/cantidad/puntos del cliente ya en memoria, llama a las calculadoras estáticas de `Venta`, habilita/deshabilita el checkbox según si `calcularMaximoPuntosRedimibles(...) > 0`, y pinta `lblDescuentoPuntos`/`lblTotal`. Si el cliente no tiene tarjeta registrada o no hay zona seleccionada, limpia el resumen.
- Si cambia la cantidad o la zona y el tope de puntos redimibles cae a 0 con el checkbox ya marcado, se desmarca y deshabilita solo — nunca queda un checkbox marcado prometiendo un descuento que ya no aplica.
- El botón "Comprar" ahora calcula los puntos a redimir con la misma fórmula que usó el preview (`calcularMaximoPuntosRedimibles`) y se los pasa a `Cliente.comprar(zona, cantidad, concierto, puntosARedimir)` — la sobrecarga que ya existía desde la iteración 4, no se tocó su firma.

### Verificación

- Prueba con reflexión sobre los `JLabel` privados de `FrmCliente` (fuera del repo, no committeada): con un cliente con 50 puntos, tarjeta VISA (5%) y una zona a S/ 200, el total antes de marcar el checkbox mostró "Total: S/ 190" (solo descuento de tarjeta); al marcarlo, "Total: S/ 185" y "Descuento por puntos: -S/ 5 (50 pts)" — coincide con el cálculo esperado a mano.
- Compilación completa del proyecto sin errores tras los cambios.

### Recomendaciones arquitectónicas descartadas en esta sesión

- **Recalcular el total en un `SwingWorker`**: rechazado explícitamente por instrucción de esta iteración. El cálculo del preview es aritmética pura sobre datos ya en memoria (sin I/O), así que corre síncrono en el EDT; los hilos de fondo se reservan para la persistencia real en Oracle al confirmar la compra (`guardarEnSegundoPlano(...)`, sin cambios).
- **Guardar `aplicoPuntos`/el estado del checkbox como parte del formulario o de `Cliente`**: se descartó — es un dato de una venta puntual, vive en `Venta` (ya persistida junto con `puntos_redimidos`... nota: por ahora `Venta` no se persiste en Oracle, sigue en memoria por sesión como en iteraciones previas; ver "Persistencia de Ventas/Entradas" en el bloque de decisiones pospuestas de la iteración 2).
- **Editar `FrmCliente.form` para mantener el diseñador visual de NetBeans sincronizado**: los 3 componentes nuevos se agregaron con sentencias declarativas simples (sin bucles), pero por alcance de tiempo no se replicaron en el XML `.form`. Sigue el mismo criterio ya aceptado para `FrmRegistroCliente`/`FrmAdministrador`: compila y corre bien por Ant, pero el diseñador visual de NetBeans puede no reflejar estos 3 componentes hasta que alguien los agregue a mano en el `.form` o los reconstruya desde el editor gráfico.

## [Sin publicar] - 2026-07-11 (iteración 4: modelo de dominio rico + puntos de fidelidad)

Migra el proyecto de un modelo anémico (validaciones y reglas en los controladores) a un modelo de
dominio rico: `Persona`, `Zona` y `Venta` ahora se protegen a sí mismas y lanzan sus propias
excepciones. Agrega el sistema de puntos de fidelidad (acumulación + redención con tope del 50%) y
elimina las estructuras globales de memoria detectadas como fuente de verdad duplicada.

> Varios puntos de esta tarea (MERGE contra colisión de rol ADMIN, verificación de DNI antes del
> SMTP, `try/finally` en la purga de `char[]`, remoción explícita de cards en `FrmPrincipal`,
> errores de registro solo al perder foco) ya se habían implementado en la iteración 3 de este
> mismo día. Se revisó el código y se confirma que siguen vigentes; no se duplicó trabajo.

### Cambios implementados

**Modelo de dominio rico (paquete `modelo/`)**
- `Persona.java`: el constructor ahora exige `LocalDate fechaNacimiento` y valida ahí mismo, con autoridad final, DNI (8 dígitos), formato de correo y mayoría de edad (18 años) — lanza `DniInvalidoException`, `CorreoInvalidoException` o `EdadInvalidaException` (las dos primeras, nuevas). Antes esta validación solo existía como código suelto en `ControladorRegistro`; ahora es imposible construir una `Persona` inválida sin importar quién la instancie.
- `Cliente.java` / `Usuario.java`: constructores actualizados para pasar `fechaNacimiento` a `Persona` y propagar sus excepciones.
- `Zona.java`: nuevo método `public synchronized Entrada comprarEntrada(Cliente cliente) throws ZonaAgotadaException` — reserva y vende una entrada de forma atómica, o lanza `ZonaAgotadaException` (nueva) si no queda cupo. Reemplaza a `venderEntrada(int)`, que vendía un lote completo sin dar al modelo ningún punto de extensión por-entrada.
- `Venta.java`: valida el tope de redención de puntos en el propio constructor (ver más abajo) — única fuente de verdad de esa regla, no se recalcula en ningún controlador.

**Eliminación de contenedores de memoria globales redundantes**
- Se eliminaron `modelo/EntradaArreglo.java` y `modelo/VentaArreglo.java`: no tenían ninguna referencia en el resto del código (confirmado por `grep` antes de borrar), eran una segunda fuente de verdad muerta que coexistía con las relaciones reales (`Zona.entradas`, `Cliente.ventas`) desde que se migró a Oracle. Esto es lo que el profesor marcó como violación de SSOT.

**Sistema de puntos de fidelidad**
- **Acumulación**: `puntosGanados = montoNeto / 10` (1 punto por cada 10 soles/dólares efectivamente pagados, calculado sobre el monto ya con el descuento por tarjeta y por puntos aplicado — no sobre el precio de lista). Se calcula y almacena dentro de `Venta`, no en `Cliente` ni en el controlador.
- **Redención**: 10 puntos = 1 sol/dólar de descuento. `Cliente.comprar(Zona, int, Concierto, int puntosARedimir)` (sobrecarga nueva; la versión de 3 parámetros existente sigue funcionando y redime 0 puntos, así que `ControladorCliente` no tuvo que cambiar su llamada).
- **Invariante de redención (tope 50%)**: validada dentro del constructor de `Venta`; si el descuento por puntos supera el 50% del monto bruto, lanza `LimiteRedencionException` (nueva) — un cliente jamás puede llevarse una entrada gratis. `Cliente.comprar(...)` revierte (libera) las entradas ya reservadas si la validación falla a mitad de camino.
- `Cliente.anularVenta(...)` ahora revierte exactamente lo que la compra había hecho: resta los puntos ganados y devuelve los puntos redimidos, leyendo ambos valores de la propia `Venta` (no se recalculan).
- **Nota de alcance**: no se agregó un control de UI (spinner/campo) para que el cliente elija cuántos puntos redimir — la tarea de esta iteración no lo pidió en `tareas_frontend`. El modelo ya soporta la funcionalidad completa; falta solo cablear un input en `FrmCliente` cuando se pida.

**Persistencia (`OracleClienteRepository.java`, `schema.sql`)**
- Se agregó la columna `fecha_nacimiento DATE NOT NULL` a la tabla `usuarios`. `schema.sql` se actualizó (CREATE TABLE nuevo + bloque de migración incremental `ALTER TABLE` para bases ya desplegadas).
- **Aplicado contra la base de desarrollo real**: `ALTER TABLE usuarios ADD fecha_nacimiento DATE`, backfill de las 2 filas existentes (`admin` y el cliente de prueba) con `1990-01-01`, y luego `MODIFY fecha_nacimiento NOT NULL`. Verificado con una consulta posterior.
- `save()`/`findByDni()`/`findAll()`/`mapear()` actualizados para persistir y reconstruir `fecha_nacimiento` (`java.sql.Date` ↔ `LocalDate`). La protección del `MERGE` contra colisión con cuentas `ADMIN` (`WHERE u.rol = 'CLIENTE'`) se mantiene sin cambios — ya estaba desde la iteración 3.

**Controladores (ahora más delgados)**
- `ControladorRegistro.java`: la Fase 2 de persistencia parsea la fecha del formulario y se la pasa al constructor de `Cliente`; si el constructor lanza una excepción de dominio (caso límite que el formulario no haya detectado), se muestra igual como mensaje de error — el controlador ya no es la única barrera.
- `ControladorCliente.java`: el botón "Comprar" ahora captura `ZonaAgotadaException` y `LimiteRedencionException` alrededor de `clienteLogueado.comprar(...)` y muestra `ex.getMessage()` directamente — cero lógica de validación de negocio en el controlador, solo traducción a UI.

### Verificación

- Batería de pruebas de dominio (fuera del repositorio, no committeadas): rechazo de menor de edad, DNI y correo inválidos; `Zona.comprarEntrada` agotándose correctamente tras 2 ventas en una zona de capacidad 2; `Venta` rechazando una redención del 60% y aceptando una del 40%; flujo completo `Cliente.comprar(...)` con descuento por tarjeta VISA (5%) + puntos ganados (38 sobre una compra de 380) y `anularVenta(...)` revirtiendo puntos y liberando las 10 entradas de la zona a su capacidad original.
- Round-trip contra Oracle real: `OracleClienteRepository.save()`/`findByDni()` con `fecha_nacimiento = 1995-03-20` guardada y releída sin pérdida.
- Compilación completa del proyecto (65 clases) sin errores.

### Recomendaciones arquitectónicas descartadas en esta sesión

- **Delegar el cálculo de puntos al controlador**: se rechazó explícitamente — viola la cohesión orientada a objetos pedida en el curso y reabre la puerta a que dos controladores calculen la regla de forma distinta (ya pasó antes con `cantidad * 10` vs. el monto real). El cálculo vive únicamente en `Venta`.
- **Triggers de Oracle para calcular/actualizar puntos en la base de datos**: se descartó por la misma razón, más el hecho de que el entorno debe permanecer JDBC clásico sin lógica de negocio escondida en la capa de persistencia — un trigger sería invisible para quien lee `Cliente.java`/`Venta.java` y rompería la trazabilidad del modelo Java como única fuente de verdad.
- **Pool de conexiones / ORM**: reafirmado el rechazo ya registrado en la iteración 3 (ver bloque anterior); no se reevaluó porque no cambió el alcance pedido por el profesor.
- **Nueva excepción unificada para todos los errores de validación de `Persona`** (en vez de `DniInvalidoException`/`CorreoInvalidoException`/`EdadInvalidaException` separadas): se prefirió mantener excepciones específicas por regla, consistente con el patrón ya establecido (`TarjetaInvalidaException`, `CodigoVerificacionException`) — permite que cada `catch` en la UI dé feedback preciso sin inspeccionar el mensaje.

## [Sin publicar] - 2026-07-11 (iteración 3: integridad de datos, orden de flujo y hardening)

Auditoría posterior a la iteración 2, sobre el código ya con Oracle y SMTP real en producción de
prueba. Corrige un problema de integridad de datos real (verificado contra la base), reordena el
flujo de registro para no gastar cuota SMTP en vano, y blinda la purga de contraseñas ante fallos
inesperados.

### Cambios implementados

**Integridad de datos — colisión de roles en el MERGE (`OracleClienteRepository.java`)**
- El `MERGE` de `save(Cliente)` no distinguía el rol de la fila existente: si alguien intentaba registrarse (o el flujo de "actualizar cliente" corría) con un DNI que ya pertenecía a un `ADMIN`, el `WHEN MATCHED` sobrescribía `nombres`/`apellidos`/`correo`/`contrasena_hash`/`salt`/`puntos` de esa cuenta administrativa con los datos del cliente — efectivamente secuestrando el login del admin.
- Se agregó `WHERE u.rol = 'CLIENTE'` a la cláusula `UPDATE SET` del `MERGE`. Si el DNI matchea una fila `ADMIN`, esa fila queda fuera tanto del `UPDATE` (falla el `WHERE`) como del `INSERT` (ya hizo match), así que no se toca y `executeUpdate()` devuelve 0 filas → `save()` retorna `false`.
- Verificado directamente contra Oracle: un intento de `MERGE` con `dni='admin'` y datos falsos afectó **0 filas fusionadas** y el admin quedó con `nombres='Admin'`, `rol='ADMIN'` intactos; el camino legítimo (cliente existente actualizando sus propios datos) siguió afectando 1 fila con normalidad. Se repitió la prueba a través de la clase Java real (`OracleClienteRepository.save(...)`, no solo SQL crudo) con el mismo resultado.

**Orden del flujo de registro (`ControladorRegistro.java`)**
- Se agregó una **Fase 0** (`iniciarFaseVerificacionDni`): un `SwingWorker` que consulta `clienteRepository.findByDni(dni)` en segundo plano *antes* de siquiera construir `EmailService` o generar el código OTP. Si el DNI ya existe, el flujo aborta ahí mismo con un mensaje en el EDT — ya no se envía un correo real por un registro que de todos modos iba a fallar en la fase de persistencia.
- La verificación de unicidad de DNI en la Fase 2 (persistencia) se mantiene como defensa en profundidad, no se eliminó: cubre la condición de carrera de que otro cliente se registre con el mismo DNI mientras el primero está completando el código de verificación.

**Purga de contraseñas blindada ante excepciones (`ControladorLogin.java`, `ControladorRegistro.java`)**
- Todo bloque que lee `char[]` de `JPasswordField.getPassword()` y lo usa para hashear ahora envuelve esa lectura en `try { ... } finally { Arrays.fill(contrasena, '0'); }`. Antes, la purga estaba en el camino feliz (o dispersa en ramas `if`); una excepción no prevista en medio (caída de conexión JDBC, timeout, error de formato) podía saltarse la limpieza y dejar la contraseña en claro viva en el heap más tiempo del necesario.
- Aplica a `ControladorLogin.loginCliente`/`loginAdmin` y a `ControladorRegistro.iniciarFasePersistencia` (esta última ya purgaba "inmediatamente después de hashear" desde la iteración 2; ahora esa purga está garantizada también si `Persona.hashPassword(...)` lanza).

**Trazabilidad operativa (`util/RegistradorErrores.java`, nuevo)**
- Clase utilitaria mínima con un único método estático `registrar(String contexto, Throwable ex)`: imprime a `System.err` (mensaje + stack trace). Se invoca desde todos los bloques `catch (Exception ex)` de los `SwingWorker` en `ControladorLogin` y `ControladorRegistro`, antes de mostrar el `JOptionPane` al usuario. No reemplaza el diálogo — lo complementa, para que los fallos queden en el log del proceso y no dependan de que alguien haya visto la ventana emergente en el momento exacto del error.

**Mitigación de fugas por acumulación en el `CardLayout` (`FrmPrincipal.java`)**
- La versión anterior ya removía la card anterior antes de agregar la nueva, pero lo hacía escaneando `panelContenedor.getComponents()` en busca de un componente cuyo `getName()` coincidiera — funcionalmente correcto, pero fràgil (dependía de que ningún otro componente compartiera accidentalmente ese `name()`, y no dejaba explícito en el código qué instancia se estaba reemplazando).
- Se reemplazó por dos campos explícitos, `cardClienteActual`/`cardAdminActual`, que guardan la referencia directa al panel montado. `mostrarCliente(...)`/`mostrarAdministrador(...)` remueven esa referencia (si existía) antes de montar la nueva — sin escaneo, sin ambigüedad.
- Verificado con una prueba automatizada: 3 logins de cliente consecutivos dejan exactamente 1 card de cliente en el contenedor (no 3); ídem para administrador.

**Suavizado de feedback visual (`ControladorRegistro.java`)**
- El botón "Registrar" se sigue habilitando/deshabilitando en vivo con cada tecla (sin cambios ahí), pero los `DocumentListener` ya no llaman a `setErrorXxx(...)` directamente — se separó la validación en métodos puros por campo (`errorDni()`, `errorContrasena()`, `errorFecha()`, `errorCorreo()`, `errorApellidos()`) que **calculan** el mensaje sin tocar la UI.
- Los `JLabel` rojos ahora solo se pintan en dos momentos: (a) cuando el campo pierde el foco (`FocusListener.focusLost`), vía un listener compartido que revela el error de ese campo puntual; (b) al intentar enviar el formulario estando inválido (clic en "Registrar" — inalcanzable si está deshabilitado, cubierto igual por defensividad — o Enter en el campo de contraseña con el formulario inválido, que revela todos los errores a la vez).
- Ya no se marca "error de formato" mientras el usuario todavía está completando un campo dígito a dígito.

### Recomendaciones de arquitectura evaluadas y descartadas en esta sesión

- **Pool de conexiones (HikariCP o similar)**: se evaluó para reemplazar `DriverManager.getConnection()` por conexión bajo demanda en `DatabaseConnection`, pero se descartó explícitamente por instrucción del profesor: el entorno debe permanecer JDBC manual puro, sin dependencias de terceros para pooling. Cada repositorio sigue abriendo y cerrando su propia conexión en `try-with-resources`.
- **ORM (Hibernate/JPA)**: se descartó por la misma razón — el alcance académico pedido es control manual y directo sobre `PreparedStatement`, no mapeo objeto-relacional declarativo. Los repositorios `Oracle*Repository` siguen siendo SQL escrito a mano.
- **Reconstrucción de `FrmRegistroCliente` en el editor visual de NetBeans** para agregar los `FocusListener`: se mantuvo el enfoque de código Java plano ya establecido en la iteración 1 — los listeners se agregan enteramente desde `ControladorRegistro` usando los getters públicos de la vista (`getTxtDni()`, etc.), sin tocar `initComponents()` ni el archivo `.form`. Cero riesgo de desincronizar el diseñador todavía más.
- **Mover la verificación de unicidad de DNI a una restricción `UNIQUE`/`PRIMARY KEY` a nivel de base de datos como único mecanismo** (en vez de la consulta explícita `findByDni` en la Fase 0): la tabla `usuarios` ya tiene `dni` como `PRIMARY KEY`, así que esa garantía ya existe a nivel de esquema como red de seguridad final. La Fase 0 se mantiene además porque el objetivo específico de esta tarea no es solo *evitar* el duplicado sino *evitar el envío de correo* cuando se sabe de antemano que va a fallar — algo que una constraint de base de datos no puede prevenir por sí sola.

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


