package co.edu.educktrack.movil;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Patterns;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Pantalla de inicio de sesion. Consume POST /api/auth/login (RF-60).
 */
public class LoginActivity extends Activity {

    private final ExecutorService hilo = Executors.newSingleThreadExecutor();
    private final Handler principal = new Handler(Looper.getMainLooper());

    private EditText correo;
    private EditText contrasena;
    private TextView mensaje;
    private TextView despertando;
    private Button entrar;
    private ProgressBar cargando;

    @Override
    protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        if (Sesion.activa()) {
            abrirNotas();
            return;
        }
        setContentView(R.layout.activity_login);
        correo = findViewById(R.id.correo);
        contrasena = findViewById(R.id.contrasena);
        mensaje = findViewById(R.id.mensaje);
        despertando = findViewById(R.id.despertando);
        entrar = findViewById(R.id.entrar);
        cargando = findViewById(R.id.cargando);
        ((TextView) findViewById(R.id.servidor)).setText(BuildConfig.API_URL.replace("https://", ""));

        entrar.setOnClickListener(v -> iniciarSesion());
        contrasena.setOnEditorActionListener((v, accion, evento) -> {
            if (accion == EditorInfo.IME_ACTION_DONE) {
                iniciarSesion();
                return true;
            }
            return false;
        });
    }

    private void iniciarSesion() {
        String c = correo.getText().toString().trim();
        String p = contrasena.getText().toString();

        // Validaciones basicas antes de ir a la red.
        correo.setError(null);
        contrasena.setError(null);
        if (!Patterns.EMAIL_ADDRESS.matcher(c).matches()) {
            correo.setError(getString(R.string.error_correo));
            correo.requestFocus();
            return;
        }
        if (p.isEmpty()) {
            contrasena.setError(getString(R.string.error_contrasena));
            contrasena.requestFocus();
            return;
        }

        mostrarCargando(true);
        // Si tarda mas de 5 s es casi seguro que el backend gratuito esta despertando.
        Runnable aviso = () -> despertando.setVisibility(View.VISIBLE);
        principal.postDelayed(aviso, 5000);

        hilo.execute(() -> {
            try {
                JSONObject cuerpo = new JSONObject().put("correo", c).put("password", p);
                JSONObject respuesta = new JSONObject(ApiCliente.post("/auth/login", cuerpo));
                JSONObject usuario = respuesta.optJSONObject("usuario");
                String nombre = usuario != null ? usuario.optString("nombre", c) : c;
                Sesion.iniciar(respuesta.getString("token"), nombre);
                principal.post(() -> {
                    principal.removeCallbacks(aviso);
                    contrasena.setText("");
                    abrirNotas();
                });
            } catch (ApiCliente.ApiException e) {
                principal.post(() -> {
                    principal.removeCallbacks(aviso);
                    mostrarError(e.getMessage());
                });
            } catch (JSONException e) {
                principal.post(() -> {
                    principal.removeCallbacks(aviso);
                    mostrarError("Respuesta inesperada del servidor.");
                });
            }
        });
    }

    private void mostrarCargando(boolean si) {
        entrar.setEnabled(!si);
        entrar.setText(si ? R.string.entrando : R.string.entrar);
        cargando.setVisibility(si ? View.VISIBLE : View.GONE);
        mensaje.setVisibility(View.GONE);
        if (!si) {
            despertando.setVisibility(View.GONE);
        }
    }

    private void mostrarError(String texto) {
        mostrarCargando(false);
        mensaje.setText(texto);
        mensaje.setVisibility(View.VISIBLE);
    }

    private void abrirNotas() {
        startActivity(new Intent(this, NotasActivity.class));
        finish();
    }

    @Override
    protected void onDestroy() {
        hilo.shutdownNow();
        super.onDestroy();
    }
}
