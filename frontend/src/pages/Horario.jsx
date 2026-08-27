import { useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import api from '../api/axios';
import Layout from '../components/Layout';
import { Alerta, Boton, Campo, Selector, Tarjeta } from '../components/ui';
import { NOMBRES_DIA, ORDEN_DIAS, useCatalogos } from '../hooks/useCatalogos';
import { useAuthStore } from '../store/authStore';

// Franjas horarias fijas para la jornada de manana. Son la grilla sobre la
// que se asignan clases con un clic: si el bloque para ese dia y hora aun no
// existe, se crea al vuelo (RF-21) y la asignacion se hace en la misma
// interaccion (RF-22), en vez de que quien asigna tenga que crear el bloque
// primero en un formulario aparte y luego volver a buscarlo en otro.
const HORAS_JORNADA = [
  ['07:00', '08:00'], ['08:00', '09:00'], ['09:00', '10:00'],
  ['10:00', '11:00'], ['11:00', '12:00'], ['12:00', '13:00'],
];
const DIAS_CLASE = ORDEN_DIAS.filter((d) => d !== 'SABADO' && d !== 'DOMINGO');

// Horario interactivo (RF-21..RF-25, RB-18). Dos modos:
// - Consulta: por salon o por docente, solo lectura, disponible para todos.
// - Asignacion: solo Coordinador Academico y Administrador (misma regla que
//   Asignaciones.jsx), sobre la grilla fija de arriba. Cada celda vacia es un
//   boton; al hacer clic se abre el formulario compacto de esa celda.
export default function Horario() {
  const [params] = useSearchParams();
  const usuario = useAuthStore((s) => s.usuario);
  const roles = usuario?.roles ?? [];
  const puedeGestionar = roles.includes('COORDINADOR_ACADEMICO') || roles.includes('ADMINISTRADOR');
  const esDocente = roles.includes('DOCENTE') && !puedeGestionar;

  const { cursos, materias, docentes, periodos, cargandoCatalogos } = useCatalogos({
    cursos: true,
    materias: true,
    docentes: !esDocente,
    periodos: true,
  });

  const [modo, setModo] = useState(puedeGestionar ? 'asignar' : esDocente ? 'docente' : 'curso');
  const [cursoId, setCursoId] = useState(params.get('cursoId') ?? '');
  const [docenteId, setDocenteId] = useState('');
  const [periodoId, setPeriodoId] = useState(params.get('periodoId') ?? '');

  const [bloques, setBloques] = useState([]);
  const [items, setItems] = useState([]);
  const [error, setError] = useState(null);
  const [cargando, setCargando] = useState(false);

  useEffect(() => {
    api.get('/horarios/bloques').then((res) => setBloques(res.data)).catch(() => {});
  }, []);

  useEffect(() => {
    if (!esDocente) return;
    api.get('/identidad/yo').then((res) => {
      if (res.data.docenteId) setDocenteId(String(res.data.docenteId));
    }).catch(() => {});
  }, [esDocente]);

  async function consultar(evento) {
    evento?.preventDefault();
    const idParaConsultar = modo === 'asignar' ? cursoId : modo === 'curso' ? cursoId : docenteId;
    if (!periodoId || !idParaConsultar) return;
    setError(null);
    setCargando(true);
    try {
      const url = modo === 'docente' ? `/horarios/docentes/${docenteId}` : `/horarios/cursos/${cursoId}`;
      const { data } = await api.get(url, { params: { periodoAcademicoId: periodoId } });
      setItems(data);
    } catch (err) {
      setError(err.response?.data?.mensaje ?? 'No fue posible consultar el horario.');
    } finally {
      setCargando(false);
    }
  }

  const bloquePorId = useMemo(() => new Map(bloques.map((b) => [b.id, b])), [bloques]);
  const materiaPorId = useMemo(() => new Map(materias.map((m) => [m.id, m])), [materias]);
  const cursoPorId = useMemo(() => new Map(cursos.map((c) => [c.id, c])), [cursos]);
  const docentePorId = useMemo(() => new Map(docentes.map((d) => [d.id, d])), [docentes]);

  function itemEn(dia, horaInicio) {
    return items.find((it) => {
      const b = bloquePorId.get(it.bloqueId) ?? it;
      return b.dia === dia && b.horaInicio?.slice(0, 5) === horaInicio;
    });
  }

  const opcionesCursos = cursos.map((c) => ({ value: c.id, label: c.nombre }));
  const opcionesDocentes = docentes.map((d) => ({ value: d.id, label: `${d.nombres} ${d.apellidos}` }));
  const opcionesPeriodos = periodos.map((p) => ({ value: p.id, label: p.nombre }));
  const listo = periodoId && (modo === 'docente' ? docenteId : cursoId);

  return (
    <Layout>
      <h2 className="text-2xl font-semibold text-gray-900">Horario</h2>
      <p className="mt-1 text-sm text-gray-600">
        {puedeGestionar
          ? 'Haga clic en una casilla vacía de la grilla para asignar una clase (RF-21, RF-22), o consulte un horario ya armado.'
          : 'Consulte el horario por salón o por docente (RF-24, RF-25).'}
      </p>

      <Tarjeta className="mt-6">
        <form onSubmit={consultar} className="grid gap-4 sm:grid-cols-4">
          <Campo etiqueta="Vista">
            <select
              value={modo}
              onChange={(e) => { setModo(e.target.value); setItems([]); }}
              className="mt-1 w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-gray-900
                         focus:border-educk-500 focus:outline-none focus:ring-2 focus:ring-educk-500"
            >
              {puedeGestionar && <option value="asignar">Asignar clases (salón)</option>}
              {!esDocente && <option value="curso">Consultar salón</option>}
              <option value="docente">{esDocente ? 'Mi horario' : 'Consultar docente'}</option>
            </select>
          </Campo>

          {(modo === 'asignar' || modo === 'curso') ? (
            <Campo etiqueta="Salon" htmlFor="cursoId">
              <Selector id="cursoId" valor={cursoId} onChange={setCursoId} opciones={opcionesCursos} />
            </Campo>
          ) : esDocente ? (
            <Campo etiqueta="Docente">
              <p className="mt-1 py-2 text-gray-900">El suyo</p>
            </Campo>
          ) : (
            <Campo etiqueta="Docente" htmlFor="docenteId">
              <Selector id="docenteId" valor={docenteId} onChange={setDocenteId} opciones={opcionesDocentes} />
            </Campo>
          )}

          <Campo etiqueta="Periodo" htmlFor="periodoId">
            <Selector id="periodoId" valor={periodoId} onChange={setPeriodoId} opciones={opcionesPeriodos} />
          </Campo>

          <div className="flex items-end">
            <Boton disabled={cargando || cargandoCatalogos || !listo}>
              {cargando ? 'Cargando...' : 'Ver'}
            </Boton>
          </div>
        </form>
      </Tarjeta>

      {error && <div className="mt-4"><Alerta>{error}</Alerta></div>}

      {listo && (modo === 'asignar' ? (
        <GrillaAsignable
          key={`${cursoId}-${periodoId}`}
          cursoId={cursoId}
          periodoId={periodoId}
          items={items}
          bloques={bloques}
          materias={materias}
          docentes={docentes}
          materiaPorId={materiaPorId}
          docentePorId={docentePorId}
          onBloqueCreado={(b) => setBloques((prev) => [...prev, b])}
          onCambio={consultar}
        />
      ) : items.length > 0 ? (
        <GrillaSoloLectura
          items={items}
          bloquePorId={bloquePorId}
          materiaPorId={materiaPorId}
          cursoPorId={cursoPorId}
          docentePorId={docentePorId}
          modo={modo}
        />
      ) : (
        !cargando && <p className="mt-6 text-sm text-gray-600">Sin clases asignadas para esta consulta.</p>
      ))}
    </Layout>
  );
}

function GrillaSoloLectura({ items, bloquePorId, materiaPorId, cursoPorId, docentePorId, modo }) {
  const filas = useMemo(() => {
    const claves = new Map();
    items.forEach((it) => {
      const b = bloquePorId.get(it.bloqueId) ?? it;
      const k = `${b.horaInicio}-${b.horaFin}`;
      if (!claves.has(k)) claves.set(k, { horaInicio: b.horaInicio, horaFin: b.horaFin });
    });
    return [...claves.values()].sort((a, b) => a.horaInicio.localeCompare(b.horaInicio));
  }, [items, bloquePorId]);

  function itemEn(dia, horaInicio) {
    return items.find((it) => {
      const b = bloquePorId.get(it.bloqueId) ?? it;
      return b.dia === dia && b.horaInicio === horaInicio;
    });
  }

  return (
    <Tarjeta className="mt-6 overflow-x-auto">
      <table className="w-full border-separate border-spacing-1 text-sm">
        <thead>
          <tr>
            <th className="w-24 text-left text-xs uppercase tracking-wide text-gray-500">Hora</th>
            {DIAS_CLASE.map((dia) => (
              <th key={dia} className="text-left text-xs uppercase tracking-wide text-gray-500">
                {NOMBRES_DIA[dia]}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {filas.map((fila) => (
            <tr key={fila.horaInicio}>
              <td className="whitespace-nowrap py-2 pr-2 text-xs font-medium text-gray-600">
                {fila.horaInicio.slice(0, 5)}–{fila.horaFin.slice(0, 5)}
              </td>
              {DIAS_CLASE.map((dia) => {
                const item = itemEn(dia, fila.horaInicio);
                if (!item) return <td key={dia} className="rounded-lg bg-gray-50 p-2" />;
                const materia = materiaPorId.get(item.materiaId);
                const curso = cursoPorId.get(item.cursoId);
                const docente = docentePorId.get(item.docenteId);
                return (
                  <td key={dia} className="rounded-lg bg-educk-50 p-2 text-xs text-educk-900">
                    <p className="font-semibold">{materia?.nombre ?? `Materia #${item.materiaId}`}</p>
                    <p className="text-educk-700">
                      {modo === 'curso'
                        ? (docente ? `${docente.nombres} ${docente.apellidos}` : `#${item.docenteId}`)
                        : (curso?.nombre ?? `#${item.cursoId}`)}
                    </p>
                  </td>
                );
              })}
            </tr>
          ))}
        </tbody>
      </table>
    </Tarjeta>
  );
}

// La grilla que se puede editar. Cada celda vacia es un boton "+"; al hacer
// clic se abre un formulario compacto justo debajo de la grilla (no un modal
// flotante, para no complicar el posicionamiento) con materia y docente.
function GrillaAsignable({ cursoId, periodoId, items: itemsIniciales, bloques, materias, docentes, materiaPorId, docentePorId, onBloqueCreado, onCambio }) {
  const [items, setItems] = useState(itemsIniciales);
  const [celda, setCelda] = useState(null); // { dia, horaInicio, horaFin }
  const [materiaId, setMateriaId] = useState('');
  const [docenteId, setDocenteId] = useState('');
  const [guardando, setGuardando] = useState(false);
  const [errorCelda, setErrorCelda] = useState(null);

  useEffect(() => setItems(itemsIniciales), [itemsIniciales]);

  function itemEn(dia, horaInicio) {
    return items.find((it) => {
      const b = bloques.find((x) => x.id === it.bloqueId);
      return b && b.dia === dia && b.horaInicio.slice(0, 5) === horaInicio;
    });
  }

  function abrirCelda(dia, horaInicio, horaFin) {
    setCelda({ dia, horaInicio, horaFin });
    setMateriaId('');
    setDocenteId('');
    setErrorCelda(null);
  }

  async function asignarCelda(evento) {
    evento.preventDefault();
    if (!materiaId || !docenteId || !celda) return;
    setGuardando(true);
    setErrorCelda(null);
    try {
      let bloque = bloques.find(
        (b) => b.dia === celda.dia && b.horaInicio.slice(0, 5) === celda.horaInicio && b.jornada === 'MANANA'
      );
      if (!bloque) {
        const { data } = await api.post('/horarios/bloques', {
          dia: celda.dia,
          horaInicio: `${celda.horaInicio}:00`,
          horaFin: `${celda.horaFin}:00`,
          jornada: 'MANANA',
        });
        bloque = data;
        onBloqueCreado(data);
      }
      await api.post('/horarios/asignaciones', {
        bloqueId: bloque.id,
        materiaId: Number(materiaId),
        docenteId: Number(docenteId),
        cursoId: Number(cursoId),
        periodoAcademicoId: Number(periodoId),
      });
      setCelda(null);
      await onCambio();
    } catch (err) {
      // RB-18: cruce de horario para el mismo docente responde aqui.
      setErrorCelda(err.response?.data?.mensaje ?? 'No fue posible asignar la clase.');
    } finally {
      setGuardando(false);
    }
  }

  return (
    <>
      <Tarjeta className="mt-6 overflow-x-auto">
        <table className="w-full border-separate border-spacing-1 text-sm">
          <thead>
            <tr>
              <th className="w-24 text-left text-xs uppercase tracking-wide text-gray-500">Hora</th>
              {DIAS_CLASE.map((dia) => (
                <th key={dia} className="text-left text-xs uppercase tracking-wide text-gray-500">
                  {NOMBRES_DIA[dia]}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {HORAS_JORNADA.map(([horaInicio, horaFin]) => (
              <tr key={horaInicio}>
                <td className="whitespace-nowrap py-2 pr-2 text-xs font-medium text-gray-600">
                  {horaInicio}–{horaFin}
                </td>
                {DIAS_CLASE.map((dia) => {
                  const item = itemEn(dia, horaInicio);
                  const activa = celda?.dia === dia && celda?.horaInicio === horaInicio;
                  if (item) {
                    const materia = materiaPorId.get(item.materiaId);
                    const docente = docentePorId.get(item.docenteId);
                    return (
                      <td key={dia} className="rounded-lg bg-educk-50 p-2 text-xs text-educk-900">
                        <p className="font-semibold">{materia?.nombre ?? `#${item.materiaId}`}</p>
                        <p className="text-educk-700">{docente ? `${docente.nombres} ${docente.apellidos}` : `#${item.docenteId}`}</p>
                      </td>
                    );
                  }
                  return (
                    <td key={dia} className="p-0">
                      <button
                        type="button"
                        onClick={() => abrirCelda(dia, horaInicio, horaFin)}
                        aria-label={`Asignar clase el ${NOMBRES_DIA[dia]} de ${horaInicio} a ${horaFin}`}
                        className={`flex h-14 w-full items-center justify-center rounded-lg border-2 border-dashed text-lg font-semibold
                                    transition ${activa ? 'border-educk-500 bg-educk-50 text-educk-600' : 'border-gray-200 text-gray-300 hover:border-educk-300 hover:text-educk-500'}`}
                      >
                        +
                      </button>
                    </td>
                  );
                })}
              </tr>
            ))}
          </tbody>
        </table>
      </Tarjeta>

      {celda && (
        <Tarjeta className="mt-4">
          <h4 className="text-sm font-semibold text-gray-900">
            Asignar clase — {NOMBRES_DIA[celda.dia]} {celda.horaInicio}–{celda.horaFin}
          </h4>
          <form onSubmit={asignarCelda} className="mt-3 grid gap-4 sm:grid-cols-3">
            <Campo etiqueta="Materia" htmlFor="materiaCelda">
              <Selector
                id="materiaCelda"
                valor={materiaId}
                onChange={setMateriaId}
                opciones={materias.map((m) => ({ value: m.id, label: m.nombre }))}
              />
            </Campo>
            <Campo etiqueta="Docente" htmlFor="docenteCelda">
              <Selector
                id="docenteCelda"
                valor={docenteId}
                onChange={setDocenteId}
                opciones={docentes.map((d) => ({ value: d.id, label: `${d.nombres} ${d.apellidos}` }))}
              />
            </Campo>
            <div className="flex items-end gap-2">
              <Boton disabled={guardando}>{guardando ? 'Asignando...' : 'Asignar'}</Boton>
              <Boton type="button" variante="secundario" onClick={() => setCelda(null)}>Cancelar</Boton>
            </div>
          </form>
          {errorCelda && <div className="mt-3"><Alerta>{errorCelda}</Alerta></div>}
        </Tarjeta>
      )}
    </>
  );
}
