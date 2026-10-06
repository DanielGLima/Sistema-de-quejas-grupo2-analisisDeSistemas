# Consolidación de casos de uso: de CU-00…CU-15 a CU-00…CU-10

Los 16 casos de uso originales (versión 1.0) se agruparon en 11 (versión 2.0). No se eliminó ninguna funcionalidad:
solo cambia la agrupación y la numeración.

## Equivalencia de casos de uso

| Nuevo | Nombre | Casos de uso originales |
|---|---|---|
| CU-00 | Acceso al Portal y Cierre de Sesión | CU-00 + CU-04 |
| CU-01 | Gestionar Cuenta de Usuario | CU-01 + CU-02 + CU-03 |
| CU-02 | Registrar Nuevo Caso | CU-05 |
| CU-03 | Consultar Estado de Casos | CU-06 |
| CU-04 | Cancelar Caso | CU-07 |
| CU-05 | Evaluar Atención del Caso | CU-08 |
| CU-06 | Solicitar Reapertura de Caso | CU-09 |
| CU-07 | Gestionar y Responder Caso | CU-10 + CU-11 |
| CU-08 | Buscar y Filtrar Casos | CU-12 |
| CU-09 | Administrar Usuarios y Catálogos Internos | CU-13 |
| CU-10 | Notificaciones Automáticas y Bitácora de Auditoría | CU-14 + CU-15 |

Referencias cruzadas dentro de los documentos: lo que antes decía CU-05 ahora dice CU-02; CU-06 → CU-03; CU-10 y CU-11 → CU-07;
CU-12 → CU-08; CU-13 → CU-09; CU-14 y CU-15 → CU-10.

## Equivalencia de flujos alternos en los casos fusionados

**CU-00** (flujo 1 = iniciar sesión, flujo 2 = cerrar sesión)

| Nuevo | Original |
|---|---|
| FA01 a FA05 | FA01 a FA05 de CU-00 (sin cambios de numeración) |
| FA06 | FA01 de CU-04 (sesión ya expirada) |

**CU-01** (flujo 1 = registrarse, flujo 2 = recuperar contraseña, flujo 3 = actualizar perfil)

| Nuevo | Original |
|---|---|
| FA01 | CU-01 FA01 (campos incompletos) |
| FA02 | CU-01 FA01.1 (correo ya registrado) |
| FA03 / FA04 / FA05 | Nuevos: longitud, formato y coincidencia de contraseña en el registro (el CU-01 original solo definía el formato) |
| FA06 / FA07 | CU-01 FA03 / FA04 (cancelar confirmación / volver al inicio) |
| FA08 a FA18 | CU-02 FA01 a FA11 (FA10 = “Correo no registrado”, por decisión del equipo) |
| FA19 / FA20 / FA21 | CU-03 FA01 / FA02 / FA03 |

**CU-07** (flujo 1 = gestionar estado, flujo 2 = responder)

| Nuevo | Original |
|---|---|
| FA01 a FA04 | CU-10 FA01 a FA04 |
| FA05 / FA06 / FA07 | CU-11 FA01 / FA02 / FA03 |

**CU-10** (flujo 1 = notificaciones, flujo 2 = bitácora, flujo 3 = consulta de bitácora)

| Nuevo | Original |
|---|---|
| FA01 / FA02 | CU-14 FA01 / FA02 |
| FA03 / FA04 | CU-15 FA01 / FA02 |
| FA05 | Nuevo: consulta de la bitácora sin permisos |

El resto de casos (CU-02 a CU-06, CU-08 y CU-09) conserva la numeración de sus flujos alternos; CU-02 agrega FA05 (nombre del empleado).

## Cambios de contenido respecto de la versión 1.0

Los documentos v2.0 incorporan además las correcciones de `docs/CAMBIOS_A_CASOS_DE_USO.md`, para que el texto coincida con el sistema:

- CU-00: el paso 6 ya no menciona un “panel de reportes” inexistente; indica a qué panel va cada rol.
- CU-01: se agregan los mensajes de longitud y coincidencia de contraseña en el registro; “Correo no registrado” en la recuperación;
  contraseña actual y nueva no obligatorias en el perfil; mensaje “Formato de datos no válido. Revise el campo: <campo>”.
- CU-02: notificación al personal (Administradores Generales y Gerentes de la sucursal); número de factura solo letras y números;
  nuevo FA05 para el nombre del empleado; la denuncia anónima oculta los datos en todos los paneles.
- CU-04 y CU-06: destinatario preciso de las notificaciones.
- CU-07: tablas de campos corregidas, tabla de transiciones de estado válidas y mensajes exactos de los flujos alternos;
  toda respuesta de un Operador queda pendiente de aprobación.
- CU-08: todos los criterios son opcionales; se agregan responsable, texto libre y los mensajes de fecha inválida.
- CU-09: los estados del catálogo maestro no se renombran ni se desactivan; contraseña temporal de 6 a 20 caracteres; mensajes exactos.
- CU-10: formato del mensaje (código y fecha/hora al final), estados de notificación, valores anteriores y nuevos en JSON y consulta de la bitácora.

## Pendiente en el código (opcional)

El programa no cambia su funcionamiento. Solo quedan comentarios y textos internos que citan la numeración vieja (por ejemplo
“CU-12” en `AdminCasoController`); se pueden actualizar cuando se decida.
