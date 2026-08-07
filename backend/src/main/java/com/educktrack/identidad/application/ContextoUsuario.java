package com.educktrack.identidad.application;

import com.educktrack.docentes.infrastructure.persistence.DocenteJpaEntity;
import com.educktrack.docentes.infrastructure.persistence.DocenteRepository;
import com.educktrack.estudiantes.infrastructure.persistence.EstudianteJpaEntity;
import com.educktrack.estudiantes.infrastructure.persistence.EstudianteRepository;
import com.educktrack.identidad.infrastructure.persistence.VinculoAcudienteJpaEntity;
import com.educktrack.identidad.infrastructure.persistence.VinculoAcudienteRepository;
import com.educktrack.usuarios.domain.NombreRol;
import com.educktrack.usuarios.infrastructure.persistence.RolJpaEntity;
import com.educktrack.usuarios.infrastructure.persistence.UsuarioJpaEntity;
import com.educktrack.usuarios.infrastructure.persistence.UsuarioRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Resuelve la identidad del usuario autenticado (RS-03, RS-04) y responde la
 * pregunta que hasta ahora el sistema no podia responder: <em>a que registro
 * academico corresponde quien esta llamando a la API</em>.
 *
 * <p>Es el punto unico de verdad para el control de acceso a nivel de dato
 * (ownership): RNF-07 (cada usuario accede solo a lo que su rol permite) y
 * RB-08 (un padre de familia solo ve a los estudiantes vinculados a su cuenta).
 * Los controladores nunca deben confiar en un identificador recibido por
 * parametro para recursos privados; deben resolverlo aqui.</p>
 *
 * <p>DECISION DE DISENO: la identidad se resuelve consultando la base de datos
 * a partir del correo del token, no incrustando identificadores en el JWT.
 * Incrustarlos ahorraria una consulta por peticion, pero deja tokens
 * desincronizados cuando cambia un vinculo y obliga a reemitir las sesiones
 * vivas. El costo actual es equivalente al que ya paga
 * {@code UsuarioDetailsService} en la autenticacion.</p>
 */
@Service
@Transactional(readOnly = true)
public class ContextoUsuario {

    /**
     * Roles con visibilidad academica sobre cualquier estudiante de la
     * institucion (RS-03). Los dos coordinadores, la rectoria y la
     * administracion ejercen funciones transversales; el docente accede a los
     * estudiantes en el ejercicio de su carga academica.
     *
     * <p>DECISION DE DISENO: en esta fase el docente tiene visibilidad
     * institucional. Restringirlo a los estudiantes de los cursos que tiene
     * asignados (tabla {@code asignacion_docente}) es una decision de la Fase 2,
     * porque cambia el alcance de RF-08, RF-28 y RF-45 y debe validarse contra
     * los requisitos antes de aplicarse.</p>
     */
    private static final Set<NombreRol> ROLES_PERSONAL_ACADEMICO = EnumSet.of(
            NombreRol.ADMINISTRADOR,
            NombreRol.RECTOR,
            NombreRol.COORDINADOR_ACADEMICO,
            NombreRol.COORDINADOR_CONVIVENCIA,
            NombreRol.DOCENTE);

    private final UsuarioRepository usuarioRepository;
    private final EstudianteRepository estudianteRepository;
    private final DocenteRepository docenteRepository;
    private final VinculoAcudienteRepository vinculoRepository;

    public ContextoUsuario(UsuarioRepository usuarioRepository,
                           EstudianteRepository estudianteRepository,
                           DocenteRepository docenteRepository,
                           VinculoAcudienteRepository vinculoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.estudianteRepository = estudianteRepository;
        this.docenteRepository = docenteRepository;
        this.vinculoRepository = vinculoRepository;
    }

    // ---------------------------------------------------------------------
    // Identidad basica
    // ---------------------------------------------------------------------

    /** Correo institucional del token (subject del JWT, RS-04). */
    public String correoActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null) {
            throw new AccessDeniedException("No hay un usuario autenticado en el contexto de la peticion.");
        }
        return auth.getName();
    }

    /** Cuenta del usuario autenticado. */
    public UsuarioJpaEntity usuarioActual() {
        return usuarioRepository.findByCorreoInstitucional(correoActual())
                .orElseThrow(() -> new AccessDeniedException(
                        "La cuenta asociada a la sesion ya no existe en el sistema."));
    }

    public Long usuarioIdActual() {
        return usuarioActual().getId();
    }

    /** Roles efectivos de la cuenta, leidos de la base de datos y no del token. */
    public Set<NombreRol> rolesActuales() {
        return usuarioActual().getRoles().stream()
                .map(RolJpaEntity::getNombre)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(NombreRol.class)));
    }

    // ---------------------------------------------------------------------
    // Perfiles asociados (V9)
    // ---------------------------------------------------------------------

    /** Estudiante asociado a la cuenta, si la cuenta es de un estudiante. */
    public Optional<Long> estudianteIdActual() {
        return estudianteRepository.findByUsuarioId(usuarioIdActual())
                .map(EstudianteJpaEntity::getId);
    }

    /** Docente asociado a la cuenta, si la cuenta es de un docente. */
    public Optional<Long> docenteIdActual() {
        return docenteRepository.findByUsuarioId(usuarioIdActual())
                .map(DocenteJpaEntity::getId);
    }

    /** RB-08: estudiantes formalmente vinculados a la cuenta del acudiente. */
    public List<Long> estudiantesTutelados() {
        return vinculoRepository.findByUsuarioId(usuarioIdActual()).stream()
                .map(VinculoAcudienteJpaEntity::getEstudianteId)
                .toList();
    }

    // ---------------------------------------------------------------------
    // Control de acceso a nivel de dato (ownership)
    // ---------------------------------------------------------------------

    /** Indica si la cuenta ejerce una funcion academica institucional (RS-03). */
    public boolean esPersonalAcademico() {
        return rolesActuales().stream().anyMatch(ROLES_PERSONAL_ACADEMICO::contains);
    }

    /**
     * RNF-07 / RB-08: indica si el usuario autenticado puede ver la informacion
     * de un estudiante. Personal academico: si. Estudiante: solo la propia.
     * Acudiente: solo la de los estudiantes vinculados a su cuenta.
     */
    public boolean puedeVerEstudiante(Long estudianteId) {
        if (estudianteId == null) {
            return false;
        }
        if (esPersonalAcademico()) {
            return true;
        }
        Long usuarioId = usuarioIdActual();
        boolean esElMismo = estudianteRepository.findByUsuarioId(usuarioId)
                .map(e -> estudianteId.equals(e.getId()))
                .orElse(false);
        return esElMismo || vinculoRepository.existsByUsuarioIdAndEstudianteId(usuarioId, estudianteId);
    }

    /**
     * Resuelve el estudiante sobre el que opera la peticion, sin confiar en el
     * identificador recibido cuando el solicitante es el propio estudiante.
     *
     * <ul>
     *   <li>Personal academico: usa el identificador solicitado (RS-03).</li>
     *   <li>Estudiante: <strong>ignora</strong> el solicitado y devuelve el
     *       propio, de modo que manipular el parametro no da acceso a datos
     *       ajenos (RNF-07).</li>
     *   <li>Acudiente: exige el identificador y comprueba el vinculo (RB-08).</li>
     * </ul>
     *
     * @param solicitado identificador recibido por la API; puede ser {@code null}
     *                   cuando quien consulta es el propio estudiante
     * @throws AccessDeniedException si el solicitante no puede ver ese estudiante
     */
    public Long resolverEstudianteId(Long solicitado) {
        if (esPersonalAcademico()) {
            if (solicitado == null) {
                throw new IllegalArgumentException("Debe indicar el estudiante sobre el que desea consultar.");
            }
            return solicitado;
        }

        Optional<Long> propio = estudianteIdActual();
        if (propio.isPresent()) {
            return propio.get();
        }

        if (solicitado != null && vinculoRepository.existsByUsuarioIdAndEstudianteId(usuarioIdActual(), solicitado)) {
            return solicitado;
        }

        throw new AccessDeniedException(
                "No tiene permisos para consultar la informacion de este estudiante.");
    }

    /**
     * RNF-07: devuelve el docente asociado a la cuenta o falla. Se usa en las
     * operaciones que un docente ejecuta sobre su propia carga academica.
     */
    public Long exigirDocenteId() {
        return docenteIdActual().orElseThrow(() -> new AccessDeniedException(
                "La cuenta autenticada no esta vinculada a un docente."));
    }

    /**
     * RNF-07: devuelve el estudiante asociado a la cuenta o falla. Se usa en las
     * operaciones que un estudiante ejecuta sobre si mismo (p. ej. RF-39).
     */
    public Long exigirEstudianteId() {
        return estudianteIdActual().orElseThrow(() -> new AccessDeniedException(
                "La cuenta autenticada no esta vinculada a un estudiante."));
    }
}
