import { useState } from 'react';
import { Link } from 'react-router-dom';
import api from '../api/axios';
import Layout from '../components/Layout';
import ImportadorExcel, { BotonPlantilla } from '../components/ImportadorExcel';
import { Alerta, Etiqueta, Tarjeta } from '../components/ui';
import { useCatalogos } from '../hooks/useCatalogos';
import { useAuthStore } from '../store/authStore';

const COLUMNAS_ESTUDIANTES = [
  'documento', 'nombres', 'apellidos', 'fechaNacimiento',
  'acudienteNombre', 'acudienteTelefono', 'acudienteParentesco',
];

// Listado institucional de cursos (RF-43..RF-46). Es el punto de entrada al
// resto de la gestion academica: desde cada curso se llega a su horario, sus
// notas y su reporte de rendimiento, en vez de repetir el identificador de
// curso a mano en cada pantalla.
//
// Visible para todo perfil con consulta institucional o de aula (docente
// incluido): el backend ya filtra lo que cada quien puede ver realmente en
// cada endpoint (RB-02); esta pantalla solo organiza la navegacion.
export default function Cursos() {
  const { cursos, cargandoCatalogos, errorCatalogos } = useCatalogos({ cursos: true });
  const [expandido, setExpandido] = useState(null);
  const [estudiantesPorCurso, setEstudiantesPorCurso] = useState({});
  const [cupoPorCurso, setCupoPorCurso] = useState({});
  const [errorDetalle, setErrorDetalle] = useState(null);

  const roles = useAuthStore((s) => s.usuario?.roles) ?? [];
  // RF-06 (registrar estudiante) exige Coordinacion/Rector/Admin; RF-09
  // (matricular) exige Coordinacion/Admin. La importacion hace las dos cosas
  // seguidas por fila, asi que solo se ofrece a quien tiene ambos permisos.
  const puedeImportar = roles.includes('COORDINADOR_ACADEMICO') || roles.includes('ADMINISTRADOR');

  async function cargarDetalle(id) {
    try {
      const [estRes, cupoRes] = await Promise.all([
        api.get(`/cursos/${id}/estudiantes`),
        api.get(`/cursos/${id}/cupo`),
      ]);
      setEstudiantesPorCurso((prev) => ({ ...prev, [id]: estRes.data }));
      setCupoPorCurso((prev) => ({ ...prev, [id]: cupoRes.data }));
    } catch (err) {
      setErrorDetalle(err.response?.data?.mensaje ?? 'No fue posible consultar el curso.');
    }
  }

  async function alternarCurso(id) {
    if (expandido === id) {
      setExpandido(null);
      return;
    }
    setExpandido(id);
    setErrorDetalle(null);
    if (!estudiantesPorCurso[id]) await cargarDetalle(id);
  }

  return (
    <Layout>
      <h2 className="text-2xl font-semibold text-gray-900">Cursos</h2>
      <p className="mt-1 text-sm text-gray-600">
        Salones de la institucion. Abra uno para ver sus estudiantes y el cupo
        disponible (RF-45, RF-46), o vaya directo a su horario, notas o reporte.
      </p>

      {cargandoCatalogos && <p className="mt-6 text-sm text-gray-600">Cargando cursos...</p>}
      {errorCatalogos && <Alerta>{errorCatalogos}</Alerta>}

      <div className="mt-6 space-y-3">
        {cursos.map((curso) => (
          <Tarjeta key={curso.id} className="p-0">
            <button
              type="button"
              onClick={() => alternarCurso(curso.id)}
              className="flex w-full items-center justify-between gap-3 px-6 py-4 text-left"
              aria-expanded={expandido === curso.id}
            >
              <div>
                <p className="font-semibold text-gray-900">{curso.nombre}</p>
                <p className="text-xs text-gray-500">
                  Grado {curso.grado} · {curso.nivel} · {curso.jornada} · cupo {curso.cupoMaximo}
                </p>
              </div>
              <span className="text-sm text-educk-700">
                {expandido === curso.id ? 'Ocultar' : 'Ver detalle'}
              </span>
            </button>

            {expandido === curso.id && (
              <div className="border-t border-gray-100 px-6 py-4">
                {errorDetalle && <Alerta>{errorDetalle}</Alerta>}

                {cupoPorCurso[curso.id] && (
                  <p className="text-sm text-gray-700">
                    Matriculados:{' '}
                    <span className="font-semibold tabular-nums">
                      {cupoPorCurso[curso.id].matriculados}
                    </span>{' '}
                    / {cupoPorCurso[curso.id].cupoMaximo} · Disponibles:{' '}
                    <span className="font-semibold tabular-nums">
                      {cupoPorCurso[curso.id].disponibles}
                    </span>
                  </p>
                )}

                <div className="mt-3 flex flex-wrap gap-2">
                  <Link
                    to={`/horario?cursoId=${curso.id}&periodoId=${curso.periodoAcademicoId}`}
                    className="rounded-lg border border-gray-300 px-3 py-1.5 text-xs font-medium text-gray-800 hover:bg-gray-50"
                  >
                    Ver horario
                  </Link>
                  <Link
                    to={`/notas?cursoId=${curso.id}&periodoId=${curso.periodoAcademicoId}`}
                    className="rounded-lg border border-gray-300 px-3 py-1.5 text-xs font-medium text-gray-800 hover:bg-gray-50"
                  >
                    Ver notas
                  </Link>
                  <Link
                    to={`/reportes?cursoId=${curso.id}&periodoId=${curso.periodoAcademicoId}`}
                    className="rounded-lg border border-gray-300 px-3 py-1.5 text-xs font-medium text-gray-800 hover:bg-gray-50"
                  >
                    Ver reporte
                  </Link>
                </div>

                {puedeImportar && (
                  <div className="mt-4">
                    <div className="mb-2 flex items-center justify-between">
                      <p className="text-sm font-medium text-gray-800">Inscribir estudiantes desde Excel</p>
                      <BotonPlantilla
                        nombre="plantilla_estudiantes"
                        columnas={COLUMNAS_ESTUDIANTES}
                        filaEjemplo={{
                          documento: '3000001', nombres: 'Juan', apellidos: 'Perez',
                          fechaNacimiento: '2013-01-01', acudienteNombre: 'Maria Perez',
                          acudienteTelefono: '3000000000', acudienteParentesco: 'MADRE',
                        }}
                      />
                    </div>
                    <ImportadorExcel
                      titulo={`Estudiantes que se matricularán en ${curso.nombre}`}
                      columnas={COLUMNAS_ESTUDIANTES}
                      procesarFila={async (fila) => {
                        const nuevo = await api.post('/estudiantes', {
                          documento: String(fila.documento),
                          nombres: String(fila.nombres),
                          apellidos: String(fila.apellidos),
                          fechaNacimiento: fila.fechaNacimiento || null,
                          acudienteNombre: fila.acudienteNombre || '',
                          acudienteTelefono: fila.acudienteTelefono ? String(fila.acudienteTelefono) : '',
                          acudienteParentesco: fila.acudienteParentesco || '',
                        });
                        await api.post('/matriculas', {
                          estudianteId: nuevo.data.id,
                          cursoId: curso.id,
                          periodoAcademicoId: curso.periodoAcademicoId,
                        });
                      }}
                    />
                    <button
                      type="button"
                      onClick={() => cargarDetalle(curso.id)}
                      className="mt-2 text-xs font-medium text-educk-700 hover:underline"
                    >
                      Actualizar listado
                    </button>
                  </div>
                )}

                <div className="mt-4 overflow-x-auto">
                  <table className="w-full text-left text-sm">
                    <thead>
                      <tr className="border-b border-gray-200 text-xs uppercase tracking-wide text-gray-500">
                        <th scope="col" className="py-2 pr-4">Documento</th>
                        <th scope="col" className="py-2 pr-4">Estudiante</th>
                        <th scope="col" className="py-2">Estado</th>
                      </tr>
                    </thead>
                    <tbody>
                      {(estudiantesPorCurso[curso.id] ?? []).map((e) => (
                        <tr key={e.estudianteId} className="border-b border-gray-100">
                          <td className="py-2 pr-4 tabular-nums text-gray-700">{e.documento}</td>
                          <td className="py-2 pr-4 text-gray-900">
                            {e.nombres} {e.apellidos}
                          </td>
                          <td className="py-2">
                            <Etiqueta tono={e.estado === 'ACTIVO' ? 'verde' : 'ambar'}>
                              {e.estado}
                            </Etiqueta>
                          </td>
                        </tr>
                      ))}
                      {estudiantesPorCurso[curso.id]?.length === 0 && (
                        <tr>
                          <td colSpan={3} className="py-3 text-gray-500">
                            Sin estudiantes matriculados.
                          </td>
                        </tr>
                      )}
                    </tbody>
                  </table>
                </div>
              </div>
            )}
          </Tarjeta>
        ))}

        {!cargandoCatalogos && cursos.length === 0 && (
          <p className="text-sm text-gray-600">No hay cursos registrados.</p>
        )}
      </div>
    </Layout>
  );
}
