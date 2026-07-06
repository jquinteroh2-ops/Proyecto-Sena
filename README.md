# EduckTrack

Plataforma web de **gestión académica** para instituciones educativas colombianas (Universidad de Cartagena — Análisis y Diseño de Software).

Toda entidad, endpoint y validación es trazable a un identificador de [`EDUCKTRACK_REQUIREMENTS.md`](./EDUCKTRACK_REQUIREMENTS.md) (RN, RB, RS, RD, RF, RNF, HU), que es la **fuente única de verdad** del proyecto.

---

## Stack tecnológico

| Capa | Tecnología |
| --- | --- |
| Backend | Java 21, Spring Boot 3.3, Spring Data JPA, Spring Security + JWT, Flyway, Maven |
| Base de datos | MySQL 8 (RS-02) |
| Frontend | React 18, Vite, TailwindCSS, React Router, Axios, Zustand |
| Documentación API | springdoc-openapi / Swagger UI (RS-09) |
| Infraestructura | Docker + Docker Compose, Nginx (RS-10, RS-01) |

## Arquitectura

Backend organizado por **arquitectura hexagonal (Ports & Adapters) + DDD** (RNF-17), un módulo de negocio por paquete:

```
com.educktrack.<modulo>
 ├── domain/          -> entidades de dominio puras, value objects, reglas de negocio (RB)
 ├── application/      -> casos de uso / servicios (orquestan el dominio, aplican las RB)
 └── infrastructure/
      ├── persistence/ -> entidades JPA, repositorios, mappers domain<->entity
      ├── rest/         -> controladores REST + DTOs
      └── security/     -> solo en el módulo de seguridad: filtros JWT, UserDetailsService
```

`com.educktrack.shared` agrupa configuración transversal (OpenAPI, seguridad base, web).

> **Decisión de diseño:** un solo módulo Maven organizado por paquetes (no multi-módulo Maven). Simplifica el build y la dockerización sin perder la separación hexagonal, cuyos límites se imponen por paquete. Las reglas de negocio (RB-01..RB-20) se validan en la capa `application`, nunca solo en el frontend.

## Estructura del repositorio

```
EduckTrack/
├── EDUCKTRACK_REQUIREMENTS.md   # fuente única de verdad (requisitos)
├── docker-compose.yml           # mysql + backend + frontend
├── .env.example                 # variables de entorno para compose
├── backend/                     # API REST Spring Boot
└── frontend/                    # SPA React + Vite
```

---

## Puesta en marcha

### Requisitos previos
- Java 21, Maven 3.9+
- Node 20+
- Docker + Docker Compose

### Opción A — desarrollo local (backend + BD en Docker)

```bash
# 1. Levantar solo MySQL
docker compose up -d mysql

# 2. Backend (perfil dev por defecto)
cd backend
mvn spring-boot:run

# 3. Frontend
cd frontend
npm install
npm run dev
```

- Backend (perfil dev): http://localhost:8081 — salud en `/api/health`
- Swagger UI: http://localhost:8081/swagger-ui.html (RS-09)
- Frontend: http://localhost:5173

> El perfil `dev` usa el puerto **8081** (el 8080 suele estar ocupado en Windows por otras apps, p. ej. NVIDIA Broadcast). En Docker el backend expone 8080.

### Opción B — stack completo con Docker

```bash
cp .env.example .env
docker compose up --build
```

- Aplicación: http://localhost (Nginx sirve el frontend y proxya `/api` al backend)

### Autenticación (Fase 2)

Al primer arranque se crea un **Administrador inicial** (idempotente). Credenciales por defecto (configurables por `EDUCKTRACK_ADMIN_CORREO` / `EDUCKTRACK_ADMIN_PASSWORD`):

- Correo: `admin@educktrack.edu.co`
- Contraseña: `Admin123*`

```bash
# Login (RF-60) -> devuelve un JWT
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"correo":"admin@educktrack.edu.co","password":"Admin123*"}'

# Usar el token en endpoints protegidos (RBAC, RS-03)
curl http://localhost:8081/api/usuarios -H "Authorization: Bearer <TOKEN>"
```

> Rutas públicas: `/api/auth/login`, `/api/auth/recuperar-password`, `/api/health`, Swagger. Todo lo demás exige JWT válido; las acciones sobre usuarios exigen rol `ADMINISTRADOR`.

---

## Estado del proyecto — construcción por fases

| Fase | Contenido | Estado |
| --- | --- | --- |
| 0 | Setup: estructura, docker-compose, perfiles, README | ✅ |
| 1 | Dominio y persistencia base (Usuario, Rol, Estudiante, Docente, Curso, Materia, PeriodoAcademico) | ✅ |
| 2 | Seguridad: registro/login, JWT, BCrypt, RBAC (RF-01, RF-03, RF-60..RF-64) | ✅ |
| 3 | Módulo académico: estudiantes, docentes, materias, cursos, plan, asignación, matrícula (RF-06..RF-19, RF-43..RF-46, RF-57; RB-01, RB-02, RB-11, RB-16, RB-17, RB-20) | ✅ |
| 4 | Horarios: bloques y validación de cruces (RF-21..RF-25, RB-18, HU-11) | ✅ |
| 5 | Asistencia (RF-26..RF-30, RB-04, RB-06) | ⏳ |
| 6 | Calificaciones y boletín (RF-31..RF-37, RB-03, RB-07, RB-15, RB-19) | ⏳ |
| 7 | Tareas (RF-38..RF-42, RB-10) | ⏳ |
| 8 | Reportes y notificaciones (RF-47..RF-56) | ⏳ |
| 9 | Configuración institucional y auditoría (RF-57..RF-59, RF-63, RS-07) | ⏳ |
| 10 | Documentación de API (Swagger) | ⏳ |
| 11 | Frontend por rol (HU-01..HU-30) | ⏳ |
| 12 | Dockerización final y despliegue | ⏳ |

## Convenciones

- **Conventional Commits** (`feat:`, `fix:`, `chore:`, `docs:`).
- Nombres de dominio en español (`Estudiante`, `Calificacion`, `Matricula`); términos técnicos internos en inglés (`repository`, `mapper`, `dto`).
- Cada endpoint documenta el RF que implementa; cada validación de negocio, la RB.
- Pruebas unitarias para las reglas críticas: RB-03, RB-04, RB-07, RB-15, RB-18 (RNF-19).
