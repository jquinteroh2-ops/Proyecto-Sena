-- ==========================================================================
-- V2 - Entidades nucleo de EduckTrack (Fase 1)
-- Usuario/Rol (RF-01, RD-07), Estudiante (RF-06, RD-10), Docente (RF-12, RD-09),
-- Materia (RF-17), PeriodoAcademico (RF-57, RD-02), Curso (RF-43).
-- Flyway es la unica fuente de verdad del esquema (RS-02); las entidades JPA
-- se validan contra estas tablas (ddl-auto: validate).
-- Tipos alineados con Hibernate 6 / MySQL 8: boolean -> BIT(1),
-- LocalDateTime -> DATETIME(6), LocalDate -> DATE.
-- ==========================================================================

-- ---------- Catalogo de roles (RD-07, RS-03) ----------
CREATE TABLE rol (
    nombre      VARCHAR(40)  NOT NULL PRIMARY KEY,
    descripcion VARCHAR(150)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------- Usuario (RF-01) ----------
CREATE TABLE usuario (
    id                     BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    nombre                 VARCHAR(120) NOT NULL,
    correo_institucional   VARCHAR(150) NOT NULL,
    password_hash          VARCHAR(100) NOT NULL,
    activo                 BIT(1)       NOT NULL DEFAULT b'1',
    debe_cambiar_password  BIT(1)       NOT NULL DEFAULT b'1',
    fecha_creacion         DATETIME(6)  NOT NULL,
    CONSTRAINT uq_usuario_correo UNIQUE (correo_institucional)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------- Relacion Usuario <-> Rol (M:N, RS-03) ----------
CREATE TABLE usuario_rol (
    usuario_id  BIGINT      NOT NULL,
    rol_nombre  VARCHAR(40) NOT NULL,
    PRIMARY KEY (usuario_id, rol_nombre),
    CONSTRAINT fk_usuariorol_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE,
    CONSTRAINT fk_usuariorol_rol     FOREIGN KEY (rol_nombre) REFERENCES rol (nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------- Estudiante (RF-06, RD-10) ----------
CREATE TABLE estudiante (
    id                    BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    documento             VARCHAR(30)  NOT NULL,
    nombres               VARCHAR(100) NOT NULL,
    apellidos             VARCHAR(100) NOT NULL,
    fecha_nacimiento      DATE,
    direccion             VARCHAR(200),
    estado                VARCHAR(20)  NOT NULL,
    acudiente_nombre      VARCHAR(120),
    acudiente_telefono    VARCHAR(30),
    acudiente_parentesco  VARCHAR(40),
    CONSTRAINT uq_estudiante_documento UNIQUE (documento)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------- Docente (RF-12, RD-09) ----------
CREATE TABLE docente (
    id         BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    documento  VARCHAR(30)  NOT NULL,
    nombres    VARCHAR(100) NOT NULL,
    apellidos  VARCHAR(100) NOT NULL,
    correo     VARCHAR(150),
    telefono   VARCHAR(30),
    CONSTRAINT uq_docente_documento UNIQUE (documento)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Areas de formacion del docente (RD-09) - tabla de coleccion
CREATE TABLE docente_area (
    docente_id  BIGINT      NOT NULL,
    area        VARCHAR(80) NOT NULL,
    PRIMARY KEY (docente_id, area),
    CONSTRAINT fk_docentearea_docente FOREIGN KEY (docente_id) REFERENCES docente (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------- Materia (RF-17) ----------
CREATE TABLE materia (
    id                          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    codigo                      VARCHAR(30)  NOT NULL,
    nombre                      VARCHAR(120) NOT NULL,
    area                        VARCHAR(80)  NOT NULL,
    intensidad_horaria_semanal  INT          NOT NULL,
    CONSTRAINT uq_materia_codigo UNIQUE (codigo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------- Periodo academico (RF-57, RD-02) ----------
CREATE TABLE periodo_academico (
    id            BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    nombre        VARCHAR(80) NOT NULL,
    anio_lectivo  INT         NOT NULL,
    fecha_inicio  DATE,
    fecha_fin     DATE,
    activo        BIT(1)      NOT NULL DEFAULT b'0',
    cerrado       BIT(1)      NOT NULL DEFAULT b'0'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------- Curso (RF-43) ----------
CREATE TABLE curso (
    id                    BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    nombre                VARCHAR(80) NOT NULL,
    grado                 INT         NOT NULL,
    nivel                 VARCHAR(25) NOT NULL,
    jornada               VARCHAR(20) NOT NULL,
    cupo_maximo           INT         NOT NULL,
    periodo_academico_id  BIGINT      NOT NULL,
    director_grupo_id     BIGINT,
    CONSTRAINT fk_curso_periodo  FOREIGN KEY (periodo_academico_id) REFERENCES periodo_academico (id),
    CONSTRAINT fk_curso_director FOREIGN KEY (director_grupo_id)    REFERENCES docente (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==========================================================================
-- Precarga del catalogo de roles institucionales (RD-07)
-- ==========================================================================
INSERT INTO rol (nombre, descripcion) VALUES
    ('ADMINISTRADOR',           'Administra usuarios, roles y parametros del sistema'),
    ('RECTOR',                  'Supervisa indicadores institucionales y reportes'),
    ('COORDINADOR_ACADEMICO',   'Gestiona estudiantes, matriculas, horarios y cortes'),
    ('COORDINADOR_CONVIVENCIA', 'Gestiona convivencia escolar'),
    ('DOCENTE',                 'Registra asistencia, notas y tareas de sus materias'),
    ('ESTUDIANTE',              'Consulta sus notas, asistencia y entrega tareas'),
    ('PADRE_FAMILIA',           'Consulta el seguimiento academico de sus hijos');
