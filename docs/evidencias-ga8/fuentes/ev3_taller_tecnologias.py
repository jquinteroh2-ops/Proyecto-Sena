"""GA8_220501096_AA2_EV03 - Taller sobre tecnologias emergentes y disruptivas."""
from comun import REPOSITORIO, documento, e, imprimir, portada

ASPECTOS = ["En qué consiste", "Qué problemas resuelve", "Aplicaciones posibles", "Recursos técnicos",
            "Ventajas", "Limitaciones", "Riesgos éticos, de seguridad y datos"]

TECNOLOGIAS = [
    ("Machine Learning (aprendizaje automático)", [
        "Rama de la inteligencia artificial en la que un sistema aprende patrones a partir de datos de ejemplo, en lugar de seguir reglas escritas a mano. En el aprendizaje <b>supervisado</b> se entrena con ejemplos ya etiquetados (por ejemplo, estudiantes que aprobaron o reprobaron); en el <b>no supervisado</b> el algoritmo agrupa los datos sin etiquetas.",
        "Predicción y clasificación cuando hay muchas variables y datos históricos: anticipar un resultado, detectar anomalías, agrupar casos parecidos.",
        "Alertas tempranas de deserción o bajo rendimiento, detección de fraude, diagnóstico médico asistido, filtros de spam, predicción de demanda.",
        "Datos históricos suficientes y de calidad; Python con scikit-learn, pandas o TensorFlow; capacidad de cómputo modesta para modelos clásicos (un portátil basta).",
        "Descubre relaciones que no son evidentes; mejora al reentrenarse con más datos; los modelos simples (regresión logística, árboles) son explicables.",
        "Depende de la calidad de los datos; puede aprender sesgos; un modelo entrenado en un contexto no siempre sirve en otro; requiere mantenimiento.",
        "Sesgos que discriminen a grupos de estudiantes; decisiones automáticas sin revisión humana; uso de datos personales de menores sin consentimiento (Ley 1581 de 2012).",
    ]),
    ("Blockchain (cadena de bloques)", [
        "Registro distribuido en el que los datos se guardan en bloques encadenados por funciones <b>hash</b>: cada bloque contiene la huella del anterior, así que alterar un dato rompe toda la cadena. Varias partes mantienen copias y validan los cambios por consenso, sin una autoridad central.",
        "Confianza entre partes que no se conocen; trazabilidad e inmutabilidad de registros; certificación de autenticidad.",
        "Criptomonedas, trazabilidad de productos, contratos inteligentes, certificados y diplomas verificables, votaciones.",
        "Una red (pública como Ethereum o privada como Hyperledger Fabric), nodos, billeteras y llaves criptográficas; en redes públicas, costo por transacción (gas).",
        "Inmutabilidad y auditoría verificable por cualquiera; elimina intermediarios; alta disponibilidad.",
        "Lenta y costosa frente a una base de datos; no permite corregir ni borrar errores; complejidad alta; consumo energético en algunos mecanismos de consenso.",
        "Choca con el derecho a rectificar y suprimir datos personales (habeas data); pérdida de llaves = pérdida de acceso; contratos inteligentes con errores explotables.",
    ]),
    ("Inteligencia artificial generativa", [
        "Modelos entrenados con enormes cantidades de texto, imagen o audio que <b>generan contenido nuevo</b> a partir de una instrucción (prompt): redactan, resumen, traducen o explican.",
        "Atención y orientación a usuarios, redacción de textos, apoyo al aprendizaje personalizado.",
        "Chatbots de orientación (como el asistente AVI de Atalaya), tutores virtuales, generación de retroalimentación para el estudiante.",
        "Acceso a un modelo por API (OpenAI, Anthropic, Groq, Gemini) o un modelo abierto local; control de costos por uso.",
        "Interacción en lenguaje natural; respuestas inmediatas; se integra por API sin entrenar nada.",
        "Puede inventar datos (alucinaciones); costo por consulta; dependencia de un proveedor externo.",
        "Envío de datos personales a terceros; respuestas incorrectas presentadas como ciertas; uso para suplantar el trabajo del estudiante.",
    ]),
    ("Internet de las cosas (IoT)", [
        "Dispositivos físicos con sensores y conexión a internet que envían datos automáticamente: lectores, cámaras, sensores de temperatura o presencia.",
        "Captura automática de datos del mundo físico sin digitarlos.",
        "Toma de asistencia con carné NFC o código QR al entrar al salón, control de aforo, monitoreo ambiental de aulas.",
        "Hardware (lectores NFC/QR, microcontroladores como ESP32), red Wi-Fi, un servicio que reciba los eventos (MQTT o HTTP).",
        "Elimina la toma manual de asistencia; datos en tiempo real.",
        "Inversión en hardware y mantenimiento; fallas de conectividad; difícil de demostrar sin los dispositivos.",
        "Rastreo excesivo de personas; dispositivos vulnerables a ataques; datos biométricos si se usa reconocimiento facial.",
    ]),
    ("Sistemas de recomendación", [
        "Algoritmos que sugieren elementos relevantes a cada usuario según su historial y el de usuarios parecidos (filtrado colaborativo o basado en contenido).",
        "Personalizar contenido cuando hay muchas opciones.",
        "Recomendar material de refuerzo, tareas o recursos según las materias en las que el estudiante tiene notas bajas.",
        "Historial de interacciones o desempeño; librerías como scikit-learn o Surprise.",
        "Personaliza el aprendizaje; aumenta el uso de los recursos.",
        "Necesita un catálogo de recursos y datos de uso; “arranque en frío” con usuarios nuevos.",
        "Encasillar al estudiante en un nivel; burbujas de contenido; perfiles a partir de datos personales.",
    ]),
]

COMPARACION = [
    ("Propósito", "Aprender patrones de los datos para predecir o clasificar.", "Registrar datos de forma inmutable y verificable sin autoridad central."),
    ("Tipo de información", "Datos históricos numéricos o categóricos (notas, asistencia), texto, imágenes.", "Transacciones y registros que no deben cambiar (certificados, pagos, trazas)."),
    ("Aplicaciones en software", "Alertas de riesgo, recomendaciones, detección de anomalías, clasificación.", "Certificados verificables, trazabilidad, contratos inteligentes, pagos."),
    ("Recursos técnicos", "Python, scikit-learn; un equipo común; datos de calidad.", "Red blockchain, nodos, llaves, billeteras; costo por transacción."),
    ("Complejidad de implementación", "Media: un modelo clásico se entrena en horas y se integra por API.", "Alta: contratos inteligentes, infraestructura y seguridad de llaves."),
    ("Riesgos y limitaciones", "Sesgo, datos insuficientes, decisiones sin supervisión humana.", "No se pueden corregir datos; choca con el habeas data; costos y lentitud."),
    ("Relación con EduckTrack", "<b>Alta:</b> el sistema ya tiene notas y asistencia; la necesidad de detectar a tiempo a quien va a perder es real.", "<b>Baja-media:</b> serviría para certificar boletines, pero las notas deben poder corregirse (RF-35, novedades)."),
]

FLUJO = """
<svg class="diagrama" viewBox="0 0 760 230" xmlns="http://www.w3.org/2000/svg" font-family="Segoe UI, Arial" font-size="11.5">
  <defs><marker id="f" markerWidth="10" markerHeight="10" refX="9" refY="5" orient="auto"><path d="M0,0 L10,5 L0,10 z" fill="#4B5563"/></marker></defs>
  <g>
    <rect x="5" y="20" width="160" height="72" rx="10" fill="#F3F5FB" stroke="#DDE2EC"/>
    <text x="85" y="44" text-anchor="middle" font-weight="700">1. Datos sintéticos</text>
    <text x="85" y="62" text-anchor="middle">1.500 estudiantes</text><text x="85" y="78" text-anchor="middle">generar_datos.py</text>
    <rect x="200" y="20" width="160" height="72" rx="10" fill="#F3F5FB" stroke="#DDE2EC"/>
    <text x="280" y="44" text-anchor="middle" font-weight="700">2. Entrenamiento</text>
    <text x="280" y="62" text-anchor="middle">3 modelos comparados</text><text x="280" y="78" text-anchor="middle">entrenar.py</text>
    <rect x="395" y="20" width="160" height="72" rx="10" fill="#6D2BD9"/>
    <text x="475" y="44" text-anchor="middle" font-weight="700" fill="#fff">3. Modelo elegido</text>
    <text x="475" y="62" text-anchor="middle" fill="#fff">Regresión logística</text><text x="475" y="78" text-anchor="middle" fill="#fff">modelo_riesgo.json</text>
    <line x1="165" y1="56" x2="198" y2="56" stroke="#4B5563" stroke-width="1.6" marker-end="url(#f)"/>
    <line x1="360" y1="56" x2="393" y2="56" stroke="#4B5563" stroke-width="1.6" marker-end="url(#f)"/>
  </g>
  <g>
    <rect x="5" y="140" width="160" height="72" rx="10" fill="#2350D6"/>
    <text x="85" y="164" text-anchor="middle" font-weight="700" fill="#fff">4. API de EduckTrack</text>
    <text x="85" y="182" text-anchor="middle" fill="#fff">login JWT, notas,</text><text x="85" y="198" text-anchor="middle" fill="#fff">asistencia del curso</text>
    <rect x="200" y="140" width="160" height="72" rx="10" fill="#F3F5FB" stroke="#DDE2EC"/>
    <text x="280" y="164" text-anchor="middle" font-weight="700">5. Variables</text>
    <text x="280" y="182" text-anchor="middle">promedio, notas &lt; 3,</text><text x="280" y="198" text-anchor="middle">tendencia, asistencia…</text>
    <rect x="395" y="140" width="160" height="72" rx="10" fill="#F3F5FB" stroke="#6D2BD9" stroke-width="2"/>
    <text x="475" y="164" text-anchor="middle" font-weight="700">6. Predicción</text>
    <text x="475" y="182" text-anchor="middle">probabilidad de reprobar</text><text x="475" y="198" text-anchor="middle">predecir_desde_api.py</text>
    <rect x="590" y="140" width="165" height="72" rx="10" fill="#FEF3C7" stroke="#B45309"/>
    <text x="672" y="164" text-anchor="middle" font-weight="700" fill="#92400E">7. Alerta al docente</text>
    <text x="672" y="182" text-anchor="middle" fill="#92400E">ALTO / MEDIO / BAJO</text><text x="672" y="198" text-anchor="middle" fill="#92400E">+ causa principal</text>
    <line x1="165" y1="176" x2="198" y2="176" stroke="#4B5563" stroke-width="1.6" marker-end="url(#f)"/>
    <line x1="360" y1="176" x2="393" y2="176" stroke="#4B5563" stroke-width="1.6" marker-end="url(#f)"/>
    <line x1="555" y1="176" x2="588" y2="176" stroke="#4B5563" stroke-width="1.6" marker-end="url(#f)"/>
    <line x1="475" y1="92" x2="475" y2="138" stroke="#4B5563" stroke-width="1.6" stroke-dasharray="4 3" marker-end="url(#f)"/>
  </g>
  <text x="600" y="50" font-size="10.5" fill="#4B5563">Fase de entrenamiento</text>
  <text x="600" y="66" font-size="10.5" fill="#4B5563">(una vez, sin datos reales)</text>
  <text x="5" y="228" font-size="10.5" fill="#4B5563">Fase de uso: con los datos reales del curso, a mitad del periodo</text>
</svg>"""


def ficha(nombre, textos):
    filas = "".join(f"<tr><td style='width:23%'><b>{a}</b></td><td>{b}</td></tr>" for a, b in zip(ASPECTOS, textos))
    return f"<h3>{e(nombre)}</h3><table class='t'>{filas}</table>"


cuerpo = portada("Taller sobre tecnologías emergentes y disruptivas", "GA8_220501096_AA2_EV03",
                 "Exploración, comparación y selección de una tecnología para EduckTrack") + f"""
<h2>Contexto</h2>
<p><b>EduckTrack</b> es una plataforma web de gestión académica (notas, asistencia, horarios, boletines y reportes por rol).
El taller busca qué tecnología emergente resuelve mejor una necesidad concreta del sistema y cómo probarla.</p>

<h2>Sección 1. Exploración de tecnologías</h2>
{ficha(*TECNOLOGIAS[0])}
{ficha(*TECNOLOGIAS[1])}
<div class="caja violeta"><b>¿Qué es una criptomoneda?</b> Es dinero digital que no emite un banco central: sus transacciones se registran
en una blockchain y se validan por consenso entre los nodos de la red, y la propiedad se demuestra con llaves criptográficas.
Bitcoin (2009) fue la primera; Ethereum añadió los contratos inteligentes. Su valor depende de la oferta y la demanda, por eso es
muy volátil, y en Colombia no es moneda de curso legal.</div>
{ficha(*TECNOLOGIAS[2])}
{ficha(*TECNOLOGIAS[3])}
{ficha(*TECNOLOGIAS[4])}

<h2>Sección 2. Comparación de Machine Learning y Blockchain</h2>
<table class="t"><tr><th style="width:20%">Criterio</th><th>Machine Learning</th><th>Blockchain</th></tr>
{''.join(f"<tr><td><b>{a}</b></td><td>{b}</td><td>{c}</td></tr>" for a, b, c in COMPARACION)}
</table>

<h2>Sección 3. Selección de una tecnología: Machine Learning</h2>
<table class="t">
<tr><td style="width:24%"><b>Problema o necesidad</b></td><td>El docente se entera de que un estudiante perdió el periodo cuando el corte ya se cerró (RF-34). Para entonces no hay tiempo de nivelarlo.</td></tr>
<tr><td><b>Usuario beneficiado</b></td><td>Docentes, directores de grupo y coordinación académica; indirectamente, el estudiante que recibe apoyo a tiempo.</td></tr>
<tr><td><b>Funcionalidad propuesta</b></td><td>Alerta temprana: a mitad del periodo, la probabilidad de reprobar de cada estudiante, su nivel (ALTO, MEDIO, BAJO) y la causa principal.</td></tr>
<tr><td><b>Pertinencia</b></td><td>EduckTrack ya existe para mejorar el seguimiento académico. Hoy solo alerta por asistencia (RB-04); el modelo combina notas, tendencia, asistencia e historial.</td></tr>
<tr><td><b>Datos y recursos</b></td><td>El sistema ya guarda calificaciones por periodo y asistencia, y los expone por la API. Python y scikit-learn son gratuitos y corren en cualquier equipo.</td></tr>
<tr><td><b>Tiempo</b></td><td>Prueba de concepto: 1 a 2 semanas. Integración en el panel del docente: 2 a 3 semanas más.</td></tr>
<tr><td><b>Integración</b></td><td>Consume la API REST existente (login JWT, histórico de notas, reporte de asistencia), sin cambiar la base de datos. El modelo se exporta a coeficientes JSON, que se pueden aplicar desde Java en el backend.</td></tr>
<tr><td><b>Riesgos y ética</b></td><td>Sesgo y uso indebido: el modelo solo alerta al docente; no decide, no se le muestra al estudiante y no usa datos sensibles.</td></tr>
</table>
<div class="caja"><b>¿Por qué no Blockchain?</b> Las notas de EduckTrack deben poder corregirse con una novedad justificada (RF-35), y un
registro inmutable va en contra de eso y del derecho a rectificar datos personales. Además exige infraestructura y costos que no
resuelven el problema principal del colegio, que es <i>anticipar</i> el bajo rendimiento, no certificarlo.</div>

<h2>Sección 4. Diseño de la prueba de concepto</h2>
<table class="t">
<tr><td style="width:24%"><b>Nombre</b></td><td>Alerta temprana de riesgo académico con Machine Learning</td></tr>
<tr><td><b>Objetivo</b></td><td>Demostrar que con las notas y la asistencia de la primera mitad del periodo se puede predecir, con un recall de al menos 80 %, qué estudiantes terminarán reprobando.</td></tr>
<tr><td><b>Problema</b></td><td>La detección del bajo rendimiento llega tarde, cuando el corte ya se cerró.</td></tr>
<tr><td><b>Tecnología</b></td><td>Machine Learning supervisado: clasificación binaria (reprueba / aprueba).</td></tr>
<tr><td><b>Datos de entrada</b></td><td>Por estudiante: promedio parcial, número de notas menores a 3.0, tendencia (última − primera nota), % de asistencia y promedio del periodo anterior. Para entrenar: 1.500 estudiantes <b>sintéticos</b>, para no usar datos reales de menores.</td></tr>
<tr><td><b>Procesamiento</b></td><td>Estandarización de las variables y comparación de regresión logística, árbol de decisión y bosque aleatorio, con validación cruzada. Se elige el modelo con mejor recall y que se pueda explicar.</td></tr>
<tr><td><b>Resultado esperado</b></td><td>Probabilidad de reprobar, nivel de riesgo y causa principal de cada estudiante de un curso.</td></tr>
<tr><td><b>Usuario beneficiado</b></td><td>Docente y coordinación académica.</td></tr>
<tr><td><b>Herramientas</b></td><td>Python 3, scikit-learn, NumPy, joblib, Matplotlib; API REST de EduckTrack (Spring Boot); Git y GitHub.</td></tr>
<tr><td><b>Integración</b></td><td>Script que inicia sesión en la API, lee los estudiantes del curso con sus notas y asistencia, y aplica el modelo. Paso siguiente: un endpoint <code>/api/reportes/riesgo-predictivo</code> en el backend con los coeficientes exportados.</td></tr>
<tr><td><b>Alcance mínimo</b></td><td>Generación de datos, entrenamiento y evaluación, exportación del modelo, predicción sobre un curso real de EduckTrack y criterios de aceptación automáticos.</td></tr>
<tr><td><b>Criterios de éxito</b></td><td>Recall ≥ 0,80 y AUC ROC ≥ 0,85 en datos de prueba; el riesgo sube si bajan el promedio o la asistencia; un caso claramente malo da ALTO y uno destacado da BAJO.</td></tr>
</table>
<h3>Esquema de funcionamiento</h3>
{FLUJO}

<h2>Recursos técnicos requeridos</h2>
<ul><li>Equipo con Python 3.10+ (sin GPU); librerías gratuitas: scikit-learn, NumPy, joblib, Matplotlib.</li>
<li>EduckTrack en ejecución (local o en Render) y una cuenta con permiso de consulta.</li>
<li>Repositorio Git: <a href="{REPOSITORIO}">{REPOSITORIO}</a>, carpeta <code>poc-ml/</code>.</li></ul>

<h2>Plan breve de implementación</h2>
<table class="t"><tr><th style="width:14%">Semana</th><th>Actividad</th><th style="width:30%">Entregable</th></tr>
<tr><td>1</td><td>Definir variables con la API disponible y generar datos sintéticos</td><td><code>generar_datos.py</code>, CSV</td></tr>
<tr><td>1</td><td>Entrenar y comparar modelos; elegir y exportar</td><td><code>entrenar.py</code>, métricas, gráficas</td></tr>
<tr><td>2</td><td>Integración con la API y criterios de aceptación</td><td><code>predecir_desde_api.py</code>, <code>probar_modelo.py</code></td></tr>
<tr><td>2</td><td>Documentación y video demostrativo</td><td>README, PDF y video</td></tr></table>

<h2>Consideraciones éticas, de seguridad y protección de datos</h2>
<ul>
<li><b>Supervisión humana:</b> la predicción es una alerta para el docente; nunca decide sobre el estudiante ni se le muestra a él.</li>
<li><b>Sin datos sensibles:</b> no se usan género, estrato, edad, etnia ni discapacidad, solo desempeño académico.</li>
<li><b>Datos de menores:</b> el entrenamiento usa datos sintéticos; un uso real exige consentimiento y anonimización (Ley 1581 de 2012).</li>
<li><b>Explicabilidad:</b> se eligió un modelo cuyos pesos se pueden explicar, y cada alerta trae su causa principal.</li>
<li><b>Seguridad:</b> los datos reales se leen con el mismo control del API (JWT y rol); las credenciales van en variables de entorno.</li>
<li><b>Sesgo:</b> antes de un uso real se debe medir si el modelo falla más con algún grupo de estudiantes.</li>
</ul>

<h2>Referencias</h2>
<ul class="pie" style="font-size:9pt">
<li>Antonopoulos, A. M. (2017). <i>Mastering Bitcoin: Programming the open blockchain</i> (2.ª ed.). O'Reilly Media.</li>
<li>Argoshub. (2021). <i>10 tecnologías emergentes o disruptivas a observar en 2021</i>. https://www.argoshub.com/10-tecnologias-emergentes-o-disruptivas-a-observar-en-2021/</li>
<li>Congreso de Colombia. (2012). <i>Ley Estatutaria 1581 de 2012, por la cual se dictan disposiciones generales para la protección de datos personales</i>.</li>
<li>Géron, A. (2019). <i>Hands-On Machine Learning with Scikit-Learn, Keras, and TensorFlow</i> (2.ª ed.). O'Reilly Media.</li>
<li>Mitchell, T. M. (1997). <i>Machine Learning</i>. McGraw-Hill.</li>
<li>Nakamoto, S. (2008). <i>Bitcoin: A peer-to-peer electronic cash system</i>. https://bitcoin.org/bitcoin.pdf</li>
<li>Pedregosa, F. et al. (2011). Scikit-learn: Machine learning in Python. <i>Journal of Machine Learning Research, 12</i>, 2825–2830.</li>
<li>SENA. (2022). <i>Construir aplicativo según las características de la arquitectura Android – Tecnologías emergentes y disruptivas</i> [Componente formativo].</li>
<li>UNESCO. (2021). <i>Recomendación sobre la ética de la inteligencia artificial</i>.</li>
</ul>
"""

imprimir("GA8_220501096_AA2_EV03_Taller_tecnologias_emergentes", documento("Taller de tecnologías emergentes", cuerpo))
