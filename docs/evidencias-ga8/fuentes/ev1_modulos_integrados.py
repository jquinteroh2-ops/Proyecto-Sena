"""GA8_220501096_AA1_EV02 - Desarrollo del software con modulos integrados."""
import json

from comun import API, EVIDENCIAS, REPO, REPOSITORIO, WEB, documento, e, imprimir, portada, uri

CAP = EVIDENCIAS / "capturas"
pruebas = json.loads((REPO / "pruebas" / "resultados.json").read_text(encoding="utf-8"))["pruebas"]

MODULOS = [
    ("Seguridad e identidad", "RF-01, RF-03, RF-60..64", "Login, Recuperar contraseña", "/api/auth, /api/identidad, /api/usuarios", "usuario, rol, usuario_rol, token_recuperacion, vinculo_acudiente"),
    ("Estudiantes", "RF-06..RF-08, RF-10, RF-11", "Estudiantes", "/api/estudiantes", "estudiante"),
    ("Docentes y asignaciones", "RF-12..RF-16", "Asignaciones", "/api/docentes, /api/asignaciones", "docente, docente_area, asignacion_docente"),
    ("Cursos, materias y matrículas", "RF-09, RF-17..RF-19, RF-43..RF-46", "Cursos", "/api/cursos, /api/materias, /api/matriculas", "curso, materia, plan_estudios, matricula"),
    ("Horarios", "RF-21..RF-25", "Horario", "/api/horarios", "bloque_horario, asignacion_horaria"),
    ("Asistencia", "RF-26..RF-30", "Asistencia", "/api/asistencia", "asistencia"),
    ("Calificaciones y boletín", "RF-20, RF-31..RF-37", "Notas, Mis notas, Boletín", "/api/calificaciones, /api/ponderaciones", "calificacion, ponderacion_evaluacion, cierre_corte, novedad_nota"),
    ("Tareas", "RF-38..RF-42", "(panel docente)", "/api/tareas", "tarea, entrega_tarea"),
    ("Reportes y notificaciones", "RF-47..RF-56", "Inicio, Reportes", "/api/reportes, /api/notificaciones", "notificacion, configuracion_notificacion"),
    ("Configuración y auditoría", "RF-57..RF-59, RF-63", "Parámetros", "/api/configuracion/parametros, /api/periodos, /api/auditoria", "parametro_institucional, periodo_academico, auditoria"),
]

ERRORES = [
    ("Los errores de la API llegaban como “401 Debe autenticarse”",
     "Una petición con un parámetro faltante (por ejemplo, el boletín sin el curso) respondía 401 aunque el usuario sí tuviera sesión. Spring reenviaba el error a <code>/error</code> y esa ruta exigía token.",
     "Se hizo pública la ruta <code>/error</code> y el manejador global (<code>GlobalExceptionHandler</code>) ahora responde 400 (parámetro faltante, tipo inválido, JSON mal formado), 404, 405 y 500, siempre con el mismo formato <code>ApiError</code>. Prueba 17 de la tabla."),
    ("El estudiante veía “Materia #1” en vez del nombre de la materia",
     "La pantalla “Mis notas” no podía leer el catálogo de materias y periodos: el backend respondía 403 a los roles ESTUDIANTE y PADRE_FAMILIA.",
     "Se abrió solo la lectura (GET) del catálogo a esos roles; crear y editar sigue siendo de coordinación. Prueba 13 y capturas antes/después."),
    ("Contraseña escrita en el código del script de datos de demostración",
     "<code>backend/seed-demo.mjs</code> tenía la contraseña de una cuenta real escrita en el repositorio.",
     "Ahora las credenciales llegan por las variables <code>SEED_CORREO</code> y <code>SEED_PASSWORD</code>."),
    ("Migraciones incompatibles con la base de producción (TiDB)",
     "El despliegue gratuito usa TiDB Cloud (compatible con MySQL). Varias migraciones fallaban: <code>ALTER</code> con varias cláusulas sobre una columna recién creada y columnas generadas <code>STORED</code>.",
     "Se creó <code>db/migration-tidb</code> con la misma secuencia adaptada (un <code>ALTER</code> por cláusula, columnas <code>VIRTUAL</code>), probada contra TiDB 8.5 en Docker antes de desplegar. Las originales siguen siendo las de MySQL."),
    ("La primera conexión a la base fallaba en el servidor gratuito",
     "Desde Render la primera conexión TLS a TiDB tarda unos 12 s y el pool esperaba solo 8 s.",
     "Se amplió el tiempo de conexión por configuración (<code>connectTimeout=30000</code>)."),
]


def fila_prueba(p):
    estado = '<span class="ok">Exitosa</span>' if p["estado"] == "Exitosa" else '<span class="mal">Fallida</span>'
    obtenido = p["obtenido"] if len(p["obtenido"]) < 150 else p["obtenido"][:147] + "…"
    return (f"<tr><td>{p['n']}</td><td>{e(p['funcionalidad'])}</td><td>{e(p['datos'])}</td>"
            f"<td>{e(p['esperado'])}</td><td>{e(obtenido)}</td><td>{estado}</td></tr>")


def figura(nombre, texto):
    return f'<figure><img src="{uri(CAP / nombre)}"><figcaption>{texto}</figcaption></figure>'


DIAGRAMA = """
<svg class="diagrama" viewBox="0 0 760 210" xmlns="http://www.w3.org/2000/svg" font-family="Segoe UI, Arial" font-size="12">
  <defs><marker id="f" markerWidth="10" markerHeight="10" refX="9" refY="5" orient="auto"><path d="M0,0 L10,5 L0,10 z" fill="#4B5563"/></marker>
  <linearGradient id="g" x1="0" x2="1"><stop offset="0" stop-color="#2350D6"/><stop offset="1" stop-color="#6D2BD9"/></linearGradient></defs>
  <rect x="5" y="70" width="120" height="70" rx="12" fill="#F3F5FB" stroke="#DDE2EC"/>
  <text x="65" y="98" text-anchor="middle" font-weight="700">Navegador</text><text x="65" y="116" text-anchor="middle" fill="#4B5563">o app Android</text>
  <rect x="160" y="55" width="160" height="100" rx="12" fill="url(#g)"/>
  <text x="240" y="85" text-anchor="middle" fill="#fff" font-weight="700">Frontend</text>
  <text x="240" y="103" text-anchor="middle" fill="#fff">React + Vite + Tailwind</text>
  <text x="240" y="121" text-anchor="middle" fill="#fff">rutas por rol</text>
  <text x="240" y="139" text-anchor="middle" fill="#fff" font-size="10">Render Static Site</text>
  <rect x="360" y="40" width="210" height="130" rx="12" fill="#fff" stroke="#2350D6" stroke-width="2"/>
  <text x="465" y="64" text-anchor="middle" font-weight="700" fill="#2350D6">Backend — API REST</text>
  <text x="465" y="84" text-anchor="middle">Spring Boot 3 · Java 21</text>
  <text x="465" y="102" text-anchor="middle">Seguridad JWT + roles (RBAC)</text>
  <text x="465" y="120" text-anchor="middle">Hexagonal: rest → application → domain</text>
  <text x="465" y="138" text-anchor="middle">→ persistence (JPA)</text>
  <text x="465" y="158" text-anchor="middle" font-size="10" fill="#4B5563">Render Web Service (Docker)</text>
  <rect x="610" y="65" width="145" height="80" rx="12" fill="#F3F5FB" stroke="#6D2BD9" stroke-width="2"/>
  <text x="682" y="92" text-anchor="middle" font-weight="700" fill="#6D2BD9">Base de datos</text>
  <text x="682" y="110" text-anchor="middle">MySQL 8 / TiDB</text>
  <text x="682" y="128" text-anchor="middle" font-size="10">Flyway (16 migraciones)</text>
  <line x1="125" y1="105" x2="158" y2="105" stroke="#4B5563" stroke-width="1.6" marker-end="url(#f)"/>
  <line x1="320" y1="105" x2="358" y2="105" stroke="#4B5563" stroke-width="1.6" marker-end="url(#f)"/>
  <line x1="570" y1="105" x2="608" y2="105" stroke="#4B5563" stroke-width="1.6" marker-end="url(#f)"/>
  <text x="339" y="97" text-anchor="middle" font-size="10" fill="#4B5563">/api</text>
  <text x="589" y="97" text-anchor="middle" font-size="10" fill="#4B5563">JDBC/TLS</text>
  <text x="380" y="200" text-anchor="middle" font-size="10.5" fill="#4B5563">Todas las llamadas pasan por /api (proxy de Vite en desarrollo, Nginx en Docker, reescritura en Render): sin CORS.</text>
</svg>"""

exitosas = sum(p["estado"] == "Exitosa" for p in pruebas)
cuerpo = portada("Desarrollo del software con módulos integrados", "GA8_220501096_AA1_EV02",
                 "Integración del frontend, el backend, la API y la base de datos de EduckTrack") + f"""
<h2>1. Nombre y descripción del proyecto</h2>
<p><b>EduckTrack</b> es una plataforma web de gestión académica para instituciones educativas colombianas. Centraliza en un solo
sistema la matrícula, los horarios, la asistencia, las calificaciones con su ponderación y cierre de corte, el boletín, las tareas,
los reportes institucionales y las notificaciones, con un acceso distinto para cada rol: administrador, rector, coordinación,
docente, estudiante y acudiente.</p>
<p>En esta evidencia se consolidó una versión <b>integrada, ejecutable y verificable</b>: se revisaron los módulos de la guía 07,
se verificó la comunicación entre las cuatro capas con pruebas automáticas, se corrigieron los errores encontrados y se publicó
la aplicación en internet.</p>
<div class="caja"><b>Versión en línea:</b> <a href="{WEB}">{WEB}</a> · <b>API:</b> <a href="{API}/api/health">{API}/api/health</a>
· <b>Documentación de la API (Swagger):</b> <a href="{WEB}/swagger-ui/index.html">{WEB}/swagger-ui/index.html</a></div>

<h2>2. Integrantes</h2>
<ul><li>José Antonio Quintero Herrera</li><li>María José Grijalba Torres</li></ul>

<h2>3. Módulos integrados</h2>
<table class="t"><tr><th>Módulo</th><th>Requisitos</th><th>Pantalla (frontend)</th><th>Endpoints (API)</th><th>Tablas (BD)</th></tr>
{''.join(f"<tr><td><b>{e(m)}</b></td><td>{e(r)}</td><td>{e(p)}</td><td><code>{e(a)}</code></td><td>{e(t)}</td></tr>" for m, r, p, a, t in MODULOS)}
</table>
<p>Además se integró un <b>módulo móvil Android</b> que consume la misma API (evidencia GA8-AA2-EV02).</p>

<h2>4. Arquitectura implementada</h2>
{DIAGRAMA}
<ul>
<li><b>Frontend:</b> SPA en React 18 con Vite, TailwindCSS, React Router y Zustand. Cada rol ve solo su menú.</li>
<li><b>Backend:</b> Spring Boot 3 con arquitectura <b>hexagonal + DDD</b>: cada módulo tiene sus capas <code>domain</code> (reglas de negocio),
<code>application</code> (casos de uso) e <code>infrastructure</code> (controladores REST, persistencia JPA). Las reglas RB-01..RB-20 se validan
en el servidor, nunca solo en el navegador.</li>
<li><b>API REST:</b> JSON, autenticación con <b>JWT</b>, autorización por rol con <code>@PreAuthorize</code> y errores con un formato único
(<code>ApiError</code>: estado, mensaje y detalle por campo). Documentada con OpenAPI/Swagger.</li>
<li><b>Base de datos:</b> MySQL 8 en desarrollo y TiDB Cloud en producción; el esquema lo versiona Flyway.</li>
<li><b>Seguridad de credenciales:</b> contraseñas con BCrypt; secretos y conexiones solo por variables de entorno (<code>.env</code> excluido del repositorio).</li>
</ul>

<h2 class="salto">5. Evidencias del funcionamiento integrado</h2>
<p>Capturas tomadas con la aplicación corriendo localmente (frontend en el puerto 5173, backend en el 8081 y base de datos),
con los datos de demostración de <code>seed-demo.mjs</code>.</p>
<div class="grid2">
{figura("01-login.jpg", "Inicio de sesión: el frontend envía las credenciales a <code>POST /api/auth/login</code> y recibe un JWT.")}
{figura("02-inicio-administrador.jpg", "Panel del administrador: indicadores calculados por el backend con datos de la BD.")}
{figura("03-estudiantes.jpg", "Módulo Estudiantes: el registro creado por la prueba 5 (“Prueba Integracion”) aparece guardado en la BD.")}
{figura("06-reporte-rendimiento.jpg", "Reportes: rendimiento del salón calculado desde las calificaciones (RF-48), exportable a PDF y Excel.")}
{figura("09-inicio-estudiante.jpg", "Vista del estudiante: el servidor resuelve la identidad a partir del token (<code>/api/identidad/yo</code>).")}
{figura("08-swagger-api.jpg", "Documentación de la API generada con OpenAPI/Swagger.")}
</div>

<h2 class="salto">6. Pruebas funcionales y de integración</h2>
<p>Se automatizaron con <code>pruebas/pruebas_integracion.py</code>. Cada prueba entra por el <b>frontend</b> (puerto 5173), que reenvía
<code>/api</code> al <b>backend</b>, que lee o escribe en la <b>base de datos</b>: si pasa, las cuatro capas se comunicaron bien.
Resultado: <b>{exitosas} de {len(pruebas)} exitosas</b>. Además, el backend tiene <b>147 pruebas unitarias</b> de reglas de negocio (<code>mvn test</code>), todas en verde.</p>
<table class="t"><colgroup><col style="width:3%"><col style="width:17%"><col style="width:21%"><col style="width:22%"><col style="width:28%"><col style="width:9%"></colgroup><tr><th>#</th><th>Funcionalidad evaluada</th><th>Datos utilizados</th><th>Resultado esperado</th><th>Resultado obtenido</th><th>Estado</th></tr>
{''.join(fila_prueba(p) for p in pruebas)}
</table>

<h2>7. Errores encontrados y ajustes realizados</h2>
<table class="t"><tr><th style="width:24%">Error</th><th style="width:36%">Causa</th><th>Ajuste</th></tr>
{''.join(f"<tr><td><b>{a}</b></td><td>{b}</td><td>{c}</td></tr>" for a, b, c in ERRORES)}
</table>
<div class="grid2">
{figura("10-mis-notas-antes.jpg", "Antes: el estudiante veía “Materia #1”.")}
{figura("11-mis-notas-despues.jpg", "Después: se muestran los nombres reales de las materias.")}
</div>

<h2>8. Repositorio, versión y video</h2>
<table class="t">
<tr><td style="width:30%"><b>Repositorio remoto</b></td><td><a href="{REPOSITORIO}">{REPOSITORIO}</a></td></tr>
<tr><td><b>Rama / versión a evaluar</b></td><td>rama <code>main</code>, etiqueta <code>ga8-modulos-integrados</code></td></tr>
<tr><td><b>Instalación y ejecución</b></td><td><code>README.md</code> del repositorio: requisitos, dependencias, variables de entorno, ejecución local o con Docker, datos de demostración y pruebas.</td></tr>
<tr><td><b>Commits</b></td><td>Mensajes descriptivos con Conventional Commits (<code>fix(api): …</code>, <code>test(integracion): …</code>, <code>feat(movil): …</code>).</td></tr>
<tr><td><b>Video demostrativo (máx. 5 min)</b></td><td><span class="pendiente">[pegar aquí el enlace del video]</span></td></tr>
</table>
"""

imprimir("GA8_220501096_AA1_EV02_Modulos_integrados", documento("Módulos integrados — EduckTrack", cuerpo))
