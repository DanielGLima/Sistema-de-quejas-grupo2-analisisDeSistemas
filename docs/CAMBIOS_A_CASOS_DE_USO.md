# Cambios sugeridos a los casos de uso (para que el documento coincida con el sistema)

El sistema se auditó caso por caso contra los documentos CU-00 a CU-15: **todos los flujos básicos y alternos y todos los
mensajes definidos en los documentos coinciden de forma exacta con el programa** (133 comprobaciones automáticas en el
servidor y verificación de cada pantalla en el navegador).

Lo que sigue son los **únicos puntos donde el documento es ambiguo, contradictorio o no define algo que el sistema necesita**.
Para cada uno se indica el texto exacto que conviene copiar al documento.

---

## CU-00 Acceso al Portal

**Paso 6.** El documento manda al Administrador/Gerente a un "panel de reportes e indicadores (CU-13)", pero CU-13 es
*Administrar Usuarios y Catálogos* y no existe un panel de reportes. Texto sugerido:

> 6. El sistema inicia la sesión del usuario, determina su rol y lo redirige al panel principal correspondiente según sus
> permisos de acceso: al panel de consulta y seguimiento de casos (CU-06) si es Cliente; a la bandeja de gestión y filtrado
> de casos (CU-10 / CU-12) si es Operador o Gerente; o al panel de administración interna (CU-13) si es Administrador General.

---

## CU-01 Registrarse

**Agregar dos flujos alternos** (hoy el sistema los usa y el documento no los define):

> **FA05 – Longitud de la contraseña fuera de rango:**
> 1. El sistema muestra el mensaje: "La contraseña debe tener entre 6 y 20 caracteres".
> 2. El sistema retorna al paso 5 del Flujo Normal Básico.
>
> **FA06 – Las contraseñas no coinciden:**
> 1. El sistema muestra el mensaje: "Las contraseñas ingresadas no coinciden".
> 2. El sistema retorna al paso 5 del Flujo Normal Básico.

---

## CU-02 Recuperar Contraseña

Por decisión del equipo el sistema **sí** indica cuando el correo no está registrado.

**FA03** — reemplazar el texto por:

> **FA03: Correo electrónico no registrado:**
> 1. El sistema muestra el mensaje: "Correo no registrado".
> 2. El sistema retorna al paso 3 del Flujo Normal Básico.
> 3. Fin del flujo alterno.

**Requerimiento de Privacidad** — eliminar la línea
"Por seguridad, el sistema no debe revelar si un correo electrónico está registrado o no en la plataforma."

---

## CU-03 Actualizar Perfil de Usuario

**Tabla del paso 3.** La contraseña actual y la nueva solo se piden si el usuario quiere cambiar su contraseña:

| Nombre | Tipo | Obligatoriedad | Tamaño |
|---|---|---|---|
| contraseña actual | alfanumérico | **No** (solo si cambia la contraseña) | 20 caracteres |
| nueva contraseña | alfanumérico con caracteres especiales (al menos una mayúscula, un número y un carácter especial) | **No** (solo si cambia la contraseña) | mínimo 6 y máximo 20 caracteres |

**FA03** — texto del mensaje:

> 1. El sistema muestra el mensaje: "Formato de datos no válido. Revise el campo: <nombre del campo con error>".

---

## CU-05 Registrar Nuevo Caso

- **Paso 8**, precisar el destinatario (el caso aún no tiene responsable):
  > 8. El sistema notifica al personal administrativo (CU-14): a los Administradores Generales y a los Gerentes de la
  > sucursal del caso.
- **Campo número de factura**: tipo "alfanumérico (solo letras y números, sin símbolos)".
- **Agregar FA05:**
  > **FA05: Formato del nombre del empleado no válido:**
  > 1. El sistema muestra el mensaje: "El nombre del empleado involucrado solo puede contener letras (máximo 100 caracteres)".
  > 2. Retorna al paso 3 del Flujo Normal Básico.
- **FA04**, precisar: "…oculta sus datos personales **en todos los paneles administrativos, incluido el del Administrador General**".

---

## CU-07 Cancelar Caso

**Paso 10**, precisar el destinatario:
> 10. El sistema notifica al personal administrativo (CU-14): a los Administradores Generales y a los Gerentes de la sucursal
> del caso (un caso cancelable aún no tiene responsable asignado).

---

## CU-09 Solicitar Reapertura

**Paso 9**: el sistema notifica al responsable asignado al caso; si no tiene, a los Administradores Generales y Gerentes de la sucursal.

---

## CU-10 Gestionar Estado de Casos

**Tabla del paso 5** — corregir los tamaños (los de empleado y estado están repetidos del campo de notas):

| Nombre | Tipo | Obligatoriedad | Tamaño |
|---|---|---|---|
| empleado responsable asignado | catalogo_empleado | No | — |
| nuevo estado del caso | catalogo_estado | Sí | 25 caracteres |
| observaciones y notas internas | texto alfanumérico | No | 500 caracteres |

**Agregar al paso 6 la tabla de transiciones válidas** (es la que valida el sistema):

| Estado actual | Estados permitidos |
|---|---|
| Nuevo | En espera, En Proceso, Cerrado |
| En espera | En Proceso, Cerrado |
| En Proceso | En espera, Resuelto, Cerrado |
| Resuelto | En Proceso, Cerrado |
| Reapertura solicitada | En Proceso, Cerrado |
| Cerrado / Cancelado por el usuario | (ninguno; solo se reabre por solicitud del usuario, CU-09) |

**FA01** — mensaje que muestra el sistema: "Cambio de estado no permitido. Estados permitidos: <lista>".
**FA02** — mensaje: "No posee permisos suficientes para realizar la acción".
**FA03** — mensaje: "El caso fue actualizado por otro usuario. Recargue la información antes de realizar nuevos cambios".

---

## CU-11 Responder Caso

- **FA02** — mensaje: "Debe completar la información requerida: <campos faltantes>".
- **FA03** — mensaje: "El caso debe encontrarse en un estado válido para poder responder".
- **FA01** — precisar: toda respuesta de un Operador queda "Pendiente de aprobación" y el caso continúa "En Proceso" hasta que
  el Gerente (o el Administrador General) la apruebe.

---

## CU-12 Buscar y Filtrar Casos

**Tabla del paso 3:** todos los criterios de búsqueda son **opcionales** (Obligatoriedad = **No**); se pueden combinar los que se
necesiten. Fechas en formato DD/MM/AAAA.

**FA02** — mensajes: "La fecha inicial es inválida. Use el formato DD/MM/AAAA" /
"La fecha inicial no puede ser posterior a la fecha final (formato DD/MM/AAAA)".

---

## CU-13 Administrar Usuarios y Catálogos Internos

- **Estados:** los nombres del catálogo maestro **no se pueden renombrar ni desactivar** (forman parte del flujo de CU-10).
- **Contraseña temporal:** alfanumérica, entre 6 y 20 caracteres.
- **FA03** — mensaje: "Datos inválidos. Corrija: <detalle del dato>".
- **FA02** — mensaje: "El usuario tiene N caso(s) activo(s) asignado(s). Debe reasignarlos antes de desactivar la cuenta".
- **FA01** — mensaje: "Advertencia: existen N caso(s) activo(s) asociados a <elemento>. Confirme la operación para desactivarlo de todas formas".

---

## CU-14 Enviar Notificaciones Automáticas

El mensaje enviado incluye siempre al final el código del caso y la fecha y hora del evento:
`… | Caso: <código> | Fecha y hora: DD/MM/AAAA HH:MM:SS`.
Eventos que generan notificación: registro de caso (usuario y personal administrativo), cancelación, cambio de estado,
asignación/reasignación, respuesta oficial y reapertura solicitada.

---

## CU-15 Registrar Bitácora de Auditoría

- **Módulo del sistema afectado** (texto alfabético, 40): se registra el nombre del módulo, p. ej. "Registro de casos",
  "Gestión de casos", "Respuesta de casos", "Reapertura de casos", "Administración interna", "Administración de catálogos".
- **Valores anteriores y nuevos**: se guardan siempre como JSON con la forma `{"anterior": {...}, "nuevo": {...}}`.
- **Consulta de la bitácora:** solo el Administrador General.

---

## CU-05 Registrar Nuevo Caso (actualización)

- **Adjuntar evidencias:** Obligatoriedad = **No** (opcional). Un caso puede registrarse sin archivos; si se adjuntan, deben ser JPG, PNG o PDF de máximo 2 MB cada uno.

## CU-00 / CU-04 Cierre de sesión (actualización)

- El mensaje “Sesión finalizada correctamente” se muestra en la pantalla del portal tras redirigir (no en una ventana emergente).
