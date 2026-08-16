# EduckTrack — Estado del proyecto y plan de trabajo

> **Documento de continuidad.** Recoge en qué punto está el proyecto, qué se
> decidió y qué falta, para que el trabajo pueda continuar en otra máquina o en
> otra sesión sin volver a deducirlo todo.
>
> Última actualización: **16 de agosto de 2026**.

---

## 1. Cómo continuar

**La siguiente fase pendiente es la 4 (Auditoría).** Las fases 1, 2 y 3 están
cerradas. Lo que quedó deliberadamente fuera de la Fase 3 está anotado en la
sección 6.

Antes de tocar nada, leer la sección 2 (reglas absolutas). Y para compilar:

```bash
export JAVA_HOME="/c/Users/joseq/.jdks/temurin-21.0.11"   # o el JDK 21 local
export PATH="$JAVA_HOME/bin:$PATH"
cd backend && mvn test
```

**El backend solo compila con JDK 21.** Con el JDK 26 que suele venir por
defecto, Lombok 1.18.34 falla con `NoClassDefFoundError: ... lombok.javac.Javac`
y javac reporta "cannot find symbol" en todos los getters/setters generados de
las entidades JPA. El síntoma engaña porque apunta a archivos que nadie tocó.

---

## 2. Reglas absolutas

Acordadas con el responsable del proyecto. No son sugerencias:

- **Nunca editar las migraciones Flyway ya ejecutadas** (V1–V9). Todo cambio de
  esquema va en V10 o posterior.
- **No tocar `EDUCKTRACK_REQUIREMENTS.md`**: es la fuente de verdad y la rúbrica
  de evaluación.
- **No borrar los comentarios de trazabilidad RF/RB** del código. Cada método
  referencia el requisito que implementa; es lo que hace auditable el proyecto.
- **No tocar el historial de git ni variables sensibles sin consultar.**
- **Nada de tablas "por si algún día"**: solo se crea lo que una funcionalidad
  inmediata necesite.
- **Cada fase cierra compilando y con los tests en verde** antes de pasar a la
  siguiente, con un informe de 7 puntos.

Esto **no es una reescritura**: se corrige lo roto y se construye sobre lo que
ya funciona.

---

## 3. Situación actual

| | Estado |
|---|---|
| Rama de trabajo | `main` (las fases 1, 2 y 3 se integraron ahí el 16/08/2026) |
| Pruebas | **64 en verde** |
| Fases cerradas | 1 (Identidad), 2 (Ownership/IDOR) y 3 (Migraciones) |
| Desplegado | Sí, en Railway (ver sección 5) |

### Fase 1 — Identidad (cerrada)

Hasta V8 no existía ninguna relación entre la tabla `usuario` y los perfiles
`estudiante` / `docente`, de modo que el sistema no podía resolver a qué
registro corresponde el usuario autenticado: el RBAC (RS-03) era nominal y
RB-08 era inaplicable.

- Migración **V9**: `estudiante.usuario_id`, `docente.usuario_id` y la tabla
  `vinculo_acudiente` (RF-11, RB-08, RD-08).
- `ContextoUsuario` resuelve identidad y perfil desde la base, **no desde el
  token**.
- Se adelantó la pantalla de login (RF-60, RF-61) porque sin ella el sistema
  solo era operable por API y las fases siguientes habrían sido invisibles.

### Fase 2 — Ownership / IDOR (cerrada)

Cambio de fondo: **el docente deja de tener visibilidad institucional**. Su
alcance pasa a ser el de su carga académica (`asignacion_docente`) más los
cursos que dirige como director de grupo (RB-02).

IDOR que estaban abiertos y quedaron cerrados:

| Endpoint | Qué permitía antes |
|---|---|
| `GET /api/notificaciones/bandeja?usuarioId=X` | cualquier autenticado leía la bandeja ajena |
| `POST /api/notificaciones/{id}/leida` | marcar como leída una notificación de otro |
| `POST /api/tareas/{id}/entregar` | un estudiante entregaba en nombre de otro (RF-39) |
| `GET /api/tareas/estado` | consultar el estado de tareas ajeno (RF-41) |
| `POST /api/tareas` | un docente asignaba tareas a nombre de un compañero |
| notas y asistencia | calificar y pasar lista de materias que no dicta |
| promedio, boletín, histórico, perfil | aceptaban cualquier `estudianteId` |
| listados de cursos, estudiantes y tareas próximas | eran institucionales |

**Decisión de diseño importante:** el control se aplica en los **servicios de
aplicación**, no en los controladores, para que valga sea cual sea el punto de
entrada. Los `@PreAuthorize` de los controladores siguen siendo la primera
barrera por rol, pero no son el control de acceso al dato.

Piezas clave en `ContextoUsuario`:

- `tieneVisionInstitucional()` — Administrador, Rector y los dos Coordinadores.
- `cursosDelDocente()` — asignación **o** dirección de grupo (RB-02).
- `puedeVerEstudiante()` / `exigirAccesoEstudiante()`
- `puedeVerCurso()` / `exigirAccesoCurso()`
- `puedeGestionarMateria()` / `exigirGestionMateria()` — más estricto: dirigir un
  grupo da visibilidad sobre el curso, **no** potestad para calificar materias
  que no se dictan.
- `exigirCuentaPropia()` — la bandeja de notificaciones es estrictamente
  personal; ni la visión institucional la abre.
- `resolverEstudianteId()` — si quien pregunta es un estudiante, **ignora** el
  parámetro recibido y devuelve el propio.

**Rendimiento:** `CalificacionService.promedio()` comprueba el acceso y delega
en `calcularPromedio()`, que no lo comprueba. Los reportes de grupo (RF-47)
autorizan el curso **una sola vez** y usan `calcularPromedio` para no repetir la
comprobación una vez por nota. Si añades un cálculo masivo, sigue ese patrón.

---

## 4. Pruebas

64 en verde. Tres piezas que conviene entender antes de tocarlas:

- **`ContextoUsuarioTest`** (20) — la *lógica* de la decisión de acceso, con
  dobles de prueba (Mockito).
- **`ConsultasDeAlcanceTest`** (8) — las *consultas* en las que esa lógica se
  apoya, contra **H2** (`@DataJpaTest`, perfil `test`). Existe porque con
  Mockito un nombre de método derivado mal escrito no se detecta hasta arrancar
  la aplicación.
- **`ContextoDeAplicacionTest`** (1) — `@SpringBootTest` que solo comprueba que
  el contexto se levanta. Red de seguridad barata: la Fase 2 inyectó
  `ContextoUsuario` en siete servicios, y un ciclo de beans o una consulta
  derivada inválida no rompe la compilación, solo el arranque.

**Perfil `test`** (`src/test/resources/application-test.yml`): Flyway
desactivado y `ddl-auto: create-drop`, porque las migraciones son específicas de
MySQL. La equivalencia con el esquema real la sigue garantizando
`ddl-auto: validate` al arrancar la aplicación de verdad.

### Deuda de pruebas conocida

**Testcontainers con MySQL real está acordado pero no implementado.** Se decidió
usarlo para las pruebas de ownership, pero el daemon de Docker no estaba
levantado en la máquina de trabajo. Se sustituyó por H2. Si se retoma: añadir la
dependencia al `pom.xml`, y **Docker pasa a ser obligatorio para `mvn test`**.

---

## 5. Despliegue (Railway)

Desplegado como **entorno de prueba** el 16/08/2026, con las fases 1 y 2.

- **URL:** https://educktrack-frontend-production.up.railway.app
- Proyecto `educktrack`, id `8c58f62b-66b1-42be-86ec-30c3059b3047`, entorno
  `production`.
- Tres servicios: `MySQL` (imagen 9.4), `educktrack-backend`,
  `educktrack-frontend`.

**Topología:** solo el frontend tiene dominio público. nginx hace de proxy
inverso hacia `educktrack-backend.railway.internal:8080` por la red privada, de
modo que la API no está expuesta a internet y no hace falta configurar CORS.

**Redesplegar** (el CLI sube la raíz del repo por defecto, que es lo que rompió
el primer intento — de ahí `--path-as-root`):

```bash
railway up ./backend  --path-as-root --service educktrack-backend  --detach
railway up ./frontend --path-as-root --service educktrack-frontend --detach
```

### Trampas que ya costaron tiempo

- El backend necesita **`SERVER_ADDRESS=::`**: la red privada de Railway es
  IPv6 y Spring bindea `0.0.0.0` por defecto. Y `PORT=8080` fijo, para que el
  dominio privado sea predecible.
- **nginx resuelve los upstream al cargar la configuración.** Con un nombre
  literal en `proxy_pass`, el contenedor cae en bucle si el backend todavía no
  existe. Por eso `default.conf.template` usa `resolver` más una variable, y
  `$request_uri` (concatenar una URI literal a una variable descarta la ruta y
  la query).
- El script `05-set-resolver.envsh` necesita `chmod +x` en el Dockerfile: el bit
  de ejecución no sobrevive a un `COPY` desde un checkout en Windows.
- Flyway avisa de que **MySQL 9.4 no está probado**, pero aplica las 9
  migraciones sin problema.
- **Al cambiar variables, Railway redespliega.** Esperar a que propaguen antes
  de hacer `railway up`. Una carrera ahí creó una cuenta de administrador
  (id 1, `admin@educktrack.edu.co`) con credenciales que no coinciden con las
  fijadas; quedó **desactivada**, no borrada.

### Credenciales

El administrador inicial lo siembra `BootstrapAdministrador` a partir de
`EDUCKTRACK_BOOTSTRAP_ADMIN_CORREO` / `_PASSWORD` / `_NOMBRE`. Crea la cuenta
solo si ese correo no existe, así que es idempotente y no pisa una cuenta cuya
contraseña ya se cambió.

**El secreto JWT y la contraseña del administrador viven solo en las variables
de Railway, nunca en el repositorio.** Para conocerlos:
`railway variables --service educktrack-backend`.

---

## 6. Fase 3 — Migraciones (cerrada)

El esquema resultó estar **mejor de lo esperado**: todas las tablas tienen
claves foráneas, `utf8mb4`, y las restricciones de unicidad importantes (RB-06,
RB-18, RB-19, plan de estudios, asignación docente). InnoDB indexa
automáticamente las columnas con clave foránea, así que las consultas de alcance
de la Fase 2 **ya estaban cubiertas** por índices y no hizo falta añadir
ninguno. Esa comprobación es parte del resultado de la fase: no había un
problema de rendimiento de esquema que arreglar.

### Lo que se hizo

**Migración V10 — RB-01 y RB-05 pasan a garantizarse en la base de datos.**
Ambas reglas se comprobaban solo en la capa de aplicación, con un `SELECT`
previo al `INSERT`. Ese patrón no es atómico: dos peticiones concurrentes leen
"no existe" a la vez y crean las dos filas que la regla prohíbe. En matrículas
de inicio de curso, con varias personas de Coordinación trabajando a la vez, no
es un caso hipotético.

MySQL no admite índices únicos parciales, así que la restricción se expresa con
una **columna generada que vale NULL cuando la fila no participa de la regla**
(un índice UNIQUE admite tantos NULL como haga falta):

- `matricula.rb01_matricula_activa` → una sola matrícula ACTIVA por estudiante y
  periodo. Las RETIRADA y FINALIZADA quedan fuera del índice, que es lo que
  permite volver a matricular tras un retiro.
- `periodo_academico.rb05_periodo_activo` → un solo periodo activo por año
  lectivo.

Esas columnas **no se mapean en las entidades JPA** a propósito: son una
restricción del esquema, no un dato del dominio, y mapearlas invitaría a
escribir en ellas. No rompen `ddl-auto: validate`, que solo comprueba que
existan las columnas mapeadas.

**`baseline-on-migrate` pasa de `true` a `false`.** En `true`, ante una base con
tablas pero sin historial de Flyway, la herramienta se inventa una línea base y
sigue adelante como si el esquema fuera suyo: o se salta migraciones o intenta
crear tablas que ya existen, y en silencio. Es justo lo contrario de que Flyway
sea la única fuente de verdad del esquema (RS-02). En `false` ese caso falla con
un error explícito, que es el comportamiento que se quiere.

### Lo que se dejó fuera deliberadamente

**`calificacion.valor` es `DOUBLE`.** Para notas en escala 1.0–5.0,
`DECIMAL(3,2)` sería exacto y `DOUBLE` arrastra error de redondeo. Cambiarlo
obliga a pasar `double` → `BigDecimal` en entidad, dominio, DTOs y pruebas: es
un refactor transversal que no cabe en una fase de migraciones sin arrastrar
medio backend. **Decidir explícitamente si entra en la Fase 6 (reglas
académicas) o en la 8 (rendimiento).**

**`app_metadata`.** Tabla creada en V1, no mapeada a ninguna entidad y sin uso.
Candidata a eliminarse por la regla de "nada por si acaso", pero eliminar tablas
es destructivo y la regla dice consultar antes. **Pendiente de decisión.**

**Prueba de las migraciones.** Sigue sin haberla: las pruebas usan H2 con
`create-drop` y Flyway desactivado, así que **nadie ejecuta las migraciones en
CI**. V10 se verificó aplicándola contra el MySQL real de Railway, que es una
verificación de verdad pero manual. Es el caso de uso natural de Testcontainers
(ver sección 4); sigue bloqueado por Docker.

---

## 7. Fases restantes

| # | Fase | Notas |
|---|---|---|
| 3 | **Migraciones** | Siguiente. Análisis en la sección 6. |
| 4 | Auditoría | Trazabilidad de quién cambió qué. Hoy solo `novedad_nota` (RB-15) y `cierre_corte` guardan autor. |
| 5 | Notificaciones por eventos Spring | Hoy `NotificacionService.notificar()` se llama directamente; pasar a eventos de dominio. |
| 6 | Reglas académicas | Repasar RB-03, RB-04, RB-07, RB-10, RB-12, RB-17 contra los requisitos. |
| 7 | Recuperación de contraseña | Existe `POST /api/auth/recuperar-password`: verificar si está realmente implementado. |
| 8 | Rendimiento | Ver la nota de `calcularPromedio` en la sección 3. `ContextoUsuario` consulta el usuario en cada comprobación: hay margen para memorizarlo por petición. |
| 9 | Limpieza | |
| 10 | Preparación frontend | El frontend hoy es mínimo: login y pantalla de inicio. |

### Decisiones ya tomadas (no volver a discutirlas)

- **No se implementa multi-institución.** RNF-14 es prioridad Baja y está
  marcado como "futura".
- **"Simulacros" no existe en este proyecto**: venía de otro enunciado.
- El alcance del docente es el de sus cursos, **no** institucional (Fase 2).

---

## 8. Estructura del repositorio

```
backend/          Spring Boot 3.3.5, Java 21, arquitectura hexagonal por módulo
  src/main/java/com/educktrack/
    <modulo>/domain           reglas de negocio puras
    <modulo>/application      casos de uso (aquí vive el control de acceso)
    <modulo>/infrastructure   persistence (JPA) y rest (controladores)
  src/main/resources/db/migration   Flyway V1..V9
frontend/         React 18 + Vite + Tailwind, servido por nginx
docker-compose.yml   mysql + backend + frontend
EDUCKTRACK_REQUIREMENTS.md   fuente de verdad (NO TOCAR)
```

Los módulos se comunican por identificadores (`curso_id`, `materia_id`), no por
relaciones JPA entre agregados: es deliberado, para mantenerlos desacoplados. La
integridad referencial se declara en las migraciones.
