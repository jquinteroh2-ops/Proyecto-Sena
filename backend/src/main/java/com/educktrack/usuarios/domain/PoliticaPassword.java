package com.educktrack.usuarios.domain;

import com.educktrack.shared.domain.ReglaNegocioException;

/**
 * Politica minima de seguridad de las contrasenas (HU-01, HU-04).
 *
 * <p>Existe porque HU-04 exige que la contrasena elegida al recuperar la cuenta
 * "cumpla la politica minima de seguridad", y esa politica solo vivia como una
 * anotacion {@code @Size} en el DTO de registro. Una regla declarada en un
 * unico punto de entrada no es una politica: cualquier via nueva de fijar
 * contrasena (recuperacion, cambio obligatorio en el primer acceso, alta
 * masiva) nace sin ella, y nadie se entera hasta que alguien la usa.</p>
 *
 * <p>El almacenamiento cifrado con BCrypt es cosa aparte (RS-05, RNF-05) y
 * sigue siendo responsabilidad del {@code PasswordEncoder}.</p>
 */
public final class PoliticaPassword {

    /** Longitud minima exigida, la misma que ya aplicaba el registro (RF-01). */
    public static final int LONGITUD_MINIMA = 8;

    private PoliticaPassword() {
    }

    /** Falla si la contrasena no cumple la politica minima. */
    public static void exigirCumplimiento(String password) {
        if (password == null || password.isBlank()) {
            throw new ReglaNegocioException("HU-04", "La contrasena es obligatoria.");
        }
        if (password.length() < LONGITUD_MINIMA) {
            throw new ReglaNegocioException("HU-04",
                    "La contrasena debe tener al menos " + LONGITUD_MINIMA + " caracteres.");
        }
    }
}
