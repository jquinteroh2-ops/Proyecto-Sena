"""GA8_220501096_AA2_EV02 - APK: modulo movil "Mis notas" que consume el API de EduckTrack."""
from comun import API, EVIDENCIAS, REPOSITORIO, documento, e, imprimir, portada, uri

APK = EVIDENCIAS / "apk"
CAP = APK / "capturas"
texto = lambda n: e((APK / n).read_text(encoding="utf-8").strip())

TERMINAL = ("background:#0F172A;color:#E2E8F0;border-radius:10px;padding:12px 14px;font-family:Consolas,monospace;"
            "font-size:7.4pt;white-space:pre-wrap;overflow:hidden;line-height:1.35;margin:6px 0 12px")


def celular(nombre, pie):
    return (f'<figure style="margin:0"><img src="{uri(CAP / nombre)}" style="border-radius:14px">'
            f'<figcaption>{pie}</figcaption></figure>')


def fila(*figuras):
    return ('<div style="display:grid;grid-template-columns:repeat(3,38mm);justify-content:center;gap:12mm;margin:6px 0 8px;break-inside:avoid">'
            + "".join(figuras) + "</div>")


cuerpo = portada("Módulo móvil EduckTrack (APK)", "GA8_220501096_AA2_EV02",
                 "Aplicación Android “Mis notas” conectada al API REST del proyecto") + f"""
<h2>1. Proyecto y propósito del módulo</h2>
<p><b>Proyecto:</b> EduckTrack, plataforma web de gestión académica para colegios (cursos, matrículas, notas, asistencia, horarios y reportes).</p>
<table class="t">
<tr><td style="width:24%"><b>Funcionalidad móvil</b></td><td><b>Inicio de sesión y consulta de “Mis notas”</b>: el estudiante entra con su cuenta institucional y ve sus calificaciones del periodo más reciente, agrupadas por materia, con el promedio de cada una y el promedio general.</td></tr>
<tr><td><b>Usuario</b></td><td>Estudiante del colegio (rol <code>ESTUDIANTE</code>). Si entra un docente o un administrador, la app le explica que el módulo es solo para estudiantes.</td></tr>
<tr><td><b>Alcance</b></td><td>Solo consulta (no registra ni modifica notas). Dos pantallas: inicio de sesión y “Mis notas”. Datos reales del sistema en producción.</td></tr>
<tr><td><b>Requerimientos atendidos</b></td><td><b>RF-60</b> Autenticación de usuarios con correo y contraseña (JWT).<br><b>RF-37</b> Consulta del histórico de calificaciones del estudiante.</td></tr>
</table>

<h2>2. Tecnología utilizada</h2>
<table class="t">
<tr><td style="width:24%"><b>Plataforma</b></td><td>Android nativo, <b>Java 17</b>, Android Gradle Plugin. SDK mínimo 24 (Android 7.0), SDK objetivo 35 (Android 15).</td></tr>
<tr><td><b>Red</b></td><td><code>HttpURLConnection</code> y <code>org.json</code> de Android, sin librerías externas (APK de 30 KB). Las llamadas se hacen en un hilo aparte (<code>ExecutorService</code>) para no congelar la pantalla.</td></tr>
<tr><td><b>Backend consumido</b></td><td>API REST de EduckTrack (Spring Boot 3 + JWT) publicada en <code>{API}</code>.</td></tr>
<tr><td><b>Herramientas</b></td><td>Gradle Wrapper, Android SDK (build-tools, platform-tools/adb), emulador de Android 11 (API 30), Git y GitHub.</td></tr>
<tr><td><b>Código fuente</b></td><td>Carpeta <code>movil/</code> del repositorio: <code>LoginActivity</code>, <code>NotasActivity</code>, <code>ApiCliente</code> y <code>Sesion</code>.</td></tr>
</table>

<h2>3. Endpoints consumidos</h2>
<table class="t"><tr><th style="width:9%">Método</th><th style="width:36%">Endpoint</th><th>Para qué lo usa la app</th></tr>
<tr><td><b>POST</b></td><td><code>/api/auth/login</code></td><td>Iniciar sesión; devuelve el token JWT y el nombre del usuario (RF-60).</td></tr>
<tr><td><b>GET</b></td><td><code>/api/identidad/yo</code></td><td>Saber quién inició sesión y su <code>estudianteId</code>.</td></tr>
<tr><td><b>GET</b></td><td><code>/api/calificaciones/historico/{{estudianteId}}</code></td><td>Traer todas las notas del estudiante (RF-37).</td></tr>
<tr><td><b>GET</b></td><td><code>/api/materias</code> · <code>/api/periodos</code></td><td>Mostrar el nombre de la materia y del periodo en lugar de sus números.</td></tr>
</table>

<h3 class="salto">3.1 Datos de entrada y respuesta obtenida (servidor en producción)</h3>
<div class="grid2" style="break-inside:avoid">
<div><b>Entrada</b> — <code>POST /api/auth/login</code>
<div style="{TERMINAL}">Content-Type: application/json

{texto('login_entrada.json')}</div></div>
<div><b>Respuesta 200 OK</b> (token recortado)
<div style="{TERMINAL}">{texto('login_respuesta.json')}</div></div>
</div>
<div class="grid2" style="break-inside:avoid">
<div><b>Entrada</b> — <code>GET /api/calificaciones/historico/1</code>
<div style="{TERMINAL}">Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
Accept: application/json</div>
<b>Respuesta con contraseña incorrecta: 401</b>
<div style="{TERMINAL}">{texto('login_401.json')}</div></div>
<div><b>Respuesta 200 OK</b> (primeras 2 de {texto('historico_total.txt')} notas)
<div style="{TERMINAL}">{texto('historico_respuesta.json')}</div></div>
</div>
<p>La app toma solo el periodo más reciente, agrupa las notas por <code>materiaId</code>, saca el promedio de cada materia y el general
y los pinta en la escala colombiana: <span class="ok">verde ≥ 4,0</span> <span class="pendiente" style="font-weight:600">ámbar ≥ 3,0</span> <span class="mal">rojo &lt; 3,0</span>.</p>

<h2 class="salto">4. Capturas del módulo en funcionamiento</h2>
<p>Capturas tomadas en el emulador de Android 11 con el APK entregado, conectado al servidor de internet con la cuenta de demostración de la estudiante Sofía Martínez.</p>
{fila(celular('01-icono.png', '1. La app instalada en el teléfono, con su ícono.'),
      celular('02-login.png', '2. Inicio de sesión: correo institucional y contraseña.'),
      celular('03-cargando.png', '3. Estado de carga: botón “Conectando…” y aviso si el servidor gratuito está despertando.'))}
{fila(celular('04-mis-notas.png', '4. Respuesta exitosa: “Mis notas” del periodo con promedio general y cada evaluación por materia.'),
      celular('05-salir.png', '5. Botón “Salir”: borra el token y vuelve al inicio de sesión vacío.'),
      celular('06-validacion.png', '6. Validación local: correo con formato inválido, sin ir al servidor.'))}
{fila(celular('07-error-401.png', '7. Error: contraseña incorrecta (respuesta 401 del API).'),
      celular('08-docente.png', '8. Una cuenta de docente: el módulo explica que es solo para estudiantes.'),
      celular('09-sin-internet.png', '9. Error de red: sin internet se muestra el mensaje y el botón Reintentar.'))}

<h2>5. Evidencia de ejecución en el emulador</h2>
<p>Instalación del APK y apertura de la app desde la línea de comandos con <code>adb</code> (Android Debug Bridge):</p>
<div style="{TERMINAL}">{texto('ejecucion_emulador.txt')}</div>

<h2>6. Resultado de las pruebas realizadas</h2>
<table class="t" style="break-inside:avoid"><tr><th style="width:4%">#</th><th style="width:24%">Prueba</th><th style="width:30%">Datos de entrada</th><th>Resultado esperado</th><th style="width:20%">Obtenido</th></tr>
<tr><td>1</td><td>Instalación del APK</td><td><code>adb install EduckTrack-movil-1.0.apk</code></td><td>Se instala y aparece el ícono</td><td>Success <span class="ok">Cumple</span></td></tr>
<tr><td>2</td><td>Inicio de sesión correcto</td><td>sofia.martinez@… / Demo12345</td><td>Abre “Mis notas”</td><td>Captura 4 <span class="ok">Cumple</span></td></tr>
<tr><td>3</td><td>Consulta de notas reales</td><td>Token JWT de la estudiante</td><td>Materias, evaluaciones y promedios del último periodo</td><td>Captura 4 <span class="ok">Cumple</span></td></tr>
<tr><td>4</td><td>Estado de carga</td><td>Pulsar “Iniciar sesión”</td><td>Botón deshabilitado y barra de progreso</td><td>Captura 3 <span class="ok">Cumple</span></td></tr>
<tr><td>5</td><td>Validación de correo</td><td><code>sofia.martinez</code> (sin @)</td><td>Aviso en el campo, sin llamar al API</td><td>Captura 6 <span class="ok">Cumple</span></td></tr>
<tr><td>6</td><td>Contraseña incorrecta</td><td>sofia.martinez@… / mala</td><td>Mensaje del API, sin cerrar la app</td><td>Captura 7 <span class="ok">Cumple</span></td></tr>
<tr><td>7</td><td>Usuario sin perfil de estudiante</td><td>laura.gomez@… (docente)</td><td>Mensaje “solo para estudiantes”</td><td>Captura 8 <span class="ok">Cumple</span></td></tr>
<tr><td>8</td><td>Sin conexión a internet</td><td>Wi-Fi y datos apagados en el emulador (<code>adb shell svc wifi disable</code>)</td><td>Mensaje de red y botón Reintentar</td><td>Captura 9 <span class="ok">Cumple</span></td></tr>
<tr><td>9</td><td>Cerrar sesión</td><td>Botón “Salir”</td><td>Vuelve al inicio de sesión y borra el token</td><td>Captura 5 <span class="ok">Cumple</span></td></tr>
</table>

<h2>7. Permisos y seguridad</h2>
<ul>
<li><b>Permisos:</b> solo <code>INTERNET</code> y <code>ACCESS_NETWORK_STATE</code> en el <code>AndroidManifest.xml</code>. No pide cámara, ubicación ni contactos.</li>
<li><b>Sin credenciales en el código:</b> la URL del API llega desde <code>gradle.properties</code> al compilar (<code>BuildConfig.API_URL</code>); no hay usuarios ni contraseñas escritos en el código.</li>
<li><b>El token no se guarda en disco:</b> vive en memoria mientras la app está abierta; la contraseña solo se envía en el login.</li>
<li><b>Solo HTTPS:</b> <code>network_security_config.xml</code> bloquea el tráfico sin cifrar, salvo el servidor local de desarrollo (10.0.2.2).</li>
<li>Copia de seguridad desactivada (<code>allowBackup=false</code>) para que los datos no salgan del teléfono.</li>
</ul>

<h2>8. Instrucciones de instalación del APK</h2>
<div class="caja">
<b>En un celular Android (7.0 o superior)</b>
<ol>
<li>Copiar <code>EduckTrack-movil-1.0.apk</code> al celular (cable, Drive o WhatsApp) y abrirlo.</li>
<li>Si Android lo pide, permitir “Instalar apps desconocidas” para esa aplicación (la app no está en Play Store).</li>
<li>Pulsar <b>Instalar</b> y luego <b>Abrir</b>. El celular debe tener internet.</li>
<li>Entrar con la cuenta de estudiante de demostración: <code>sofia.martinez@educktrack.edu.co</code> / <code>Demo12345</code>.</li>
<li>El servidor es gratuito: si estaba dormido, la primera entrada tarda entre 1 y 2 minutos. La app lo avisa.</li>
</ol>
<b>En el emulador de Android Studio</b>
<ol>
<li>Crear y encender un dispositivo virtual (Device Manager) y arrastrar el APK sobre la ventana, o ejecutar <code>adb install EduckTrack-movil-1.0.apk</code>.</li>
</ol>
<b>Compilar desde el código</b>
<ol>
<li><code>git clone {REPOSITORIO}</code> → <code>cd Proyecto-Sena/movil</code> → <code>gradlew assembleRelease</code> (requiere JDK 17 y Android SDK).</li>
<li>Para usar el backend local: <code>gradlew assembleRelease -PapiUrl=http://10.0.2.2:8081</code>.</li>
</ol>
</div>

<h2>9. Enlaces</h2>
<table class="t">
<tr><td style="width:30%"><b>Repositorio (código fuente)</b></td><td><a href="{REPOSITORIO}/tree/main/movil">{REPOSITORIO}/tree/main/movil</a></td></tr>
<tr><td><b>API en producción</b></td><td><a href="{API}/api/health">{API}/api/health</a></td></tr>
<tr><td><b>Video demostrativo (máx. 5 min)</b></td><td><span class="pendiente">[pegar aquí el enlace del video]</span></td></tr>
</table>
"""

imprimir("GA8_220501096_AA2_EV02_APK_modulo_movil", documento("APK — Módulo móvil EduckTrack", cuerpo))
