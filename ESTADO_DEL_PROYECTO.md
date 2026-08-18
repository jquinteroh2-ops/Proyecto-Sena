# EduckTrack — Estado del proyecto y plan de trabajo

> **Documento de continuidad.** Recoge en qué punto está el proyecto, qué se
> decidió y qué falta, para que el trabajo pueda continuar en otra máquina o en
> otra sesión sin volver a deducirlo todo.
>
> Última actualización: **18 de agosto de 2026**.

---

## 1. Cómo continuar

**Las diez fases del plan están cerradas.** Lo que queda pendiente está
recogido en la sección 7. Lo que quedó deliberadamente fuera está anotado al
final de cada sección de fase.

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
| Rama de trabajo | `main` (las 10 fases integradas el 18/08/2026) |
| Pruebas | **147 en verde** |
| Fases cerradas | 1 (Identidad), 2 (Ownership/IDOR), 3 (Migraciones), 4 (Auditoría), 5 (Notificaciones), 6 (Reglas académicas), 7 (Recuperación de contraseña), 8 (Rendimiento), 9 (Configuración institucional y limpieza) y 10 (Preparación frontend) |
| Desplegado | Hasta la Fase 5. **Las fases 6 a 10 no se han desplegado: V12 a V15 no se han aplicado al MySQL real.** |

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

147 en verde. Piezas que conviene entender antes de tocarlas:

- **`ContextoUsuarioTest`** (20) — la *lógica* de la decisión de acceso, con
  dobles de prueba (Mockito).
- **`ConsultasDeAlcanceTest`** (8) — las *consultas* en las que esa lógica se
  apoya, contra **H2** (`@DataJpaTest`, perfil `test`). Existe porque con
  Mockito un nombre de método derivado mal escrito no se detecta hasta arrancar
  la aplicación.
- **`ContextoDeAplicacionTest`** (1) — `@SpringBootTest` que solo comprueba que
  el contexto se levanta. Red de seguridad barata: la Fase 2 inyectó
  `ContextoUsuario` en siete servicios, y un ciclo de beans o una consulta
  derivada inválida no rompe la compilación, solo el arranque. Sigue ganándose el
  sitio: la Fase 6 hizo que `notas` dependa de `asistencia`, y un ciclo de beans
  ahí no lo detecta ninguna otra prueba.
- **`BoletinAcademicoTest`** (6) y **`CargaAcademicaTest`** (4) — las reglas de la
  Fase 6 (RB-12, RB-04, RB-09) con dobles de prueba.
- **`ConsultaDeCupoTest`** (2) — la consulta con `@Lock` de RB-17 contra H2. No
  demuestra la exclusión mutua (haría falta concurrencia real contra MySQL), pero
  sí que la consulta existe y es JPQL válido, que es donde está el riesgo
  práctico: un fallo ahí solo aparecería al matricular.
- **`IdentidadDeLaPeticionTest`** (4) — la memorizacion de identidad de la Fase
  8, y sobre todo que **ninguna identidad sobreviva a la limpieza**: es la red
  que protege el aislamiento entre peticiones que comparten hilo.
- **`ParametrosServiceTest`** (8) y **`EscalaCalificacionTest`** (6) — RF-59.
  Lo que fijan es que un parametro invalido se rechace **al guardarlo**: un
  valor imposible no falla donde se escribe sino mucho despues, calificando,
  y para entonces nadie relaciona el sintoma con el cambio que lo provoco.
- **`RecuperacionPasswordServiceTest`** (12) — las propiedades de seguridad de
  HU-04: que solicitar no revele si la cuenta existe, que en base de datos quede
  el hash y no el token, que el enlace se consuma una sola vez y que una
  contraseña rechazada **no** lo gaste. Son justo las que se rompen sin que
  ninguna prueba funcional se entere.

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

## 6-bis. Fase 4 — Auditoría (cerrada)

**RS-07** exige que toda operación crítica quede registrada con usuario, fecha y
descripción; **RF-63** que se registre automáticamente; **RF-05** un historial de
inicios de sesión. Nada de esto existía: eran los dos requisitos completamente
sin implementar del proyecto.

### Lo que se hizo

**Migración V11 — tabla `auditoria`.** Una sola tabla para operaciones críticas y
accesos: un inicio de sesión es una operación crítica más, y separarlos obligaría
a consultar en dos sitios para responder "qué hizo esta persona".

El campo `usuario` guarda el **correo en texto, no una clave foránea**. Es
deliberado: el registro debe sobrevivir al borrado de la cuenta y debe poder
anotar intentos de acceso con correos que no existen. Un log que se queda sin
filas por un `ON DELETE` no sirve para auditar.

**Módulo `auditoria`** con dos servicios separados a propósito:

- `AuditoriaService` — **el único punto por el que se escribe el log**. El
  usuario se resuelve del contexto de seguridad, **no lo pasa el llamador**: un
  log donde cada servicio elige qué nombre escribe es falsificable por descuido.
- `ConsultaAuditoriaService` — solo lectura, siempre paginada.

**Dos decisiones sobre transacciones que conviene no revertir sin pensarlo:**

1. `registrar(...)` participa de la transacción del llamador. Si la operación se
   deshace, su anotación también: auditar algo que no ocurrió induce a error a
   quien luego revisa.
2. `registrarAcceso(...)` abre transacción propia (`REQUIRES_NEW`), porque el
   caso que más importa auditar es justo el que **falla**, y no debe arrastrarlo
   el rollback de la autenticación rechazada.

**Operaciones registradas** (enum `TipoOperacion`, cerrado a propósito para que
añadir una sea una decisión explícita): alta y desactivación de cuenta (HU-02),
retiro de estudiante (HU-07), matrícula y anulación, nota registrada, editada y
novedad (RS-07, HU-22), cierre de corte (HU-20), justificación de inasistencia
(HU-15), ponderación (HU-10), y accesos exitosos y fallidos (RF-05).

En las ediciones de nota **el log guarda el valor anterior**: una nota cambiada
sin saber de qué valor venía no es auditable.

**Endpoints** `GET /api/auditoria` y `GET /api/auditoria/accesos`, ambos
**solo Administrador**. El log revela qué hizo cada persona y a qué hora;
abrirlo a más roles convertiría una herramienta de control en una de vigilancia
entre compañeros. No hay endpoint de escritura: el registro lo hace el sistema
(RF-63) desde los casos de uso.

En el acceso fallido **no se anota el motivo exacto** (cuenta inexistente frente
a contraseña errónea) para no convertir el log en un oráculo que confirme qué
correos están dados de alta.

### Lo que quedó fuera

**HU-03 (matriz de permisos por rol y módulo) no se auditó porque no existe.**
Los roles hoy son fijos y los permisos están en anotaciones `@PreAuthorize`, no
en datos configurables. Cuando esa funcionalidad se implemente, `TipoOperacion`
necesitará un valor nuevo.

**HU-02 pide además notificar por correo al usuario desactivado.** Eso es
notificaciones, no auditoría: entra de forma natural en la **Fase 5**.

---

## 6-ter. Fase 5 — Notificaciones por eventos (cerrada)

**El hallazgo de partida:** `NotificacionService.notificar()` **no lo llamaba
ningún servicio de negocio**. Solo el envío manual (RF-53). Su Javadoc afirmaba
ser el punto de entrada de las alertas automáticas, pero RB-13, RF-30, RF-42,
RF-55 y RF-56 estaban sin implementar. El módulo existía y no notificaba nada.

### Arquitectura

Los servicios de negocio **publican hechos**, no envían avisos. Los eventos son
records en el `domain` del módulo que los publica (`EventosDeNotas`,
`EventosDeAsistencia`, …), y el módulo de notificaciones los escucha. Así
`notas` no conoce a `notificaciones`, y cambiar el texto de una alerta no obliga
a tocar el módulo de calificaciones.

Los eventos declaran **lo que ocurrió**, no lo que hay que hacer: no llevan
destinatarios ni textos. Eso vive en `AlertasAcademicasListener`.

**Tres decisiones que conviene no revertir sin entenderlas:**

1. **`@TransactionalEventListener(AFTER_COMMIT)`.** Un correo no se puede
   deshacer: si la nota que provocó la alerta acaba en rollback, el aviso ya
   habría salido. Avisar de algo que no ocurrió es peor que avisar tarde.
2. **`notificar()` es `REQUIRES_NEW`.** Es la trampa clásica: en un listener
   `AFTER_COMMIT` la transacción ya confirmó, y un `@Transactional` normal
   participaría de ella sin llegar a persistir nunca. Además hace cada aviso
   independiente.
3. **Los listeners capturan sus propias excepciones.** Que no se pueda calificar
   porque el servidor de correo está caído sería un acoplamiento absurdo.

**`DestinatariosService`** traduce estudiante/docente/curso a cuentas de
usuario. Existe porque una notificación se dirige a un `usuario_id` pero los
hechos hablan de personas; sin él cada listener repetiría el cruce y alguno se
olvidaría del acudiente. Los perfiles **sin cuenta** se descartan en silencio:
el vínculo de V9 es opcional a propósito.

### Alertas implementadas

Por evento: bajo rendimiento (RB-13), asistencia bajo el mínimo (RF-30), cierre
de corte (RF-55 a docentes + RF-56 boletín disponible a estudiantes y
acudientes), retiro de estudiante al acudiente (HU-07) y desactivación de cuenta
a la persona afectada (HU-02, que quedó pendiente de la Fase 4).

Por calendario (`AlertasProgramadas`, `@Scheduled` diario a las 7:00): tareas
próximas a vencer (RF-42) y fecha límite de cierre del periodo (RF-55). Van por
planificador porque **nadie "hace" que una fecha se acerque**; el disparador es
el calendario. Configurables con `educktrack.alertas.*`.

**Dos detalles de diseño con intención:**

- RF-30 avisa solo de quien **acaba de cruzar** el mínimo hacia abajo, no de
  quien ya estaba por debajo. Una alerta diaria repetida se convierte en ruido y
  deja de leerse. Para detectar el cruce basta medir a los estudiantes con
  ausencia registrada: presente o tarde solo pueden subir el porcentaje.
- RF-56 se dispara al **cerrar el corte**, no al generar el boletín. HU-21 pide
  avisar "cuando el boletín está disponible", y lo está justo al cerrar (RB-19).
  Dispararlo en la generación notificaría en cada consulta.

### Lo que queda pendiente

**El planificador no está preparado para varias instancias.** Con escalado
horizontal (RNF-13) correría en todas y los avisos saldrían duplicados. Haría
falta un cerrojo compartido; hoy el despliegue es de una sola instancia. **Es la
deuda más importante de esta fase.**

**El envío de correo sigue siendo best-effort** y depende de que
`spring.mail.*` esté configurado; en Railway no lo está, así que hoy solo
funciona la notificación interna. RF-52 permite elegir canal, y el valor por
defecto es `INTERNO`.

---

## 6-quater. Fase 6 — Reglas académicas (cerrada)

Repaso de las reglas RB contra `EDUCKTRACK_REQUIREMENTS.md`. El resultado del
repaso es tan parte de la fase como los cambios: **RB-03, RB-06, RB-10, RB-14,
RB-15, RB-16, RB-18, RB-19 y RB-20 ya estaban bien implementadas** y no se
tocaron. Lo que sigue es lo que no lo estaba.

### RB-12 — el boletín no miraba el plan de estudios

El defecto de fondo de la fase. RB-12 dice "promedio igual o superior a 3.0 en
**todas las materias del plan**", pero el boletín se armaba agrupando las
calificaciones que existían (`groupingBy` sobre las notas del periodo). Una
materia del plan **sin ninguna nota registrada** simplemente no aparecía, y por
tanto no podía impedir la aprobación: el estudiante salía aprobado por las
materias que alguien alcanzó a calificarle.

Ahora el boletín se arma sobre `plan_estudios` del curso, y cada materia lleva
`sinCalificar` para distinguir "sacó 0.0" de "no tiene notas". Las notas de
materias que **ya no figuran en el plan** se conservan a propósito: el plan puede
cambiar a mitad de periodo y ocultarlas haría desaparecer del boletín
calificaciones que el estudiante sí tiene.

Esto es además lo que le da contenido a **RB-11**. La inscripción del estudiante
en las materias del plan es **derivada, no una tabla aparte**: estar matriculado
en el curso ya significa cursar su plan, y duplicarlo en filas propias abriría la
posibilidad de que las dos versiones discrepen. `MatriculaService` solo informa
cuántas son; quien consume la regla es el boletín.

### RB-04 — se informa, no se bloquea (decisión tomada)

La regla dice que quien no alcanza el 80% de asistencia "pierde el derecho a
evaluación ordinaria". El sistema solo alertaba (RF-30) y nunca aplicaba la
consecuencia. **Decisión: el boletín marca la pérdida (`pierdeDerechoAEvaluacion`)
pero no impide registrar la nota.**

El motivo es que el dato es reversible: una justificación que llega tarde
(RF-27) recalcula el porcentaje hacia arriba, y bloquear el registro convertiría
un dato reversible en una puerta cerrada que alguien tendría que reabrir a mano.
Además el proyecto no modela "evaluación extraordinaria", así que un bloqueo
dejaría al estudiante sin ninguna vía.

`pierdeDerechoAEvaluacion` es información **separada de `aprobada`** a propósito:
RB-04 y RB-12 responden preguntas distintas, y una nota aprobatoria obtenida sin
el mínimo de asistencia sigue siendo aprobatoria. Mezclarlas haría imposible
distinguir a quien perdió la materia de quien perdió la asistencia.

La decisión vive en `AsistenciaService.conservaDerechoAEvaluacion(...)`, que no
comprueba acceso: sigue el patrón de `calcularPromedio` (ver sección 3) y solo
debe llamarse desde un método que ya autorizó al solicitante.

### RB-17 — el cupo no era atómico

Mismo patrón TOCTOU que la Fase 3 cerró para RB-01 y RB-05: se contaban las
matrículas activas y después se insertaba, de modo que dos matrículas simultáneas
leían el mismo recuento y ambas creían tener el último cupo.

Aquí **no sirve un índice único**: "no más de N filas" no es una regla de
unicidad y no se puede expresar con una columna generada. Se resuelve
serializando sobre la fila del curso —`findByIdParaMatricular`, un `SELECT … FOR
UPDATE` vía `@Lock(PESSIMISTIC_WRITE)`— que mantiene válido el recuento entre la
comprobación y el `INSERT`.

### RB-09 — no existía (decisión tomada)

`consultarCarga` (RF-15) informaba del total de horas, pero nada validaba ningún
máximo: RF-14 asignaba materias sin límite. Ahora se comprueba **al asignar**, que
es el único momento en que la carga crece; informar después sería informar de un
exceso ya consumado.

El máximo es `educktrack.academico.max-horas-docente` (por defecto 30). Es un
parámetro institucional y su sitio definitivo es el panel de configuración
(RF-59, Fase 9); como propiedad aplica la regla hoy, y moverlo a base de datos
más adelante no obliga a tocar la comprobación. El límite es **inclusivo**: 30
horas es carga admisible, 31 no.

### RB-07 y RB-03 — dos huecos menores

- **RB-07 admitía el mismo tipo de evaluación repetido.** `EXAMEN 50 + EXAMEN 50`
  pasaba la suma pero dejaba dos filas del mismo tipo, y el promedio ponderado
  contaría los exámenes dos veces: la materia se quedaba sin el 100% real que
  exige la regla. Importa que se rechace **antes** de tocar nada, porque
  `configurar` borra la ponderación previa: aceptar una entrada mal formada no
  dejaba la materia con la configuración vieja, sino sin ninguna.
- **RB-03 estaba duplicada.** `TareaService.calificar` repetía los límites
  1.0–5.0 en lugar de usar el dominio `Calificacion`, de modo que cambiar la
  escala habría dejado ese punto de entrada con la escala vieja.

### `app_metadata` eliminada (V12)

Decidido: se borra. Contenía solo las dos filas literales que insertaba la propia
V1 (`schema_version` = `baseline`, `proyecto` = `EduckTrack`); no estaba mapeada,
nadie la leía y nadie la escribía. La versión real del esquema la lleva
`flyway_schema_history`, que es la única fuente de verdad (RS-02).

### Lo que se dejó fuera deliberadamente

- **`calificacion.valor` sigue siendo `DOUBLE`. Decidido: entra en la Fase 8.**
  Es un refactor transversal (entidad, dominio, DTOs, pruebas y migración) que no
  aporta ninguna regla académica nueva. **Esta decisión ya no hay que volver a
  tomarla.**
- **RB-18 detecta cruces por identidad de bloque, no por solapamiento de horas.**
  Dos bloques distintos que se solapan en el tiempo (08:00–09:00 y 08:30–09:30 el
  mismo día y jornada) no se detectan como cruce. Hoy los bloques son ranuras
  institucionales fijas, así que en la práctica no ocurre; si alguna vez se
  permite crear bloques libremente, la comprobación debe pasar a comparar rangos.
- **RF-59 (escala de calificación y % de asistencia configurables por el Rector)
  sigue sin implementarse.** Son constantes en el código
  (`Calificacion.NOTA_*`, `AsistenciaService.PORCENTAJE_MINIMO`). Es Fase 9, con
  el resto del panel de configuración.
- **El boletín hace N+1 consultas**: por cada materia del plan, promedio,
  ponderaciones y asistencia. Con un plan de ~10 materias son ~30 consultas por
  boletín. Material de Fase 8.
- **V12 no se ha aplicado contra MySQL real.** Es un `DROP TABLE IF EXISTS`, pero
  las pruebas siguen usando H2 con Flyway desactivado, así que nadie ejecuta las
  migraciones en CI (ver sección 4).

---

## 6-quinquies. Fase 7 — Recuperación de contraseña (cerrada)

**El hallazgo de partida:** `POST /api/auth/recuperar-password` existía, pero
recibía un cuerpo, **lo ignoraba por completo** y devolvía un mensaje fijo. Su
Javadoc decía que el envío real llegaría "en la Fase 8". Ninguno de los cinco
criterios de HU-04 estaba implementado.

### Cómo funciona ahora

Migración **V13**, tabla `token_recuperacion`. Dos endpoints públicos:
`/api/auth/recuperar-password` (solicitar) y `/api/auth/restablecer-password`
(consumir).

**Dos propiedades sostienen todo lo demás:**

1. **Solicitar no revela nada.** La respuesta es idéntica exista o no la cuenta,
   esté activa o no, y salga o no el correo — incluso si el envío falla, la
   excepción no se propaga. Un endpoint público que responde distinto según el
   caso es un oráculo para averiguar qué correos están dados de alta; es el mismo
   motivo por el que el acceso fallido no distingue "no existe" de "contraseña
   errónea" (Fase 4).
2. **El token solo existe en claro dentro del correo.** En base de datos vive su
   hash, y ni el log de auditoría ni los eventos lo llevan. Un enlace vivo
   permite tomar la cuenta sin conocer la contraseña: es una credencial, y
   guardarla en claro sería repartirla.

**El hash del token es SHA-256, no BCrypt**, a diferencia de la contraseña.
BCrypt es lento a propósito para que no se pueda adivinar un secreto de baja
entropía elegido por una persona; aquí el token son 256 bits aleatorios y no hay
diccionario que probar. Además BCrypt lleva sal propia, lo que obligaría a
recorrer la tabla fila por fila en vez de buscar por índice.

**Caducidad y uso único son dos límites distintos**, y HU-04 pide los dos: 30
minutos acotan cuánto vive la credencial en el buzón; el consumo al primer uso
impide que quien lea el correo más tarde la reutilice. `fecha_uso` NULL es lo que
expresa "sin usar"; la fila no se borra al consumirla, porque un token gastado
que reaparece es justo lo que interesa poder auditar. Además, **pedir un enlace
nuevo anula los anteriores**: si no, un correo antiguo reenviado seguiría
abriendo la cuenta.

**El enlace no pasa por el módulo de notificaciones.** `notificar()` siempre deja
copia en la bandeja interna y solo envía correo si el canal configurado lo
incluye (y el valor por defecto es `INTERNO`). Ninguna de las dos cosas sirve: la
bandeja se lee *entrando al sistema*, que es exactamente lo que esta persona no
puede hacer, y un enlace vivo guardado ahí es una credencial en claro. Por eso
`EnvioEnlaceRecuperacion` envía por correo directo. El aviso de "tu contraseña
fue cambiada" sí va por el pipeline normal: no lleva ningún secreto.

**La política de contraseña pasó a ser un concepto de dominio**
(`PoliticaPassword`). Vivía solo como un `@Size(min = 8)` en el DTO de registro,
y una regla declarada en un único punto de entrada no es una política: la
recuperación habría nacido sin ella. Ahora la aplican el alta y el
restablecimiento.

**Orden deliberado en `restablecer`:** la política se valida **antes** de
consumir el enlace. Al revés, escribir una contraseña demasiado corta gastaría el
enlace y obligaría a pedir otro.

**Auditoría:** `RECUPERACION_SOLICITADA`, `RECUPERACION_FALLIDA` y
`PASSWORD_RESTABLECIDA`. Los intentos fallidos usan
`AuditoriaService.registrarPeseARollback` (`REQUIRES_NEW`, mismo motivo que
`registrarAcceso` en la Fase 4): el fallo se señala lanzando excepción, y una
anotación que participe de esa transacción desaparecería justo en el caso que más
interesa auditar.

### Lo que queda pendiente

- **Sin `spring.mail.*` configurado, la recuperación no funciona.** En Railway no
  lo está (nota de la Fase 5), así que hoy el enlace no llega a nadie. **Es el
  bloqueante para dar RF-64 por operativo en el entorno desplegado**, y no se
  arregla con código: hace falta un servidor SMTP.
- **No hay pantalla de frontend.** `educktrack.seguridad.recuperacion.url-base`
  apunta a `/restablecer-password`, que todavía no existe: es Fase 10.
- **No hay límite de peticiones.** Alguien puede pedir enlaces en bucle para un
  correo conocido y llenarle el buzón. El log lo deja registrado, pero no lo
  impide; un `rate limit` es material de la Fase 8.
- **Los tokens caducados no se borran nunca.** La tabla crece de forma monótona.
  Con el volumen de un colegio no es un problema a corto plazo, pero conviene una
  tarea de limpieza (`@Scheduled`, como `AlertasProgramadas`).
- **Nota sobre `BootstrapAdministrador`:** ahora la contraseña del administrador
  inicial también pasa por la política. Si `EDUCKTRACK_BOOTSTRAP_ADMIN_PASSWORD`
  tuviera menos de 8 caracteres, la cuenta no se crearía; el arranque **no** se
  rompe porque el bootstrap ya capturaba el fallo y lo registra.

---

## 6-sexies. Fase 8 — Rendimiento y exactitud (cerrada)

Cuatro asuntos acumulados de fases anteriores.

### `calificacion.valor` pasa a `BigDecimal` / `DECIMAL(3,2)` (V14)

La decisión que se venía aplazando desde la Fase 3. `DOUBLE` es binario y no
representa exactamente valores como 2.9 o 3.05. Con notas eso no es teórico:
**RB-12 decide la aprobación comparando contra 3.0**, y una nota almacenada como
2.9999999999999996 cae del lado equivocado de una comparación que decide si
alguien pierde el año. El error además se acumula al promediar ponderando.

- V14 convierte `calificacion.valor`, `novedad_nota.valor_anterior` / `_nuevo` y
  `entrega_tarea.calificacion` a `DECIMAL(3,2)`.
- El dominio `Calificacion` **normaliza a dos decimales** y compara con
  `compareTo`, no con `equals`: 3.0 y 3.00 son el mismo número pero `equals` los
  considera distintos, y ese es el error clásico al adoptar `BigDecimal`.
- En el promedio ponderado, **los aportes se acumulan sin redondear y solo se
  redondea el total**. Redondear cada sumando desplaza el resultado.
- **El porcentaje de asistencia (RB-04) se queda en `double` a propósito**: es un
  porcentaje calculado, no un valor de la escala, y ahí el redondeo binario no
  decide ninguna regla. Convertirlo habría sido churn sin beneficio.

### El boletín ya no hace N+1 consultas

Lo introdujo la propia Fase 6. Por cada materia del plan se pedían las notas, las
ponderaciones y la asistencia por separado: con un plan de diez asignaturas, una
treintena de consultas por boletín. Ahora son **tres**, y el cálculo se separó de
la carga de datos (`promedioDe` no consulta nada), que es lo que permite
resolver todas las materias con lo ya traído. `AsistenciaService` expone
`materiasSinDerechoAEvaluacion`, que resuelve RB-04 del periodo entero de una vez.

### `ContextoUsuario` memoriza la identidad por petición

Una sola llamada a `puedeVerEstudiante` leía la cuenta **cuatro veces**
(`tieneVisionInstitucional`, `usuarioIdActual` y `cursosDelDocente` la recargaban
cada una), y los servicios encadenan varias comprobaciones por petición. Dentro
de una petición la respuesta no puede cambiar, así que memorizarla es exacto, no
una aproximación.

**Es un `ThreadLocal` y no un bean `@RequestScope`** porque `ContextoUsuario`
también se usa desde listeners de eventos, donde un bean de ámbito petición
falla. `LimpiezaIdentidadFilter` lo vacía en un `finally`, el primero de la
cadena.

> **Lo importante de esta pieza no es el ahorro, es la limpieza.** El servidor
> reutiliza hilos: una identidad que sobrevive al final de la petición se la
> encuentra la siguiente que use ese hilo. Por eso la memoria va **siempre
> marcada con el correo** y se descarta si no coincide, y por eso
> `IdentidadDeLaPeticionTest` prueba explícitamente ese caso. Si algún día se
> toca el filtro, esa prueba es la red.

Las pruebas de `ContextoUsuarioTest` lo detectaron al instante: todas usan el
mismo correo, así que la identidad memorizada por una prueba se la encontraba la
siguiente. Se limpia en el `@AfterEach`, igual que hace el filtro.

### Límite de peticiones en la recuperación de contraseña

Pendiente de la Fase 7. Sin él, cualquiera podía pedir enlaces en bucle para un
correo conocido y llenarle el buzón. Se limita por cuenta y ventana
(`max-solicitudes: 3`, `ventana-minutos: 15`) **contando las filas que ya existen
en `token_recuperacion`**, sin llevar estado aparte — lo que además hace que el
límite funcione con varias instancias.

Se corta **en silencio**: responder "demasiadas peticiones" confirmaría que ese
correo tiene cuenta, que es justo lo que el endpoint evita.

### Una trampa que costó tiempo

**`mvn compile` puede pasar sin recompilar.** Tras cambiar los tipos, `mvn
compile` dio BUILD SUCCESS con el código a medio convertir; `mvn clean compile`
sacó 26 errores. Es la misma trampa del `target/` ya compilado que la sección 1
describe para el JDK. **En un refactor de tipos, usar siempre `mvn clean
compile`** o directamente `mvn test`, que sí recompila.

### Lo que se dejó fuera

- **La limpieza de tokens de recuperación caducados sigue pendiente** (tarea
  `@Scheduled`, como `AlertasProgramadas`). La tabla crece de forma monótona.
- **`ContextoUsuario` sigue haciendo varias consultas distintas por
  comprobación** (perfiles, vínculos, matrículas). Lo memorizado es la identidad,
  no el alcance completo; `cursosDelDocente()` todavía consulta cada vez que se
  le llama.

---

## 6-septies. Fase 9 — Configuración institucional y limpieza (cerrada)

### RF-59 — los parámetros dejan de estar en el código

**RF-59 estaba sin implementar.** La escala de calificación (RB-03) eran
constantes de `Calificacion`, el porcentaje mínimo de asistencia (RB-04) una
constante de `AsistenciaService`, y el máximo de horas por docente (RB-09) una
propiedad del despliegue que introdujo la Fase 6. El requisito pide que **los
defina el Rector desde el sistema**, y cambiarlos exigía recompilar o reiniciar.

Migración **V15**, tabla `parametro_institucional`, y
`GET`/`PUT /api/configuracion/parametros`.

- **Tabla clave/valor, no una columna por parámetro.** El conjunto crece con el
  tiempo, y una tabla de una fila con N columnas obliga a una migración por cada
  parámetro nuevo. El valor va como texto y lo interpreta el servicio, porque los
  tipos son distintos y una columna por tipo sería peor que convertir en un sitio.
- **Las claves son un enum cerrado** (`ParametroInstitucional`), igual que
  `TipoOperacion`: si cualquiera pudiera inventar claves, la tabla acumularía
  filas que nadie lee. Cada valor declara además su tipo y rango admisible.
- **V15 siembra los valores que hoy están en el código**, de modo que desplegarla
  no cambia el comportamiento de nada.

**La escala pasó a ser un dato que se le entrega al dominio.** `Calificacion` ya
no tiene los límites cableados: recibe una `EscalaCalificacion`, cuyo invariante
se valida **al construirla**. Una escala imposible (aprobatoria por encima del
máximo) haría que ninguna nota aprobase nunca, y ese fallo aparecería mucho
después, calificando, en vez de al guardarla.

Por eso mismo **la escala se valida como conjunto y no parámetro a parámetro**:
subir la nota aprobatoria a 9.00 es válido mirando solo ese campo y deja un
colegio donde nadie puede aprobar. `actualizar` construye la escala resultante
antes de guardar.

**Hay caché, y es deliberado.** La escala se consulta al registrar *cada* nota y
el mínimo de asistencia al calcular *cada* porcentaje; sin memoria, esta fase
desharía parte de lo que consiguió la Fase 8. Los parámetros cambian unas pocas
veces al año, así que se leen una vez y se refrescan al actualizarlos. **La caché
es por instancia**: con escalado horizontal (RNF-13) un cambio tardaría en verse
en las demás, igual que el planificador de la Fase 5.

**Si falta una fila, se usa el valor del enunciado en vez de fallar.** Un sistema
que no arranca porque falta una fila de configuración es peor que uno que arranca
con el valor por defecto. Lo mismo si la escala guardada resulta inválida: se
registra el error y se sigue con la del enunciado, porque quedarse sin poder
calificar es peor que calificar con la escala anterior.

**Permisos:** RF-59 nombra al Rector; el Administrador entra porque sostiene el
sistema. **Coordinación no**: cambiar la escala altera la aprobación de todo el
colegio y no es una decisión de gestión diaria. Leerlos sí es abierto, porque el
frontend necesita la escala para mostrarla.

### Limpieza de enlaces de recuperación caducados

Pendiente de la Fase 7: la tabla solo crecía. `LimpiezaTokensRecuperacion` los
borra de madrugada, **pero no de inmediato**: se conservan unos días
(`dias-retencion: 7`) porque un token gastado que reaparece es justo lo que
interesa poder investigar; borrándolo al usarlo, un intento de reutilización no
se distinguiría de un token inventado.

A diferencia del planificador de alertas, aquí correr en varias instancias es
inofensivo: borrar dos veces lo mismo no duplica nada.

### Código muerto retirado

- `Calificacion.getEscala()` no lo usaba nadie: fuera, por la regla de "nada por
  si acaso".
- `Calificacion.esBajoRendimiento()` expresaba RB-13 en el dominio pero solo lo
  usaba su propia prueba, mientras `CalificacionService` repetía la comparación.
  Ahora el servicio usa el dominio, que es donde vive la regla.
- La propiedad `educktrack.academico.max-horas-docente` desaparece de
  `application.yml`, sustituida por el parámetro institucional.

### Lo que queda pendiente

- **No hay pantalla de configuración**: el endpoint existe, la interfaz es Fase 10.
- **La caché de parámetros es por instancia** (ver arriba).
- **Cambiar la escala no recalcula lo ya calificado.** Las notas guardadas siguen
  con su valor; lo que cambia es la comparación de aprobación a partir de
  entonces. Es lo razonable —una nota de 4.0 sigue siendo 4.0—, pero conviene
  saberlo antes de cambiar la escala a mitad de año.

---

## 6-octies. Fase 10 — Preparación frontend (cerrada)

Última fase del plan. El frontend era login más una pantalla de inicio; ahora
cubre las tres funcionalidades que el backend había ganado en las fases 6 a 9 y
que **no tenían por dónde usarse**.

### Recuperación de contraseña (RF-64) — el hueco más grave

`educktrack.seguridad.recuperacion.url-base` apuntaba a `/restablecer-password`,
**una ruta que no existía**. El correo que envía la Fase 7 llevaba a ninguna
parte, y desde el login no había ningún enlace para pedirlo. Dos pantallas
públicas nuevas, más el enlace "Olvidé mi contraseña" en el login.

- **`/recuperar-password` muestra el mismo mensaje exista o no la cuenta.** El
  backend responde igual en ambos casos a propósito; si la pantalla distinguiera
  ("ese correo no está registrado"), el cliente desharía la propiedad que el
  servidor se cuida de mantener.
- **La confirmación de contraseña se comprueba en el cliente**, y esto sí tiene
  motivo: es un error de tecleo, no una regla de negocio, y enviarlo gastaría el
  enlace —que es de un solo uso— por haber escrito mal dos veces.
- **Entrar sin token se detecta antes de mostrar el formulario.** Pasa al abrir
  la ruta a mano o al copiar mal el enlace; sin eso, el formulario se enviaría
  solo para fallar después.

### Boletín (RF-35) y parámetros institucionales (RF-59)

El boletín es la primera pantalla que **muestra el resultado de las reglas de las
fases anteriores**: las materias del plan sin ninguna nota aparecen como "Sin
calificar" e impiden aprobar (RB-11 y RB-12, Fase 6), y la pérdida del derecho a
evaluación es una etiqueta **aparte** del estado de la materia, porque RB-04
informa y no reprueba (decisión de la Fase 6).

El identificador del estudiante sale de `/identidad/yo`, que lo resuelve el
servidor, y no de un campo que el usuario pueda escribir: si la cuenta es de un
estudiante se prefija el suyo, si es un acudiente se ofrecen sus tutelados, y
solo el personal con visión institucional teclea el número.

La pantalla de parámetros deja el borrador en el valor vigente cuando el servidor
rechaza un cambio, para no dejar en pantalla un número que parezca guardado.

### Lo que se comprobó y lo que no

- **El build pasa** (`npm run build`, 100 módulos) y las formas de las respuestas
  se contrastaron una a una contra los controladores.
- **`try_files $uri $uri/ /index.html` ya estaba** en la plantilla de nginx, así
  que el enlace del correo —que es un *deep link* a `/restablecer-password`—
  resuelve en producción. Era el riesgo más fácil de pasar por alto.
- **No se ejercitó la interfaz contra un backend real.** Requiere MySQL, y en
  esta máquina no hay ni MySQL ni Docker (el mismo bloqueo que mantiene
  Testcontainers pendiente desde la Fase 2).
- **El frontend sigue sin ninguna prueba automatizada.** No se añadió framework
  de pruebas porque significaba introducir dependencias nuevas al cierre del
  plan; es la deuda más clara que queda.

### Antes de desplegar

**`EDUCKTRACK_RECUPERACION_URL` hay que fijarla en Railway.** Su valor por
defecto es `http://localhost:5173/restablecer-password`, que en producción
enviaría a la gente a su propia máquina. Debe ser:

```
https://educktrack-frontend-production.up.railway.app/restablecer-password
```

Y sigue en pie que **sin `spring.mail.*` configurado el correo no sale**, de modo
que las pantallas funcionan pero nadie recibe el enlace.

---

## 7. Fases restantes

**No queda ninguna fase del plan.** Lo que sigue abierto, por orden de
importancia:

| Pendiente | Dónde está descrito |
|---|---|
| **Sin SMTP, RF-64 no funciona**: las pantallas están, pero el correo no sale | 6-quinquies |
| **`EDUCKTRACK_RECUPERACION_URL` sin fijar en Railway** (apunta a localhost) | 6-octies |
| **V12 a V15 sin aplicar al MySQL real**; nadie ejecuta las migraciones en CI | sección 4 |
| **Testcontainers**: bloqueado desde la Fase 2 por no haber Docker en la máquina | sección 4 |
| **El frontend no tiene ninguna prueba automatizada** | 6-octies |
| **El planificador no soporta varias instancias** (alertas duplicadas) | 6-ter |
| **La caché de parámetros es por instancia** | 6-septies |
| **RB-18 detecta cruces por identidad de bloque, no por solapamiento horario** | 6-quater |
| **HU-03 (matriz de permisos por rol) no existe**: los permisos son anotaciones | 6-bis |


### Decisiones ya tomadas (no volver a discutirlas)

- **No se implementa multi-institución.** RNF-14 es prioridad Baja y está
  marcado como "futura".
- **"Simulacros" no existe en este proyecto**: venía de otro enunciado.
- El alcance del docente es el de sus cursos, **no** institucional (Fase 2).
- **`calificacion.valor` pasa a `BigDecimal` en la Fase 8**, no antes (Fase 6).
- **RB-04 informa, no bloquea** el registro de notas (Fase 6).
- **`app_metadata` se elimina** (V12, Fase 6).

---

## 8. Estructura del repositorio

```
backend/          Spring Boot 3.3.5, Java 21, arquitectura hexagonal por módulo
  src/main/java/com/educktrack/
    <modulo>/domain           reglas de negocio puras
    <modulo>/application      casos de uso (aquí vive el control de acceso)
    <modulo>/infrastructure   persistence (JPA) y rest (controladores)
  src/main/resources/db/migration   Flyway V1..V15
frontend/         React 18 + Vite + Tailwind, servido por nginx
  src/pages       login, inicio, boletin, parametros y recuperacion de contrasena
  src/components  Layout (cabecera y navegacion) y RutaProtegida
docker-compose.yml   mysql + backend + frontend
EDUCKTRACK_REQUIREMENTS.md   fuente de verdad (NO TOCAR)
```

Los módulos se comunican por identificadores (`curso_id`, `materia_id`), no por
relaciones JPA entre agregados: es deliberado, para mantenerlos desacoplados. La
integridad referencial se declara en las migraciones.
