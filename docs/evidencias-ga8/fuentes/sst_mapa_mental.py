"""GA8-220601501-AA3-EV01 - Mapa mental: planes y acciones en medio ambiente y SST (1 hoja)."""
from comun import documento, imprimir

W, H = 980, 700
CENTRO = (490, 300, 250, 150)  # x, y (centro), ancho, alto

RAMAS = [
    # (x, y, ancho, alto, color, titulo, items)
    (8, 8, 300, 196, "#2350D6", "Sistema de Gestión de SST", [
        "Decreto 1072 de 2015 y Resolución 0312 de 2019",
        "Política de SST firmada por la gerencia",
        "Matriz de peligros y riesgos (GTC 45)",
        "Plan anual de trabajo y presupuesto",
        "COPASST o vigía y comité de convivencia",
        "Reporte e investigación de ATEL",
    ]),
    (8, 218, 300, 196, "#DC2626", "Plan de emergencias", [
        "Análisis de amenazas y vulnerabilidad",
        "Brigada de emergencias capacitada",
        "Rutas de evacuación y punto de encuentro",
        "Simulacro al menos una vez al año",
        "Extintores, botiquín y números de emergencia",
    ]),
    (8, 428, 300, 196, "#0F766E", "Medicina preventiva", [
        "Exámenes médicos de ingreso, periódicos y retiro",
        "Vigilancia epidemiológica osteomuscular y visual",
        "Pausas activas cada 2 horas",
        "Estilos de vida saludable",
        "Afiliación a la ARL desde el primer día",
    ]),
    (672, 8, 300, 196, "#7C3AED", "Riesgos del desarrollador de software", [
        "Ergonómico: silla, escritorio y pantalla a la altura de los ojos",
        "Visual: regla 20-20-20 y buena iluminación",
        "Psicosocial: estrés por entregas; derecho a la desconexión (Ley 2191 de 2022)",
        "Eléctrico: cableado ordenado, sin multitomas sobrecargadas",
        "Sedentarismo: levantarse y caminar",
    ]),
    (672, 218, 300, 196, "#15803D", "Plan de gestión ambiental", [
        "Enfoque ISO 14001: identificar aspectos e impactos",
        "Equipos dados de baja como RAEE (Ley 1672 de 2013)",
        "Ahorro de energía: apagar equipos y monitores",
        "Cero papel: documentos y firmas digitales",
        "Software eficiente: menos cómputo, menos energía",
    ]),
    (672, 428, 300, 196, "#B45309", "Seguimiento y mejora continua", [
        "Ciclo PHVA: planear, hacer, verificar, actuar",
        "Indicadores: frecuencia, severidad y ausentismo",
        "Auditoría anual del SG-SST",
        "Acciones preventivas y correctivas",
        "Capacitación e inducción a todo el equipo",
    ]),
]


def conector(rama):
    x, y, w, h, color = rama[:5]
    cx, cy, cw, ch = CENTRO
    izquierda = x < cx
    x1 = x + w if izquierda else x
    y1 = y + h / 2
    x2 = cx - cw / 2 if izquierda else cx + cw / 2
    y2 = cy + (-40 if y1 < cy - 60 else 40 if y1 > cy + 60 else 0)
    mx = (x1 + x2) / 2
    return (f'<path d="M{x1},{y1} C{mx},{y1} {mx},{y2} {x2},{y2}" fill="none" stroke="{color}" '
            f'stroke-width="3" opacity=".75"/><circle cx="{x1}" cy="{y1}" r="5" fill="{color}"/>')


def tarjeta(rama):
    x, y, w, h, color, titulo, items = rama
    lis = "".join(f"<li>{t}</li>" for t in items)
    return (f'<div style="position:absolute;left:{x}px;top:{y}px;width:{w}px;height:{h}px;border:2px solid {color};'
            f'border-radius:14px;background:#fff;overflow:hidden">'
            f'<div style="background:{color};color:#fff;font-weight:700;font-size:12.5px;padding:7px 12px">{titulo}</div>'
            f'<ul style="margin:6px 8px 0 4px;padding-left:18px;font-size:10.6px;line-height:1.35">{lis}</ul></div>')


cx, cy, cw, ch = CENTRO
svg = "".join(conector(r) for r in RAMAS)
cuerpo = f"""
<style>@page {{ size: Letter landscape; margin: 9mm; }}</style>
<div style="position:relative;width:{W}px;height:{H}px;margin:0 auto">
  <svg width="{W}" height="{H}" style="position:absolute;left:0;top:0">{svg}</svg>
  {''.join(tarjeta(r) for r in RAMAS)}
  <div style="position:absolute;left:{cx - cw / 2}px;top:{cy - ch / 2}px;width:{cw}px;height:{ch}px;border-radius:22px;
       background:linear-gradient(135deg,#2350D6,#6D2BD9);color:#fff;display:flex;flex-direction:column;
       align-items:center;justify-content:center;text-align:center;padding:10px;box-shadow:0 6px 18px rgba(35,80,214,.25)">
    <div style="font-size:10px;letter-spacing:.08em;text-transform:uppercase;opacity:.9">Mapa mental</div>
    <div style="font-size:17px;font-weight:800;line-height:1.2;margin:4px 0">Planes y acciones en medio ambiente y SST</div>
    <div style="font-size:10.5px;opacity:.95">en una empresa de desarrollo de software</div>
  </div>
  <div style="position:absolute;left:378px;top:392px;width:224px;font-size:10.5px;color:#374151;text-align:center;line-height:1.4">
    <b>Objetivo:</b> prevenir accidentes y enfermedades laborales (ATEL) y reducir el impacto ambiental del trabajo,
    cumpliendo la normatividad colombiana y mejorando de forma continua.
  </div>
  <div style="position:absolute;left:8px;top:640px;width:560px;font-size:9px;color:#6B7280">
    GA8-220601501-AA3-EV01 · Fuentes: Decreto 1072 de 2015, Resolución 0312 de 2019, GTC 45, Ley 1672 de 2013,
    Ley 2191 de 2022, NTC-ISO 14001 y componente formativo SENA “Fundamentos de medio ambiente, seguridad y salud en el trabajo”.
  </div>
  <div style="position:absolute;right:8px;top:632px;width:300px;border:1px solid #D1D5DB;border-radius:10px;
       padding:7px 11px;font-size:10px;line-height:1.4;background:#F9FAFB">
    <b>José Antonio Quintero Herrera</b><br><b>María José Grijalba Torres</b><br>
    Análisis y Desarrollo de Software (228118) · Ficha <span class="pendiente">[número]</span><br>SENA · Octubre de 2026
  </div>
</div>
"""

imprimir("GA8_220601501_AA3_EV01_Mapa_mental_SST", documento("Mapa mental SST y medio ambiente", cuerpo))
