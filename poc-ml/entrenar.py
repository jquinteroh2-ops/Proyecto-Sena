"""
Entrena y evalua el modelo de riesgo academico (aprendizaje supervisado).

Compara tres algoritmos y elige el que mejor equilibra deteccion (recall: que
no se escape ningun estudiante en riesgo) e interpretabilidad (poder decirle
al docente POR QUE el modelo marco a un estudiante).

Salidas en modelo/:
  modelo_riesgo.joblib   pipeline entrenado (escalado + regresion logistica)
  modelo_riesgo.json     el mismo modelo en coeficientes, para usarlo sin Python
  metricas.json          metricas de los tres modelos sobre datos de prueba
  matriz_confusion.png, importancia.png, comparacion.png
"""
import csv
import json
import os

import joblib
import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt
import numpy as np
from sklearn.ensemble import RandomForestClassifier
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import (accuracy_score, confusion_matrix, f1_score, precision_score, recall_score,
                             roc_auc_score)
from sklearn.model_selection import cross_val_score, train_test_split
from sklearn.pipeline import make_pipeline
from sklearn.preprocessing import StandardScaler
from sklearn.tree import DecisionTreeClassifier

AQUI = os.path.dirname(os.path.abspath(__file__))
COLUMNAS = ["promedio_parcial", "notas_bajo_3", "tendencia", "asistencia_pct", "promedio_anterior"]
ETIQUETAS = {
    "promedio_parcial": "Promedio parcial",
    "notas_bajo_3": "Notas bajo 3.0",
    "tendencia": "Tendencia",
    "asistencia_pct": "Asistencia %",
    "promedio_anterior": "Promedio periodo anterior",
}
AZUL, VIOLETA, GRIS = "#2350D6", "#6D2BD9", "#9CA3AF"


def cargar():
    with open(os.path.join(AQUI, "datos", "estudiantes_sinteticos.csv"), encoding="utf-8") as f:
        filas = list(csv.DictReader(f))
    X = np.array([[float(r[c]) for c in COLUMNAS] for r in filas])
    y = np.array([int(r["reprobo"]) for r in filas])
    return X, y


def metricas(modelo, X, y):
    pred = modelo.predict(X)
    prob = modelo.predict_proba(X)[:, 1]
    return {
        "exactitud": round(accuracy_score(y, pred), 3),
        "precision": round(precision_score(y, pred), 3),
        "recall": round(recall_score(y, pred), 3),
        "f1": round(f1_score(y, pred), 3),
        "auc_roc": round(roc_auc_score(y, prob), 3),
    }


def main():
    X, y = cargar()
    X_ent, X_pru, y_ent, y_pru = train_test_split(X, y, test_size=0.2, stratify=y, random_state=2026)

    candidatos = {
        # class_weight="balanced": perder un estudiante en riesgo cuesta mas que
        # una falsa alarma, asi que se penalizan mas los errores de la clase 1.
        "Regresion logistica": make_pipeline(StandardScaler(),
                                             LogisticRegression(class_weight="balanced", max_iter=1000)),
        "Arbol de decision": DecisionTreeClassifier(max_depth=4, class_weight="balanced", random_state=2026),
        "Bosque aleatorio": RandomForestClassifier(n_estimators=200, max_depth=6, class_weight="balanced",
                                                   random_state=2026),
    }
    resultados = {}
    for nombre, modelo in candidatos.items():
        validacion = cross_val_score(modelo, X_ent, y_ent, cv=5, scoring="recall")
        modelo.fit(X_ent, y_ent)
        resultados[nombre] = {**metricas(modelo, X_pru, y_pru),
                              "recall_validacion_cruzada": round(float(validacion.mean()), 3)}
        print(f"{nombre:22} {resultados[nombre]}")

    # Se elige la regresion logistica: rinde igual o mejor y cada coeficiente se
    # puede explicar (cuanto sube el riesgo por cada punto de asistencia, etc.).
    elegido = candidatos["Regresion logistica"]
    os.makedirs(os.path.join(AQUI, "modelo"), exist_ok=True)
    joblib.dump(elegido, os.path.join(AQUI, "modelo", "modelo_riesgo.joblib"))

    escalador, logistica = elegido.named_steps["standardscaler"], elegido.named_steps["logisticregression"]
    exportado = {
        "descripcion": "Probabilidad de reprobar el periodo. p = 1 / (1 + e^-(b + sum(w_i * (x_i - media_i) / desv_i)))",
        "columnas": COLUMNAS,
        "media": [round(float(v), 6) for v in escalador.mean_],
        "desviacion": [round(float(v), 6) for v in escalador.scale_],
        "pesos": [round(float(v), 6) for v in logistica.coef_[0]],
        "intercepto": round(float(logistica.intercept_[0]), 6),
        "umbrales": {"alto": 0.6, "medio": 0.3},
        "entrenado_con": "datos sinteticos (poc-ml/generar_datos.py, semilla 2026)",
    }
    with open(os.path.join(AQUI, "modelo", "modelo_riesgo.json"), "w", encoding="utf-8") as f:
        json.dump(exportado, f, ensure_ascii=False, indent=2)
    with open(os.path.join(AQUI, "modelo", "metricas.json"), "w", encoding="utf-8") as f:
        json.dump({"datos_entrenamiento": int(len(y_ent)), "datos_prueba": int(len(y_pru)),
                   "tasa_reprobacion": round(float(y.mean()), 3), "modelos": resultados,
                   "elegido": "Regresion logistica"}, f, ensure_ascii=False, indent=2)

    # --- Graficas para el informe ---
    plt.rcParams.update({"font.size": 11, "axes.spines.top": False, "axes.spines.right": False})

    cm = confusion_matrix(y_pru, elegido.predict(X_pru))
    fig, ax = plt.subplots(figsize=(4.6, 4))
    ax.imshow(cm, cmap="Blues")
    for (i, j), v in np.ndenumerate(cm):
        ax.text(j, i, str(v), ha="center", va="center", fontsize=16,
                color="white" if v > cm.max() / 2 else "#111827")
    ax.set_xticks([0, 1], ["Aprueba", "Reprueba"])
    ax.set_yticks([0, 1], ["Aprueba", "Reprueba"])
    ax.set_xlabel("Prediccion del modelo")
    ax.set_ylabel("Resultado real")
    ax.set_title("Matriz de confusion (datos de prueba)")
    fig.tight_layout()
    fig.savefig(os.path.join(AQUI, "modelo", "matriz_confusion.png"), dpi=160)
    plt.close(fig)

    pesos = logistica.coef_[0]
    orden = np.argsort(np.abs(pesos))
    fig, ax = plt.subplots(figsize=(6.4, 3.4))
    colores = [VIOLETA if pesos[i] > 0 else AZUL for i in orden]
    ax.barh([ETIQUETAS[COLUMNAS[i]] for i in orden], pesos[orden], color=colores)
    ax.axvline(0, color=GRIS, linewidth=1)
    ax.set_xlabel("Peso en el modelo (variables estandarizadas)")
    ax.set_title("Que empuja el riesgo: violeta sube, azul baja")
    fig.tight_layout()
    fig.savefig(os.path.join(AQUI, "modelo", "importancia.png"), dpi=160)
    plt.close(fig)

    nombres = list(resultados.keys())
    claves = ["exactitud", "recall", "f1", "auc_roc"]
    ancho = 0.26
    fig, ax = plt.subplots(figsize=(6.8, 3.8))
    for k, (nombre, color) in enumerate(zip(nombres, [AZUL, VIOLETA, GRIS])):
        ax.bar(np.arange(len(claves)) + (k - 1) * ancho, [resultados[nombre][c] for c in claves], ancho,
               label=nombre, color=color)
    ax.set_xticks(np.arange(len(claves)), ["Exactitud", "Recall", "F1", "AUC ROC"])
    ax.set_ylim(0.5, 1.0)
    ax.legend(frameon=False, fontsize=9, loc="upper center", bbox_to_anchor=(0.5, -0.12), ncol=3)
    ax.set_title("Comparacion de modelos (datos de prueba)")
    fig.tight_layout()
    fig.savefig(os.path.join(AQUI, "modelo", "comparacion.png"), dpi=160)
    plt.close(fig)
    print("Modelo y graficas guardados en poc-ml/modelo/")


if __name__ == "__main__":
    main()
