package com.educktrack.usuarios.infrastructure.persistence;

import com.educktrack.usuarios.domain.NombreRol;
import com.educktrack.usuarios.domain.Usuario;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Mapper entre el modelo de dominio {@link Usuario} y su entidad JPA
 * {@link UsuarioJpaEntity} (frontera de la capa de persistencia, RNF-17).
 */
public final class UsuarioMapper {

    private UsuarioMapper() {
    }

    public static Usuario toDomain(UsuarioJpaEntity entity) {
        Set<NombreRol> roles = entity.getRoles().stream()
                .map(RolJpaEntity::getNombre)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(NombreRol.class)));
        return new Usuario(
                entity.getId(),
                entity.getNombre(),
                entity.getCorreoInstitucional(),
                entity.getPasswordHash(),
                entity.isActivo(),
                entity.isDebeCambiarPassword(),
                roles,
                entity.getFechaCreacion());
    }

    /**
     * Convierte el dominio a entidad JPA. Los roles se resuelven contra el
     * catalogo persistido (referencias gestionadas), por lo que se reciben ya
     * cargados desde {@link RolRepository}.
     */
    public static UsuarioJpaEntity toEntity(Usuario usuario, Set<RolJpaEntity> rolesGestionados) {
        UsuarioJpaEntity entity = new UsuarioJpaEntity();
        entity.setId(usuario.getId());
        entity.setNombre(usuario.getNombre());
        entity.setCorreoInstitucional(usuario.getCorreoInstitucional());
        entity.setPasswordHash(usuario.getPasswordHash());
        entity.setActivo(usuario.isActivo());
        entity.setDebeCambiarPassword(usuario.isDebeCambiarPassword());
        entity.setFechaCreacion(usuario.getFechaCreacion());
        entity.getRoles().addAll(rolesGestionados);
        return entity;
    }
}
