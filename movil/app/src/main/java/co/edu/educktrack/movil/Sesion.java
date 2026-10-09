package co.edu.educktrack.movil;

/**
 * Sesion en memoria: el token JWT vive solo mientras la app esta abierta.
 * No se escribe en disco a proposito; al cerrar la app hay que volver a
 * iniciar sesion, que es lo esperable en un dispositivo compartido.
 */
public final class Sesion {

    private static String token;
    private static String nombre;

    private Sesion() {
    }

    public static void iniciar(String nuevoToken, String nuevoNombre) {
        token = nuevoToken;
        nombre = nuevoNombre;
    }

    public static String token() {
        return token;
    }

    public static String nombre() {
        return nombre;
    }

    public static boolean activa() {
        return token != null;
    }

    public static void cerrar() {
        token = null;
        nombre = null;
    }
}
