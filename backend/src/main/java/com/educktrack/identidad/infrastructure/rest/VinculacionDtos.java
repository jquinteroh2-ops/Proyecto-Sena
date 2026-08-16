package com.educktrack.identidad.infrastructure.rest;

import com.educktrack.identidad.domain.Parentesco;
import com.educktrack.usuarios.domain.NombreRol;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * DTOs del modulo de identidad: vinculacion de cuentas con perfiles academicos
 * (RF-11, RB-08) y consulta de la identidad del usuario autenticado.
 */
public final class VinculacionDtos {

    private VinculacionDtos() {
    }

    /** Vinculacion de una cuenta existente con un perfil academico. */
    public record VincularCuentaRequest(
            @NotNull(message = "Debe indicar la cuenta de usuario a vincular") Long usuarioId) {
    }

    /** RF-11: vinculacion de la cuenta de un acudiente con un estudiante. */
    public record VincularAcudienteRequest(
            @NotNull(message = "Debe indicar la cuenta del acudiente") Long usuarioId,
            @NotNull(message = "Debe indicar el parentesco (RD-08)") Parentesco parentesco) {
    }

    /** Resultado de vincular una cuenta con un perfil de estudiante o docente. */
    public record VinculoCuentaDto(
            Long perfilId,
            String nombreCompleto,
            Long usuarioId,
            String correo) {
    }

    /** RF-11 / RD-08: vinculo entre la cuenta de un acudiente y un estudiante. */
    public record VinculoAcudienteDto(
            Long id,
            Long usuarioId,
            String acudienteNombre,
            String acudienteCorreo,
            Long estudianteId,
            Parentesco parentesco,
            LocalDateTime fechaVinculo) {
    }

    /**
     * Identidad del usuario autenticado. Permite al cliente saber que perfil
     * academico le corresponde sin exponer identificadores manipulables
     * (RS-03, RNF-07).
     */
    public record IdentidadDto(
            Long usuarioId,
            String nombre,
            String correo,
            Set<NombreRol> roles,
            Long estudianteId,
            Long docenteId,
            List<Long> estudiantesTutelados) {
    }
}
