"""
Prueba de concepto integrada: calcula el riesgo academico de los estudiantes
de un curso con datos REALES de EduckTrack, tomados de su API REST.

Flujo:
  1. Inicia sesion en el API (credenciales por variables de entorno).
  2. Lista los estudiantes del curso y las materias de su plan de estudios.
  3. Por estudiante: historico de notas (RF-37) y reporte de asistencia (RF-28).
  4. Calcula las 5 variables con las que se entreno el modelo.
  5. Aplica el modelo y muestra probabilidad, nivel y la causa principal.

Uso:
  python predecir_desde_api.py --curso 2 --periodo 2
  BASE_URL=https://educktrack-backend.onrender.com EDUCKTRACK_CORREO=... EDUCKTRACK_PASSWORD=... \
      python predecir_desde_api.py --curso 2 --periodo 2

El modelo es solo una ALERTA TEMPRANA para orientar al docente: no toma
decisiones sobre el estudiante ni se le muestra a el.
"""
import argparse
import json
import math
import os
import sys
import urllib.error
import urllib.request

sys.stdout.reconfigure(encoding="utf-8")
AQUI = os.path.dirname(os.path.abspath(__file__))
BASE = os.environ.get("BASE_URL", "http://localhost:8081").rstrip("/") + "/api"
CORREO = os.environ.get("EDUCKTRACK_CORREO", "admin@educktrack.edu.co")
CLAVE = os.environ.get("EDUCKTRACK_PASSWORD", "Admin123*")

with open(os.path.join(AQUI, "modelo", "modelo_riesgo.json"), encoding="utf-8") as f:
    MODELO = json.load(f)

EXPLICACION = {
    "promedio_parcial": "promedio parcial bajo",
    "notas_bajo_3": "varias notas por debajo de 3.0",
    "tendencia": "sus notas vienen bajando",
    "asistencia_pct": "baja asistencia",
    "promedio_anterior": "venia con bajo promedio del periodo anterior",
}


def api(metodo, ruta, cuerpo=None, token=None):
    cab = {"Content-Type": "application/json"}
    if token:
        cab["Authorization"] = "Bearer " + token
    req = urllib.request.Request(BASE + ruta, method=metodo, headers=cab,
                                 data=json.dumps(cuerpo).encode() if cuerpo else None)
    with urllib.request.urlopen(req, timeout=180) as r:
        return json.loads(r.read() or b"null")


def predecir(variables):
    """Regresion logistica con los coeficientes exportados por entrenar.py."""
    z = MODELO["intercepto"]
    aportes = {}
    for col, media, desv, peso in zip(MODELO["columnas"], MODELO["media"], MODELO["desviacion"], MODELO["pesos"]):
        aporte = peso * (variables[col] - media) / desv
        aportes[col] = aporte
        z += aporte
    prob = 1 / (1 + math.exp(-z))
    umbral = MODELO["umbrales"]
    nivel = "ALTO" if prob >= umbral["alto"] else "MEDIO" if prob >= umbral["medio"] else "BAJO"
    if nivel == "BAJO":
        return prob, nivel, "-"
    causa = max(aportes, key=aportes.get)  # lo que mas empuja hacia el riesgo
    return prob, nivel, EXPLICACION[causa]


def variables_del_estudiante(token, estudiante_id, materias, periodo):
    notas = api("GET", f"/calificaciones/historico/{estudiante_id}", token=token)
    actuales = [n["valor"] for n in sorted(notas, key=lambda n: n["id"]) if n["periodoAcademicoId"] == periodo]
    anteriores = [n["valor"] for n in notas if n["periodoAcademicoId"] < periodo]
    if not actuales:
        return None
    asistencia = [api("GET", f"/asistencia/reporte?estudianteId={estudiante_id}&materiaId={m}"
                             f"&periodoAcademicoId={periodo}", token=token)["porcentajeAsistencia"] for m in materias]
    promedio = sum(actuales) / len(actuales)
    return {
        "promedio_parcial": promedio,
        "notas_bajo_3": sum(1 for v in actuales if v < 3.0),
        "tendencia": actuales[-1] - actuales[0],
        "asistencia_pct": sum(asistencia) / len(asistencia) if asistencia else 100.0,
        # Sin periodo anterior se usa el actual: no hay mas informacion.
        "promedio_anterior": sum(anteriores) / len(anteriores) if anteriores else promedio,
    }


def main():
    p = argparse.ArgumentParser(description="Riesgo academico de un curso con datos de EduckTrack")
    p.add_argument("--curso", type=int, required=True)
    p.add_argument("--periodo", type=int, required=True)
    args = p.parse_args()

    try:
        token = api("POST", "/auth/login", {"correo": CORREO, "password": CLAVE})["token"]
        estudiantes = api("GET", f"/cursos/{args.curso}/estudiantes", token=token)
        materias = api("GET", f"/cursos/{args.curso}/plan-estudios", token=token)
    except urllib.error.HTTPError as e:
        sys.exit(f"El API respondio {e.code}: {e.read().decode(errors='ignore')[:200]}")
    except urllib.error.URLError as e:
        sys.exit(f"No se pudo conectar con {BASE}: {e.reason}")

    filas = []
    for est in estudiantes:
        if est.get("estado") != "ACTIVO":
            continue
        variables = variables_del_estudiante(token, est["estudianteId"], materias, args.periodo)
        nombre = f'{est["nombres"]} {est["apellidos"]}'
        if variables is None:
            filas.append({"estudiante": nombre, "sin_notas": True})
            continue
        prob, nivel, causa = predecir(variables)
        filas.append({"estudianteId": est["estudianteId"], "estudiante": nombre,
                      **{k: round(v, 2) for k, v in variables.items()},
                      "probabilidad": round(prob, 3), "nivel": nivel, "causa": causa})

    filas.sort(key=lambda f: f.get("probabilidad", -1), reverse=True)
    print(f"\nRiesgo academico - curso {args.curso}, periodo {args.periodo}  ({BASE})\n")
    print(f"{'Estudiante':24} {'Prom.':>5} {'<3':>3} {'Tend.':>6} {'Asist.':>6} {'Ant.':>5}  {'Prob.':>6}  Nivel  Causa principal")
    for f in filas:
        if f.get("sin_notas"):
            print(f"{f['estudiante']:24} sin notas en el periodo")
            continue
        print(f"{f['estudiante']:24} {f['promedio_parcial']:5.2f} {f['notas_bajo_3']:3d} {f['tendencia']:6.2f} "
              f"{f['asistencia_pct']:6.1f} {f['promedio_anterior']:5.2f}  {f['probabilidad']:6.1%}  {f['nivel']:5}  {f['causa']}")
    with open(os.path.join(AQUI, "modelo", "predicciones.json"), "w", encoding="utf-8") as fh:
        json.dump({"curso": args.curso, "periodo": args.periodo, "estudiantes": filas}, fh, ensure_ascii=False, indent=2)
    print("\nDetalle guardado en poc-ml/modelo/predicciones.json")


if __name__ == "__main__":
    main()
