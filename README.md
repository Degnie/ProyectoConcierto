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
- [ ] Descuento por emisor de tarjeta configurable por concierto
- [ ] Envío real de correo con código de verificación (actualmente pendiente/simulado)
- [ ] Validación completa de dígitos por tipo de tarjeta

> README provisional, se irá actualizando conforme avance el proyecto.
