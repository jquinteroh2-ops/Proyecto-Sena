import { useState } from 'react';
import api from '../api/axios';
import Layout from '../components/Layout';
import { Alerta, Boton, Campo, Etiqueta, Selector, Tarjeta, Texto } from '../components/ui';
import { NOMBRES_DIA, useCatalogos } from '../hooks/useCatalogos';

const ESTADOS = ['PRESENTE', 'AUSENTE', 'TARDE'];
const TONO_ESTADO = { PRESENTE: 'verde', AUSENTE: 'rojo', TARDE: 'ambar' };

// Registro de asistencia diaria (RF-26, RB-06). Se toma sobre un bloque de
// horario concreto (dia+hora+materia), tal como lo exige el backend: la
// asistencia es "de esa clase, ese dia", no solo "del curso ese dia".
export default function Asistencia() {
  const { cursos, materias, periodos, cargandoCatalogos, errorCatalogos } = useCatalogos({
    cursos: true, materias: true, periodos: true,
  });
  const [bloques, setBloques] = useState([]);

  const [cursoId, setCursoId] = useState('');
  const [materiaId, setMateriaId] = useState('');
  const [periodoId, setPeriodoId] = useState('');
  const [bloqueId, setBloqueId] = useState('');
  const [fecha, setFecha] = useState(() => new Date().toISOString().slice(0, 10));

  const [estudiantes, setEstudiantes] = useState([]);
  const [estados, setEstados] = useState({}); // estudianteId -> EstadoAsistencia
  const [cargando, setCargando] = useState(false);
  const [guardando, setGuardando] = useState(false);
  const [error, setError] = useState(null);
  const [aviso, setAviso] = useState(null);

  async function cargarSalon(evento) {
    evento.preventDefault();
    if (!cursoId) return;
    setError(null);
    setAviso(null);
    setCargando(true);
    try {
      const [{ data: lista }, { data: bloquesLista }] = await Promise.all([
        api.get(`/cursos/${cursoId}/estudiantes`),
        api.get('/horarios/bloques'),
      ]);
      const activos = lista.filter((e) => e.estado === 'ACTIVO');
      setEstudiantes(activos);
      setBloques(bloquesLista);
      setEstados(Object.fromEntries(activos.map((e) => [e.estudianteId, 'PRESENTE'])));
    } catch (err) {
      setError(err.response?.data?.mensaje ?? 'No fue posible cargar el salón.');
    } finally {
      setCargando(false);
    }
  }

  async function guardarAsistencia() {
    if (!bloqueId || !materiaId || !periodoId || estudiantes.length === 0) return;
    setGuardando(true);
    setError(null);
    setAviso(null);
    try {
      await api.post('/asistencia', {
        cursoId: Number(cursoId),
        materiaId: Number(materiaId),
        bloqueId: Number(bloqueId),
        periodoAcademicoId: Number(periodoId),
        fecha,
        registros: estudiantes.map((e) => ({ estudianteId: e.estudianteId, estado: estados[e.estudianteId] })),
      });
      setAviso('Asistencia registrada correctamente (RF-26).');
    } catch (err) {
      // RB-06 (asistencia duplicada para ese bloque/fecha) responde aqui.
      setError(err.response?.data?.mensaje ?? 'No fue posible registrar la asistencia.');
    } finally {
      setGuardando(false);
    }
  }

  const opcionesCursos = cursos.map((c) => ({ value: c.id, label: c.nombre }));
  const opcionesMaterias = materias.map((m) => ({ value: m.id, label: m.nombre }));
  const opcionesPeriodos = periodos.map((p) => ({ value: p.id, label: p.nombre }));
  const opcionesBloques = bloques.map((b) => ({
    value: b.id, label: `${NOMBRES_DIA[b.dia]} ${b.horaInicio.slice(0, 5)}-${b.horaFin.slice(0, 5)}`,
  }));

  return (
    <Layout>
      <h2 className="text-2xl font-semibold text-gray-900">Asistencia</h2>
      <p className="mt-1 text-sm text-gray-600">
        Registro diario de asistencia por salón y bloque de clase (RF-26).
      </p>

      {errorCatalogos && <div className="mt-4"><Alerta>{errorCatalogos}</Alerta></div>}

      <Tarjeta className="mt-6">
        <form onSubmit={cargarSalon} className="grid gap-4 sm:grid-cols-5">
          <Campo etiqueta="Salón" htmlFor="cursoId">
            <Selector id="cursoId" valor={cursoId} onChange={setCursoId} opciones={opcionesCursos} />
          </Campo>
          <Campo etiqueta="Materia" htmlFor="materiaId">
            <Selector id="materiaId" valor={materiaId} onChange={setMateriaId} opciones={opcionesMaterias} />
          </Campo>
          <Campo etiqueta="Periodo" htmlFor="periodoId">
            <Selector id="periodoId" valor={periodoId} onChange={setPeriodoId} opciones={opcionesPeriodos} />
          </Campo>
          <Campo etiqueta="Fecha" htmlFor="fecha">
            <Texto id="fecha" tipo="date" valor={fecha} onChange={setFecha} />
          </Campo>
          <div className="flex items-end">
            <Boton disabled={cargando || cargandoCatalogos}>{cargando ? 'Cargando...' : 'Cargar salón'}</Boton>
          </div>
        </form>
      </Tarjeta>

      {error && <div className="mt-4"><Alerta>{error}</Alerta></div>}
      {aviso && <div className="mt-4"><Alerta tipo="exito">{aviso}</Alerta></div>}

      {estudiantes.length > 0 && (
        <Tarjeta className="mt-6">
          <Campo etiqueta="Bloque de clase" htmlFor="bloqueId">
            <Selector id="bloqueId" valor={bloqueId} onChange={setBloqueId} opciones={opcionesBloques} />
          </Campo>

          <div className="mt-4 overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead>
                <tr className="border-b border-gray-200 text-xs uppercase tracking-wide text-gray-500">
                  <th scope="col" className="py-2 pr-4">Estudiante</th>
                  <th scope="col" className="py-2">Estado</th>
                </tr>
              </thead>
              <tbody>
                {estudiantes.map((e) => (
                  <tr key={e.estudianteId} className="border-b border-gray-100">
                    <td className="py-2 pr-4 text-gray-900">{e.nombres} {e.apellidos}</td>
                    <td className="py-2">
                      <div className="flex gap-1.5">
                        {ESTADOS.map((estado) => {
                          const activo = estados[e.estudianteId] === estado;
                          return (
                            <button
                              key={estado}
                              type="button"
                              onClick={() => setEstados((prev) => ({ ...prev, [e.estudianteId]: estado }))}
                              className={activo ? undefined : 'opacity-40 hover:opacity-70'}
                            >
                              <Etiqueta tono={TONO_ESTADO[estado]}>{estado}</Etiqueta>
                            </button>
                          );
                        })}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="mt-5">
            <Boton type="button" onClick={guardarAsistencia} disabled={guardando || !bloqueId || !materiaId || !periodoId}>
              {guardando ? 'Guardando...' : 'Guardar asistencia'}
            </Boton>
          </div>
        </Tarjeta>
      )}
    </Layout>
  );
}
