"""
Genera el conjunto de datos SINTETICO para entrenar el modelo de riesgo.

Por que sintetico: EduckTrack todavia no tiene historicos reales de varios
anios, y entrenar con datos de menores de edad exige consentimiento y
anonimizacion. Los datos se simulan con relaciones plausibles entre las
senales tempranas del periodo y el resultado final, y una semilla fija para
que cualquiera obtenga exactamente los mismos datos.

Cada fila es un estudiante en un periodo. Columnas (las mismas que EduckTrack
puede calcular a mitad de periodo desde su API):
  promedio_parcial      promedio de las notas registradas hasta ahora (1.0-5.0)
  notas_bajo_3          cuantas de esas notas estan por debajo de 3.0
  tendencia             ultima nota - primera nota del periodo
  asistencia_pct        porcentaje de asistencia (0-100)
  promedio_anterior     promedio del periodo anterior (1.0-5.0)
  reprobo               1 si el promedio FINAL del periodo quedo < 3.0
"""
import csv
import os

import numpy as np

SEMILLA = 2026
N = 1500


def generar(n=N, semilla=SEMILLA):
    rng = np.random.default_rng(semilla)
    # "Habilidad" latente del estudiante: no se observa, explica las senales.
    habilidad = rng.normal(0, 1, n)
    compromiso = rng.normal(0, 1, n)

    promedio_anterior = np.clip(3.4 + 0.55 * habilidad + 0.15 * compromiso + rng.normal(0, 0.35, n), 1.0, 5.0)
    asistencia = np.clip(88 + 7 * compromiso + rng.normal(0, 5, n), 40, 100)
    promedio_parcial = np.clip(3.3 + 0.5 * habilidad + 0.25 * compromiso + rng.normal(0, 0.4, n), 1.0, 5.0)
    evaluaciones = rng.integers(3, 7, n)
    prob_baja = 1 / (1 + np.exp(3.2 * (promedio_parcial - 3.0)))
    notas_bajo_3 = rng.binomial(evaluaciones, prob_baja)
    tendencia = np.clip(0.3 * compromiso + rng.normal(0, 0.45, n), -2.5, 2.5)

    # Promedio final: depende de lo anterior + lo que pase en la segunda mitad.
    final = (0.55 * promedio_parcial + 0.25 * promedio_anterior + 0.012 * (asistencia - 88)
             + 0.25 * tendencia + 0.78 + rng.normal(0, 0.28, n))
    reprobo = (final < 3.0).astype(int)

    return [
        {
            "promedio_parcial": round(float(promedio_parcial[i]), 2),
            "notas_bajo_3": int(notas_bajo_3[i]),
            "tendencia": round(float(tendencia[i]), 2),
            "asistencia_pct": round(float(asistencia[i]), 1),
            "promedio_anterior": round(float(promedio_anterior[i]), 2),
            "reprobo": int(reprobo[i]),
        }
        for i in range(n)
    ]


if __name__ == "__main__":
    filas = generar()
    ruta = os.path.join(os.path.dirname(os.path.abspath(__file__)), "datos", "estudiantes_sinteticos.csv")
    os.makedirs(os.path.dirname(ruta), exist_ok=True)
    with open(ruta, "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=list(filas[0].keys()))
        w.writeheader()
        w.writerows(filas)
    tasa = sum(f["reprobo"] for f in filas) / len(filas)
    print(f"{len(filas)} estudiantes sinteticos -> {ruta}")
    print(f"Reprueban el periodo: {tasa:.1%}")
