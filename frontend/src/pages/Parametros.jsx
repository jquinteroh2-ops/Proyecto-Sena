import { useEffect, useState } from 'react';
import api from '../api/axios';
import Layout from '../components/Layout';

// Parametros institucionales (RF-59, RS-14): escala de calificacion (RB-03),
// porcentaje minimo de asistencia (RB-04) y carga maxima del docente (RB-09).
//
// La pantalla es deliberadamente sobria: cada cambio aqui altera reglas que
// afectan a todo el colegio, de modo que conviene que se note que no es una
// pantalla de uso diario. Cambiar la escala no recalcula lo ya calificado; una
// nota de 4.0 sigue siendo 4.0, lo que cambia es la comparacion de aprobacion a
// partir de entonces.
const DESCRIPCIONES = {
  NOTA_MINIMA: 'Extremo inferior de la escala de calificacion (RB-03).',
  NOTA_MAXIMA: 'Extremo superior de la escala de calificacion (RB-03).',
  NOTA_APROBATORIA: 'Nota a partir de la cual se aprueba una materia (RB-12).',
  PORCENTAJE_MINIMO_ASISTENCIA:
    'Asistencia minima para conservar el derecho a evaluacion ordinaria (RB-04).',
  MAX_HORAS_DOCENTE: 'Maximo de horas semanales asignables a un docente (RB-09).',
};

const AYUDA_TIPO = {
  DECIMAL: 'Numero mayor que cero, con decimales.',
  PORCENTAJE: 'Numero entero entre 0 y 100.',
  ENTERO_POSITIVO: 'Numero entero mayor que cero.',
};

export default function Parametros() {
  const [parametros, setParametros] = useState([]);
  const [borradores, setBorradores] = useState({});
  const [error, setError] = useState(null);
  const [aviso, setAviso] = useState(null);
  const [guardando, setGuardando] = useState(null);
  const [cargando, setCargando] = useState(true);

  useEffect(() => {
    cargar();
  }, []);

  function cargar() {
    setCargando(true);
    api
      .get('/configuracion/parametros')
      .then((res) => {
        setParametros(res.data);
        setBorradores(Object.fromEntries(res.data.map((p) => [p.clave, p.valor])));
      })
      .catch((err) =>
        setError(
          err.response?.data?.mensaje ?? 'No fue posible consultar los parametros.'
        )
      )
      .finally(() => setCargando(false));
  }

  async function guardar(clave) {
    setError(null);
    setAviso(null);
    setGuardando(clave);
    try {
      const { data } = await api.put(`/configuracion/parametros/${clave}`, {
        valor: borradores[clave],
      });
      setParametros(data);
      setBorradores(Object.fromEntries(data.map((p) => [p.clave, p.valor])));
      setAviso(`Parametro ${clave} actualizado.`);
    } catch (err) {
      // El backend valida el tipo, el rango y ademas la escala como conjunto:
      // subir la nota aprobatoria por encima del maximo deja una escala en la
      // que nadie puede aprobar. Aqui solo se muestra su mensaje (RNF-10).
      setError(
        err.response?.data?.mensaje ?? 'No fue posible actualizar el parametro.'
      );
      // Se devuelve el borrador al valor vigente para no dejar en pantalla un
      // numero que el servidor rechazo, como si estuviera guardado.
      const vigente = parametros.find((p) => p.clave === clave)?.valor ?? '';
      setBorradores((b) => ({ ...b, [clave]: vigente }));
    } finally {
      setGuardando(null);
    }
  }

  return (
    <Layout>
      <h2 className="text-2xl font-semibold text-gray-900">Parametros institucionales</h2>
      <p className="mt-1 text-sm text-gray-600">
        Definen la escala de calificacion y los minimos institucionales. Un
        cambio aqui afecta a todo el colegio y queda registrado en el log de
        auditoria (RS-07).
      </p>

      {error && (
        <p role="alert" className="mt-6 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700">
          {error}
        </p>
      )}
      {aviso && (
        <p role="status" className="mt-6 rounded-lg bg-green-50 px-4 py-3 text-sm text-green-800">
          {aviso}
        </p>
      )}

      {cargando ? (
        <p className="mt-6 text-sm text-gray-600">Cargando...</p>
      ) : (
        <ul className="mt-6 space-y-4">
          {parametros.map((p) => {
            const sinGuardar = borradores[p.clave] !== p.valor;
            return (
              <li key={p.clave} className="rounded-2xl bg-white p-6 shadow">
                <div className="flex flex-wrap items-end justify-between gap-4">
                  <div className="min-w-0 flex-1">
                    <label
                      htmlFor={`param-${p.clave}`}
                      className="block text-sm font-semibold text-gray-900"
                    >
                      {p.clave.replaceAll('_', ' ')}
                    </label>
                    <p className="mt-0.5 text-sm text-gray-600">
                      {DESCRIPCIONES[p.clave] ?? ''}
                    </p>
                    <p
                      id={`ayuda-${p.clave}`}
                      className="mt-0.5 text-xs text-gray-500"
                    >
                      {AYUDA_TIPO[p.tipo]}
                    </p>
                  </div>

                  <div className="flex items-center gap-3">
                    <input
                      id={`param-${p.clave}`}
                      type="text"
                      inputMode="decimal"
                      value={borradores[p.clave] ?? ''}
                      aria-describedby={`ayuda-${p.clave}`}
                      onChange={(e) =>
                        setBorradores((b) => ({ ...b, [p.clave]: e.target.value }))
                      }
                      className="w-28 rounded-lg border border-gray-300 px-3 py-2 text-right tabular-nums
                                 text-gray-900 focus:border-educk-500 focus:outline-none
                                 focus:ring-2 focus:ring-educk-500"
                    />
                    <button
                      type="button"
                      onClick={() => guardar(p.clave)}
                      disabled={!sinGuardar || guardando === p.clave}
                      className="rounded-lg bg-educk-600 px-4 py-2 text-sm font-semibold text-white
                                 hover:bg-educk-700 focus:outline-none focus:ring-2
                                 focus:ring-educk-500 focus:ring-offset-2
                                 disabled:cursor-not-allowed disabled:opacity-40"
                    >
                      {guardando === p.clave ? 'Guardando...' : 'Guardar'}
                    </button>
                  </div>
                </div>
              </li>
            );
          })}
        </ul>
      )}
    </Layout>
  );
}
