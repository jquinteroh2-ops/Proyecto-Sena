import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import api from '../api/axios';
import { GraficoBarras } from '../components/charts';
import Layout from '../components/Layout';
import { Alerta, Boton, Campo, Etiqueta, Selector, Tarjeta } from '../components/ui';
import { useCatalogos } from '../hooks/useCatalogos';

// Reportes institucionales (RF-47..RF-51). Competencia de Coordinacion y
// Rectoria (el backend ya lo restringe asi; ver la nota de permisos en
// Asignaciones.jsx sobre por que Rectoria consulta pero no gestiona).
export default function Reportes() {
  const [params] = useSearchParams();
  const { cursos, periodos, cargandoCatalogos } = useCatalogos({ cursos: true, periodos: true });

  const [indicadores, setIndicadores] = useState(null);
  const [errorIndicadores, setErrorIndicadores] = useState(null);

  const [cursoId, setCursoId] = useState(params.get('cursoId') ?? '');
  const [periodoId, setPeriodoId] = useState(params.get('periodoId') ?? '');
  const [rendimiento, setRendimiento] = useState(null);
  const [error, setError] = useState(null);
  const [cargando, setCargando] = useState(false);
  const [descargando, setDescargando] = useState(null);

  useEffect(() => {
    api
      .get('/reportes/indicadores')
      .then((res) => setIndicadores(res.data))
      .catch((err) => setErrorIndicadores(err.response?.data?.mensaje ?? 'No fue posible cargar los indicadores.'));
  }, []);

  async function consultar(evento) {
    evento.preventDefault();
    if (!cursoId || !periodoId) return;
    setError(null);
    setCargando(true);
    try {
      const { data } = await api.get('/reportes/rendimiento', {
        params: { cursoId, periodoAcademicoId: periodoId },
      });
      setRendimiento(data);
    } catch (err) {
      setError(err.response?.data?.mensaje ?? 'No fue posible generar el reporte.');
    } finally {
      setCargando(false);
    }
  }

  async function descargar(formato) {
    setDescargando(formato);
    try {
      const { data } = await api.get(`/reportes/rendimiento/${formato}`, {
        params: { cursoId, periodoAcademicoId: periodoId },
        responseType: 'blob',
      });
      const url = URL.createObjectURL(data);
      const enlace = document.createElement('a');
      enlace.href = url;
      enlace.download = `rendimiento_curso_${cursoId}.${formato === 'excel' ? 'xlsx' : 'pdf'}`;
      document.body.appendChild(enlace);
      enlace.click();
      enlace.remove();
      URL.revokeObjectURL(url);
    } catch {
      setError('No fue posible descargar el archivo.');
    } finally {
      setDescargando(null);
    }
  }

  const opcionesCursos = cursos.map((c) => ({ value: c.id, label: c.nombre }));
  const opcionesPeriodos = periodos.map((p) => ({ value: p.id, label: p.nombre }));

  return (
    <Layout>
      <h2 className="text-2xl font-semibold text-gray-900">Reportes</h2>
      <p className="mt-1 text-sm text-gray-600">
        Indicadores institucionales (RF-51) y rendimiento por salon, exportable
        en PDF y Excel (RF-49, RF-50).
      </p>

      {errorIndicadores && <div className="mt-4"><Alerta>{errorIndicadores}</Alerta></div>}
      {indicadores && (
        <div className="mt-6 grid gap-4 sm:grid-cols-4">
          <Indicador etiqueta="Promedio institucional" valor={indicadores.promedioGeneralInstitucional} />
          <Indicador
            etiqueta="Asistencia promedio"
            valor={`${indicadores.asistenciaPromedioInstitucional.toFixed(1)}%`}
          />
          <Indicador etiqueta="Estudiantes en riesgo" valor={indicadores.estudiantesEnRiesgo} alerta />
          <Indicador etiqueta="Con notas registradas" valor={indicadores.totalEstudiantesConNotas} />
        </div>
      )}

      <Tarjeta className="mt-6">
        <form onSubmit={consultar} className="grid gap-4 sm:grid-cols-3">
          <Campo etiqueta="Salon" htmlFor="cursoId">
            <Selector id="cursoId" valor={cursoId} onChange={setCursoId} opciones={opcionesCursos} />
          </Campo>
          <Campo etiqueta="Periodo" htmlFor="periodoId">
            <Selector id="periodoId" valor={periodoId} onChange={setPeriodoId} opciones={opcionesPeriodos} />
          </Campo>
          <div className="flex items-end">
            <Boton disabled={cargando || cargandoCatalogos}>
              {cargando ? 'Generando...' : 'Generar reporte'}
            </Boton>
          </div>
        </form>
      </Tarjeta>

      {error && <div className="mt-4"><Alerta>{error}</Alerta></div>}

      {rendimiento && (
        <Tarjeta className="mt-6">
          <header className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <h3 className="text-base font-semibold text-gray-900">
                Rendimiento del salon — promedio {rendimiento.promedioCurso}
              </h3>
              <p className="text-sm text-gray-600">{rendimiento.estudiantes.length} estudiantes</p>
            </div>
            <div className="flex gap-2">
              <Boton
                type="button"
                variante="secundario"
                disabled={descargando !== null}
                onClick={() => descargar('pdf')}
              >
                {descargando === 'pdf' ? 'Descargando...' : 'Descargar PDF'}
              </Boton>
              <Boton
                type="button"
                variante="secundario"
                disabled={descargando !== null}
                onClick={() => descargar('excel')}
              >
                {descargando === 'excel' ? 'Descargando...' : 'Descargar Excel'}
              </Boton>
            </div>
          </header>

          <div className="mt-4">
            <GraficoBarras
              barras={rendimiento.estudiantes.map((e) => ({
                etiqueta: e.nombreCompleto,
                valor: Number(e.promedioGeneral),
              }))}
            />
          </div>

          <div className="mt-4 overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead>
                <tr className="border-b border-gray-200 text-xs uppercase tracking-wide text-gray-500">
                  <th scope="col" className="py-2 pr-4">Estudiante</th>
                  <th scope="col" className="py-2 pr-4">Promedio</th>
                  <th scope="col" className="py-2">Estado</th>
                </tr>
              </thead>
              <tbody>
                {rendimiento.estudiantes.map((e) => (
                  <tr key={e.estudianteId} className="border-b border-gray-100">
                    <td className="py-2 pr-4 text-gray-900">{e.nombreCompleto}</td>
                    <td className="py-2 pr-4 tabular-nums">{e.promedioGeneral}</td>
                    <td className="py-2">
                      <Etiqueta tono={e.aprobado ? 'verde' : 'rojo'}>
                        {e.aprobado ? 'Aprobado' : 'Reprueba'}
                      </Etiqueta>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Tarjeta>
      )}
    </Layout>
  );
}

function Indicador({ etiqueta, valor, alerta = false }) {
  return (
    <Tarjeta className={alerta && Number(valor) > 0 ? 'border border-amber-300' : ''}>
      <p className="text-xs uppercase tracking-wide text-gray-500">{etiqueta}</p>
      <p className="mt-1 text-2xl font-semibold tabular-nums text-gray-900">{valor}</p>
    </Tarjeta>
  );
}
