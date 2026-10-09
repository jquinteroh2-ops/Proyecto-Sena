package co.edu.educktrack.movil;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * "Mis notas": el estudiante consulta sus calificaciones del periodo mas
 * reciente, agrupadas por materia. Consume:
 *  - GET /api/identidad/yo                          -> quien soy (estudianteId)
 *  - GET /api/calificaciones/historico/{id} (RF-37)  -> mis calificaciones
 *  - GET /api/materias y GET /api/periodos           -> nombres para mostrar
 */
public class NotasActivity extends Activity {

    private final ExecutorService hilo = Executors.newSingleThreadExecutor();
    private final Handler principal = new Handler(Looper.getMainLooper());

    private ListView lista;
    private View estado;
    private ProgressBar cargando;
    private TextView estadoTexto;
    private Button reintentar;

    /** Una fila de la lista: una materia con sus evaluaciones del periodo. */
    static final class Materia {
        final String nombre;
        final List<String> evaluaciones = new ArrayList<>();
        double suma;
        int cantidad;

        Materia(String nombre) {
            this.nombre = nombre;
        }

        double promedio() {
            return cantidad == 0 ? 0 : suma / cantidad;
        }
    }

    @Override
    protected void onCreate(Bundle guardado) {
        super.onCreate(guardado);
        if (!Sesion.activa()) {
            volverAlLogin();
            return;
        }
        setContentView(R.layout.activity_notas);
        lista = findViewById(R.id.lista);
        estado = findViewById(R.id.estado);
        cargando = findViewById(R.id.cargando);
        estadoTexto = findViewById(R.id.estado_texto);
        reintentar = findViewById(R.id.reintentar);
        ((TextView) findViewById(R.id.nombre)).setText(Sesion.nombre());

        findViewById(R.id.salir).setOnClickListener(v -> {
            Sesion.cerrar();
            volverAlLogin();
        });
        reintentar.setOnClickListener(v -> cargar());
        cargar();
    }

    private void cargar() {
        mostrarEstado(true, getString(R.string.cargando_notas), false);
        hilo.execute(() -> {
            try {
                String token = Sesion.token();
                JSONObject yo = new JSONObject(ApiCliente.get("/identidad/yo", token));
                if (yo.isNull("estudianteId")) {
                    principal.post(() -> mostrarEstado(false, getString(R.string.solo_estudiantes), false));
                    return;
                }
                long estudianteId = yo.getLong("estudianteId");

                JSONArray notas = new JSONArray(ApiCliente.get("/calificaciones/historico/" + estudianteId, token));
                Map<Long, String> materias = nombres(new JSONArray(ApiCliente.get("/materias", token)));
                Map<Long, String> periodos = nombres(new JSONArray(ApiCliente.get("/periodos", token)));

                // Solo el periodo mas reciente en el que tiene notas.
                long periodo = -1;
                for (int i = 0; i < notas.length(); i++) {
                    periodo = Math.max(periodo, notas.getJSONObject(i).getLong("periodoAcademicoId"));
                }
                Map<Long, Materia> porMateria = new LinkedHashMap<>();
                double suma = 0;
                int cantidad = 0;
                for (int i = 0; i < notas.length(); i++) {
                    JSONObject n = notas.getJSONObject(i);
                    if (n.getLong("periodoAcademicoId") != periodo) continue;
                    long materiaId = n.getLong("materiaId");
                    Materia m = porMateria.get(materiaId);
                    if (m == null) {
                        String nombre = materias.containsKey(materiaId) ? materias.get(materiaId) : "Materia " + materiaId;
                        m = new Materia(nombre);
                        porMateria.put(materiaId, m);
                    }
                    double valor = n.getDouble("valor");
                    m.evaluaciones.add(tipo(n.optString("tipo")) + " " + formato(valor));
                    m.suma += valor;
                    m.cantidad++;
                    suma += valor;
                    cantidad++;
                }
                List<Materia> filas = new ArrayList<>(porMateria.values());
                String nombrePeriodo = periodos.containsKey(periodo) ? periodos.get(periodo) : "";
                double promedio = cantidad == 0 ? 0 : suma / cantidad;
                int total = cantidad;
                principal.post(() -> mostrar(filas, nombrePeriodo, promedio, total));
            } catch (ApiCliente.ApiException e) {
                if (e.codigo == 401) {
                    // El token vencio: hay que volver a entrar.
                    Sesion.cerrar();
                    principal.post(this::volverAlLogin);
                    return;
                }
                principal.post(() -> mostrarEstado(false, e.getMessage(), true));
            } catch (JSONException e) {
                principal.post(() -> mostrarEstado(false, "Respuesta inesperada del servidor.", true));
            }
        });
    }

    private void mostrar(List<Materia> filas, String periodo, double promedio, int total) {
        TextView detalle = findViewById(R.id.detalle);
        if (filas.isEmpty()) {
            detalle.setText("Estudiante");
            mostrarEstado(false, getString(R.string.sin_notas), false);
            return;
        }
        detalle.setText(periodo + " · " + total + " evaluaciones");
        ((TextView) findViewById(R.id.promedio)).setText(formato(promedio));
        findViewById(R.id.resumen).setVisibility(View.VISIBLE);
        estado.setVisibility(View.GONE);
        lista.setVisibility(View.VISIBLE);
        lista.setAdapter(new Adaptador(filas));
    }

    private void mostrarEstado(boolean conCarga, String texto, boolean conReintento) {
        lista.setVisibility(View.GONE);
        estado.setVisibility(View.VISIBLE);
        cargando.setVisibility(conCarga ? View.VISIBLE : View.GONE);
        estadoTexto.setText(texto);
        reintentar.setVisibility(conReintento ? View.VISIBLE : View.GONE);
    }

    private static Map<Long, String> nombres(JSONArray arreglo) throws JSONException {
        Map<Long, String> mapa = new HashMap<>();
        for (int i = 0; i < arreglo.length(); i++) {
            JSONObject o = arreglo.getJSONObject(i);
            mapa.put(o.getLong("id"), o.optString("nombre"));
        }
        return mapa;
    }

    private static String tipo(String codigo) {
        switch (codigo) {
            case "QUIZ": return "Quiz";
            case "EXAMEN": return "Examen";
            case "TALLER": return "Taller";
            case "TAREA": return "Tarea";
            default: return codigo.isEmpty() ? "Nota" : codigo.charAt(0) + codigo.substring(1).toLowerCase(Locale.ROOT);
        }
    }

    static String formato(double valor) {
        return String.format(Locale.US, "%.1f", valor);
    }

    private void volverAlLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    @Override
    protected void onDestroy() {
        hilo.shutdownNow();
        super.onDestroy();
    }

    /** Escala colombiana 1.0 - 5.0: se aprueba con 3.0. */
    private final class Adaptador extends BaseAdapter {
        private final List<Materia> filas;

        Adaptador(List<Materia> filas) {
            this.filas = filas;
        }

        @Override
        public int getCount() {
            return filas.size();
        }

        @Override
        public Object getItem(int posicion) {
            return filas.get(posicion);
        }

        @Override
        public long getItemId(int posicion) {
            return posicion;
        }

        @Override
        public View getView(int posicion, View vista, ViewGroup padre) {
            if (vista == null) {
                vista = LayoutInflater.from(NotasActivity.this).inflate(R.layout.item_materia, padre, false);
            }
            Materia m = filas.get(posicion);
            ((TextView) vista.findViewById(R.id.materia)).setText(m.nombre);
            ((TextView) vista.findViewById(R.id.evaluaciones)).setText(String.join("  ·  ", m.evaluaciones));
            TextView nota = vista.findViewById(R.id.nota);
            double p = m.promedio();
            nota.setText(formato(p));
            int texto = p >= 4.0 ? R.color.verde : p >= 3.0 ? R.color.ambar : R.color.rojo;
            int fondo = p >= 4.0 ? R.color.verde_fondo : p >= 3.0 ? R.color.ambar_fondo : R.color.rojo_fondo;
            nota.setTextColor(getColor(texto));
            nota.getBackground().mutate().setTint(getColor(fondo));
            return vista;
        }
    }
}
