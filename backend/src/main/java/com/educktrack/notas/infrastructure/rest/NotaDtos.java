package com.educktrack.notas.infrastructure.rest;

import com.educktrack.notas.domain.TipoEvaluacion;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * DTOs del modulo de calificaciones (RF-20, RF-31..RF-37).
 */
public final class NotaDtos {

    private NotaDtos() {
    }

    // ---------- Ponderacion (RF-20, RB-07) ----------
    public record ConfigurarPonderacionRequest(
            @NotNull Long materiaId,
            @NotNull Long periodoAcademicoId,
            @NotEmpty(message = "Debe definir al menos una ponderacion") @Valid List<PonderacionItem> ponderaciones) {
    }

    public record PonderacionItem(
            @NotNull(message = "El tipo es obligatorio") TipoEvaluacion tipo,
            int porcentaje) {
    }

    public record PonderacionDto(TipoEvaluacion tipo, int porcentaje) {
    }

    // ---------- Calificaciones (RF-31, RF-32) ----------
    public record RegistrarCalificacionRequest(
            @NotNull Long estudianteId,
            @NotNull Long materiaId,
            @NotNull Long cursoId,
            @NotNull Long periodoAcademicoId,
            @NotNull(message = "El tipo de evaluacion es obligatorio") TipoEvaluacion tipo,
            @NotNull(message = "El valor es obligatorio") Double valor,
            String descripcion) {
    }

    public record EditarCalificacionRequest(@NotNull Double valor) {
    }

    public record CalificacionDto(
            Long id, Long estudianteId, Long materiaId, Long cursoId, Long periodoAcademicoId,
            TipoEvaluacion tipo, double valor, String descripcion) {
    }

    // ---------- Promedio (RF-33) ----------
    public record DetalleTipoDto(TipoEvaluacion tipo, Double promedioTipo, int porcentaje, double aporte) {
    }

    public record PromedioDto(
            Long estudianteId, Long materiaId, Long periodoAcademicoId,
            double promedio, boolean aprobada,
            List<TipoEvaluacion> pendientes, List<DetalleTipoDto> detalle) {
    }

    // ---------- Novedad (RF-36, RB-15) ----------
    public record NovedadRequest(
            @NotNull(message = "El nuevo valor es obligatorio") Double nuevoValor,
            @NotBlank(message = "El motivo es obligatorio") String motivo) {
    }

    // ---------- Boletin (RF-35, RD-11) ----------
    public record BoletinMateriaDto(Long materiaId, double promedio, boolean aprobada) {
    }

    public record BoletinDto(
            Long estudianteId, Long cursoId, Long periodoAcademicoId,
            List<BoletinMateriaDto> materias, double promedioGeneral, boolean aprobado) {
    }
}
