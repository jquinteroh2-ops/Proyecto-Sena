"""Piezas compartidas por los documentos de la guia 8: portada e impresion a PDF."""
import html
import os
import pathlib
import subprocess

AQUI = pathlib.Path(__file__).resolve().parent
EVIDENCIAS = AQUI.parent                      # docs/evidencias-ga8
REPO = EVIDENCIAS.parent.parent               # raiz del repositorio
SALIDA = pathlib.Path(os.environ.get("SALIDA_PDF", str(EVIDENCIAS / "pdf")))
EDGE = r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe"

INTEGRANTES = ["José Antonio Quintero Herrera", "María José Grijalba Torres"]
REPOSITORIO = "https://github.com/jquinteroh2-ops/Proyecto-Sena"
WEB = "https://educktrack-web.onrender.com"
API = "https://educktrack-backend.onrender.com"


def e(texto):
    return html.escape(str(texto))


def uri(ruta):
    return pathlib.Path(ruta).resolve().as_uri()


def portada(titulo, codigo, subtitulo, extra=None):
    filas = [
        ("Programa", "Análisis y Desarrollo de Software (228118)"),
        ("Proyecto formativo", "Construcción de software integrador de tecnologías orientadas a servicios"),
        ("Proyecto", "EduckTrack — Plataforma web de gestión académica"),
        ("Integrantes", "<br>".join(e(n) for n in INTEGRANTES)),
        ("Instructor(a)", '<span class="pendiente">[nombre del instructor]</span>'),
        ("Ficha", '<span class="pendiente">[número de ficha]</span>'),
    ] + (extra or []) + [("Fecha", "Octubre de 2026")]
    datos = "".join(f"<tr><td>{a}</td><td>{b}</td></tr>" for a, b in filas)
    return f"""
<section class="portada">
  <div class="banda">
    <div class="sena">Servicio Nacional de Aprendizaje · SENA</div>
    <h1>{e(titulo)}</h1>
    <div class="codigo">{e(codigo)}</div>
    <p style="margin-top:14px;font-size:12pt;opacity:.95">{e(subtitulo)}</p>
    <div class="logo"><span class="e">E</span> EduckTrack</div>
  </div>
  <div class="datos"><table>{datos}</table></div>
</section>"""


def documento(titulo, cuerpo, lang="es"):
    return f"""<!doctype html>
<html lang="{lang}"><head><meta charset="utf-8"><title>{e(titulo)}</title>
<link rel="stylesheet" href="{uri(AQUI / 'estilo.css')}"></head>
<body>{cuerpo}</body></html>"""


def imprimir(nombre, contenido_html):
    """Guarda el HTML junto a las fuentes y lo imprime a PDF con Edge headless."""
    SALIDA.mkdir(parents=True, exist_ok=True)
    origen = AQUI / f"{nombre}.html"
    origen.write_text(contenido_html, encoding="utf-8")
    destino = SALIDA / f"{nombre}.pdf"
    subprocess.run([EDGE, "--headless=new", "--disable-gpu", "--no-pdf-header-footer",
                    "--print-to-pdf-no-header", "--run-all-compositor-stages-before-draw",
                    "--virtual-time-budget=4000", f"--print-to-pdf={destino}", origen.as_uri()],
                   check=True, capture_output=True, timeout=120)
    print("PDF:", destino)
    return destino
