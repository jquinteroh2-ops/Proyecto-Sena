import { useEffect, useState } from 'react';
import api from '../api/axios';
import Layout from '../components/Layout';

// Boletin de calificaciones (RF-35, RB-19, RD-11).
//
// Es la primera pantalla que muestra el resultado de las reglas academicas de
// las fases anteriores: RB-12 (aprueba quien supera la nota minima en TODAS las
// materias del plan, incluidas las que no tienen ninguna nota) y RB-04 (la
// perdida del derecho a evaluacion ordinaria se informa, no reprueba).
//
// RNF-07: el estudiante consulta el suyo y el acudiente el de sus tutelados. El
// identificador se toma de /identidad/yo, que lo resuelve el servidor, y no de
// nada que el usuario pueda escribir; para el personal con vision institucional
// si se ofrece el campo, porque su alcance no depende de un vinculo.
export default function Boletin() {
  const [identidad, setIdentidad] = useState(null);
  const [estudianteId, setEstudianteId] = useState('');
  const [cursoId, setCursoId] = useState('');
  const [periodoId, setPeriodoId] = useState('');

  const [boletin, setBoletin] = useState(null);
  const [error, setError] = useState(null);
  const [cargando, setCargando] = useState(false);

  useEffect(() => {
    api
      .get('/identidad/yo')
      .then((res) => {
        setIdentidad(res.data);
        // Si la cuenta es de un estudiante, su boletin es el unico que puede
        // consultar: se prefija y no se ofrece cambiarlo.
        if (res.data.estudianteId) {
          setEstudianteId(String(res.data.estudianteId));
        } else if (res.data.estudiantesTutelados?.length === 1) {
          setEstudianteId(String(res.data.estudiantesTutelados[0]));
        }
      })
      .catch(() => setError('No fue posible consultar su identidad.'));
  }, []);

  const esEstudiante = Boolean(identidad?.estudianteId);
  const tutelados = identidad?.estudiantesTutelados ?? [];

  async function consultar(evento) {
    evento.preventDefault();
    setError(null);
    setBoletin(null);
    setCargando(true);
    try {
      const { data } = await api.get('/notas/boletin', {
        params: { estudianteId, cursoId, periodoAcademicoId: periodoId },
      });
      setBoletin(data);
    } catch (err) {
      // RB-19 responde aqui cuando el corte no esta cerrado, y RNF-07 cuando el
      // estudiante queda fuera de alcance. En ambos casos se muestra el mensaje
      // del backend (RNF-10) en vez de interpretarlo.
      setError(
        err.response?.data?.mensaje ??
          'No fue posible generar el boletin. Verifique los datos e intentelo de nuevo.'
      );
    } finally {
      setCargando(false);
    }
  }

  return (
    <Layout>
      <h2 className="text-2xl font-semibold text-gray-900">Boletin de calificaciones</h2>
      <p className="mt-1 text-sm text-gray-600">
        El boletin solo puede generarse cuando coordinacion ha cerrado el corte
        del curso (RB-19).
      </p>

      <form onSubmit={consultar} className="mt-6 rounded-2xl bg-white p-6 shadow">
        <div className="grid gap-4 sm:grid-cols-3">
          {esEstudiante ? (
            <Campo etiqueta="Estudiante">
              <p className="mt-1 py-2 text-gray-900">El suyo (#{identidad.estudianteId})</p>
            </Campo>
          ) : tutelados.length > 1 ? (
            <Campo etiqueta="Estudiante" htmlFor="estudianteId">
              <select
                id="estudianteId"
                value={estudianteId}
                onChange={(e) => setEstudianteId(e.target.value)}
                required
                className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2 text-gray-900
                           focus:border-educk-500 focus:outline-none focus:ring-2 focus:ring-educk-500"
              >
                <option value="">Seleccione...</option>
                {tutelados.map((id) => (
                  <option key={id} value={id}>
                    Estudiante #{id}
                  </option>
                ))}
              </select>
            </Campo>
          ) : (
            <Campo etiqueta="Estudiante" htmlFor="estudianteId">
              <Numero id="estudianteId" valor={estudianteId} onChange={setEstudianteId} />
            </Campo>
          )}

          <Campo etiqueta="Curso" htmlFor="cursoId">
            <Numero id="cursoId" valor={cursoId} onChange={setCursoId} />
          </Campo>

          <Campo etiqueta="Periodo academico" htmlFor="periodoId">
            <Numero id="periodoId" valor={periodoId} onChange={setPeriodoId} />
          </Campo>
        </div>

        <button
          type="submit"
          disabled={cargando}
          className="mt-5 rounded-lg bg-educk-600 px-4 py-2.5 font-semibold text-white
                     hover:bg-educk-700 focus:outline-none focus:ring-2 focus:ring-educk-500
                     focus:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-60"
        >
          {cargando ? 'Generando...' : 'Generar boletin'}
        </button>
      </form>

      {error && (
        <p role="alert" className="mt-6 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700">
          {error}
        </p>
      )}

      {boletin && <TablaBoletin boletin={boletin} />}
    </Layout>
  );
}

function TablaBoletin({ boletin }) {
  return (
    <section className="mt-6 rounded-2xl bg-white p-6 shadow">
      <header className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h3 className="text-lg font-semibold text-gray-900">
            Estudiante #{boletin.estudianteId}
          </h3>
          <p className="text-sm text-gray-600">
            Curso #{boletin.cursoId} - Periodo #{boletin.periodoAcademicoId}
          </p>
        </div>
        {/* RB-12: la aprobacion del periodo exige TODAS las materias del plan. */}
        <span
          className={
            boletin.aprobado
              ? 'rounded-full bg-green-100 px-3 py-1 text-sm font-semibold text-green-800'
              : 'rounded-full bg-red-100 px-3 py-1 text-sm font-semibold text-red-800'
          }
        >
          {boletin.aprobado ? 'Periodo aprobado' : 'Periodo no aprobado'}
        </span>
      </header>

      <div className="mt-5 overflow-x-auto">
        <table className="w-full text-left text-sm">
          <caption className="sr-only">
            Promedio por materia del plan de estudios
          </caption>
          <thead>
            <tr className="border-b border-gray-200 text-xs uppercase tracking-wide text-gray-500">
              <th scope="col" className="py-2 pr-4">Materia</th>
              <th scope="col" className="py-2 pr-4">Promedio</th>
              <th scope="col" className="py-2">Estado</th>
            </tr>
          </thead>
          <tbody>
            {boletin.materias.map((m) => (
              <tr key={m.materiaId} className="border-b border-gray-100">
                <th scope="row" className="py-3 pr-4 font-normal text-gray-900">
                  Materia #{m.materiaId}
                </th>
                <td className="py-3 pr-4 tabular-nums text-gray-900">
                  {m.sinCalificar ? '-' : m.promedio}
                </td>
                <td className="py-3">
                  <div className="flex flex-wrap gap-2">
                    {m.sinCalificar ? (
                      // RB-11 / RB-12: la materia esta en el plan pero no tiene
                      // ninguna nota. Es informacion que falta, no un cero.
                      <Etiqueta tono="ambar">Sin calificar</Etiqueta>
                    ) : m.aprobada ? (
                      <Etiqueta tono="verde">Aprobada</Etiqueta>
                    ) : (
                      <Etiqueta tono="rojo">No aprobada</Etiqueta>
                    )}
                    {/* RB-04: se informa, no reprueba. Por eso es una etiqueta
                        aparte y no sustituye al estado de la materia. */}
                    {m.pierdeDerechoAEvaluacion && (
                      <Etiqueta tono="ambar">Sin derecho a evaluacion</Etiqueta>
                    )}
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <p className="mt-5 text-sm text-gray-700">
        Promedio general:{' '}
        <span className="font-semibold tabular-nums text-gray-900">
          {boletin.promedioGeneral}
        </span>
      </p>

      {boletin.materias.some((m) => m.pierdeDerechoAEvaluacion) && (
        <p className="mt-3 rounded-lg bg-amber-50 px-4 py-3 text-xs text-amber-900">
          Las materias marcadas no alcanzaron el minimo de asistencia
          institucional (RB-04). La nota obtenida sigue siendo valida; la perdida
          del derecho a evaluacion ordinaria se gestiona con coordinacion.
        </p>
      )}
    </section>
  );
}

function Campo({ etiqueta, htmlFor, children }) {
  return (
    <div>
      <label htmlFor={htmlFor} className="block text-sm font-medium text-gray-800">
        {etiqueta}
      </label>
      {children}
    </div>
  );
}

function Numero({ id, valor, onChange }) {
  return (
    <input
      id={id}
      type="number"
      min="1"
      required
      value={valor}
      onChange={(e) => onChange(e.target.value)}
      className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2 text-gray-900
                 focus:border-educk-500 focus:outline-none focus:ring-2 focus:ring-educk-500"
    />
  );
}

function Etiqueta({ tono, children }) {
  const tonos = {
    verde: 'bg-green-100 text-green-800',
    rojo: 'bg-red-100 text-red-800',
    ambar: 'bg-amber-100 text-amber-900',
  };
  return (
    <span className={`rounded-full px-2.5 py-0.5 text-xs font-medium ${tonos[tono]}`}>
      {children}
    </span>
  );
}
