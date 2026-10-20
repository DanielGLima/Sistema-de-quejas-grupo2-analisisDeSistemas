# Diagramas del Sistema QRDS — Restaurante "Las Delicias"

Generados a partir del análisis del código del repositorio `SistemaDeQuejas` (rama `main`, commit `428a426`, 13/10/2026),
de `database/schema.sql` y de los casos de uso v2.1 (CU-00 a CU-10). Cada diagrama está en PNG (para documentos),
SVG (escalable) y fuente PlantUML `.puml` (editable). El mapa de macroprocesos se genera con `gen_macro.py`.

| Archivo | Diagrama | Qué muestra |
|---|---|---|
| 01_diagrama_clases | Clases (dominio) | Las 17 entidades JPA con atributos, tamaños, multiplicidades y enumeraciones |
| 01b_diagrama_clases_capas | Clases (diseño) | Controladores REST → servicios → repositorios y componentes de configuración |
| 02_diagrama_estados_caso | Estados | Ciclo de vida del caso: 7 estados, transiciones de `CasoService.TRANSICIONES`, cancelación, respuesta pendiente de aprobación y reapertura ≤ 15 días |
| 02b_diagrama_estados_notificacion | Estados | Estados de envío de un correo y reintentos programados |
| 03_diagrama_despliegue | Despliegue | Navegador + SPA Angular, servidor Node (ng serve), JVM con Spring Boot en :8081, PostgreSQL y SMTP de Gmail |
| 04_diagrama_secuencia_registrar_caso | Secuencia | CU-02 Registrar Nuevo Caso, de la pantalla a la base de datos, correo y bitácora |
| 04b_diagrama_secuencia_responder_caso | Secuencia | CU-07 respuesta de un Operador y aprobación del Gerente |
| 05_diagrama_procesos | Procesos | Atención de un caso de principio a fin, por carriles (Cliente, Sistema, Gerente/Administrador, Operador) |
| 06_diagrama_macroprocesos | Macroprocesos | Procesos estratégicos, operativos (cadena de valor) y de apoyo |

## Observaciones encontradas durante el análisis

Puntos del código que conviene conocer (no se modificó nada del proyecto):

1. **Sin transacciones**: ningún servicio usa `@Transactional`. Registrar un caso hace varios `save()` (caso, evidencias,
   notificaciones, bitácora); si algo falla a la mitad, pueden quedar datos parciales.
2. **Seguridad de desarrollo**: `SecurityConfig` deja `/api/**` en `permitAll()` y cada controlador verifica la sesión a mano.
   Los catálogos públicos y `/uploads/**` son accesibles sin sesión.
3. **No existe rechazo de respuestas**: una respuesta de Operador "Pendiente de aprobación" solo puede aprobarse.
4. **Transiciones en código**: el campo `orden` de `estado_caso` no se usa; el flujo válido está fijo en `CasoService.TRANSICIONES`.
   Desde "Nuevo" se puede pasar directo a "Cerrado".
5. **Cancelación**: además del estado (Nuevo / En espera), exige que el caso no tenga responsable asignado.
6. **Bitácora**: el inicio de sesión del cliente y el cierre de sesión no se registran (sí el inicio de sesión del personal).
7. **Comentario desactualizado** en `CasoController.crear`: dice que las evidencias son obligatorias, pero son opcionales.
8. **`AdminAuthController`** (`/api/admin/auth/login`) duplica el login unificado de `/api/auth/login` y el frontend no lo usa.
9. **Cambios sin commit** en `CasoService.java` y `consultar-casos.component.ts` (cálculo del plazo de 15 días por hora exacta);
   los diagramas ya reflejan esa versión.
10. La carpeta `Downloads/backend_sistema_quejas` es una versión antigua con JWT; no se usó para los diagramas.
