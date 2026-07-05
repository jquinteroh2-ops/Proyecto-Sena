# EduckTrack — Requerimientos de Referencia (fuente única de verdad)

> Extraído del documento académico "Análisis y Diseño de Software — EduckTrack" (Universidad de Cartagena).
> Úsalo como referencia normativa: toda entidad, endpoint y validación que implementes debe poder trazarse a un ID de esta lista (RN, RB, RS, RD, RF, RNF, HU).

## Actores del sistema
Administrador, Rector, Coordinador Académico, Docente, Estudiante, Padre de Familia, Sistema (procesos automáticos).

## 1. Requerimientos de Negocio (RN)
| ID | Requerimiento | Descripción | Prioridad |
| --- | --- | --- | --- |
| RN-01 | Gestión Integral de Estudiantes | Administrar el ciclo completo de información académica del estudiante: matrícula, datos personales, historial, promoción y retiro. | Alta |
| RN-02 | Gestión Integral de Docentes | Administrar la vinculación, asignación de carga académica y desempeño de los docentes. | Alta |
| RN-03 | Administración Académica Institucional | Gestionar materias, cursos, grupos, periodos académicos y horarios de la institución. | Alta |
| RN-04 | Control de Asistencia | Registrar y controlar la asistencia diaria de los estudiantes por curso y jornada. | Alta |
| RN-05 | Gestión de Calificaciones y Evaluaciones | Registrar, calcular y consolidar las calificaciones de los estudiantes conforme al sistema de evaluación institucional. | Alta |
| RN-06 | Gestión de Tareas y Seguimiento Académico | Permitir la asignación, entrega y seguimiento de tareas y actividades académicas. | Media |
| RN-07 | Comunicación entre Actores Educativos | Facilitar la comunicación formal entre docentes, estudiantes y padres de familia. | Media |
| RN-08 | Generación de Reportes y Estadísticas | Proveer reportes de rendimiento académico, asistencia e indicadores institucionales. | Alta |
| RN-09 | Seguridad y Control de Acceso por Roles | Garantizar que cada usuario acceda únicamente a la información y funciones correspondientes a su rol. | Alta |
| RN-10 | Panel Administrativo y Configuración Institucional | Permitir la configuración general de la institución: periodos, jornadas, sedes y parámetros del sistema. | Media |

## 2. Objetivos de Negocio (OBJ)
| ID | Objetivo | Descripción | Proviene de |
| --- | --- | --- | --- |
| OBJ-01 | Centralizar la información académica | Unificar en una sola plataforma los datos de estudiantes, docentes y procesos académicos. | RN-01, RN-02 |
| OBJ-02 | Optimizar la administración de cursos y horarios | Reducir el tiempo de planificación académica mediante asignación digital de materias y horarios. | RN-03 |
| OBJ-03 | Garantizar trazabilidad de la asistencia | Disponer de un registro histórico y verificable de la asistencia de cada estudiante. | RN-04 |
| OBJ-04 | Automatizar el cálculo de calificaciones | Reducir errores manuales en la consolidación de notas por periodo. | RN-05 |
| OBJ-05 | Fomentar el seguimiento del desempeño académico | Permitir a docentes y padres monitorear el avance de tareas y evaluaciones. | RN-06 |
| OBJ-06 | Mejorar la comunicación institucional | Disminuir la dependencia de canales informales (papel, mensajería externa) entre docentes, estudiantes y padres. | RN-07 |
| OBJ-07 | Proveer información para la toma de decisiones | Generar reportes y estadísticas que apoyen a coordinación y rectoría. | RN-08 |
| OBJ-08 | Asegurar la confidencialidad de la información | Restringir el acceso a datos sensibles de estudiantes y docentes según el rol autenticado. | RN-09 |
| OBJ-09 | Estandarizar la configuración institucional | Permitir que cada institución configure periodos, jornadas y sedes sin intervención técnica externa. | RN-10 |
| OBJ-10 | Reducir el uso de procesos en papel | Digitalizar formatos de matrícula, boletines y reportes de asistencia. | RN-01, RN-04, RN-08 |

## 3. Reglas de Negocio (RB) — validar siempre en capa de dominio/servicio, nunca solo en frontend
| ID | Regla | Descripción | Proviene de |
| --- | --- | --- | --- |
| RB-01 | Unicidad de Matrícula | Un estudiante solo puede estar matriculado en un curso activo por periodo académico. | RN-01 |
| RB-02 | Docente Titular Único por Grupo | Cada curso debe tener asignado exactamente un docente titular (director de grupo). | RN-02, RN-03 |
| RB-03 | Escala de Calificación Institucional | Las calificaciones se registran en escala numérica de 1.0 a 5.0, siendo 3.0 la nota mínima aprobatoria. | RN-05 |
| RB-04 | Porcentaje Mínimo de Asistencia | Un estudiante que no alcance el 80% de asistencia en una materia pierde el derecho a evaluación ordinaria. | RN-04 |
| RB-05 | Periodo Académico Activo | Solo puede existir un periodo académico activo por año lectivo en un momento dado. | RN-03 |
| RB-06 | Registro de Asistencia por Jornada | La asistencia debe registrarse una vez por bloque de clase y no puede modificarse después de 48 horas sin autorización de coordinación. | RN-04 |
| RB-07 | Ponderación de Evaluaciones | Cada materia debe tener definidos los porcentajes de ponderación de sus evaluaciones, y estos deben sumar 100%. | RN-05 |
| RB-08 | Vínculo Padre-Estudiante Verificado | Un padre de familia solo puede visualizar la información de los estudiantes formalmente vinculados a su cuenta. | RN-07, RN-09 |
| RB-09 | Carga Académica Máxima Docente | Un docente no puede exceder el número máximo de horas semanales definido por la institución. | RN-02 |
| RB-10 | Entrega de Tareas dentro de Plazo | Una tarea solo puede entregarse dentro de la fecha límite, salvo autorización expresa del docente. | RN-06 |
| RB-11 | Un Estudiante, Múltiples Materias | Un estudiante matriculado en un curso queda automáticamente inscrito en todas las materias del plan de estudios de ese curso. | RN-01, RN-03 |
| RB-12 | Aprobación de Periodo | Un estudiante aprueba el año lectivo si obtiene promedio igual o superior a 3.0 en todas las materias del plan. | RN-05 |
| RB-13 | Notificación de Bajo Rendimiento | El sistema debe generar notificación automática cuando un estudiante obtenga nota inferior a 3.0 en un corte parcial. | RN-06, RN-08 |
| RB-14 | Roles Excluyentes por Cuenta | Un usuario del sistema no puede tener simultáneamente el rol de Estudiante y el rol de Docente. | RN-09 |
| RB-15 | Histórico Inmutable de Notas | Una calificación publicada y cerrada en un corte no puede eliminarse, solo corregirse mediante registro de novedad auditada. | RN-05, RN-09 |
| RB-16 | Asignación de Materia a Docente por Área | Un docente solo puede ser asignado a materias correspondientes a su área de formación registrada. | RN-02, RN-03 |
| RB-17 | Capacidad Máxima de Curso | Un curso no puede matricular estudiantes por encima del cupo máximo definido por la institución. | RN-01, RN-03 |
| RB-18 | Horario sin Cruces | El sistema no debe permitir asignar a un docente dos clases simultáneas en el mismo bloque horario. | RN-03 |
| RB-19 | Reporte de Boletín por Periodo | El boletín de calificaciones solo puede generarse una vez el periodo académico ha sido cerrado por coordinación. | RN-05, RN-08 |
| RB-20 | Retiro de Estudiante con Justificación | Todo retiro de un estudiante debe registrar motivo, fecha y autorización de un coordinador o rector. | RN-01 |

## 4. Requerimientos del Sistema (RS)
| ID | Requerimiento | Descripción | Proviene de |
| --- | --- | --- | --- |
| RS-01 | Arquitectura Cliente-Servidor Web Segura | El sistema debe operar bajo arquitectura cliente-servidor con comunicación cifrada HTTPS (TLS 1.2+). | RN-09 |
| RS-02 | Base de Datos Relacional con Integridad Referencial | La base de datos debe ser relacional (MySQL) garantizando integridad referencial entre entidades académicas. | RN-01, RN-03 |
| RS-03 | Control de Acceso Basado en Roles (RBAC) | El sistema debe restringir funcionalidades y datos visibles según el rol autenticado (Administrador, Rector, Coordinador, Docente, Estudiante, Padre). | RN-09 |
| RS-04 | Autenticación Segura con JWT | El sistema debe emitir tokens JWT firmados con expiración configurable para gestionar sesiones sin estado. | RN-09 |
| RS-05 | Cifrado de Contraseñas | Las contraseñas deben almacenarse cifradas mediante BCrypt con un mínimo de 10 rondas. | RN-09 |
| RS-06 | Generación de Reportes Exportables | El sistema debe permitir exportar boletines, reportes de asistencia y estadísticas en formato PDF y Excel. | RN-08 |
| RS-07 | Log de Auditoría | Toda operación crítica (cambios de nota, retiros, cambios de rol) debe quedar registrada con usuario, fecha y descripción. | RN-09 |
| RS-08 | Módulo de Notificaciones | El sistema debe generar notificaciones internas y por correo ante eventos académicos relevantes. | RN-06, RN-07 |
| RS-09 | Documentación de API con OpenAPI/Swagger | La API REST debe estar documentada mediante especificación OpenAPI accesible vía Swagger UI. | RN-10 |
| RS-10 | Contenerización con Docker | El backend, frontend y base de datos deben desplegarse en contenedores Docker independientes. | RN-10 |
| RS-11 | Copias de Seguridad Automáticas | El sistema debe ejecutar copias de seguridad diarias de la base de datos con retención mínima de 30 días. | RN-09 |
| RS-12 | Acceso Concurrente Multiusuario | El sistema debe soportar el acceso simultáneo de múltiples usuarios sin degradación perceptible. | RN-01, RN-03 |
| RS-13 | API REST bajo Arquitectura MVC | El backend debe exponer sus funcionalidades mediante una API REST estructurada bajo el patrón MVC. | RN-10 |
| RS-14 | Panel de Configuración Institucional | El sistema debe permitir configurar periodos académicos, jornadas y sedes desde un panel administrativo. | RN-10 |
| RS-15 | Escalabilidad Horizontal del Backend | La arquitectura debe permitir agregar instancias adicionales del backend sin cambios en el código fuente. | RN-08, RN-10 |

## 5. Requerimientos de Dominio (RD)
| ID | Requerimiento de Dominio | Descripción | Proviene de |
| --- | --- | --- | --- |
| RD-01 | Escala de Calificación Colombiana | Las instituciones educativas colombianas utilizan una escala numérica de 1.0 a 5.0 según el Decreto 1290 del MEN. | RS-02 |
| RD-02 | Estructura de Periodo Académico | El año lectivo se divide en periodos académicos (típicamente 4 cortes), cada uno con fecha de inicio y cierre. | RS-02, RS-14 |
| RD-03 | Niveles y Grados Educativos | La educación básica y media colombiana se organiza en niveles (Preescolar, Básica Primaria, Básica Secundaria, Media) y grados numerados. | RS-02 |
| RD-04 | Jornadas Escolares | Las instituciones operan bajo jornadas (Mañana, Tarde, Única, Nocturna, Fin de Semana), cada una con horarios propios. | RS-02, RS-14 |
| RD-05 | Plan de Estudios por Curso | Cada curso tiene asociado un plan de estudios con las materias obligatorias correspondientes a su grado. | RS-02 |
| RD-06 | Tipos de Evaluación | Las evaluaciones se clasifican en: Quiz, Examen, Taller, Proyecto y Autoevaluación, cada una con ponderación propia. | RS-02 |
| RD-07 | Roles Institucionales | La estructura organizacional educativa contempla los roles: Rector, Coordinador Académico, Coordinador de Convivencia, Docente y Docente Director de Grupo. | RS-03 |
| RD-08 | Vínculo Familiar | La relación entre un padre de familia y un estudiante puede ser de parentesco (padre, madre, acudiente autorizado). | RS-02 |
| RD-09 | Áreas de Formación Docente | Cada docente pertenece a una o más áreas de conocimiento (Matemáticas, Ciencias, Humanidades, etc.) que determinan las materias que puede dictar. | RS-02 |
| RD-10 | Estado de Matrícula | Un estudiante puede encontrarse en los estados: Activo, Retirado, Graduado o Trasladado. | RS-02 |
| RD-11 | Boletín Académico | El boletín es el documento oficial que consolida las calificaciones de un estudiante en un periodo determinado. | RS-06 |
| RD-12 | Inasistencia Justificada vs. Injustificada | La normativa educativa distingue entre inasistencias justificadas (con soporte médico o autorización) e injustificadas, con efectos distintos sobre el reporte final. | RS-02 |

## 6. Requisitos Funcionales (RF) por módulo
### Módulo: Gestión de Usuarios
| ID | Nombre | Descripción | Actor | Proviene de |
| --- | --- | --- | --- | --- |
| RF-01 | Registrar Usuario | Crear una cuenta de usuario con nombre, correo institucional, contraseña y rol asignado. | Administrador | RS-03, RS-05 |
| RF-02 | Editar Usuario | Modificar los datos de un usuario existente, exceptuando su correo institucional. | Administrador | RS-03 |
| RF-03 | Desactivar Usuario | Inhabilitar el acceso de un usuario sin eliminar su historial de datos. | Administrador | RS-03, RS-07 |
| RF-04 | Asignar Rol a Usuario | Asignar uno o más roles a una cuenta de usuario según su función institucional. | Administrador | RS-03, RB-14 |
| RF-05 | Consultar Historial de Accesos | Visualizar el historial de inicios de sesión de un usuario. | Administrador | RS-07 |

### Módulo: Gestión de Estudiantes
| ID | Nombre | Descripción | Actor | Proviene de |
| --- | --- | --- | --- | --- |
| RF-06 | Registrar Estudiante | Registrar un nuevo estudiante con documento de identidad, nombre completo, fecha de nacimiento, dirección y datos de acudiente. | Coordinador | RS-02, RB-01 |
| RF-07 | Editar Datos del Estudiante | Actualizar los datos personales de un estudiante registrado. | Coordinador | RS-02 |
| RF-08 | Consultar Perfil del Estudiante | Consultar el perfil académico completo de un estudiante: curso, materias, calificaciones y asistencia. | Docente | RS-02 |
| RF-09 | Matricular Estudiante en Curso | Asignar un estudiante a un curso y periodo académico específico, inscribiéndolo automáticamente en el plan de estudios. | Coordinador | RS-02, RB-01, RB-11 |
| RF-10 | Retirar Estudiante | Registrar el retiro de un estudiante con motivo, fecha y autorización de coordinación o rectoría. | Coordinador | RS-02, RB-20 |
| RF-11 | Vincular Padre a Estudiante | Asociar la cuenta de un padre de familia al perfil de uno o más estudiantes bajo su responsabilidad. | Coordinador | RS-02, RB-08, RD-08 |

### Módulo: Gestión de Docentes
| ID | Nombre | Descripción | Actor | Proviene de |
| --- | --- | --- | --- | --- |
| RF-12 | Registrar Docente | Registrar un nuevo docente con documento, nombre, área de formación y datos de contacto. | Administrador | RS-02, RD-09 |
| RF-13 | Editar Datos del Docente | Actualizar los datos personales y profesionales de un docente. | Administrador | RS-02 |
| RF-14 | Asignar Materia a Docente | Asignar una materia y curso específico a un docente según su área de formación. | Coordinador | RS-02, RB-16 |
| RF-15 | Consultar Carga Académica | Consultar el total de horas y materias asignadas a un docente en el periodo actual. | Coordinador | RS-02, RB-09 |
| RF-16 | Designar Director de Grupo | Asignar a un docente como director de grupo titular de un curso. | Coordinador | RS-02, RB-02 |

### Módulo: Gestión de Materias
| ID | Nombre | Descripción | Actor | Proviene de |
| --- | --- | --- | --- | --- |
| RF-17 | Registrar Materia | Registrar una nueva materia con nombre, código, área e intensidad horaria semanal. | Coordinador | RS-02 |
| RF-18 | Editar Materia | Modificar los atributos de una materia existente. | Coordinador | RS-02 |
| RF-19 | Asociar Materia a Plan de Estudios | Vincular una materia a un plan de estudios de un grado específico. | Coordinador | RS-02, RD-05 |
| RF-20 | Configurar Ponderación de Evaluaciones | Definir los porcentajes de ponderación de los tipos de evaluación de una materia. | Docente | RS-02, RB-07, RD-06 |

### Módulo: Gestión de Horarios
| ID | Nombre | Descripción | Actor | Proviene de |
| --- | --- | --- | --- | --- |
| RF-21 | Registrar Bloque Horario | Crear un bloque horario con día, hora de inicio, hora de fin y jornada. | Coordinador | RS-02, RD-04 |
| RF-22 | Asignar Materia a Bloque Horario | Asignar una materia, docente y curso a un bloque horario disponible. | Coordinador | RS-02, RB-18 |
| RF-23 | Validar Cruce de Horario | Verificar automáticamente que no exista un cruce de horario para el docente o el curso al asignar un bloque. | Sistema | RB-18 |
| RF-24 | Consultar Horario por Curso | Visualizar el horario semanal completo de un curso. | Docente | RS-02 |
| RF-25 | Consultar Horario por Docente | Visualizar el horario semanal completo asignado a un docente. | Docente | RS-02 |

### Módulo: Gestión de Asistencia
| ID | Nombre | Descripción | Actor | Proviene de |
| --- | --- | --- | --- | --- |
| RF-26 | Registrar Asistencia Diaria | Registrar la asistencia de todos los estudiantes de un curso en un bloque horario determinado. | Docente | RS-02, RB-06 |
| RF-27 | Justificar Inasistencia | Registrar el soporte y justificación de una inasistencia de un estudiante. | Coordinador | RS-02, RD-12 |
| RF-28 | Consultar Reporte de Asistencia | Consultar el porcentaje de asistencia de un estudiante por materia y periodo. | Docente | RS-02, RB-04 |
| RF-29 | Editar Registro de Asistencia | Modificar un registro de asistencia dentro de las 48 horas siguientes a su creación. | Docente | RS-02, RB-06 |
| RF-30 | Alertar Bajo Porcentaje de Asistencia | Generar alerta automática cuando un estudiante esté por debajo del porcentaje mínimo de asistencia permitido. | Sistema | RS-08, RB-04 |

### Módulo: Gestión de Notas
| ID | Nombre | Descripción | Actor | Proviene de |
| --- | --- | --- | --- | --- |
| RF-31 | Registrar Calificación | Registrar la calificación de un estudiante en una evaluación específica de una materia. | Docente | RS-02, RB-03 |
| RF-32 | Editar Calificación | Modificar una calificación previamente registrada, siempre que el corte no esté cerrado. | Docente | RS-02, RB-15 |
| RF-33 | Calcular Promedio por Materia | Calcular automáticamente el promedio ponderado de un estudiante en una materia según las evaluaciones registradas. | Sistema | RB-03, RB-07 |
| RF-34 | Cerrar Corte Académico | Cerrar formalmente un corte o periodo académico, bloqueando modificaciones posteriores de notas. | Coordinador | RS-02, RB-19 |
| RF-35 | Generar Boletín de Calificaciones | Generar el boletín consolidado de un estudiante para un periodo académico cerrado. | Coordinador | RS-06, RB-19, RD-11 |
| RF-36 | Registrar Novedad de Nota | Registrar una corrección auditada sobre una calificación de un corte ya cerrado. | Coordinador | RS-07, RB-15 |
| RF-37 | Consultar Histórico de Calificaciones | Consultar el historial completo de calificaciones de un estudiante en todos los periodos cursados. | Docente | RS-02 |

### Módulo: Gestión de Tareas
| ID | Nombre | Descripción | Actor | Proviene de |
| --- | --- | --- | --- | --- |
| RF-38 | Asignar Tarea | Crear una tarea o actividad académica con descripción, materia, curso y fecha límite de entrega. | Docente | RS-02, RB-10 |
| RF-39 | Entregar Tarea | Permitir al estudiante subir la evidencia de entrega de una tarea asignada. | Estudiante | RS-02, RB-10 |
| RF-40 | Calificar Tarea | Registrar la calificación y retroalimentación de una tarea entregada por un estudiante. | Docente | RS-02 |
| RF-41 | Consultar Estado de Tareas | Consultar el estado (pendiente, entregada, calificada) de las tareas asignadas a un estudiante. | Padre de Familia | RS-02 |
| RF-42 | Notificar Tarea Próxima a Vencer | Generar notificación automática a estudiantes y padres cuando una tarea esté próxima a vencer. | Sistema | RS-08 |

### Módulo: Gestión de Cursos
| ID | Nombre | Descripción | Actor | Proviene de |
| --- | --- | --- | --- | --- |
| RF-43 | Registrar Curso | Registrar un nuevo curso con nombre, grado, jornada y cupo máximo. | Coordinador | RS-02, RB-17 |
| RF-44 | Editar Curso | Modificar los atributos de un curso existente. | Coordinador | RS-02 |
| RF-45 | Consultar Listado de Estudiantes por Curso | Listar todos los estudiantes matriculados en un curso con su estado. | Docente | RS-02 |
| RF-46 | Consultar Cupo Disponible | Consultar el número de cupos disponibles en un curso antes de matricular un estudiante. | Coordinador | RS-02, RB-17 |

### Módulo: Gestión de Reportes
| ID | Nombre | Descripción | Actor | Proviene de |
| --- | --- | --- | --- | --- |
| RF-47 | Generar Reporte de Rendimiento Académico | Generar un reporte consolidado del rendimiento académico de un curso o estudiante. | Coordinador | RS-06 |
| RF-48 | Generar Reporte de Asistencia Institucional | Generar un reporte estadístico de asistencia por curso, materia o institución. | Rector | RS-06 |
| RF-49 | Exportar Reporte en PDF | Exportar cualquier reporte generado en formato PDF. | Coordinador | RS-06 |
| RF-50 | Exportar Reporte en Excel | Exportar cualquier reporte generado en formato Excel. | Coordinador | RS-06 |
| RF-51 | Consultar Panel de Indicadores Institucionales | Visualizar un panel con indicadores clave: promedio general, asistencia promedio y estudiantes en riesgo académico. | Rector | RS-06, RS-15 |

### Módulo: Gestión de Notificaciones
| ID | Nombre | Descripción | Actor | Proviene de |
| --- | --- | --- | --- | --- |
| RF-52 | Configurar Canal de Notificación | Configurar si las notificaciones se envían por correo, notificación interna o ambos. | Administrador | RS-08 |
| RF-53 | Enviar Notificación Manual | Permitir a un docente o coordinador enviar una notificación manual a estudiantes o padres. | Docente | RS-08 |
| RF-54 | Consultar Bandeja de Notificaciones | Consultar el historial de notificaciones recibidas por un usuario. | Estudiante | RS-08 |
| RF-55 | Notificar Cierre de Periodo | Notificar automáticamente a docentes la fecha límite para el cierre de un corte académico. | Sistema | RS-08, RB-19 |
| RF-56 | Notificar Publicación de Boletín | Notificar a estudiantes y padres cuando un boletín de calificaciones ha sido generado. | Sistema | RS-08, RF-35 |

### Módulo: Configuración
| ID | Nombre | Descripción | Actor | Proviene de |
| --- | --- | --- | --- | --- |
| RF-57 | Configurar Periodo Académico | Crear y activar un periodo académico institucional con fechas de inicio y cierre. | Administrador | RS-02, RS-14, RB-05 |
| RF-58 | Configurar Jornadas y Sedes | Registrar las jornadas y sedes de la institución educativa. | Administrador | RS-14, RD-04 |
| RF-59 | Configurar Parámetros de Evaluación Institucional | Definir la escala de calificación y el porcentaje mínimo de asistencia institucional. | Rector | RS-14, RB-03, RB-04 |

### Módulo: Seguridad
| ID | Nombre | Descripción | Actor | Proviene de |
| --- | --- | --- | --- | --- |
| RF-60 | Autenticar Usuario | Autenticar usuarios mediante credenciales y emitir un token JWT válido. | Todos | RS-04, RS-05 |
| RF-61 | Cerrar Sesión | Invalidar el token de sesión activo del usuario. | Todos | RS-04 |
| RF-62 | Gestionar Permisos por Rol | Configurar la matriz de permisos (ver, crear, editar, eliminar) por rol y módulo. | Administrador | RS-03 |
| RF-63 | Registrar Auditoría de Operación Crítica | Registrar automáticamente toda operación crítica del sistema con usuario, fecha y descripción. | Sistema | RS-07 |
| RF-64 | Recuperar Contraseña | Permitir a un usuario restablecer su contraseña mediante verificación por correo institucional. | Todos | RS-04, RS-05 |

## 7. Requisitos No Funcionales (RNF)
### Rendimiento
| ID | Nombre | Descripción | Prioridad |
| --- | --- | --- | --- |
| RNF-01 | Tiempo de Respuesta | El sistema debe responder en menos de 3 segundos para el 95% de las consultas bajo carga normal. | Alta |
| RNF-02 | Concurrencia | Debe soportar al menos 300 usuarios concurrentes sin degradación perceptible. | Alta |
| RNF-03 | Tiempo de Generación de Reportes | Los reportes en PDF o Excel deben generarse en menos de 8 segundos para hasta 5.000 registros. | Media |

### Seguridad
| ID | Nombre | Descripción | Prioridad |
| --- | --- | --- | --- |
| RNF-04 | Cifrado en Tránsito | Toda comunicación cliente-servidor debe estar cifrada mediante TLS 1.2 o superior. | Alta |
| RNF-05 | Cifrado de Contraseñas | Las contraseñas deben almacenarse con hash BCrypt de mínimo 10 rondas. | Alta |
| RNF-06 | Expiración de Sesión | Los tokens JWT deben expirar tras 8 horas de emisión o 20 minutos de inactividad. | Media |
| RNF-07 | Control de Acceso por Rol | Cada usuario solo puede acceder a los módulos y acciones permitidos por su rol. | Alta |

### Usabilidad
| ID | Nombre | Descripción | Prioridad |
| --- | --- | --- | --- |
| RNF-08 | Interfaz Intuitiva | Un usuario nuevo debe poder completar tareas básicas en menos de 15 minutos sin capacitación técnica. | Media |
| RNF-09 | Compatibilidad de Navegadores | El sistema debe funcionar en Chrome, Firefox y Edge en sus últimas dos versiones. | Media |
| RNF-10 | Mensajes de Error Claros | Los errores de validación deben mostrarse en español, indicando causa y acción correctiva. | Media |

### Disponibilidad
| ID | Nombre | Descripción | Prioridad |
| --- | --- | --- | --- |
| RNF-11 | Disponibilidad Mínima | El sistema debe garantizar 99% de disponibilidad en horario escolar (6:00 a 18:00, lunes a viernes). | Alta |
| RNF-12 | Copias de Seguridad | La base de datos debe respaldarse automáticamente cada 24 horas, con retención mínima de 30 días. | Alta |

### Escalabilidad
| ID | Nombre | Descripción | Prioridad |
| --- | --- | --- | --- |
| RNF-13 | Escalabilidad Horizontal | La arquitectura debe permitir agregar instancias de backend sin modificar el código fuente. | Media |
| RNF-14 | Soporte Multi-institucional | El diseño de datos debe permitir la futura operación de múltiples instituciones sobre la misma plataforma. | Baja |

### Portabilidad
| ID | Nombre | Descripción | Prioridad |
| --- | --- | --- | --- |
| RNF-15 | Despliegue Multiplataforma | El sistema debe poder desplegarse en cualquier entorno Linux compatible con Docker. | Baja |
| RNF-16 | Independencia de Proveedor de Base de Datos | El acceso a datos debe realizarse mediante ORM que facilite una futura migración de motor de base de datos. | Baja |

### Mantenibilidad
| ID | Nombre | Descripción | Prioridad |
| --- | --- | --- | --- |
| RNF-17 | Modularidad del Código | El backend debe organizarse siguiendo principios de arquitectura hexagonal y SOLID. | Media |
| RNF-18 | Documentación Técnica | El sistema debe contar con documentación de API (Swagger), modelo de datos y manual de usuario. | Media |
| RNF-19 | Pruebas Automatizadas | Los módulos críticos (notas, asistencia, autenticación) deben contar con pruebas unitarias automatizadas. | Media |

### Accesibilidad
| ID | Nombre | Descripción | Prioridad |
| --- | --- | --- | --- |
| RNF-20 | Contraste y Legibilidad | La interfaz debe cumplir un contraste mínimo de color conforme a las pautas WCAG 2.1 nivel AA. | Media |
| RNF-21 | Compatibilidad con Zoom del Navegador | La interfaz debe mantenerse funcional con niveles de zoom de hasta 150%. | Baja |
| RNF-22 | Etiquetado Semántico | Los formularios deben incluir etiquetas semánticas compatibles con lectores de pantalla. | Baja |
| RNF-23 | Diseño Responsivo | La interfaz debe adaptarse correctamente a dispositivos móviles, tabletas y escritorio. | Media |
| RNF-24 | Textos Alternativos en Imágenes | Todo elemento gráfico informativo debe incluir texto alternativo descriptivo. | Baja |
| RNF-25 | Navegación por Teclado | Los formularios y menús principales deben ser completamente operables mediante teclado. | Baja |

## 8. Historias de Usuario (HU) por épica
### Épica 1: Gestión de Usuarios y Roles

**HU-01** — Como administrador del sistema, quiero registrar cuentas de usuario asignando el rol correspondiente, para garantizar que cada persona acceda solo a las funciones de su cargo.
Criterios de aceptación:
- El sistema valida que el correo institucional no esté duplicado.
- Se debe asignar al menos un rol al crear la cuenta.
- La contraseña inicial se genera de forma segura y debe cambiarse en el primer ingreso.
- El sistema no permite asignar simultáneamente los roles Estudiante y Docente a una misma cuenta.
- Se muestra confirmación del registro exitoso con el rol asignado.

**HU-02** — Como administrador del sistema, quiero desactivar la cuenta de un usuario sin eliminar su historial, para preservar la trazabilidad de la información académica asociada.
Criterios de aceptación:
- La cuenta desactivada no puede iniciar sesión.
- El historial académico del usuario permanece intacto y consultable.
- Se registra la fecha y el responsable de la desactivación.
- El sistema notifica al usuario afectado por correo institucional.
- La acción queda registrada en el log de auditoría.

**HU-03** — Como administrador del sistema, quiero configurar la matriz de permisos por rol y módulo, para controlar de forma granular qué puede hacer cada tipo de usuario.
Criterios de aceptación:
- Se pueden definir permisos de ver, crear, editar y eliminar por módulo.
- Los cambios se aplican inmediatamente a todos los usuarios con ese rol.
- El sistema impide eliminar el rol Administrador por completo del sistema.
- Se muestra una vista previa de los permisos antes de guardar.
- Los cambios quedan registrados en el log de auditoría.

**HU-04** — Como usuario del sistema, quiero recuperar mi contraseña mediante mi correo institucional, para volver a acceder al sistema en caso de olvido.
Criterios de aceptación:
- El sistema envía un enlace de recuperación válido por 30 minutos.
- El enlace expira automáticamente al usarse una vez.
- La nueva contraseña debe cumplir la política mínima de seguridad.
- Se notifica al usuario que su contraseña fue cambiada exitosamente.
- Los intentos de recuperación fallidos quedan registrados.

### Épica 2: Gestión Académica (Estudiantes, Docentes, Materias y Cursos)

**HU-05** — Como coordinador académico, quiero registrar un nuevo estudiante con sus datos personales y de acudiente, para contar con su perfil completo antes de matricularlo.
Criterios de aceptación:
- El sistema valida que el documento de identidad no esté duplicado.
- Se exige al menos un dato de contacto de acudiente.
- El estudiante queda en estado 'Activo' tras el registro.
- El sistema muestra confirmación con el código de estudiante asignado.
- Los campos obligatorios vacíos generan un mensaje de error específico.

**HU-06** — Como coordinador académico, quiero matricular un estudiante en un curso y periodo académico, para mantener actualizada la relación entre estudiantes y cursos.
Criterios de aceptación:
- El sistema verifica que el curso tenga cupo disponible.
- El estudiante queda automáticamente inscrito en todas las materias del plan de estudios del curso.
- No se permite matricular a un estudiante en dos cursos activos del mismo periodo.
- Se registra la fecha de matrícula.
- El estudiante aparece de inmediato en el listado del curso.

**HU-07** — Como coordinador académico, quiero registrar el retiro de un estudiante con su motivo y autorización, para mantener actualizado el estado académico institucional.
Criterios de aceptación:
- El retiro exige indicar motivo, fecha y responsable de autorización.
- El estudiante cambia su estado a 'Retirado' sin eliminar su historial.
- El estudiante retirado deja de aparecer en los listados activos del curso.
- Se notifica el retiro al padre de familia vinculado.
- La acción queda registrada en el log de auditoría.

**HU-08** — Como administrador del sistema, quiero registrar un nuevo docente con su área de formación, para poder asignarle materias acordes a su especialidad.
Criterios de aceptación:
- El sistema valida que el documento de identidad no esté duplicado.
- Se debe seleccionar al menos un área de formación.
- El docente queda disponible para asignación de carga académica.
- El sistema muestra confirmación con el código docente asignado.
- Los campos obligatorios vacíos generan mensaje de error específico.

**HU-09** — Como coordinador académico, quiero asignar una materia y curso a un docente según su área de formación, para garantizar la idoneidad de la asignación académica.
Criterios de aceptación:
- El sistema solo permite asignar materias correspondientes al área del docente.
- El sistema valida que la carga horaria no exceda el máximo permitido.
- El sistema verifica que no exista cruce de horario para el docente.
- La asignación queda visible en el horario del docente y del curso.
- Se puede reasignar la materia a otro docente en cualquier momento.

**HU-10** — Como docente, quiero configurar la ponderación de las evaluaciones de mi materia, para que el cálculo del promedio refleje el criterio de evaluación definido.
Criterios de aceptación:
- El sistema exige que los porcentajes de todos los tipos de evaluación sumen 100%.
- Los cambios de ponderación solo aplican al periodo académico en curso.
- El sistema muestra una advertencia si la suma no es 100%.
- La configuración queda visible para el coordinador académico.
- Los cambios quedan registrados en el log de auditoría.

### Épica 3: Horarios

**HU-11** — Como coordinador académico, quiero registrar los bloques horarios de la institución, para estructurar la jornada académica de cada curso.
Criterios de aceptación:
- Cada bloque debe tener día, hora de inicio y hora de fin definidos.
- El sistema valida que la hora de fin sea posterior a la hora de inicio.
- Los bloques quedan asociados a una jornada específica.
- El sistema impide crear bloques duplicados para el mismo día y hora.
- Los bloques creados quedan disponibles para asignación de materias.

**HU-12** — Como coordinador académico, quiero asignar una materia, docente y curso a un bloque horario, para construir el horario semanal completo de la institución.
Criterios de aceptación:
- El sistema valida que no exista cruce de horario para el docente.
- El sistema valida que no exista cruce de horario para el curso.
- La asignación queda reflejada inmediatamente en ambos horarios (curso y docente).
- El sistema rechaza la asignación si detecta un conflicto, mostrando el detalle del cruce.
- Se puede modificar o eliminar una asignación existente.

**HU-13** — Como docente, quiero consultar mi horario semanal completo, para organizar mi disponibilidad y desplazamiento entre cursos.
Criterios de aceptación:
- El horario muestra materia, curso, día, hora y jornada.
- El horario puede visualizarse por semana o por día.
- El horario puede exportarse en PDF.
- Los cambios de horario se reflejan de inmediato en la consulta.
- Se resalta si existe algún conflicto pendiente de resolver.

### Épica 4: Asistencia

**HU-14** — Como docente, quiero registrar la asistencia diaria de mi curso, para llevar un control exacto de la presencialidad de los estudiantes.
Criterios de aceptación:
- El sistema muestra el listado completo de estudiantes matriculados en el curso.
- Se puede marcar cada estudiante como presente, ausente o tarde.
- El registro queda asociado a la materia, fecha y bloque horario.
- El sistema impide registrar asistencia duplicada para el mismo bloque y fecha.
- El docente puede editar el registro dentro de las 48 horas siguientes.

**HU-15** — Como coordinador académico, quiero justificar la inasistencia de un estudiante con su soporte, para que el reporte final de asistencia refleje inasistencias justificadas.
Criterios de aceptación:
- El sistema exige adjuntar o describir el motivo de la justificación.
- La inasistencia justificada no afecta el porcentaje mínimo de asistencia exigido.
- El cambio queda reflejado en el reporte de asistencia del estudiante.
- Se notifica al docente titular del cambio realizado.
- La acción queda registrada en el log de auditoría.

**HU-16** — Como docente, quiero consultar el porcentaje de asistencia de un estudiante por materia, para identificar oportunamente estudiantes en riesgo de perder el derecho a evaluación.
Criterios de aceptación:
- El sistema calcula el porcentaje de asistencia acumulado del periodo.
- Se distingue entre inasistencias justificadas e injustificadas.
- El sistema resalta a los estudiantes por debajo del umbral mínimo.
- El reporte puede filtrarse por materia y periodo.
- El reporte puede exportarse en PDF.

**HU-17** — Como padre de familia, quiero recibir una alerta cuando mi hijo esté por debajo del porcentaje mínimo de asistencia, para tomar acciones a tiempo.
Criterios de aceptación:
- La alerta se genera automáticamente al cruzar el umbral mínimo definido.
- La alerta se envía por notificación interna y correo electrónico.
- La alerta indica la materia y el porcentaje actual de asistencia.
- El padre puede consultar el detalle completo de inasistencias desde la alerta.
- El coordinador académico también recibe copia de la alerta.

### Épica 5: Calificaciones y Evaluaciones

**HU-18** — Como docente, quiero registrar la calificación de un estudiante en una evaluación, para llevar el control del desempeño académico de mi materia.
Criterios de aceptación:
- El sistema valida que la nota esté dentro de la escala 1.0 a 5.0.
- La calificación queda asociada al tipo de evaluación y su ponderación.
- El sistema recalcula automáticamente el promedio de la materia.
- No se permite registrar notas en un corte ya cerrado.
- El estudiante puede visualizar su calificación de inmediato.

**HU-19** — Como docente, quiero calcular automáticamente el promedio ponderado de un estudiante, para evitar errores manuales en la consolidación de notas.
Criterios de aceptación:
- El cálculo utiliza la ponderación configurada para cada tipo de evaluación.
- El sistema muestra el detalle del cálculo (nota por tipo y ponderación aplicada).
- El promedio se actualiza en tiempo real al registrar nuevas notas.
- El sistema advierte si existen evaluaciones pendientes de calificar.
- El resultado es consistente con el reportado en el boletín.

**HU-20** — Como coordinador académico, quiero cerrar formalmente un corte académico, para impedir modificaciones posteriores no autorizadas sobre las notas.
Criterios de aceptación:
- El sistema exige que todas las materias del curso tengan notas completas antes del cierre.
- Una vez cerrado, ningún docente puede modificar notas directamente.
- Las correcciones posteriores requieren registrar una novedad auditada.
- El sistema notifica a los docentes la fecha límite de cierre.
- El cierre queda registrado en el log de auditoría.

**HU-21** — Como coordinador académico, quiero generar el boletín de calificaciones de un estudiante, para entregar un documento oficial y consolidado del periodo.
Criterios de aceptación:
- El boletín solo puede generarse si el periodo está cerrado.
- El boletín incluye todas las materias, notas y observaciones del periodo.
- El boletín puede exportarse en PDF.
- El sistema notifica al estudiante y al padre cuando el boletín está disponible.
- El boletín refleja el promedio general del periodo.

**HU-22** — Como coordinador académico, quiero registrar una novedad de corrección sobre una nota de un corte cerrado, para corregir errores manteniendo la trazabilidad histórica.
Criterios de aceptación:
- El sistema exige justificar el motivo de la corrección.
- La nota original permanece visible en el historial junto con la corrección.
- La corrección requiere el registro de un usuario con rol autorizado.
- El sistema recalcula el boletín afectado tras la corrección.
- La acción queda registrada en el log de auditoría.

### Épica 6: Tareas

**HU-23** — Como docente, quiero asignar una tarea con fecha límite a mi curso, para dar seguimiento a las actividades académicas complementarias.
Criterios de aceptación:
- La tarea debe tener descripción, materia, curso y fecha límite.
- El sistema notifica a los estudiantes del curso al momento de la asignación.
- No se puede asignar una tarea con fecha límite anterior a la fecha actual.
- La tarea queda visible en el panel del estudiante y del padre.
- El docente puede editar la tarea mientras no existan entregas registradas.

**HU-24** — Como estudiante, quiero entregar la evidencia de una tarea asignada, para cumplir con mis responsabilidades académicas.
Criterios de aceptación:
- El sistema solo permite la entrega dentro del plazo definido, salvo autorización del docente.
- La entrega queda con marca de fecha y hora.
- El estudiante recibe confirmación de la entrega exitosa.
- El docente puede visualizar de inmediato la entrega en su panel.
- El estudiante puede ver el estado (pendiente, entregada, calificada) de la tarea.

**HU-25** — Como padre de familia, quiero consultar el estado de las tareas asignadas a mi hijo, para apoyar su seguimiento académico en casa.
Criterios de aceptación:
- Se muestra el estado de cada tarea: pendiente, entregada o calificada.
- Se pueden filtrar las tareas por materia y por fecha límite.
- El padre puede ver la calificación obtenida una vez publicada.
- El padre recibe notificación cuando una tarea está próxima a vencer.
- La información es de solo lectura para el rol Padre de Familia.

### Épica 7: Comunicación y Notificaciones

**HU-26** — Como docente, quiero enviar una notificación manual a los padres de un curso, para comunicar información relevante de forma oportuna.
Criterios de aceptación:
- El docente puede seleccionar uno, varios o todos los estudiantes del curso.
- La notificación se envía por el canal configurado (correo y/o interno).
- El sistema confirma el envío exitoso y muestra los destinatarios alcanzados.
- El historial de notificaciones enviadas queda disponible para consulta.
- El padre recibe la notificación en su bandeja dentro de la plataforma.

**HU-27** — Como estudiante, quiero consultar mi bandeja de notificaciones, para estar informado de eventos académicos relevantes.
Criterios de aceptación:
- Las notificaciones se muestran ordenadas de la más reciente a la más antigua.
- Se distingue entre notificaciones leídas y no leídas.
- El estudiante puede marcar una notificación como leída.
- Las notificaciones críticas (bajo rendimiento, tareas por vencer) se resaltan visualmente.
- El historial de notificaciones es consultable por al menos 90 días.

**HU-28** — Como administrador del sistema, quiero configurar el canal por el que se envían las notificaciones institucionales, para adaptar el sistema a la infraestructura de comunicación de la institución.
Criterios de aceptación:
- Se puede habilitar notificación interna, correo electrónico o ambos.
- La configuración aplica a nivel institucional para todos los usuarios.
- El sistema valida la configuración del servidor de correo antes de guardar.
- Los cambios de configuración quedan registrados en el log de auditoría.
- Se puede realizar un envío de prueba antes de aplicar la configuración.

### Épica 8: Reportes y Estadísticas

**HU-29** — Como rector, quiero consultar un panel de indicadores institucionales, para supervisar el desempeño académico general de la institución.
Criterios de aceptación:
- El panel muestra el promedio general institucional, por curso y por materia.
- El panel muestra el porcentaje de asistencia promedio institucional.
- Se identifican los estudiantes en riesgo académico (promedio inferior a 3.0).
- El panel puede filtrarse por periodo académico.
- El panel puede exportarse en PDF para presentación a entes de control.

**HU-30** — Como coordinador académico, quiero generar un reporte de rendimiento académico por curso, para identificar tendencias y necesidades de refuerzo académico.
Criterios de aceptación:
- El reporte muestra el promedio por materia y por estudiante del curso.
- El reporte permite comparar el rendimiento entre periodos académicos.
- El reporte puede exportarse en PDF y Excel.
- El reporte resalta a los estudiantes con promedio inferior a 3.0.
- El reporte se genera únicamente con datos de periodos cerrados.
