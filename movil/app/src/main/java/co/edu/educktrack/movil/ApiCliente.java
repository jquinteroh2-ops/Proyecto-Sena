package co.edu.educktrack.movil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Cliente HTTP minimo para el API REST de EduckTrack.
 *
 * - La URL base sale de BuildConfig.API_URL (gradle.properties), no del codigo.
 * - El token JWT viaja en la cabecera Authorization; la contrasena solo se usa
 *   para el login y no se guarda en ninguna parte.
 * - Los errores del backend llegan con el formato ApiError ({"mensaje": ...});
 *   ese mensaje es el que se le muestra al usuario.
 */
public final class ApiCliente {

    /** El plan gratuito de Render apaga el backend y tarda en despertar. */
    private static final int TIEMPO_CONEXION_MS = 30_000;
    private static final int TIEMPO_LECTURA_MS = 180_000;

    private ApiCliente() {
    }

    public static final class ApiException extends Exception {
        public final int codigo;

        ApiException(int codigo, String mensaje) {
            super(mensaje);
            this.codigo = codigo;
        }
    }

    public static String get(String ruta, String token) throws ApiException {
        return llamar("GET", ruta, null, token);
    }

    public static String post(String ruta, JSONObject cuerpo) throws ApiException {
        return llamar("POST", ruta, cuerpo, null);
    }

    private static String llamar(String metodo, String ruta, JSONObject cuerpo, String token) throws ApiException {
        HttpURLConnection con = null;
        try {
            con = (HttpURLConnection) new URL(BuildConfig.API_URL + "/api" + ruta).openConnection();
            con.setRequestMethod(metodo);
            con.setConnectTimeout(TIEMPO_CONEXION_MS);
            con.setReadTimeout(TIEMPO_LECTURA_MS);
            con.setRequestProperty("Accept", "application/json");
            if (token != null) {
                con.setRequestProperty("Authorization", "Bearer " + token);
            }
            if (cuerpo != null) {
                con.setDoOutput(true);
                con.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                try (OutputStream out = con.getOutputStream()) {
                    out.write(cuerpo.toString().getBytes(StandardCharsets.UTF_8));
                }
            }
            int codigo = con.getResponseCode();
            String texto = leer(codigo >= 400 ? con.getErrorStream() : con.getInputStream());
            if (codigo >= 400) {
                throw new ApiException(codigo, mensajeDeError(codigo, texto));
            }
            return texto;
        } catch (SocketTimeoutException e) {
            throw new ApiException(0, "El servidor tardó demasiado en responder. Intenta de nuevo.");
        } catch (IOException e) {
            throw new ApiException(0, "No se pudo conectar con el servidor. Revisa tu conexión a internet.");
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
    }

    private static String mensajeDeError(int codigo, String texto) {
        try {
            String mensaje = new JSONObject(texto).optString("mensaje", "");
            if (!mensaje.isEmpty()) {
                return mensaje;
            }
        } catch (JSONException ignorado) {
            // la respuesta no era JSON: se usa el mensaje generico
        }
        if (codigo == 401) return "Correo o contraseña incorrectos.";
        if (codigo == 403) return "Tu cuenta no tiene permiso para esta consulta.";
        return "El servidor respondió con un error (" + codigo + ").";
    }

    private static String leer(InputStream in) throws IOException {
        if (in == null) {
            return "";
        }
        try (InputStream entrada = in; ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            byte[] bloque = new byte[8192];
            int n;
            while ((n = entrada.read(bloque)) != -1) {
                buffer.write(bloque, 0, n);
            }
            return buffer.toString(StandardCharsets.UTF_8.name());
        }
    }
}
