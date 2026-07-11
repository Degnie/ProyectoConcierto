# ProyectoConcierto

Sistema de venta de entradas para conciertos (proyecto de curso, NetBeans / Java Swing).

## Requisitos del trabajo (encargo del profesor)

1. **Validar el emisor de la tarjeta** para aplicar descuentos según el tipo, idealmente configurable por concierto:

   | Emisor            | Descuento |
   |-------------------|-----------|
   | VISA              | 5%        |
   | MASTERCARD        | 10%       |
   | DINNERS           | 15%       |
   | AMERICAN EXPRESS  | 7%        |

2. **Registro de cliente/usuario con verificación por correo**: al registrarse se debe enviar un código de 4 dígitos que el usuario debe validar para completar el registro.

3. **Validaciones con manejo de excepciones**, por ejemplo:
   - El cliente debe ser mayor de edad.
   - Cantidad de dígitos de la tarjeta según el tipo de emisor.

4. Crear el proyecto en NetBeans.

5. Agregar las clases del diagrama de clases propuesto (`Persona`, `Usuario`, `Cliente`, `Tarjeta`, `Concierto`, `Zona`, `Entrada`, `Venta`).

## Estado actual

- [x] Proyecto NetBeans creado
- [x] Clases base del diagrama (`modelo/`)
- [x] `TipoTarjeta`, `TarjetaInvalidaException`, `EdadInvalidaException`, `CodigoVerificacionException`
- [x] Descuento por emisor de tarjeta configurable por concierto (`Concierto.setDescuento`)
- [x] Validación de dígitos/Luhn por tipo de tarjeta (`Tarjeta`, `TipoTarjeta`)
- [x] Persistencia en Oracle (`conexion.DatabaseConnection` + `Oracle*Repository`), reemplaza los `.txt`
- [x] Contraseñas con salt por usuario (`Persona.hashPassword`), sin credenciales de admin hardcodeadas
- [x] SPA con `CardLayout` (`FrmPrincipal`) en vez de ventanas que se destruyen y recrean
- [x] I/O de base de datos fuera del EDT (`SwingWorker`) y validación en vivo del formulario de registro
- [ ] Envío real de correo con código de verificación (actualmente pendiente/simulado, ver comentario `ponytail:` en `ControladorRegistro`)

## Base de datos

1. Copiar `config.properties.example` a `config.properties` (ignorado por git) y completar credenciales de Oracle.
2. Ejecutar `schema.sql` contra ese usuario.
3. Insertar manualmente la cuenta de administrador (ver instrucciones al final de `schema.sql`); no hay usuario/clave de admin hardcodeados en el código.
4. Agregar `ojdbc.jar` al classpath del proyecto en NetBeans (Properties → Libraries).

> README provisional, se irá actualizando conforme avance el proyecto.
