package com.educktrack.identidad.application;

import com.educktrack.cursos.infrastructure.persistence.CursoRepository;
import com.educktrack.docentes.infrastructure.persistence.AsignacionDocenteRepository;
import com.educktrack.docentes.infrastructure.persistence.DocenteJpaEntity;
import com.educktrack.docentes.infrastructure.persistence.DocenteRepository;
import com.educktrack.estudiantes.infrastructure.persistence.EstudianteJpaEntity;
import com.educktrack.estudiantes.infrastructure.persistence.EstudianteRepository;
import com.educktrack.identidad.infrastructure.persistence.VinculoAcudienteJpaEntity;
import com.educktrack.identidad.infrastructure.persistence.VinculoAcudienteRepository;
import com.educktrack.matriculas.domain.EstadoMatriculaCurso;
import com.educktrack.matriculas.infrastructure.persistence.MatriculaRepository;
import com.educktrack.usuarios.domain.NombreRol;
import com.educktrack.usuarios.infrastructure.persistence.RolJpaEntity;
import com.educktrack.usuarios.infrastructure.persistence.UsuarioJpaEntity;
import com.educktrack.usuarios.infrastructure.persistence.UsuarioRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
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
     * Roles con visibilidad sobre cualquier estudiante de la institucion
     * (RS-03). Los dos coordinadores, la rectoria y la administracion ejercen
     * funciones transversales, de modo que su alcance no depende de ningun
     * vinculo academico concreto.
     *
     * <p>DECISION DE DISENO (Fase 2): el docente <strong>no</strong> figura
     * aqui. Su alcance no es institucional sino el de su carga academica: los
     * estudiantes matriculados activamente en los cursos que tiene asignados
     * ({@code asignacion_docente}) mas los cursos que dirige como director de
     * grupo (RB-02). Asi RF-08, RF-28 y RF-45 siguen siendo operaciones del
     * Docente, pero acotadas a sus propios cursos: un docente de septimo no
     * puede leer el historial academico de un estudiante de once.</p>
     */
    private static final Set<NombreRol> ROLES_VISION_INSTITUCIONAL = EnumSet.of(
            NombreRol.ADMINISTRADOR,
            NombreRol.RECTOR,
            NombreRol.COORDINADOR_ACADEMICO,
            NombreRol.COORDINADOR_CONVIVENCIA);

    private final UsuarioRepository usuarioRepository;
    private final EstudianteRepository estudianteRepository;
    private final DocenteRepository docenteRepository;
    private final VinculoAcudienteRepository vinculoRepository;
    private final AsignacionDocenteRepository asignacionRepository;
    private final CursoRepository cursoRepository;
    private final MatriculaRepository matriculaRepository;

    public ContextoUsuario(UsuarioRepository usuarioRepository,
                           EstudianteRepository estudianteRepository,
                           DocenteRepository docenteRepository,
                           VinculoAcudienteRepository vinculoRepository,
                           AsignacionDocenteRepository asignacionRepository,
                           CursoRepository cursoRepository,
                           MatriculaRepository matriculaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.estudianteRepository = estudianteRepository;
        this.docenteRepository = docenteRepository;
        this.vinculoRepository = vinculoRepository;
        this.asignacionRepository = asignacionRepository;
        this.cursoRepository = cursoRepository;
        this.matriculaRepository = matriculaRepository;
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

    /**
     * Cuenta del usuario autenticado.
     *
     * <p>Se memoriza durante la peticion ({@link IdentidadDeLaPeticion}): una
     * sola comprobacion de acceso la consultaba hasta cuatro veces, y dentro de
     * una peticion la respuesta no puede cambiar.</p>
     */
    public UsuarioJpaEntity usuarioActual() {
        String correo = correoActual();
        return IdentidadDeLaPeticion.usuario(correo,
                () -> usuarioRepository.findByCorreoInstitucional(correo)
                        .orElseThrow(() -> new AccessDeniedException(
                                "La cuenta asociada a la sesion ya no existe en el sistema.")));
    }

    public Long usuarioIdActual() {
        return usuarioActual().getId();
    }

    /** Roles efectivos de la cuenta, leidos de la base de datos y no del token. */
    public Set<NombreRol> rolesActuales() {
        return rolesDe(usuarioActual());
    }

    private Set<NombreRol> rolesDe(UsuarioJpaEntity usuario) {
        return usuario.getRoles().stream()
                .map(RolJpaEntity::getNombre)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(NombreRol.class)));
    }

    // ---------------------------------------------------------------------
    // Perfiles asociados (V9)
    // ---------------------------------------------------------------------

    /**
     * Estudiante asociado a la cuenta, si la cuenta es de un estudiante.
     *
     * <p>Memorizado por peticion: {@code puedeVerEstudiante},
     * {@code puedeVerCurso} y {@code resolverEstudianteId} lo piden por
     * separado en la misma llamada.</p>
     */
    public Optional<Long> estudianteIdActual() {
        return IdentidadDeLaPeticion.estudianteId(correoActual(),
                () -> estudianteRepository.findByUsuarioId(usuarioIdActual())
                        .map(EstudianteJpaEntity::getId));
    }

    /** Docente asociado a la cuenta, si la cuenta es de un docente. */
    public Optional<Long> docenteIdActual() {
        return IdentidadDeLaPeticion.docenteId(correoActual(),
                () -> docenteRepository.findByUsuarioId(usuarioIdActual())
                        .map(DocenteJpaEntity::getId));
    }

    /** RB-08: estudiantes formalmente vinculados a la cuenta del acudiente. */
    public List<Long> estudiantesTutelados() {
        return vinculoRepository.findByUsuarioId(usuarioIdActual()).stream()
                .map(VinculoAcudienteJpaEntity::getEstudianteId)
                .toList();
    }

    // ---------------------------------------------------------------------
    // Alcance academico del docente (RNF-07, RB-02)
    // ---------------------------------------------------------------------

    /**
     * Cursos que quedan dentro de la carga academica del docente autenticado,
     * por cualquiera de las dos vias que reconocen los requisitos:
     *
     * <ul>
     *   <li>tener una materia asignada en el curso ({@code asignacion_docente},
     *       RF-14);</li>
     *   <li>ser su director de grupo ({@code curso.director_grupo_id}, RB-02),
     *       aunque no dicte ninguna materia alli.</li>
     * </ul>
     *
     * <p>Devuelve un conjunto vacio si la cuenta no corresponde a un docente,
     * de modo que quien no es docente nunca gana alcance por esta via.</p>
     */
    public Set<Long> cursosDelDocente() {
        Optional<Long> docenteId = docenteIdActual();
        if (docenteId.isEmpty()) {
            return Set.of();
        }
        Set<Long> cursos = new LinkedHashSet<>(asignacionRepository.findCursoIdsByDocenteId(docenteId.get()));
        cursos.addAll(cursoRepository.findIdsByDirectorGrupoId(docenteId.get()));
        return cursos;
    }

    // ---------------------------------------------------------------------
    // Control de acceso a nivel de dato (ownership)
    // ---------------------------------------------------------------------

    /** Indica si la cuenta ve a cualquier estudiante de la institucion (RS-03). */
    public boolean tieneVisionInstitucional() {
        return rolesActuales().stream().anyMatch(ROLES_VISION_INSTITUCIONAL::contains);
    }

    /**
     * RNF-07 / RB-08: indica si el usuario autenticado puede ver la informacion
     * de un estudiante. Vision institucional: si. Estudiante: solo la propia.
     * Acudiente: solo la de los estudiantes vinculados a su cuenta. Docente:
     * solo la de los estudiantes activos en los cursos que atiende.
     */
    public boolean puedeVerEstudiante(Long estudianteId) {
        if (estudianteId == null) {
            return false;
        }
        if (tieneVisionInstitucional()) {
            return true;
        }
        Long usuarioId = usuarioIdActual();
        // Reutiliza el perfil ya memorizado en vez de volver a consultarlo.
        boolean esElMismo = estudianteIdActual().map(estudianteId::equals).orElse(false);
        if (esElMismo || vinculoRepository.existsByUsuarioIdAndEstudianteId(usuarioId, estudianteId)) {
            return true;
        }
        Set<Long> cursos = cursosDelDocente();
        return !cursos.isEmpty() && matriculaRepository.existsByEstudianteIdAndCursoIdInAndEstado(
                estudianteId, cursos, EstadoMatriculaCurso.ACTIVA);
    }

    /**
     * RNF-07: indica si el usuario autenticado puede consultar un curso como
     * conjunto (listados, reportes de grupo). El docente accede a los cursos de
     * su carga; el estudiante y el acudiente, unicamente al curso que cursan
     * ellos o sus tutelados.
     */
    public boolean puedeVerCurso(Long cursoId) {
        if (cursoId == null) {
            return false;
        }
        if (tieneVisionInstitucional() || cursosDelDocente().contains(cursoId)) {
            return true;
        }
        List<Long> propios = estudiantesPropios();
        return !propios.isEmpty() && matriculaRepository.existsByEstudianteIdInAndCursoIdAndEstado(
                propios, cursoId, EstadoMatriculaCurso.ACTIVA);
    }

    /** Estudiantes que "son" el usuario: el propio perfil y los tutelados (RB-08). */
    private List<Long> estudiantesPropios() {
        List<Long> propios = new ArrayList<>();
        estudianteIdActual().ifPresent(propios::add);
        propios.addAll(estudiantesTutelados());
        return propios;
    }

    /**
     * RNF-07: cursos que el usuario autenticado puede consultar cuando no tiene
     * vision institucional: los de su carga academica si es docente, o los que
     * cursan el y sus tutelados si es estudiante o acudiente. El llamador debe
     * comprobar antes {@link #tieneVisionInstitucional()}, porque para esos
     * roles el alcance es "todos" y esta lista no lo representa.
     */
    public Set<Long> cursosVisibles() {
        Set<Long> cursos = new LinkedHashSet<>(cursosDelDocente());
        List<Long> propios = estudiantesPropios();
        if (!propios.isEmpty()) {
            cursos.addAll(matriculaRepository.findCursoIdsByEstudianteIdInAndEstado(
                    propios, EstadoMatriculaCurso.ACTIVA));
        }
        return cursos;
    }

    /**
     * RNF-07: estudiantes que el usuario autenticado puede enumerar. Solo tiene
     * sentido para quien no tiene vision institucional; el llamador debe
     * comprobar {@link #tieneVisionInstitucional()} antes de filtrar por esta
     * lista, porque para esos roles el alcance es "todos".
     */
    public List<Long> estudiantesVisibles() {
        Set<Long> cursos = cursosDelDocente();
        if (!cursos.isEmpty()) {
            return matriculaRepository.findEstudianteIdsByCursoIdInAndEstado(
                    cursos, EstadoMatriculaCurso.ACTIVA);
        }
        return estudiantesPropios();
    }

    /**
     * Resuelve el estudiante sobre el que opera la peticion, sin confiar en el
     * identificador recibido cuando el solicitante es el propio estudiante.
     *
     * <ul>
     *   <li>Estudiante: <strong>ignora</strong> el solicitado y devuelve el
     *       propio, de modo que manipular el parametro no da acceso a datos
     *       ajenos (RNF-07).</li>
     *   <li>Vision institucional: usa el identificador solicitado (RS-03).</li>
     *   <li>Docente: exige el identificador y comprueba que el estudiante este
     *       activo en alguno de sus cursos (RB-02).</li>
     *   <li>Acudiente: exige el identificador y comprueba el vinculo (RB-08).</li>
     * </ul>
     *
     * @param solicitado identificador recibido por la API; puede ser {@code null}
     *                   cuando quien consulta es el propio estudiante
     * @throws AccessDeniedException si el solicitante no puede ver ese estudiante
     */
    public Long resolverEstudianteId(Long solicitado) {
        if (!tieneVisionInstitucional()) {
            Optional<Long> propio = estudianteIdActual();
            if (propio.isPresent()) {
                return propio.get();
            }
        }
        if (solicitado == null) {
            throw new IllegalArgumentException("Debe indicar el estudiante sobre el que desea consultar.");
        }
        exigirAccesoEstudiante(solicitado);
        return solicitado;
    }

    /**
     * RNF-07: corta la peticion si el usuario autenticado no alcanza a ese
     * estudiante. Es la version imperativa de {@link #puedeVerEstudiante(Long)},
     * pensada para usarse al principio de los metodos de consulta.
     */
    public void exigirAccesoEstudiante(Long estudianteId) {
        if (!puedeVerEstudiante(estudianteId)) {
            throw new AccessDeniedException(
                    "No tiene permisos para consultar la informacion de este estudiante.");
        }
    }

    /** RNF-07: corta la peticion si el curso queda fuera del alcance del usuario. */
    public void exigirAccesoCurso(Long cursoId) {
        if (!puedeVerCurso(cursoId)) {
            throw new AccessDeniedException(
                    "No tiene permisos para consultar la informacion de este curso.");
        }
    }

    /**
     * RNF-07 / RF-54: corta la peticion si el usuario autenticado no es el
     * titular de la cuenta consultada. La bandeja de notificaciones es
     * estrictamente personal: ni siquiera la vision institucional la abre,
     * porque leer los mensajes de otro no es una funcion academica.
     */
    public void exigirCuentaPropia(Long usuarioId) {
        if (usuarioId == null || !usuarioId.equals(usuarioIdActual())) {
            throw new AccessDeniedException(
                    "Solo puede consultar las notificaciones de su propia cuenta.");
        }
    }

    /**
     * RNF-07 / RB-02: indica si el usuario puede <strong>escribir</strong> sobre
     * un curso y materia concretos (registrar asistencia, calificar, asignar
     * tareas). Es mas estricto que {@link #puedeVerCurso(Long)}: al docente no
     * le basta con alcanzar el curso, necesita dictar esa materia alli.
     *
     * <p>La direccion de grupo (RB-02) da visibilidad sobre el curso pero
     * <strong>no</strong> potestad para calificar materias que no dicta.</p>
     */
    public boolean puedeGestionarMateria(Long cursoId, Long materiaId) {
        if (cursoId == null || materiaId == null) {
            return false;
        }
        if (tieneVisionInstitucional()) {
            return true;
        }
        return docenteIdActual()
                .map(id -> asignacionRepository.existsByDocenteIdAndCursoIdAndMateriaId(id, cursoId, materiaId))
                .orElse(false);
    }

    /** RNF-07: version imperativa de {@link #puedeGestionarMateria(Long, Long)}. */
    public void exigirGestionMateria(Long cursoId, Long materiaId) {
        if (!puedeGestionarMateria(cursoId, materiaId)) {
            throw new AccessDeniedException(
                    "No tiene asignada esa materia en ese curso, de modo que no puede registrar informacion academica sobre ella.");
        }
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
