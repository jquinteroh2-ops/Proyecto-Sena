"""Guiones de los videos de la guia 8 (documento de apoyo, no se entrega)."""
from comun import REPOSITORIO, WEB, documento, e, imprimir


def guion(titulo, codigo, duracion, consejo, filas):
    cuerpo = "".join(
        f"<tr><td style='white-space:nowrap'><b>{t}</b></td><td>{q}</td><td>{p}</td><td>{d}</td></tr>"
        for t, q, p, d in filas)
    return f"""
<h2>{titulo}</h2>
<p><b>{codigo}</b> · duración máxima <b>{duracion}</b>. {consejo}</p>
<table class="t"><tr><th style="width:9%">Tiempo</th><th style="width:9%">Quién</th><th style="width:30%">Qué se ve en pantalla</th><th>Qué se dice</th></tr>{cuerpo}</table>"""


J, M = "José", "María"

EV1 = guion("Video 1 · Software con módulos integrados", "GA8_220501096_AA1_EV02", "5 minutos",
            "Graben la pantalla con OBS o la Xbox Game Bar (Windows + G). Abran la app 3 minutos antes para que el servidor gratuito despierte.", [
    ("0:00–0:30", J, "Portada del PDF", "Buenas, somos José Antonio Quintero y María José Grijalba. Presentamos EduckTrack, una plataforma web de gestión académica para colegios: matrícula, horarios, asistencia, notas, boletín y reportes por rol."),
    ("0:30–1:15", M, "Diagrama de arquitectura del PDF", "La solución tiene cuatro partes integradas: el frontend en React, el backend en Spring Boot con arquitectura hexagonal, una API REST protegida con JWT y la base de datos MySQL, que en producción es TiDB. Todo pasa por la ruta /api, así evitamos problemas de CORS."),
    ("1:15–2:30", J, f"{WEB}: login como administrador, Inicio, Estudiantes, Reportes → Generar reporte", "Iniciamos sesión: el frontend envía las credenciales a la API, que responde con un token. Aquí están los indicadores calculados por el backend con datos de la base. En Estudiantes vemos los registros guardados, y en Reportes el rendimiento del salón, que también se puede exportar a PDF y Excel."),
    ("2:30–3:15", M, "Salir y entrar como estudiante → Mis notas", "Con el rol estudiante el menú cambia: solo ve sus notas, su boletín y su horario. El servidor identifica al estudiante por el token, nunca por un dato que mande el navegador."),
    ("3:15–4:15", J, "Terminal: python pruebas/pruebas_integracion.py, y la tabla del PDF", "Automatizamos 17 pruebas de integración que recorren frontend, API, backend y base de datos: login válido e inválido, permisos por rol, validaciones y reglas de negocio. Las 17 pasan, y el backend tiene además 147 pruebas unitarias."),
    ("4:15–5:00", M, "Sección de errores del PDF y el repositorio en GitHub", f"Corregimos errores reales: los errores llegaban como 401, el estudiante veía “Materia #1” y había una contraseña en el código. El repositorio es {REPOSITORIO}, rama main, con el README para instalarlo. Gracias."),
])

EV2 = guion("Video 2 · Módulo móvil (APK)", "GA8_220501096_AA2_EV02", "5 minutos",
            "Graben el celular con su grabador de pantalla, o el emulador. Inicien sesión con la cuenta de estudiante de demostración.", [
    ("0:00–0:30", M, "Portada del PDF del APK", "Presentamos el módulo móvil de EduckTrack: una app Android nativa en Java para que el estudiante consulte sus notas desde el celular."),
    ("0:30–1:15", J, "Sección de requerimiento y endpoints del PDF", "Atiende el RF-37, consulta del histórico de calificaciones. Consume cuatro endpoints de la API: POST /api/auth/login para entrar, y GET identidad/yo, calificaciones/historico y materias para mostrar las notas."),
    ("1:15–2:00", M, "Instalar el APK en el celular", "Instalamos el APK de 30 KB. Solo pide permiso de internet y únicamente se conecta por HTTPS."),
    ("2:00–3:00", J, "Login: primero correo inválido, luego clave errada, luego correcto", "Las validaciones van antes de ir a la red: un correo mal escrito se marca de una vez. Con una clave incorrecta la API responde 401 y la app muestra el mensaje. Con los datos correctos entra; si el servidor gratuito está dormido, la app avisa que puede tardar."),
    ("3:00–4:15", M, "Pantalla Mis notas", "Aquí están las notas del periodo más reciente por materia, con el promedio y un color según la escala de 1 a 5. Si falla la red aparece un botón para reintentar; si el token vence, la app vuelve al login. La contraseña no se guarda en ningún lado."),
    ("4:15–5:00", J, "Carpeta movil/ en GitHub", f"El código está en {REPOSITORIO}, carpeta movil. La URL del API se configura al compilar, no está escrita en el código. Gracias."),
])

EV4 = guion("Video 3 · Prueba de concepto con Machine Learning", "GA8_220501096_AA2_EV04", "7 minutos",
            "Tengan EduckTrack corriendo y una terminal abierta en la carpeta poc-ml.", [
    ("0:00–0:40", J, "Portada del PDF", "Presentamos la prueba de concepto “Alerta temprana de riesgo académico con Machine Learning” para EduckTrack."),
    ("0:40–1:30", M, "Sección 3 del taller", "El problema: el docente se entera de que un estudiante perdió cuando el periodo ya cerró. Elegimos Machine Learning porque EduckTrack ya tiene las notas y la asistencia, y no Blockchain, porque las notas deben poder corregirse."),
    ("1:30–2:30", J, "Esquema de funcionamiento", "Usamos Python con scikit-learn. Primero generamos 1.500 estudiantes sintéticos, para no usar datos reales de menores. Con cinco señales de mitad de periodo (promedio, notas bajo 3, tendencia, asistencia y promedio anterior) entrenamos el modelo."),
    ("2:30–3:45", M, "Terminal: python entrenar.py y las gráficas", "Comparamos regresión logística, árbol de decisión y bosque aleatorio. Elegimos la regresión logística: detecta al 87 % de los que reprueban, tiene un AUC de 0,95 y se puede explicar. La matriz de confusión muestra que encontró 68 de 78."),
    ("3:45–5:00", J, "Terminal: python predecir_desde_api.py --curso 2 --periodo 2", "Ahora lo integramos con EduckTrack: el script inicia sesión en la API, lee las notas y la asistencia del curso y calcula el riesgo. Santiago Vargas aparece con 83 %, riesgo alto, porque tiene promedio 2,9 y seis notas por debajo de 3."),
    ("5:00–5:40", M, "Terminal: python probar_modelo.py", "Estos son los ocho criterios de aceptación: calidad mínima, coherencia y casos extremos. Los ocho se cumplen."),
    ("5:40–6:30", J, "Sección de limitaciones del PDF", "Limitaciones: está entrenado con datos sintéticos, los datos de demostración no tienen asistencia y por ahora corre como script. El siguiente paso es un endpoint en el backend y una alerta en el panel del docente."),
    ("6:30–7:00", M, "Sección de ética del PDF", "En lo ético: es solo una alerta para el docente, no decide sobre el estudiante ni se le muestra a él, y no usa datos sensibles. Gracias."),
])

EN = guion("Video 4 · English showcase (in English)", "GA8_AA1_EV02_IIN", "3 minutes",
           "Speak slowly and look at the camera. The adverbs are in <b>bold</b>; each one appears at least once.", [
    ("0:00–0:20", J, "Camera", "Good morning, teacher. My name is José Antonio Quintero, and this is my classmate María José Grijalba. Today we present emerging technologies for our project, EduckTrack."),
    ("0:20–1:00", M, "Section 1 slide", "Have you <b>ever</b> wondered how your phone recommends videos? That is machine learning, a branch of artificial intelligence. Algorithms learn patterns from training data and make predictions. It is <b>already</b> used in healthcare, banking and education, and <b>lately</b> it has become crucial in cybersecurity."),
    ("1:00–1:35", J, "Section 2 table", "We compared machine learning and blockchain. Blockchain keeps records that cannot be changed, and cryptocurrencies <b>still</b> depend on it. But in EduckTrack grades must be corrected sometimes, so blockchain was not a good fit."),
    ("1:35–2:10", M, "Section 3", "We chose machine learning. Teachers usually discover that a student failed when the term has <b>already</b> finished, and our system has not <b>yet</b> warned them earlier. Our model only warns the teacher; it never decides."),
    ("2:10–2:50", J, "Section 4 charts", "We have <b>just</b> finished training a machine learning model with supervised learning. It detected 87 percent of the students at risk. The model is <b>still</b> a prototype, because we have not <b>yet</b> trained it with real data."),
    ("2:50–3:00", M, "Camera", "Thank you for watching. Goodbye!"),
])

cuerpo = f"""
<h1 style="font-size:22pt;color:#2350D6">Guiones de los videos · Guía 8</h1>
<p class="pie">EduckTrack · José Antonio Quintero Herrera y María José Grijalba Torres · documento de apoyo para grabar (no se entrega).</p>
<div class="caja"><b>Antes de grabar:</b> abran {WEB} unos 3 minutos antes para que el servidor gratuito despierte.
Suban cada video a YouTube como <i>no listado</i> o a Google Drive con acceso “cualquier persona con el enlace”, y peguen el enlace en el PDF de cada evidencia.</div>
{EV1}{EV2}{EV4}{EN}
"""

imprimir("Guiones_videos_GA8", documento("Guiones de video GA8", cuerpo))
