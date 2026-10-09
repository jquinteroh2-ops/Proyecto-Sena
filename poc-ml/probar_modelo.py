"""
Criterios de aceptacion de la prueba de concepto (se ejecutan con
`python probar_modelo.py`). Si alguno falla, la PoC no se da por valida.
"""
import json
import os
import sys

sys.stdout.reconfigure(encoding="utf-8")
AQUI = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, AQUI)
from predecir_desde_api import predecir  # noqa: E402

with open(os.path.join(AQUI, "modelo", "metricas.json"), encoding="utf-8") as f:
    METRICAS = json.load(f)["modelos"]["Regresion logistica"]

BASE = {"promedio_parcial": 3.4, "notas_bajo_3": 1, "tendencia": 0.0, "asistencia_pct": 88.0,
        "promedio_anterior": 3.4}
fallos = 0


def criterio(descripcion, cumple):
    global fallos
    fallos += 0 if cumple else 1
    print(f"[{'OK' if cumple else 'FALLA'}] {descripcion}")


# 1. Calidad minima sobre datos que el modelo no vio
criterio(f"Detecta al menos el 80% de los que reprueban (recall {METRICAS['recall']})", METRICAS["recall"] >= 0.80)
criterio(f"Separa bien las dos clases (AUC ROC {METRICAS['auc_roc']} >= 0.85)", METRICAS["auc_roc"] >= 0.85)

# 2. Coherencia: el riesgo se mueve en la direccion esperada
p_base = predecir(BASE)[0]
criterio("Bajar el promedio parcial sube el riesgo", predecir({**BASE, "promedio_parcial": 2.6})[0] > p_base)
criterio("Bajar la asistencia sube el riesgo", predecir({**BASE, "asistencia_pct": 65.0})[0] > p_base)
criterio("Una tendencia positiva baja el riesgo", predecir({**BASE, "tendencia": 0.8})[0] < p_base)

# 3. Casos claros
alto = predecir({"promedio_parcial": 2.3, "notas_bajo_3": 5, "tendencia": -0.6, "asistencia_pct": 70.0,
                 "promedio_anterior": 2.5})
bajo = predecir({"promedio_parcial": 4.6, "notas_bajo_3": 0, "tendencia": 0.2, "asistencia_pct": 97.0,
                 "promedio_anterior": 4.4})
criterio(f"Estudiante con notas y asistencia bajas -> riesgo ALTO ({alto[0]:.0%})", alto[1] == "ALTO")
criterio(f"Estudiante destacado -> riesgo BAJO ({bajo[0]:.0%})", bajo[1] == "BAJO")
criterio("La probabilidad siempre queda entre 0 y 1", 0 <= alto[0] <= 1 and 0 <= bajo[0] <= 1)

print(f"\n{8 - fallos}/8 criterios cumplidos")
sys.exit(1 if fallos else 0)
