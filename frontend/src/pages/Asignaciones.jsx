import { useState } from 'react';
import api from '../api/axios';
import Layout from '../components/Layout';
import { Alerta, Boton, Campo, Selector, Tarjeta } from '../components/ui';
import { useCatalogos } from '../hooks/useCatalogos';

// Asignacion de carga academica (RF-14, RF-15, RF-16). Es la pantalla donde
// se decide que docente dicta que materia en que salon, y quien dirige cada
// grupo.
//
// PERMISOS (RB-16, RB-02): el backend solo autoriza esta escritura a
// Coordinador Academico y Administrador. Rectoria tiene vision institucional
// para CONSULTAR todo (cursos, notas, horarios, reportes), pero no gestiona
// la carga docente por diseno del sistema: es una decision academica
// operativa, no de direccion. Si se intenta sin el rol correcto, el backend
// devuelve 403 y aqui se muestra tal cual (RNF-10).
export default function Asignaciones() {
  const { cursos, materias, docentes, periodos, cargandoCatalogos, errorCatalogos } = useCatalogos({
    cursos: true,
    materias: true,
    docentes: true,
    periodos: true,
  });

  const [docenteId, setDocenteId] = useState('');
  const [materiaId, setMateriaId] = useState('');
  const [cursoId, setCursoId] = useState('');
  const [periodoId, setPeriodoId] = useState('');
  const [guardando, setGuardando] = useState(false);
  const [error, setError] = useState(null);
  const [aviso, setAviso] = useState(null);

  const [cargaDocenteId, setCargaDocenteId] = useState('');
  const [cargaPeriodoId, setCargaPeriodoId] = useState('');
  const [carga, setCarga] = useState(null);
  const [errorCarga, setErrorCarga] = useState(null);

  const [directorCursoId, setDirectorCursoId] = useState('');
  const [directorDocenteId, setDirectorDocenteId] = useState('');
  const [guardandoDirector, setGuardandoDirector] = useState(false);
  const [avisoDirector, setAvisoDirector] = useState(null);
  const [errorDirector, setErrorDirector] = useState(null);

  const opcionesDocentes = docentes.map((d) => ({ value: d.id, label: `${d.nombres} ${d.apellidos}` }));
  const opcionesMaterias = materias.map((m) => ({ value: m.id, label: m.nombre }));
  const opcionesCursos = cursos.map((c) => ({ value: c.id, label: `${c.nombre} (grado ${c.grado})` }));
  const opcionesPeriodos = periodos.map((p) => ({ value: p.id, label: p.nombre }));

  async function asignar(evento) {
    evento.preventDefault();
    setError(null);
    setAviso(null);
    setGuardando(true);
    try {
      await api.post('/asignaciones', {
        docenteId: Number(docenteId),
        materiaId: Number(materiaId),
        cursoId: Number(cursoId),
        periodoAcademicoId: Number(periodoId),
      });
      setAviso('Materia asignada correctamente.');
      setDocenteId('');
      setMateriaId('');
      setCursoId('');
    } catch (err) {
      // RB-16 (choque de carga) y el 403 por rol responden aqui (RNF-10).
      setError(err.response?.data?.mensaje ?? 'No fue posible asignar la materia.');
    } finally {
      setGuardando(false);
    }
  }

  async function consultarCarga(evento) {
    evento.preventDefault();
    setErrorCarga(null);
    setCarga(null);
    if (!cargaDocenteId || !cargaPeriodoId) return;
    try {
      const { data } = await api.get('/asignaciones/carga', {
        params: { docenteId: cargaDocenteId, periodoAcademicoId: cargaPeriodoId },
      });
      setCarga(data);
    } catch (err) {
      setErrorCarga(err.response?.data?.mensaje ?? 'No fue posible consultar la carga academica.');
    }
  }

  async function designarDirector(evento) {
    evento.preventDefault();
    setErrorDirector(null);
    setAvisoDirector(null);
    setGuardandoDirector(true);
    try {
      await api.post(`/asignaciones/cursos/${directorCursoId}/director`, {
        docenteId: Number(directorDocenteId),
      });
      setAvisoDirector('Director de grupo designado.');
    } catch (err) {
      setErrorDirector(err.response?.data?.mensaje ?? 'No fue posible designar el director.');
    } finally {
      setGuardandoDirector(false);
    }
  }

  return (
    <Layout>
      <h2 className="text-2xl font-semibold text-gray-900">Asignacion de clases</h2>
      <p className="mt-1 text-sm text-gray-600">
        Carga academica de los docentes: que materia dictan en que salon
        (RF-14), y quien dirige cada grupo (RF-16).
      </p>

      {errorCatalogos && <Alerta>{errorCatalogos}</Alerta>}

      <div className="mt-6 grid gap-6 lg:grid-cols-2">
        <Tarjeta>
          <h3 className="text-base font-semibold text-gray-900">Asignar materia a docente</h3>
          <form onSubmit={asignar} className="mt-4 space-y-4">
            <Campo etiqueta="Docente" htmlFor="docenteId">
              <Selector id="docenteId" valor={docenteId} onChange={setDocenteId} opciones={opcionesDocentes} />
            </Campo>
            <Campo etiqueta="Materia" htmlFor="materiaId">
              <Selector id="materiaId" valor={materiaId} onChange={setMateriaId} opciones={opcionesMaterias} />
            </Campo>
            <Campo etiqueta="Salon" htmlFor="cursoId">
              <Selector id="cursoId" valor={cursoId} onChange={setCursoId} opciones={opcionesCursos} />
            </Campo>
            <Campo etiqueta="Periodo" htmlFor="periodoId">
              <Selector id="periodoId" valor={periodoId} onChange={setPeriodoId} opciones={opcionesPeriodos} />
            </Campo>
            {error && <Alerta>{error}</Alerta>}
            {aviso && <Alerta tipo="exito">{aviso}</Alerta>}
            <Boton disabled={guardando || cargandoCatalogos}>
              {guardando ? 'Asignando...' : 'Asignar'}
            </Boton>
          </form>
        </Tarjeta>

        <Tarjeta>
          <h3 className="text-base font-semibold text-gray-900">Director de grupo</h3>
          <form onSubmit={designarDirector} className="mt-4 space-y-4">
            <Campo etiqueta="Salon" htmlFor="directorCursoId">
              <Selector
                id="directorCursoId"
                valor={directorCursoId}
                onChange={setDirectorCursoId}
                opciones={opcionesCursos}
              />
            </Campo>
            <Campo etiqueta="Docente" htmlFor="directorDocenteId">
              <Selector
                id="directorDocenteId"
                valor={directorDocenteId}
                onChange={setDirectorDocenteId}
                opciones={opcionesDocentes}
              />
            </Campo>
            {errorDirector && <Alerta>{errorDirector}</Alerta>}
            {avisoDirector && <Alerta tipo="exito">{avisoDirector}</Alerta>}
            <Boton disabled={guardandoDirector || cargandoCatalogos}>
              {guardandoDirector ? 'Guardando...' : 'Designar director'}
            </Boton>
          </form>
        </Tarjeta>

        <Tarjeta className="lg:col-span-2">
          <h3 className="text-base font-semibold text-gray-900">Consultar carga academica</h3>
          <form onSubmit={consultarCarga} className="mt-4 grid gap-4 sm:grid-cols-3">
            <Campo etiqueta="Docente" htmlFor="cargaDocenteId">
              <Selector
                id="cargaDocenteId"
                valor={cargaDocenteId}
                onChange={setCargaDocenteId}
                opciones={opcionesDocentes}
              />
            </Campo>
            <Campo etiqueta="Periodo" htmlFor="cargaPeriodoId">
              <Selector
                id="cargaPeriodoId"
                valor={cargaPeriodoId}
                onChange={setCargaPeriodoId}
                opciones={opcionesPeriodos}
              />
            </Campo>
            <div className="flex items-end">
              <Boton type="submit" variante="secundario">Consultar</Boton>
            </div>
          </form>

          {errorCarga && <div className="mt-4"><Alerta>{errorCarga}</Alerta></div>}
          {carga && (
            <p className="mt-4 text-sm text-gray-700">
              Materias asignadas:{' '}
              <span className="font-semibold tabular-nums">{carga.totalMaterias}</span> · Horas
              semanales:{' '}
              <span className="font-semibold tabular-nums">{carga.totalHorasSemanales}</span>{' '}
              (RB-09, tope institucional configurable en Parametros)
            </p>
          )}
        </Tarjeta>
      </div>
    </Layout>
  );
}
