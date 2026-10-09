"""
Pruebas de integracion de EduckTrack (GA8-220501096-AA1-EV02).

Recorren la cadena completa: el cliente llama al FRONTEND (Vite en :5173),
que reenvia /api al BACKEND (Spring Boot), que lee y escribe en la BASE DE
DATOS. Si una prueba pasa, los cuatro componentes se comunicaron bien.

Uso:
    python pruebas/pruebas_integracion.py                  # contra http://localhost:5173
    BASE_URL=https://educktrack-web.onrender.com python pruebas/pruebas_integracion.py

Credenciales por variables de entorno (por defecto, las cuentas de demo que
crea backend/seed-demo.mjs en desarrollo):
    ADMIN_CORREO, ADMIN_PASSWORD, ESTUDIANTE_CORREO, ESTUDIANTE_PASSWORD

Escribe el resultado en pruebas/resultados.json.
"""
import json
import os
import random
import sys
import time
import urllib.error
import urllib.request

sys.stdout.reconfigure(encoding="utf-8")

BASE = os.environ.get("BASE_URL", "http://localhost:5173").rstrip("/") + "/api"
ADMIN = (os.environ.get("ADMIN_CORREO", "admin@educktrack.edu.co"), os.environ.get("ADMIN_PASSWORD", "Admin123*"))
ESTUDIANTE = (os.environ.get("ESTUDIANTE_CORREO", "sofia.martinez@educktrack.edu.co"),
              os.environ.get("ESTUDIANTE_PASSWORD", "Demo12345"))


def llamar(metodo, ruta, cuerpo=None, token=None):
    cabeceras = {"Content-Type": "application/json"}
    if token:
        cabeceras["Authorization"] = "Bearer " + token
    datos = json.dumps(cuerpo).encode() if cuerpo is not None else None
    peticion = urllib.request.Request(BASE + ruta, method=metodo, data=datos, headers=cabeceras)
    inicio = time.time()
    try:
        with urllib.request.urlopen(peticion, timeout=120) as r:
            texto = r.read()
            codigo = r.status
    except urllib.error.HTTPError as e:
        texto = e.read()
        codigo = e.code
    ms = int((time.time() - inicio) * 1000)
    try:
        cuerpo_resp = json.loads(texto) if texto else None
    except ValueError:
        cuerpo_resp = texto.decode(errors="ignore")[:200]
    return codigo, cuerpo_resp, ms


resultados = []


def prueba(funcionalidad, datos, esperado, codigo_esperado, ejecutar, validar=None):
    codigo, cuerpo, ms = ejecutar()
    ok = codigo == codigo_esperado and (validar is None or validar(cuerpo))
    if isinstance(cuerpo, dict) and cuerpo.get("token"):
        detalle = f"token JWT recibido ({cuerpo.get('tipo')}, expira en {cuerpo.get('expiraEnSegundos')} s)"
    elif isinstance(cuerpo, dict):
        detalle = cuerpo.get("mensaje") or ", ".join(f"{k}={v}" for k, v in list(cuerpo.items())[:3])
    elif isinstance(cuerpo, list):
        detalle = f"lista con {len(cuerpo)} registro(s)"
    else:
        detalle = str(cuerpo)[:80]
    obtenido = f"HTTP {codigo} - {detalle}"
    resultados.append({"n": len(resultados) + 1, "funcionalidad": funcionalidad, "datos": datos,
                       "esperado": esperado, "obtenido": obtenido, "estado": "Exitosa" if ok else "Fallida",
                       "ms": ms})
    print(f"[{'OK' if ok else 'FALLA'}] {len(resultados):>2}. {funcionalidad}: {obtenido} ({ms} ms)")
    return cuerpo


# --- Disponibilidad y autenticacion ---
prueba("Disponibilidad de la API", "GET /api/health", "HTTP 200, servicio arriba", 200,
       lambda: llamar("GET", "/health"))

sesion = prueba("Inicio de sesion valido (administrador)", f"correo {ADMIN[0]} + clave",
                "HTTP 200 con token JWT y datos del usuario", 200,
                lambda: llamar("POST", "/auth/login", {"correo": ADMIN[0], "password": ADMIN[1]}),
                lambda c: bool(c and c.get("token")))
token_admin = (sesion or {}).get("token")

prueba("Inicio de sesion con clave incorrecta", f"correo {ADMIN[0]} + clave 'incorrecta'",
       "HTTP 401 con mensaje de credenciales invalidas", 401,
       lambda: llamar("POST", "/auth/login", {"correo": ADMIN[0], "password": "incorrecta"}))

prueba("Acceso a recurso protegido sin token", "GET /api/estudiantes sin Authorization",
       "HTTP 401: no se entrega informacion", 401, lambda: llamar("GET", "/estudiantes"))

# --- Modulo estudiantes: escritura y lectura en la base de datos ---
documento = f"10{random.randint(10000000, 99999999)}"
ACUDIENTE = {"acudienteNombre": "Carmen Integracion", "acudienteTelefono": "3001234567",
             "acudienteParentesco": "MADRE"}
nuevo = prueba("Registrar estudiante (RF-06)",
               f"documento {documento}, 'Prueba Integracion', acudiente con telefono",
               "HTTP 201 con el estudiante creado y su id", 201,
               lambda: llamar("POST", "/estudiantes", {"documento": documento, "nombres": "Prueba",
                                                       "apellidos": "Integracion", "fechaNacimiento": "2010-05-14",
                                                       **ACUDIENTE},
                              token_admin),
               lambda c: bool(c and c.get("id")))
nuevo_id = (nuevo or {}).get("id")

prueba("Consultar el estudiante recien creado", f"GET /api/estudiantes/{nuevo_id}",
       "HTTP 200 con los mismos datos guardados en la BD", 200,
       lambda: llamar("GET", f"/estudiantes/{nuevo_id}", token=token_admin),
       lambda c: bool(c and c.get("documento") == documento))

prueba("Registrar estudiante con datos incompletos", "nombres y apellidos vacios",
       "HTTP 400 con el detalle de cada campo invalido", 400,
       lambda: llamar("POST", "/estudiantes", {"documento": "123", "nombres": "", "apellidos": ""}, token_admin),
       lambda c: bool(c and c.get("detalles")))

prueba("Registrar estudiante con documento repetido", f"documento {documento} (ya existe)",
       "Rechazo por regla de negocio (HTTP 409) que menciona el documento", 409,
       lambda: llamar("POST", "/estudiantes", {"documento": documento, "nombres": "Otro",
                                               "apellidos": "Duplicado", **ACUDIENTE}, token_admin),
       lambda c: "document" in str((c or {}).get("mensaje", "")).lower())

prueba("Registrar estudiante sin contacto del acudiente", "acudiente sin nombre ni telefono",
       "Rechazo por regla de negocio (HTTP 409)", 409,
       lambda: llamar("POST", "/estudiantes", {"documento": documento + "9", "nombres": "Sin",
                                               "apellidos": "Acudiente"}, token_admin),
       lambda c: "acudiente" in str((c or {}).get("mensaje", "")).lower())

# --- Rol estudiante ---
s_est = prueba("Inicio de sesion del estudiante", f"correo {ESTUDIANTE[0]}",
               "HTTP 200 con token del rol ESTUDIANTE", 200,
               lambda: llamar("POST", "/auth/login", {"correo": ESTUDIANTE[0], "password": ESTUDIANTE[1]}),
               lambda c: bool(c and c.get("token")))
token_est = (s_est or {}).get("token")

yo = prueba("Perfil del usuario autenticado", "GET /api/identidad/yo con token del estudiante",
            "HTTP 200 con su nombre, rol y estudianteId", 200,
            lambda: llamar("GET", "/identidad/yo", token=token_est),
            lambda c: bool(c and c.get("estudianteId")))
est_id = (yo or {}).get("estudianteId")

prueba("Historico de notas del estudiante (RF-37)", f"GET /api/calificaciones/historico/{est_id}",
       "HTTP 200 con sus calificaciones registradas", 200,
       lambda: llamar("GET", f"/calificaciones/historico/{est_id}", token=token_est),
       lambda c: isinstance(c, list) and len(c) > 0)

prueba("Estudiante consulta el catalogo de materias", "GET /api/materias con token del estudiante",
       "HTTP 200 con los nombres de las materias (antes 403)", 200,
       lambda: llamar("GET", "/materias", token=token_est),
       lambda c: isinstance(c, list) and len(c) > 0)

prueba("Estudiante intenta administrar usuarios", "GET /api/usuarios con token del estudiante",
       "HTTP 403: su rol no tiene permiso", 403, lambda: llamar("GET", "/usuarios", token=token_est))

# --- Calificaciones y reportes ---
prueba("Registrar nota fuera de la escala (1.0 - 5.0)", "valor 7.5 para el estudiante 1, curso 2",
       "Rechazo de la nota invalida (HTTP 400 o 409)", 409,
       lambda: llamar("POST", "/calificaciones", {"estudianteId": 1, "materiaId": 1, "cursoId": 2,
                                                  "periodoAcademicoId": 2, "tipo": "QUIZ", "valor": 7.5},
                      token_admin))

prueba("Reporte de rendimiento del curso (RF-48)", "GET /api/reportes/rendimiento?cursoId=1&periodoAcademicoId=1",
       "HTTP 200 con promedios calculados desde la BD", 200,
       lambda: llamar("GET", "/reportes/rendimiento?cursoId=1&periodoAcademicoId=1", token=token_admin))

prueba("Peticion con un parametro faltante", "GET /api/calificaciones/boletin sin cursoId ni periodo",
       "HTTP 400 indicando que parametro falta (antes respondia 401)", 400,
       lambda: llamar("GET", "/calificaciones/boletin?estudianteId=1", token=token_admin))

ruta = os.path.join(os.path.dirname(os.path.abspath(__file__)), "resultados.json")
with open(ruta, "w", encoding="utf-8") as f:
    json.dump({"base": BASE, "fecha": time.strftime("%Y-%m-%d %H:%M"), "pruebas": resultados}, f,
              ensure_ascii=False, indent=2)
exitosas = sum(r["estado"] == "Exitosa" for r in resultados)
print(f"\n{exitosas}/{len(resultados)} pruebas exitosas. Resultado en {ruta}")
sys.exit(0 if exitosas == len(resultados) else 1)
