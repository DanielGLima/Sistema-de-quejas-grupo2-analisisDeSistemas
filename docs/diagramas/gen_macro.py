import cairosvg
from xml.sax.saxutils import escape

W, H = 1800, 960
F = "DejaVu Sans"
out = []
def t(x, y, s, size=13, weight="normal", anchor="start", color="#1F2933", style="normal"):
    out.append(f'<text x="{x}" y="{y}" font-family="{F}" font-size="{size}" font-weight="{weight}" '
               f'font-style="{style}" xml:space="preserve" text-anchor="{anchor}" fill="{color}">{escape(s)}</text>')
def lines(x, y, arr, size=12.5, lh=17, anchor="start", first_bold=True, color="#1F2933"):
    for i, s in enumerate(arr):
        w = "bold" if (i == 0 and first_bold) else "normal"
        st = "normal"
        if s.startswith("_"):
            s = s[1:]; st = "italic"
        t(x, y + i * lh, s, size=size if i else size + 1, weight=w, anchor=anchor, color=color, style=st)
def rect(x, y, w, h, fill, stroke="#455A64", rx=8, dash=None, sw=1.4):
    d = f' stroke-dasharray="{dash}"' if dash else ""
    out.append(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="{rx}" fill="{fill}" stroke="{stroke}" stroke-width="{sw}"{d}/>')

out.append(f'<rect width="{W}" height="{H}" fill="#FFFFFF"/>')
t(W/2, 38, 'Mapa de Macroprocesos — Sistema QRDS (Quejas, Reclamos, Denuncias y Sugerencias)', 22, "bold", "middle")
t(W/2, 64, 'Restaurante "Las Delicias" · derivado del código y de los casos de uso CU-00 a CU-10 (v2.1)', 14, "normal", "middle", "#52606D")

top, bottom = 90, 920
# Columna de entrada y salida
def side(x, label, body, fill):
    rect(x, top, 150, bottom - top, fill, "#37474F", 10)
    t(x + 75, top + 30, label, 14, "bold", "middle")
    for i, s in enumerate(body):
        t(x + 75, (top + bottom) / 2 - 60 + i * 20, s, 12.5, "normal", "middle")
side(20, "ENTRADA", ["Cliente de cualquier", "sucursal con una", "queja, reclamo,", "denuncia o", "sugerencia", "", "Personal interno", "que debe atenderla"], "#FFF8E1")
side(W - 170, "SALIDA", ["Caso resuelto", "o cerrado", "", "Cliente informado", "en cada cambio", "", "Trazabilidad", "completa y", "retroalimentación", "para mejorar"], "#E8F5E9")
# flechas grandes hacia columnas
def arrow(x1, y1, x2, y2, color="#37474F", w=2.2, dash=None):
    d = f' stroke-dasharray="{dash}"' if dash else ""
    out.append(f'<line x1="{x1}" y1="{y1}" x2="{x2}" y2="{y2}" stroke="{color}" stroke-width="{w}" marker-end="url(#ah)"{d}/>')

bx, bw = 200, W - 400   # zona central

# ---------- Estratégicos ----------
sy, sh = top, 250
rect(bx, sy, bw, sh, "#EDE7F6", "#5E35B1", 10, sw=1.8)
t(bx + 18, sy + 28, "PROCESOS ESTRATÉGICOS", 15, "bold", color="#4527A0")
t(bx + bw - 18, sy + 28, "Dirigen y controlan el sistema", 12, "normal", "end", "#5E35B1", )
cw = (bw - 4 * 24) / 3
est = [
    ["E1. Administración del sistema", "CU-09 · solo Administrador General", "• Alta, edición y desactivación de usuarios", "   internos (Administrador, Gerente, Operador)", "• Sucursales, tipos de caso, categorías y estados", "• Desactivación lógica con aviso si hay", "   casos activos asociados"],
    ["E2. Supervisión y control", "CU-10 · flujo 3", "• Consulta de la bitácora de auditoría", "   con filtros (Administrador General)", "• Historial de notificaciones enviadas", "   (Administrador General y Gerente)", "• Alcance por sucursal para el Gerente"],
    ["E3. Reportes e indicadores", "_Fase futura — fuera del alcance actual", "• Tiempos de atención por estado", "• Satisfacción (evaluaciones 1–5)", "• Casos por tipo, categoría y sucursal", "", "Datos ya disponibles: historial, evaluaciones"],
]
for i, b in enumerate(est):
    x = bx + 24 + i * (cw + 24)
    rect(x, sy + 46, cw, sh - 66, "#F5F5F5" if i == 2 else "#FFFFFF", "#7E57C2", 8, dash="6,4" if i == 2 else None)
    lines(x + 16, sy + 74, b)

# ---------- Operativos ----------
oy, oh = sy + sh + 24, 330
rect(bx, oy, bw, oh, "#E3F2FD", "#1565C0", 10, sw=1.8)
t(bx + 18, oy + 28, "PROCESOS OPERATIVOS — CADENA DE VALOR", 15, "bold", color="#0D47A1")
t(bx + bw - 18, oy + 28, "Generan el resultado para el cliente", 12, "normal", "end", "#1565C0")
ops = [
    ["M1. Acceso y cuenta", "CU-00 · CU-01", "• Iniciar y cerrar sesión", "• Registrarse como cliente", "• Recuperar contraseña", "   (código 6 car., 15 min)", "• Actualizar perfil"],
    ["M2. Recepción del caso", "CU-02", "• Tipo, sucursal y categoría", "• Descripción 10–1000", "• Evidencias JPG/PNG/PDF", "• Denuncia anónima", "• Código TIPO-CAT-id", "• Estado «Nuevo»"],
    ["M3. Atención y resolución", "CU-07 · CU-08", "• Bandeja y filtros", "• Asignar / reasignar", "• Cambiar estado según", "   transiciones válidas", "• Responder y aprobar", "   respuestas de Operador", "• Dictaminar reaperturas"],
    ["M4. Seguimiento", "CU-03 · CU-04", "• «Mis casos» y detalle", "• Historial de estados", "• Respuesta oficial", "• Cancelar (Nuevo o", "   En espera, sin", "   responsable)"],
    ["M5. Cierre y", "retroalimentación", "CU-05 · CU-06", "• Evaluar atención 1–5", "   (una sola vez)", "• Solicitar reapertura", "   ≤ 15 días del cierre"],
]
n = len(ops); gap = 6; tip = 26
chev_w = (bw - 48 - (n - 1) * gap + (n - 1) * tip) / n
cy0, ch = oy + 48, 230
for i, b in enumerate(ops):
    x = bx + 24 + i * (chev_w + gap - tip)
    left_notch = 0 if i == 0 else tip
    pts = [(x, cy0), (x + chev_w - tip, cy0), (x + chev_w, cy0 + ch / 2), (x + chev_w - tip, cy0 + ch),
           (x, cy0 + ch), (x + left_notch, cy0 + ch / 2)]
    fill = ["#FFFFFF", "#F1F8FF", "#FFFFFF", "#F1F8FF", "#FFFFFF"][i]
    out.append('<polygon points="' + " ".join(f"{px:.1f},{py:.1f}" for px, py in pts) + f'" fill="{fill}" stroke="#1565C0" stroke-width="1.6"/>')
    tx = x + (left_notch or 10) + 12
    if i == 4:
        t(tx, cy0 + 30, b[0], 13.5, "bold"); lines(tx, cy0 + 48, b[1:], first_bold=True)
    else:
        lines(tx, cy0 + 30, b)
# reapertura: flecha punteada M5 -> M3 bajo los chevrons
def cx(i): return bx + 24 + i * (chev_w + gap - tip) + chev_w / 2
ry = cy0 + ch + 30
out.append(f'<path d="M {cx(4):.0f} {cy0+ch} L {cx(4):.0f} {ry} L {cx(2):.0f} {ry} L {cx(2):.0f} {cy0+ch+4}" fill="none" stroke="#C62828" stroke-width="2" stroke-dasharray="7,5" marker-end="url(#ahr)"/>')
t((cx(2) + cx(4)) / 2, ry - 7, "Reapertura solicitada → vuelve a atención (En Proceso) o se confirma el cierre", 12, "normal", "middle", "#C62828")
# flechas entrada/salida
arrow(170, oy + oh / 2, bx - 4, oy + oh / 2, w=3)
arrow(bx + bw + 2, oy + oh / 2, W - 174, oy + oh / 2, w=3)

# ---------- Apoyo ----------
ay = oy + oh + 24; ah_ = bottom - ay
rect(bx, ay, bw, ah_, "#E8F5E9", "#2E7D32", 10, sw=1.8)
t(bx + 18, ay + 28, "PROCESOS DE APOYO", 15, "bold", color="#1B5E20")
t(bx + bw - 18, ay + 28, "Transversales: soportan a todos los procesos operativos", 12, "normal", "end", "#2E7D32")
sup = [
    ["A1. Notificaciones", "CU-10 · flujo 1", "• Correo SMTP en cada", "   evento del caso", "• Reintento c/5 min,", "   máx. 3 → revisión"],
    ["A2. Bitácora", "CU-10 · flujo 2", "• Quién, qué, cuándo, IP", "• JSON anterior / nuevo", "• Solo inserción,", "   no editable"],
    ["A3. Evidencias", "Archivos del caso", "• JPG / PNG / PDF", "• Máx. 2 MB c/u", "• Validación de firma", "• Carpeta uploads/"],
    ["A4. Seguridad", "Acceso y control", "• Contraseñas BCrypt", "• Sesión de servidor", "• Permisos por rol", "   y por sucursal"],
    ["A5. Datos y catálogos", "Infraestructura", "• PostgreSQL (17 tablas)", "• Catálogo maestro", "   de 7 estados", "• Historial de estados"],
]
sw_ = (bw - 6 * 20) / 5
for i, b in enumerate(sup):
    x = bx + 20 + i * (sw_ + 20)
    rect(x, ay + 44, sw_, ah_ - 62, "#FFFFFF", "#43A047", 8)
    lines(x + 14, ay + 70, b)
# flechas de soporte hacia arriba
for i in range(5):
    x = bx + 20 + i * (sw_ + 20) + sw_ / 2
    arrow(x, ay + 2, x, ay - 20, color="#2E7D32", w=1.8)
for i in range(3):
    x = bx + 24 + i * (cw + 24) + cw / 2
    arrow(x, sy + sh + 2, x, oy - 4, color="#5E35B1", w=1.8)

defs = ('<defs><marker id="ah" markerWidth="12" markerHeight="10" markerUnits="userSpaceOnUse" viewBox="0 0 10 8" refX="9" refY="4" orient="auto">'
        '<path d="M0,0 L10,4 L0,8 z" fill="#37474F"/></marker>'
        '<marker id="ahr" markerWidth="12" markerHeight="10" markerUnits="userSpaceOnUse" viewBox="0 0 10 8" refX="9" refY="4" orient="auto">'
        '<path d="M0,0 L10,4 L0,8 z" fill="#C62828"/></marker></defs>')
svg = f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}">{defs}' + "".join(out) + "</svg>"
open("06_diagrama_macroprocesos.svg", "w").write(svg)
cairosvg.svg2png(bytestring=svg.encode(), write_to="06_diagrama_macroprocesos.png", output_width=W * 2)
print("ok")
