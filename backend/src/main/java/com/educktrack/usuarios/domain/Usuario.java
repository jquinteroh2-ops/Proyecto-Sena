package com.educktrack.usuarios.domain;

import com.educktrack.shared.domain.ReglaNegocioException;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

/**
 * Modelo de dominio puro de una cuenta de usuario (RF-01).
 *
 * <p>Contiene la invariante RB-14 (una cuenta no puede ser Estudiante y Docente
 * a la vez). No lleva anotaciones de framework: la persistencia la resuelve la
 * entidad JPA equivalente en la capa de infraestructura (RNF-17).</p>
 */
public class Usuario {

    private Long id;
    private String nombre;
    private String correoInstitucional;
    private String passwordHash;
    private boolean activo;
    private boolean debeCambiarPassword;
    private final Set<NombreRol> roles;
    private LocalDateTime fechaCreacion;

    public Usuario(Long id, String nombre, String correoInstitucional, String passwordHash,
                   boolean activo, boolean debeCambiarPassword, Set<NombreRol> roles,
                   LocalDateTime fechaCreacion) {
        this.id = id;
        this.nombre = nombre;
        this.correoInstitucional = correoInstitucional;
        this.passwordHash = passwordHash;
        this.activo = activo;
        this.debeCambiarPassword = debeCambiarPassword;
        this.roles = (roles == null || roles.isEmpty()) ? EnumSet.noneOf(NombreRol.class) : EnumSet.copyOf(roles);
        this.fechaCreacion = fechaCreacion;
        validarRolesExcluyentes();
    }

    /**
     * Crea una cuenta nueva (aun sin id). La contrasena inicial debe cambiarse en
     * el primer ingreso (HU-01).
     */
    public static Usuario nueva(String nombre, String correoInstitucional, String passwordHash,
                                Set<NombreRol> roles) {
        return new Usuario(null, nombre, correoInstitucional, passwordHash,
                true, true, roles, LocalDateTime.now());
    }

    /** Asigna un rol validando la exclusion mutua Estudiante/Docente (RB-14). */
    public void asignarRol(NombreRol rol) {
        this.roles.add(rol);
        validarRolesExcluyentes();
    }

    /** RB-14: un usuario no puede tener simultaneamente los roles Estudiante y Docente. */
    private void validarRolesExcluyentes() {
        if (roles.contains(NombreRol.ESTUDIANTE) && roles.contains(NombreRol.DOCENTE)) {
            throw new ReglaNegocioException("RB-14",
                    "Un usuario no puede tener simultaneamente los roles Estudiante y Docente.");
        }
    }

    /** RF-03 / HU-02: desactivar sin eliminar historial. */
    public void desactivar() {
        this.activo = false;
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getCorreoInstitucional() { return correoInstitucional; }
    public String getPasswordHash() { return passwordHash; }
    public boolean isActivo() { return activo; }
    public boolean isDebeCambiarPassword() { return debeCambiarPassword; }
    public Set<NombreRol> getRoles() { return EnumSet.copyOf(roles.isEmpty() ? EnumSet.noneOf(NombreRol.class) : roles); }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
}
