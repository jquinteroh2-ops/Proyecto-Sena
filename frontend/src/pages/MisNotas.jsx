import { useEffect, useMemo, useState } from 'react';
import api from '../api/axios';
import { GraficoLinea } from '../components/charts';
import Layout from '../components/Layout';
import { Alerta, Campo, Etiqueta, Selector, Tarjeta } from '../components/ui';
import { NOMBRES_TIPO_EVALUACION } from '../hooks/useCatalogos';

// Estadisticas individuales del estudiante (RF-37): como va su promedio
// periodo a periodo, y el detalle de notas del periodo mas reciente. RF-37
// solo expone el historico crudo (una fila por nota); el promedio por
// periodo y la tendencia se calculan aqui, no en el backend, porque es una
// vista de lectura para quien consulta, no una regla de negocio que otro caso
// de uso necesite reutilizar.
//
// RNF-07: el estudiante ve el suyo; el acudiente elige entre sus tutelados.
export default function MisNotas() {
  const [identidad, setIdentidad] = useState(null);
  const [estudianteId, setEstudianteId] = useState('');
  const [historico, setHistorico] = useState([]);
  const [periodos, setPeriodos] = useState([]);
  const [materias, setMaterias] = useState([]);
  const [error, setError] = useState(null);
  const [cargando, setCargando] = useState(false);

  useEffect(() => {
    api.get('/identidad/yo').then((res) => {
      setIdentidad(res.data);
      if (res.data.estudianteId) setEstudianteId(String(res.data.estudianteId));
      else if (res.data.estudiantesTutelados?.length === 1) {
        setEstudianteId(String(res.data.estudiantesTutelados[0]));
      }
    }).catch(() => setError('No fue posible consultar su identidad.'));

    Promise.all([api.get('/periodos'), api.get('/materias')])
      .then(([p, m]) => { setPeriodos(p.data); setMaterias(m.data); })
      .catch(() => {});
  }, []);

  useEffect(() => {
    if (!estudianteId) return;
    setCargando(true);
    setError(null);
    api
      .get(`/calificaciones/historico/${estudianteId}`)
      .then((res) => setHistorico(res.data))
      .catch((err) => setError(err.response?.data?.mensaje ?? 'No fue posible consultar el historico.'))
      .finally(() => setCargando(false));
  }, [estudianteId]);

  const periodoPorId = useMemo(() => new Map(periodos.map((p) => [p.id, p])), [periodos]);
  const materiaPorId = useMemo(() => new Map(materias.map((m) => [m.id, m])), [materias]);

  const promediosPorPeriodo = useMemo(() => {
    const grupos = new Map();
    historico.forEach((c) => {
      const lista = grupos.get(c.periodoAcademicoId) ?? [];
      lista.push(Number(c.valor));
      grupos.set(c.periodoAcademicoId, lista);
    });
    return [...grupos.entries()]
      .map(([periodoId, valores]) => ({
        periodoId,
        etiqueta: periodoPorId.get(periodoId)?.nombre ?? `Periodo #${periodoId}`,
        valor: valores.reduce((a, b) => a + b, 0) / valores.length,
        fechaInicio: periodoPorId.get(periodoId)?.fechaInicio ?? '',
      }))
      .sort((a, b) => (a.fechaInicio || '').localeCompare(b.fechaInicio || '') || a.periodoId - b.periodoId);
  }, [historico, periodoPorId]);

  const tendencia = useMemo(() => {
    if (promediosPorPeriodo.length < 2) return null;
    const ultimo = promediosPorPeriodo[promediosPorPeriodo.length - 1];
    const anterior = promediosPorPeriodo[promediosPorPeriodo.length - 2];
    const delta = ultimo.valor - anterior.valor;
    return { delta, subio: delta > 0.001, bajo: delta < -0.001 };
  }, [promediosPorPeriodo]);

  const ultimoPeriodo = promediosPorPeriodo[promediosPorPeriodo.length - 1];
  const notasUltimoPeriodo = historico.filter((c) => c.periodoAcademicoId === ultimoPeriodo?.periodoId);

  const esEstudiante = Boolean(identidad?.estudianteId);
  const tutelados = identidad?.estudiantesTutelados ?? [];

  return (
    <Layout>
      <h2 className="text-2xl font-semibold text-gray-900">Mis notas</h2>
      <p className="mt-1 text-sm text-gray-600">
        Evolucion del promedio periodo a periodo y detalle de las notas mas
        recientes.
      </p>

      {!esEstudiante && tutelados.length > 1 && (
        <Tarjeta className="mt-6">
          <Campo etiqueta="Estudiante" htmlFor="estudianteId">
            <Selector
              id="estudianteId"
              valor={estudianteId}
              onChange={setEstudianteId}
              opciones={tutelados.map((id) => ({ value: id, label: `Estudiante #${id}` }))}
            />
          </Campo>
        </Tarjeta>
      )}

      {error && <div className="mt-4"><Alerta>{error}</Alerta></div>}
      {cargando && <p className="mt-6 text-sm text-gray-600">Cargando historico...</p>}

      {!cargando && promediosPorPeriodo.length > 0 && (
        <Tarjeta className="mt-6">
          <header className="flex flex-wrap items-center justify-between gap-3">
            <h3 className="text-base font-semibold text-gray-900">Tendencia del promedio general</h3>
            {tendencia && (
              <span
                className={`flex items-center gap-1 rounded-full px-3 py-1 text-sm font-semibold ${
                  tendencia.subio
                    ? 'bg-green-100 text-green-800'
                    : tendencia.bajo
                      ? 'bg-red-100 text-red-800'
                      : 'bg-gray-100 text-gray-700'
                }`}
              >
                {tendencia.subio ? '▲' : tendencia.bajo ? '▼' : '＝'}{' '}
                {tendencia.subio ? 'Subió' : tendencia.bajo ? 'Bajó' : 'Se mantuvo'}{' '}
                {Math.abs(tendencia.delta).toFixed(2)} pts. vs. el periodo anterior
              </span>
            )}
          </header>

          <div className="mt-4">
            <GraficoLinea puntos={promediosPorPeriodo.map((p) => ({ etiqueta: p.etiqueta, valor: p.valor }))} />
          </div>
        </Tarjeta>
      )}

      {!cargando && ultimoPeriodo && (
        <Tarjeta className="mt-6">
          <h3 className="text-base font-semibold text-gray-900">
            Notas de {ultimoPeriodo.etiqueta}
          </h3>
          <div className="mt-4 overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead>
                <tr className="border-b border-gray-200 text-xs uppercase tracking-wide text-gray-500">
                  <th scope="col" className="py-2 pr-4">Materia</th>
                  <th scope="col" className="py-2 pr-4">Tipo</th>
                  <th scope="col" className="py-2">Nota</th>
                </tr>
              </thead>
              <tbody>
                {notasUltimoPeriodo.map((c) => (
                  <tr key={c.id} className="border-b border-gray-100">
                    <td className="py-2 pr-4 text-gray-900">
                      {materiaPorId.get(c.materiaId)?.nombre ?? `Materia #${c.materiaId}`}
                    </td>
                    <td className="py-2 pr-4 text-gray-600">{NOMBRES_TIPO_EVALUACION[c.tipo]}</td>
                    <td className="py-2 tabular-nums">
                      <Etiqueta tono={Number(c.valor) >= 3 ? 'verde' : 'rojo'}>{c.valor}</Etiqueta>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Tarjeta>
      )}

      {!cargando && historico.length === 0 && !error && (
        <p className="mt-6 text-sm text-gray-600">Aun no hay notas registradas.</p>
      )}
    </Layout>
  );
}
