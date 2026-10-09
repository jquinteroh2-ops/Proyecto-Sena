# Prueba de concepto — Alerta temprana de riesgo académico con Machine Learning

**GA8-220501096-AA2-EV04** · EduckTrack · José Antonio Quintero Herrera y María José Grijalba Torres

## Problema

En EduckTrack el docente se entera de que un estudiante perdió el periodo cuando
el corte ya se cerró. Con las notas y la asistencia de la primera mitad del
periodo ya se puede anticipar quién va camino a reprobar, para intervenir a tiempo.

## Qué hace

Un modelo de **aprendizaje supervisado** (regresión logística) estima la
probabilidad de que cada estudiante termine el periodo con promedio menor a 3.0,
lo clasifica en riesgo **ALTO / MEDIO / BAJO** y explica la causa principal.

| Variable | Cómo la obtiene EduckTrack |
| --- | --- |
| `promedio_parcial` | promedio de las notas del periodo actual (`/api/calificaciones/historico/{id}`) |
| `notas_bajo_3` | cuántas de esas notas están por debajo de 3.0 |
| `tendencia` | última nota − primera nota del periodo |
| `asistencia_pct` | promedio del `% de asistencia` por materia (`/api/asistencia/reporte`) |
| `promedio_anterior` | promedio del periodo anterior |

## Archivos

| Archivo | Para qué |
| --- | --- |
| `generar_datos.py` | crea 1.500 estudiantes **sintéticos** (semilla fija, sin datos personales) |
| `entrenar.py` | compara 3 modelos, elige el mejor y exporta `modelo/modelo_riesgo.joblib` y `.json` |
| `predecir_desde_api.py` | **integración**: toma los estudiantes reales de un curso desde el API y calcula su riesgo |
| `probar_modelo.py` | 8 criterios de aceptación (calidad mínima y coherencia del modelo) |
| `modelo/` | modelo, métricas, gráficas y predicciones |

## Ejecución

```bash
pip install scikit-learn numpy joblib matplotlib
python generar_datos.py
python entrenar.py
python probar_modelo.py
# con EduckTrack corriendo (local o en Render)
python predecir_desde_api.py --curso 2 --periodo 2
BASE_URL=https://educktrack-backend.onrender.com EDUCKTRACK_CORREO=... EDUCKTRACK_PASSWORD=... \
  python predecir_desde_api.py --curso 2 --periodo 2
```

Las credenciales se leen de `EDUCKTRACK_CORREO` / `EDUCKTRACK_PASSWORD`; nunca van en el código.

## Resultados (datos de prueba, 300 estudiantes no vistos)

| Modelo | Exactitud | Recall | F1 | AUC ROC |
| --- | --- | --- | --- | --- |
| **Regresión logística (elegido)** | 0.870 | **0.872** | 0.777 | **0.954** |
| Árbol de decisión | 0.877 | 0.859 | 0.784 | 0.929 |
| Bosque aleatorio | 0.863 | 0.872 | 0.768 | 0.952 |

Se eligió la regresión logística: detecta igual o más estudiantes en riesgo,
separa mejor las clases y cada peso se puede explicar al docente.

## Limitaciones

- Entrenado con datos **sintéticos**: antes de usarlo de verdad hay que reentrenarlo
  con históricos reales anonimizados y medir de nuevo.
- La demo de EduckTrack no tiene registros de asistencia, así que esa variable vale 100 %.
- El promedio parcial es simple; EduckTrack pondera por tipo de evaluación (RF-31).

## Ética y protección de datos

- Es una **alerta para el docente**, no una decisión: no reprueba, no etiqueta ni se
  le muestra al estudiante.
- No usa género, estrato, edad ni ningún dato sensible; solo desempeño académico.
- Los datos reales se leen con el mismo control de acceso del API (JWT + rol) y no se
  guardan fuera de EduckTrack (Ley 1581 de 2012, habeas data).
