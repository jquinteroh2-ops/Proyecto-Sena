package com.educktrack.estudiantes.domain;

import com.educktrack.shared.domain.ReglaNegocioException;

import java.time.LocalDate;

/**
 * Modelo de dominio de un estudiante (RF-06). Nace en estado ACTIVO (HU-05) y su
 * retiro (RB-20) exige motivo, fecha y autorizacion.
 */
public class Estudiante {

    private Long id;
    private String documento;
    private String nombres;
    private String apellidos;
    private LocalDate fechaNacimiento;
    private String direccion;
    private EstadoMatricula estado;
    // Datos de acudiente (HU-05: al menos un dato de contacto)
    private String acudienteNombre;
    private String acudienteTelefono;
    private String acudienteParentesco;

    public Estudiante(Long id, String documento, String nombres, String apellidos,
                      LocalDate fechaNacimiento, String direccion, EstadoMatricula estado,
                      String acudienteNombre, String acudienteTelefono, String acudienteParentesco) {
        this.id = id;
        this.documento = documento;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.fechaNacimiento = fechaNacimiento;
        this.direccion = direccion;
        this.estado = estado;
        this.acudienteNombre = acudienteNombre;
        this.acudienteTelefono = acudienteTelefono;
        this.acudienteParentesco = acudienteParentesco;
    }

    /** Registra un nuevo estudiante en estado ACTIVO (HU-05). */
    public static Estudiante registrar(String documento, String nombres, String apellidos,
                                       LocalDate fechaNacimiento, String direccion,
                                       String acudienteNombre, String acudienteTelefono,
                                       String acudienteParentesco) {
        return new Estudiante(null, documento, nombres, apellidos, fechaNacimiento, direccion,
                EstadoMatricula.ACTIVO, acudienteNombre, acudienteTelefono, acudienteParentesco);
    }

    /**
     * RB-20 / HU-07: el retiro cambia el estado a RETIRADO sin borrar historial.
     * Los datos de motivo/fecha/autorizacion se registran en el modulo academico.
     */
    public void retirar() {
        if (this.estado == EstadoMatricula.RETIRADO) {
            throw new ReglaNegocioException("RB-20", "El estudiante ya se encuentra retirado.");
        }
        this.estado = EstadoMatricula.RETIRADO;
    }

    public boolean estaActivo() {
        return this.estado == EstadoMatricula.ACTIVO;
    }

    public Long getId() { return id; }
    public String getDocumento() { return documento; }
    public String getNombres() { return nombres; }
    public String getApellidos() { return apellidos; }
    public LocalDate getFechaNacimiento() { return fechaNacimiento; }
    public String getDireccion() { return direccion; }
    public EstadoMatricula getEstado() { return estado; }
    public String getAcudienteNombre() { return acudienteNombre; }
    public String getAcudienteTelefono() { return acudienteTelefono; }
    public String getAcudienteParentesco() { return acudienteParentesco; }
}
