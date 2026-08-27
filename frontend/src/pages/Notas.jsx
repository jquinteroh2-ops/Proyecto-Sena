import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import api from '../api/axios';
import Layout from '../components/Layout';
import ImportadorExcel, { BotonPlantilla } from '../components/ImportadorExcel';
import { Alerta, Boton, Campo, Etiqueta, Selector, Tarjeta } from '../components/ui';
import { NOMBRES_TIPO_EVALUACION, useCatalogos } from '../hooks/useCatalogos';

const TIPOS = Object.keys(NOMBRES_TIPO_EVALUACION);

// Planilla de notas por salon (RF-20, RF-31, RF-32, RF-33). Se elige
// curso + materia + periodo + tipo de evaluacion una sola vez y se ve el
// salon completo: grado, listado de estudiantes y, para cada uno, su nota de
// ese tipo de evaluacion y su promedio ponderado ya calculado (RB-07).
//
// DECISION DE DISENO: registrar y editar son la misma casilla. Si el
// estudiante ya tiene una nota de ese tipo en ese periodo se PUT, si no existe
// se POST; quien califica no necesita saber cual de las dos operaciones es.
export default function Notas() {
  const [params] = useSearchParams();
  const { cursos, materias, cargandoCatalogos, errorCatalogos } = useCatalogos({
    cursos: true,
    materias: true,
  });

  const [cursoId, setCursoId] = useState(params.get('cursoId') ?? '');
  const [materiaId, setMateriaId] = useState('');
  const [periodoId, setPeriodoId] = useState(params.get('periodoId') ?? '');
  const [tipo, setTipo] = useState('QUIZ');

  const [curso, setCurso] = useState(null);
  const [estudiantes, setEstudiantes] = useState([]);
  const [notas, setNotas] = useState({}); // estudianteId -> { [tipo]: CalificacionDto }
  const [promedios, setPromedios] = useState({}); // estudianteId -> PromedioDto
  const [borradores, setBorradores] = useState({}); // estudianteId -> string
  const [guardandoId, setGuardandoId] = useState(null);
  const [error, setError] = useState(null);
  const [cargando, setCargando] = useState(false);

  useEffect(() => {
    if (cursoId && cursos.length > 0) {
      setCurso(cursos.find((c) => String(c.id) === String(cursoId)) ?? null);
    }
  }, [cursoId, cursos]);

  async function cargarSalon(evento) {
    evento?.preventDefault();
    if (!cursoId || !materiaId || !periodoId) return;
    setError(null);
    setCargando(true);
    try {
      const { data: listaEstudiantes } = await api.get(`/cursos/${cursoId}/estudiantes`);
      const activos = listaEstudiantes.filter((e) => e.estado === 'ACTIVO');
      setEstudiantes(activos);

      const resultados = await Promise.all(
        activos.map((e) =>
          api
            .get('/calificaciones/promedio', {
              params: { estudianteId: e.estudianteId, materiaId, periodoAcademicoId: periodoId },
            })
            .then((res) => ({ id: e.estudianteId, promedio: res.data }))
            .catch(() => ({ id: e.estudianteId, promedio: null }))
        )
      );
      const nuevoPromedios = {};
      const nuevasNotas = {};
      const nuevosBorradores = {};
      resultados.forEach(({ id, promedio }) => {
        nuevoPromedios[id] = promedio;
        const detalleTipo = promedio?.detalle?.find((d) => d.tipo === tipo);
        nuevasNotas[id] = detalleTipo ?? null;
        nuevosBorradores[id] = '';
      });
      setPromedios(nuevoPromedios);
      setNotas(nuevasNotas);
      setBorradores(nuevosBorradores);
    } catch (err) {
      setError(err.response?.data?.mensaje ?? 'No fue posible cargar el salon.');
    } finally {
      setCargando(false);
    }
  }

  async function guardarNota(estudianteId) {
    const valor = borradores[estudianteId];
    if (valor === undefined || valor === '') return;
    setGuardandoId(estudianteId);
    setError(null);
    try {
      await api.post('/calificaciones', {
        estudianteId,
        materiaId: Number(materiaId),
        cursoId: Number(cursoId),
        periodoAcademicoId: Number(periodoId),
        tipo,
        valor: Number(valor),
      });
      const { data: promedio } = await api.get('/calificaciones/promedio', {
        params: { estudianteId, materiaId, periodoAcademicoId: periodoId },
      });
      setPromedios((prev) => ({ ...prev, [estudianteId]: promedio }));
      setBorradores((prev) => ({ ...prev, [estudianteId]: '' }));
    } catch (err) {
      // RB-03 (escala) y RB-07 (ponderacion sin configurar) responden aqui.
      setError(err.response?.data?.mensaje ?? 'No fue posible guardar la nota.');
    } finally {
      setGuardandoId(null);
    }
  }

  const opcionesCursos = cursos.map((c) => ({ value: c.id, label: `${c.nombre} (grado ${c.grado})` }));
  const opcionesMaterias = materias.map((m) => ({ value: m.id, label: m.nombre }));

  return (
    <Layout>
      <h2 className="text-2xl font-semibold text-gray-900">Notas por salon</h2>
      <p className="mt-1 text-sm text-gray-600">
        Seleccione salon, materia, periodo y tipo de evaluacion para ver el
        listado completo y registrar la nota de cada estudiante.
      </p>

      {errorCatalogos && <Alerta>{errorCatalogos}</Alerta>}

      <Tarjeta className="mt-6">
        <form onSubmit={cargarSalon} className="grid gap-4 sm:grid-cols-4">
          <Campo etiqueta="Salon" htmlFor="cursoId">
            <Selector id="cursoId" valor={cursoId} onChange={setCursoId} opciones={opcionesCursos} />
          </Campo>
          <Campo etiqueta="Materia" htmlFor="materiaId">
            <Selector id="materiaId" valor={materiaId} onChange={setMateriaId} opciones={opcionesMaterias} />
          </Campo>
          <Campo etiqueta="Periodo" htmlFor="periodoId">
            <input
              id="periodoId"
              type="number"
              min="1"
              required
              value={periodoId}
              onChange={(e) => setPeriodoId(e.target.value)}
              className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2 text-gray-900
                         focus:border-educk-500 focus:outline-none focus:ring-2 focus:ring-educk-500"
            />
          </Campo>
          <Campo etiqueta="Tipo de evaluacion" htmlFor="tipo">
            <Selector
              id="tipo"
              valor={tipo}
              onChange={setTipo}
              required={false}
              opciones={TIPOS.map((t) => ({ value: t, label: NOMBRES_TIPO_EVALUACION[t] }))}
            />
          </Campo>
          <div className="sm:col-span-4">
            <Boton disabled={cargando || cargandoCatalogos}>
              {cargando ? 'Cargando...' : 'Cargar salon'}
            </Boton>
          </div>
        </form>
      </Tarjeta>

      {error && <div className="mt-4"><Alerta>{error}</Alerta></div>}

      {curso && estudiantes.length > 0 && (
        <Tarjeta className="mt-6">
          <header className="mb-4">
            <h3 className="text-lg font-semibold text-gray-900">{curso.nombre}</h3>
            <p className="text-sm text-gray-600">
              Grado {curso.grado} · {curso.nivel} · {curso.jornada} · {estudiantes.length} estudiantes
            </p>
          </header>

          <div className="mb-4">
            <div className="mb-2 flex items-center justify-between">
              <p className="text-sm font-medium text-gray-800">
                Cargar notas de {NOMBRES_TIPO_EVALUACION[tipo]} desde Excel
              </p>
              <BotonPlantilla
                nombre="plantilla_notas"
                columnas={['documento', 'valor']}
                filaEjemplo={{ documento: estudiantes[0]?.documento ?? '3000001', valor: 4.5 }}
              />
            </div>
            <ImportadorExcel
              titulo="Notas por documento del estudiante"
              columnas={['documento', 'valor']}
              procesarFila={async (fila) => {
                const doc = String(fila.documento).trim();
                const est = estudiantes.find((e) => String(e.documento).trim() === doc);
                if (!est) throw new Error(`No hay un estudiante activo en este salón con documento ${doc}.`);
                const valor = Number(fila.valor);
                if (Number.isNaN(valor)) throw new Error('La columna "valor" no es un número.');
                await api.post('/calificaciones', {
                  estudianteId: est.estudianteId,
                  materiaId: Number(materiaId),
                  cursoId: Number(cursoId),
                  periodoAcademicoId: Number(periodoId),
                  tipo,
                  valor,
                });
              }}
            />
            <button
              type="button"
              onClick={cargarSalon}
              className="mt-2 text-xs font-medium text-educk-700 hover:underline"
            >
              Actualizar promedios
            </button>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead>
                <tr className="border-b border-gray-200 text-xs uppercase tracking-wide text-gray-500">
                  <th scope="col" className="py-2 pr-4">Estudiante</th>
                  <th scope="col" className="py-2 pr-4">Nota {NOMBRES_TIPO_EVALUACION[tipo]}</th>
                  <th scope="col" className="py-2 pr-4">Guardar</th>
                  <th scope="col" className="py-2">Promedio materia</th>
                </tr>
              </thead>
              <tbody>
                {estudiantes.map((e) => {
                  const notaActual = notas[e.estudianteId];
                  const promedio = promedios[e.estudianteId];
                  return (
                    <tr key={e.estudianteId} className="border-b border-gray-100">
                      <th scope="row" className="py-3 pr-4 font-normal text-gray-900">
                        {e.nombres} {e.apellidos}
                        <span className="ml-2 text-xs text-gray-500">{e.documento}</span>
                      </th>
                      <td className="py-3 pr-4">
                        <input
                          type="number"
                          min="1"
                          max="5"
                          step="0.1"
                          placeholder={notaActual ? String(notaActual.promedioTipo) : '—'}
                          value={borradores[e.estudianteId] ?? ''}
                          onChange={(ev) =>
                            setBorradores((prev) => ({ ...prev, [e.estudianteId]: ev.target.value }))
                          }
                          className="w-24 rounded-lg border border-gray-300 px-2 py-1.5 text-gray-900
                                     focus:border-educk-500 focus:outline-none focus:ring-2 focus:ring-educk-500"
                        />
                      </td>
                      <td className="py-3 pr-4">
                        <Boton
                          type="button"
                          variante="secundario"
                          disabled={guardandoId === e.estudianteId || !borradores[e.estudianteId]}
                          onClick={() => guardarNota(e.estudianteId)}
                        >
                          {guardandoId === e.estudianteId ? 'Guardando...' : 'Guardar'}
                        </Boton>
                      </td>
                      <td className="py-3 tabular-nums">
                        {promedio ? (
                          <span className="flex items-center gap-2">
                            {promedio.promedio}
                            <Etiqueta tono={promedio.aprobada ? 'verde' : 'rojo'}>
                              {promedio.aprobada ? 'Aprueba' : 'No aprueba'}
                            </Etiqueta>
                          </span>
                        ) : (
                          <span className="text-gray-400">Sin notas</span>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </Tarjeta>
      )}
    </Layout>
  );
}
