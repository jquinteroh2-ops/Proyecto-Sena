"""GA8_220501096_AA2_EV04 - Prueba de concepto con tecnologia emergente (Machine Learning)."""
import json

from comun import REPO, REPOSITORIO, documento, e, imprimir, portada, uri

POC = REPO / "poc-ml"
MOD = POC / "modelo"
metricas = json.loads((MOD / "metricas.json").read_text(encoding="utf-8"))
salida = lambda n: e((MOD / n).read_text(encoding="utf-8").strip())

TERMINAL = ("background:#0F172A;color:#E2E8F0;border-radius:10px;padding:12px 14px;font-family:Consolas,monospace;"
            "font-size:7.1pt;white-space:pre;overflow:hidden;line-height:1.35;margin:6px 0 12px")

filas_modelos = "".join(
    f"<tr><td><b>{e(n)}</b>{' (elegido)' if n == metricas['elegido'] else ''}</td>"
    f"<td>{m['exactitud']}</td><td>{m['precision']}</td><td>{m['recall']}</td><td>{m['f1']}</td>"
    f"<td>{m['auc_roc']}</td><td>{m['recall_validacion_cruzada']}</td></tr>"
    for n, m in metricas["modelos"].items())

cuerpo = portada("Prueba de concepto con tecnología emergente", "GA8_220501096_AA2_EV04",
                 "Alerta temprana de riesgo académico con Machine Learning para EduckTrack") + f"""
<h2>1. Nombre y objetivo</h2>
<p><b>Alerta temprana de riesgo académico con Machine Learning.</b> Objetivo: demostrar que, con las notas y la asistencia de la
primera mitad del periodo, EduckTrack puede predecir qué estudiantes terminarán reprobando, con un recall de al menos 80 %,
para que el docente intervenga a tiempo.</p>

<h2>2. Funcionalidad implementada</h2>
<ol>
<li><b>Datos sintéticos</b> (<code>generar_datos.py</code>): 1.500 estudiantes simulados con semilla fija (25,9 % reprueba). No se usaron datos reales de menores.</li>
<li><b>Entrenamiento y evaluación</b> (<code>entrenar.py</code>): aprendizaje supervisado; se comparan tres algoritmos con 80 % de los datos para entrenar y 20 % para probar, más validación cruzada de 5 particiones.</li>
<li><b>Modelo exportado</b>: <code>modelo_riesgo.joblib</code> para Python y <code>modelo_riesgo.json</code> con los coeficientes, para aplicarlo desde Java en el backend.</li>
<li><b>Integración con EduckTrack</b> (<code>predecir_desde_api.py</code>): inicia sesión en la API REST con JWT, lee los estudiantes del curso, su histórico de notas (RF-37) y su asistencia (RF-28), calcula las 5 variables y entrega probabilidad, nivel (ALTO ≥ 60 %, MEDIO ≥ 30 %, BAJO) y causa principal.</li>
<li><b>Criterios de aceptación</b> (<code>probar_modelo.py</code>): 8 comprobaciones automáticas de calidad y coherencia.</li>
</ol>

<h2>3. Tecnología y recursos utilizados</h2>
<table class="t">
<tr><td style="width:26%"><b>Tecnología</b></td><td>Machine Learning supervisado: clasificación binaria con regresión logística.</td></tr>
<tr><td><b>Lenguaje y librerías</b></td><td>Python 3.14, scikit-learn 1.9 (modelos, métricas, validación cruzada), NumPy, joblib, Matplotlib.</td></tr>
<tr><td><b>Servicios / API</b></td><td>API REST de EduckTrack (Spring Boot 3): <code>POST /api/auth/login</code>, <code>GET /api/cursos/{{id}}/estudiantes</code>, <code>GET /api/cursos/{{id}}/plan-estudios</code>, <code>GET /api/calificaciones/historico/{{id}}</code>, <code>GET /api/asistencia/reporte</code>.</td></tr>
<tr><td><b>Variables del modelo</b></td><td>Promedio parcial, número de notas menores a 3.0, tendencia (última − primera nota), % de asistencia y promedio del periodo anterior.</td></tr>
<tr><td><b>Control de versiones</b></td><td>Git y GitHub, carpeta <code>poc-ml/</code> con su README.</td></tr>
</table>

<h2 class="salto">4. Capturas del resultado</h2>
<h3>4.1 Comparación de modelos (300 estudiantes de prueba que el modelo no vio)</h3>
<table class="t"><tr><th>Modelo</th><th>Exactitud</th><th>Precisión</th><th>Recall</th><th>F1</th><th>AUC ROC</th><th>Recall val. cruzada</th></tr>{filas_modelos}</table>
<div class="grid2">
<figure><img src="{uri(MOD / 'comparacion.png')}"><figcaption>Las tres opciones rinden parecido; la regresión logística tiene el mejor recall en validación cruzada (0,90) y el mejor AUC.</figcaption></figure>
<figure><img src="{uri(MOD / 'matriz_confusion.png')}"><figcaption>Matriz de confusión del modelo elegido: de 78 estudiantes que reprobaron, detectó 68.</figcaption></figure>
</div>
<figure style="width:72%;margin:0 auto 10px"><img src="{uri(MOD / 'importancia.png')}"><figcaption>Explicabilidad: el promedio parcial es lo que más pesa; un mejor promedio anterior, una tendencia al alza y más asistencia bajan el riesgo.</figcaption></figure>

<h3>4.2 Predicción con datos reales de EduckTrack (curso Sexto A, periodo 2)</h3>
<div style="{TERMINAL}">$ python predecir_desde_api.py --curso 2 --periodo 2
{salida('salida_prediccion.txt')}</div>
<p>Santiago Vargas aparece con <b>83 % de riesgo (ALTO)</b>: promedio parcial 2,9, seis notas por debajo de 3,0 y un periodo anterior de 2,2.
Es exactamente el caso que el docente debería atender primero.</p>

<h2>5. Pruebas realizadas</h2>
<div style="{TERMINAL}">$ python probar_modelo.py
{salida('salida_pruebas.txt')}</div>
<table class="t"><tr><th>Prueba</th><th>Datos de entrada</th><th>Resultado esperado</th><th>Resultado obtenido</th></tr>
<tr><td>Calidad sobre datos no vistos</td><td>300 estudiantes de prueba</td><td>Recall ≥ 0,80 y AUC ≥ 0,85</td><td>Recall 0,872 · AUC 0,954 <span class="ok">Cumple</span></td></tr>
<tr><td>Coherencia del modelo</td><td>Mismo estudiante bajando promedio, asistencia o tendencia</td><td>El riesgo se mueve en la dirección lógica</td><td>3 de 3 <span class="ok">Cumple</span></td></tr>
<tr><td>Casos extremos</td><td>Promedio 2,3 / 70 % asistencia vs. promedio 4,6 / 97 %</td><td>ALTO y BAJO</td><td>ALTO (100 %) y BAJO (0 %) <span class="ok">Cumple</span></td></tr>
<tr><td>Integración con el API</td><td>Curso 2, periodo 2 de EduckTrack</td><td>Riesgo de cada estudiante con notas</td><td>6 estudiantes evaluados <span class="ok">Cumple</span></td></tr>
<tr><td>Credenciales inválidas</td><td>Contraseña incorrecta en <code>EDUCKTRACK_PASSWORD</code></td><td>Mensaje claro, sin traza</td><td>“El API respondió 401…” <span class="ok">Cumple</span></td></tr>
</table>

<h2>6. Integración con el proyecto</h2>
<p>Hoy la prueba de concepto se conecta a EduckTrack <b>sin modificarlo</b>: usa la misma API y los mismos permisos que la aplicación web.
Para la integración completa se propone un endpoint <code>GET /api/reportes/riesgo-predictivo?cursoId=&amp;periodoAcademicoId=</code> en el
backend que lea <code>modelo_riesgo.json</code> y aplique la fórmula en Java (son cinco multiplicaciones y una función logística), y una
tarjeta “Estudiantes en riesgo” en el panel del docente.</p>

<h2>7. Limitaciones y posibles mejoras</h2>
<table class="t"><tr><th style="width:42%">Limitación</th><th>Mejora propuesta</th></tr>
<tr><td>Entrenado con datos sintéticos</td><td>Reentrenar con históricos reales anonimizados del colegio y volver a medir.</td></tr>
<tr><td>Los datos de demostración no tienen asistencia registrada (vale 100 %)</td><td>Probar con un curso que tenga asistencia real.</td></tr>
<tr><td>El promedio parcial es simple</td><td>Usar la ponderación por tipo de evaluación que ya calcula EduckTrack (RF-31).</td></tr>
<tr><td>Corre como script</td><td>Endpoint en el backend y alerta en el panel del docente.</td></tr>
<tr><td>Precisión de 0,70 (3 de cada 10 alertas son falsas)</td><td>Es aceptable para una alerta, pero se puede ajustar el umbral por colegio.</td></tr>
</table>

<h2>8. Consideraciones éticas, de seguridad y protección de datos</h2>
<ul>
<li>La predicción es una <b>alerta para el docente</b>: no decide, no reprueba, no etiqueta ni se le muestra al estudiante.</li>
<li>No usa datos sensibles (género, estrato, edad); solo desempeño académico. Se entrenó con datos sintéticos para no usar datos de menores (Ley 1581 de 2012).</li>
<li>Los datos reales se leen con el control de acceso del API (JWT y rol) y las credenciales van en variables de entorno.</li>
<li>El modelo es explicable: cada alerta trae su causa principal y los pesos se pueden revisar.</li>
</ul>

<h2>9. Enlaces</h2>
<table class="t">
<tr><td style="width:30%"><b>Repositorio (código fuente)</b></td><td><a href="{REPOSITORIO}/tree/main/poc-ml">{REPOSITORIO}/tree/main/poc-ml</a></td></tr>
<tr><td><b>Video demostrativo (máx. 7 min)</b></td><td><span class="pendiente">[pegar aquí el enlace del video]</span></td></tr>
</table>
"""

imprimir("GA8_220501096_AA2_EV04_Prueba_de_concepto_ML", documento("Prueba de concepto — Machine Learning", cuerpo))
