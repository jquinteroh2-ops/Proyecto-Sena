import { useEffect, useMemo, useState } from 'react';
import api from '../api/axios';
import Layout from '../components/Layout';
import { Alerta, Boton, Campo, Etiqueta, Modal, Selector, Tarjeta, Texto } from '../components/ui';

const POR_PAGINA = 8;
const ESTADOS = ['ACTIVO', 'RETIRADO', 'GRADUADO', 'TRASLADADO'];

const FORM_VACIO = {
  documento: '', nombres: '', apellidos: '', fechaNacimiento: '',
  direccion: '', acudienteNombre: '', acudienteTelefono: '', acudienteParentesco: '',
};

// Gestion de estudiantes (RF-06..RF-09). Competencia de Coordinacion
// Academica, Rectoria y Administracion (RF-06/07/10); la consulta la
// comparten tambien los docentes.
export default function Estudiantes() {
  const [estudiantes, setEstudiantes] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState(null);

  const [busqueda, setBusqueda] = useState('');
  const [filtroEstado, setFiltroEstado] = useState('');
  const [pagina, setPagina] = useState(1);

  const [modalAbierto, setModalAbierto] = useState(false);
  const [editandoId, setEditandoId] = useState(null);
  const [form, setForm] = useState(FORM_VACIO);
  const [errorForm, setErrorForm] = useState(null);
  const [guardando, setGuardando] = useState(false);

  function cargar() {
    setCargando(true);
    api
      .get('/estudiantes')
      .then((res) => setEstudiantes(res.data))
      .catch((err) => setError(err.response?.data?.mensaje ?? 'No fue posible consultar los estudiantes.'))
      .finally(() => setCargando(false));
  }

  useEffect(cargar, []);

  const filtrados = useMemo(() => {
    const q = busqueda.trim().toLowerCase();
    return estudiantes.filter((e) => {
      const coincideTexto =
        !q ||
        e.documento.toLowerCase().includes(q) ||
        e.nombres.toLowerCase().includes(q) ||
        e.apellidos.toLowerCase().includes(q);
      const coincideEstado = !filtroEstado || e.estado === filtroEstado;
      return coincideTexto && coincideEstado;
    });
  }, [estudiantes, busqueda, filtroEstado]);

  const totalPaginas = Math.max(1, Math.ceil(filtrados.length / POR_PAGINA));
  const paginaSegura = Math.min(pagina, totalPaginas);
  const visibles = filtrados.slice((paginaSegura - 1) * POR_PAGINA, paginaSegura * POR_PAGINA);

  function abrirNuevo() {
    setEditandoId(null);
    setForm(FORM_VACIO);
    setErrorForm(null);
    setModalAbierto(true);
  }

  function abrirEditar(e) {
    setEditandoId(e.estudianteId ?? e.id);
    setForm({
      documento: e.documento, nombres: e.nombres, apellidos: e.apellidos,
      fechaNacimiento: e.fechaNacimiento ?? '', direccion: e.direccion ?? '',
      acudienteNombre: e.acudienteNombre ?? '', acudienteTelefono: e.acudienteTelefono ?? '',
      acudienteParentesco: e.acudienteParentesco ?? '',
    });
    setErrorForm(null);
    setModalAbierto(true);
  }

  async function guardar(evento) {
    evento.preventDefault();
    setGuardando(true);
    setErrorForm(null);
    try {
      if (editandoId) {
        await api.put(`/estudiantes/${editandoId}`, {
          nombres: form.nombres, apellidos: form.apellidos, fechaNacimiento: form.fechaNacimiento || null,
          direccion: form.direccion, acudienteNombre: form.acudienteNombre,
          acudienteTelefono: form.acudienteTelefono, acudienteParentesco: form.acudienteParentesco,
        });
      } else {
        await api.post('/estudiantes', {
          ...form, fechaNacimiento: form.fechaNacimiento || null,
        });
      }
      setModalAbierto(false);
      cargar();
    } catch (err) {
      setErrorForm(err.response?.data?.mensaje ?? 'No fue posible guardar el estudiante.');
    } finally {
      setGuardando(false);
    }
  }

  return (
    <Layout>
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="text-2xl font-semibold text-gray-900">Estudiantes</h2>
          <p className="mt-1 text-sm text-gray-600">Registro, edición y consulta de estudiantes (RF-06..RF-09).</p>
        </div>
        <Boton type="button" onClick={abrirNuevo}>+ Nuevo estudiante</Boton>
      </div>

      {error && <div className="mt-6"><Alerta>{error}</Alerta></div>}

      <Tarjeta className="mt-6">
        <div className="grid gap-4 sm:grid-cols-3">
          <Campo etiqueta="Buscar" htmlFor="busqueda">
            <Texto
              id="busqueda"
              required={false}
              placeholder="Documento, nombres o apellidos..."
              valor={busqueda}
              onChange={(v) => { setBusqueda(v); setPagina(1); }}
            />
          </Campo>
          <Campo etiqueta="Estado" htmlFor="filtroEstado">
            <Selector
              id="filtroEstado"
              required={false}
              placeholder="Todos"
              valor={filtroEstado}
              onChange={(v) => { setFiltroEstado(v); setPagina(1); }}
              opciones={ESTADOS.map((e) => ({ value: e, label: e }))}
            />
          </Campo>
        </div>

        {cargando ? (
          <p className="mt-6 text-sm text-gray-600">Cargando...</p>
        ) : (
          <>
            <div className="mt-4 overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead>
                  <tr className="border-b border-gray-200 text-xs uppercase tracking-wide text-gray-500">
                    <th scope="col" className="py-2 pr-4">Documento</th>
                    <th scope="col" className="py-2 pr-4">Nombre</th>
                    <th scope="col" className="py-2 pr-4">Estado</th>
                    <th scope="col" className="py-2">Acudiente</th>
                  </tr>
                </thead>
                <tbody>
                  {visibles.map((e) => (
                    <tr
                      key={e.id}
                      onClick={() => abrirEditar(e)}
                      className="cursor-pointer border-b border-gray-100 hover:bg-gray-50"
                    >
                      <td className="py-2 pr-4 tabular-nums text-gray-700">{e.documento}</td>
                      <td className="py-2 pr-4 text-gray-900">{e.nombres} {e.apellidos}</td>
                      <td className="py-2 pr-4">
                        <Etiqueta tono={e.estado === 'ACTIVO' ? 'verde' : 'ambar'}>{e.estado}</Etiqueta>
                      </td>
                      <td className="py-2 text-gray-600">{e.acudienteNombre || '—'}</td>
                    </tr>
                  ))}
                  {visibles.length === 0 && (
                    <tr><td colSpan={4} className="py-4 text-gray-500">Sin resultados.</td></tr>
                  )}
                </tbody>
              </table>
            </div>

            <div className="mt-4 flex items-center justify-between text-sm text-gray-600">
              <span>{filtrados.length} estudiante(s)</span>
              <div className="flex items-center gap-2">
                <Boton
                  type="button" variante="secundario"
                  disabled={paginaSegura <= 1}
                  onClick={() => setPagina((p) => p - 1)}
                >Anterior</Boton>
                <span>Página {paginaSegura} de {totalPaginas}</span>
                <Boton
                  type="button" variante="secundario"
                  disabled={paginaSegura >= totalPaginas}
                  onClick={() => setPagina((p) => p + 1)}
                >Siguiente</Boton>
              </div>
            </div>
          </>
        )}
      </Tarjeta>

      {modalAbierto && (
        <Modal titulo={editandoId ? 'Editar estudiante' : 'Nuevo estudiante'} onCerrar={() => setModalAbierto(false)}>
          <form onSubmit={guardar} className="space-y-4">
            <div className="grid gap-4 sm:grid-cols-2">
              <Campo etiqueta="Documento" htmlFor="documento">
                <Texto id="documento" valor={form.documento} onChange={(v) => setForm((f) => ({ ...f, documento: v }))}
                       required={!editandoId} tipo={editandoId ? 'text' : 'text'} />
                {editandoId && <p className="mt-1 text-xs text-gray-400">El documento no se puede editar.</p>}
              </Campo>
              <Campo etiqueta="Fecha de nacimiento" htmlFor="fechaNacimiento">
                <Texto id="fechaNacimiento" tipo="date" required={false}
                       valor={form.fechaNacimiento} onChange={(v) => setForm((f) => ({ ...f, fechaNacimiento: v }))} />
              </Campo>
              <Campo etiqueta="Nombres" htmlFor="nombres">
                <Texto id="nombres" valor={form.nombres} onChange={(v) => setForm((f) => ({ ...f, nombres: v }))} />
              </Campo>
              <Campo etiqueta="Apellidos" htmlFor="apellidos">
                <Texto id="apellidos" valor={form.apellidos} onChange={(v) => setForm((f) => ({ ...f, apellidos: v }))} />
              </Campo>
              <Campo etiqueta="Dirección" htmlFor="direccion">
                <Texto id="direccion" required={false} valor={form.direccion} onChange={(v) => setForm((f) => ({ ...f, direccion: v }))} />
              </Campo>
              <Campo etiqueta="Acudiente" htmlFor="acudienteNombre">
                <Texto id="acudienteNombre" required={false} valor={form.acudienteNombre} onChange={(v) => setForm((f) => ({ ...f, acudienteNombre: v }))} />
              </Campo>
              <Campo etiqueta="Teléfono acudiente" htmlFor="acudienteTelefono">
                <Texto id="acudienteTelefono" required={false} valor={form.acudienteTelefono} onChange={(v) => setForm((f) => ({ ...f, acudienteTelefono: v }))} />
              </Campo>
              <Campo etiqueta="Parentesco" htmlFor="acudienteParentesco">
                <Texto id="acudienteParentesco" required={false} valor={form.acudienteParentesco} onChange={(v) => setForm((f) => ({ ...f, acudienteParentesco: v }))} />
              </Campo>
            </div>

            {errorForm && <Alerta>{errorForm}</Alerta>}

            <div className="flex justify-end gap-2">
              <Boton type="button" variante="secundario" onClick={() => setModalAbierto(false)}>Cancelar</Boton>
              <Boton disabled={guardando}>{guardando ? 'Guardando...' : 'Guardar'}</Boton>
            </div>
          </form>
        </Modal>
      )}
    </Layout>
  );
}
