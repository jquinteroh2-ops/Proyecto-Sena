package com.educktrack.identidad.infrastructure.rest;

import com.educktrack.identidad.application.VinculacionService;
import com.educktrack.identidad.infrastructure.rest.VinculacionDtos.IdentidadDto;
import com.educktrack.identidad.infrastructure.rest.VinculacionDtos.VincularAcudienteRequest;
import com.educktrack.identidad.infrastructure.rest.VinculacionDtos.VincularCuentaRequest;
import com.educktrack.identidad.infrastructure.rest.VinculacionDtos.VinculoAcudienteDto;
import com.educktrack.identidad.infrastructure.rest.VinculacionDtos.VinculoCuentaDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Vinculacion de cuentas de usuario con perfiles academicos (RF-11, RS-03).
 *
 * <p>La vinculacion es competencia de coordinacion academica y administracion:
 * determina que puede ver cada cuenta, por lo que es una operacion sensible
 * (RB-08, RNF-07). La consulta de la identidad propia esta abierta a cualquier
 * usuario autenticado, ya que solo devuelve datos de su propia cuenta.</p>
 */
@RestController
@RequestMapping("/api/identidad")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Identidad", description = "Vinculacion de cuentas con perfiles academicos (RF-11, RB-08)")
public class VinculacionController {

    private static final String GESTION = "hasAnyRole('COORDINADOR_ACADEMICO','ADMINISTRADOR')";

    private final VinculacionService service;

    public VinculacionController(VinculacionService service) {
        this.service = service;
    }

    /** Identidad del usuario autenticado: perfil academico que le corresponde (RS-03). */
    @GetMapping("/yo")
    @Operation(summary = "Consultar la identidad del usuario autenticado (RS-03)")
    public ResponseEntity<IdentidadDto> identidadActual() {
        return ResponseEntity.ok(service.identidadActual());
    }

    /** Vincula una cuenta con el perfil de un estudiante (RS-03, RNF-07). */
    @PostMapping("/estudiantes/{estudianteId}/cuenta")
    @PreAuthorize(GESTION)
    @Operation(summary = "Vincular cuenta de usuario a un estudiante (RS-03)")
    public ResponseEntity<VinculoCuentaDto> vincularEstudiante(@PathVariable Long estudianteId,
                                                               @Valid @RequestBody VincularCuentaRequest req) {
        return ResponseEntity.ok(service.vincularEstudiante(estudianteId, req.usuarioId()));
    }

    /** Vincula una cuenta con el perfil de un docente (RS-03, RNF-07). */
    @PostMapping("/docentes/{docenteId}/cuenta")
    @PreAuthorize(GESTION)
    @Operation(summary = "Vincular cuenta de usuario a un docente (RS-03)")
    public ResponseEntity<VinculoCuentaDto> vincularDocente(@PathVariable Long docenteId,
                                                            @Valid @RequestBody VincularCuentaRequest req) {
        return ResponseEntity.ok(service.vincularDocente(docenteId, req.usuarioId()));
    }

    /** RF-11: vincula la cuenta de un padre de familia a un estudiante (RB-08, RD-08). */
    @PostMapping("/estudiantes/{estudianteId}/acudientes")
    @PreAuthorize(GESTION)
    @Operation(summary = "Vincular padre de familia a un estudiante (RF-11, RB-08)")
    public ResponseEntity<VinculoAcudienteDto> vincularAcudiente(@PathVariable Long estudianteId,
                                                                 @Valid @RequestBody VincularAcudienteRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.vincularAcudiente(estudianteId, req.usuarioId(), req.parentesco()));
    }

    /** RF-11: consulta los acudientes vinculados a un estudiante. */
    @GetMapping("/estudiantes/{estudianteId}/acudientes")
    @PreAuthorize(GESTION)
    @Operation(summary = "Listar acudientes vinculados a un estudiante (RF-11)")
    public ResponseEntity<List<VinculoAcudienteDto>> acudientesDe(@PathVariable Long estudianteId) {
        return ResponseEntity.ok(service.acudientesDe(estudianteId));
    }

    /** RF-11: revoca el vinculo de un acudiente (retira la visibilidad de RB-08). */
    @DeleteMapping("/acudientes/{vinculoId}")
    @PreAuthorize(GESTION)
    @Operation(summary = "Revocar vinculo de acudiente (RF-11, RB-08)")
    public ResponseEntity<Void> desvincularAcudiente(@PathVariable Long vinculoId) {
        service.desvincularAcudiente(vinculoId);
        return ResponseEntity.noContent().build();
    }
}
