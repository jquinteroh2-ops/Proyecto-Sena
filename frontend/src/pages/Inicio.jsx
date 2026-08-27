import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import api from '../api/axios';
import { GraficoBarras } from '../components/charts';
import Layout from '../components/Layout';
import { Alerta, Tarjeta as Panel } from '../components/ui';
import { useAuthStore } from '../store/authStore';

// Pantalla posterior al inicio de sesion. Consume GET /api/identidad/yo (RS-03)
// para mostrar que perfil academico corresponde a la cuenta autenticada, sin
// que el cliente envie identificadores manipulables (RNF-07).
//
// Para quien tiene vision institucional (RB-02: Rector, Coordinacion,
// Administrador) se añade un vistazo del rendimiento de todos los salones,
// para que "como esta el colegio" se responda sin entrar a Reportes.
export default function Inicio() {
  const usuario = useAuthStore((s) => s.usuario);
  const roles = usuario?.roles ?? [];
  const tieneVisionInstitucional =
    roles.includes('RECTOR') || roles.includes('COORDINADOR_ACADEMICO') || roles.includes('ADMINISTRADOR');

  const [identidad, setIdentidad] = useState(null);
  const [error, setError] = useState(null);

  const [indicadores, setIndicadores] = useState(null);
  const [rendimientoCursos, setRendimientoCursos] = useState(null);
  const [errorPanel, setErrorPanel] = useState(null);

  useEffect(() => {
    api
      .get('/identidad/yo')
      .then((res) => setIdentidad(res.data))
      .catch((err) =>
        setError(err.response?.data?.mensaje ?? 'No fue posible consultar su identidad.')
      );
  }, []);

  useEffect(() => {
    if (!tieneVisionInstitucional) return;
    api
      .get('/reportes/indicadores')
      .then((res) => setIndicadores(res.data))
      .catch(() => setErrorPanel('No fue posible cargar los indicadores institucionales.'));

    api
      .get('/cursos')
      .then(async (res) => {
        const cursos = res.data;
        const resultados = await Promise.all(
          cursos.map((c) =>
            api
              .get('/reportes/rendimiento', { params: { cursoId: c.id, periodoAcademicoId: c.periodoAcademicoId } })
              .then((r) => ({ curso: c, rendimiento: r.data }))
              .catch(() => null)
          )
        );
        setRendimientoCursos(resultados.filter(Boolean));
      })
      .catch(() => {});
  }, [tieneVisionInstitucional]);

  const barrasPorCurso = useMemo(
    () =>
      (rendimientoCursos ?? [])
        .filter((r) => r.rendimiento.estudiantes.length > 0)
        .map((r) => ({
          etiqueta: `${r.curso.nombre} · P${r.curso.periodoAcademicoId}`,
          valor: Number(r.rendimiento.promedioCurso),
        })),
    [rendimientoCursos]
  );

  return (
    <Layout>
      <section className="overflow-hidden rounded-3xl bg-marca px-6 py-8 text-white shadow-tarjeta sm:px-10 sm:py-10">
        <p className="text-sm font-medium text-white/70">Bienvenido de nuevo</p>
        <h2 className="mt-1 text-3xl font-extrabold tracking-tight sm:text-4xl">{usuario?.nombre}</h2>
        <p className="mt-2 max-w-xl text-sm text-white/80">
          {(usuario?.roles ?? []).join(' · ')}
        </p>

        {usuario?.debeCambiarPassword && (
          <Link
            to="/recuperar-password"
            className="mt-5 inline-flex items-center gap-2 rounded-full bg-white/15 px-4 py-2 text-sm font-semibold
                       text-white ring-1 ring-inset ring-white/30 backdrop-blur transition hover:bg-white/25"
          >
            ⚠ Está usando una contraseña inicial — cámbiela ahora
          </Link>
        )}
      </section>

      {error && <div className="mt-6"><Alerta>{error}</Alerta></div>}

      {tieneVisionInstitucional && (
        <section className="mt-6">
          <h3 className="text-lg font-semibold text-gray-900">Cómo está la institución</h3>
          {errorPanel && <div className="mt-3"><Alerta>{errorPanel}</Alerta></div>}

          {indicadores && (
            <div className="mt-4 grid gap-4 sm:grid-cols-4">
              <Indicador etiqueta="Promedio institucional" valor={indicadores.promedioGeneralInstitucional} tono="azul" />
              <Indicador
                etiqueta="Asistencia promedio"
                valor={`${indicadores.asistenciaPromedioInstitucional.toFixed(1)}%`}
                tono="violeta"
              />
              <Indicador etiqueta="Estudiantes en riesgo" valor={indicadores.estudiantesEnRiesgo} tono="ambar" />
              <Indicador etiqueta="Con notas registradas" valor={indicadores.totalEstudiantesConNotas} tono="verde" />
            </div>
          )}

          {barrasPorCurso.length > 0 && (
            <Panel className="mt-4">
              <h4 className="text-sm font-semibold text-gray-900">Promedio por salón</h4>
              <div className="mt-3">
                <GraficoBarras barras={barrasPorCurso} />
              </div>
            </Panel>
          )}
        </section>
      )}

      {identidad && (
        <Panel className="mt-6">
          <h3 className="text-sm font-semibold uppercase tracking-wide text-gray-500">
            Identidad resuelta por el servidor
          </h3>
          <dl className="mt-4 grid gap-4 sm:grid-cols-2">
            <Dato etiqueta="Usuario" valor={`#${identidad.usuarioId}`} />
            <Dato etiqueta="Correo" valor={identidad.correo} />
            <Dato etiqueta="Roles" valor={identidad.roles.join(', ') || '-'} />
            <Dato
              etiqueta="Perfil académico"
              valor={
                identidad.estudianteId
                  ? `Estudiante #${identidad.estudianteId}`
                  : identidad.docenteId
                    ? `Docente #${identidad.docenteId}`
                    : 'Sin perfil vinculado'
              }
            />
            {identidad.estudiantesTutelados.length > 0 && (
              <Dato
                etiqueta="Estudiantes a cargo"
                valor={identidad.estudiantesTutelados.map((id) => `#${id}`).join(', ')}
              />
            )}
          </dl>
        </Panel>
      )}

      <section className="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {(usuario?.roles ?? []).some((r) => r === 'ESTUDIANTE' || r === 'PADRE_FAMILIA') && (
          <Tarjeta a="/mis-notas" icono="📈" titulo="Mis notas" descripcion="Tendencia del promedio por periodo y detalle de notas (RF-37)." />
        )}
        <Tarjeta a="/boletin" icono="📋" titulo="Boletín de calificaciones" descripcion="Consulte el boletín de un periodo cerrado (RF-35)." />
        {(usuario?.roles ?? []).some(
          (r) => r === 'DOCENTE' || r === 'COORDINADOR_ACADEMICO' || r === 'RECTOR' || r === 'ADMINISTRADOR'
        ) && <Tarjeta a="/cursos" icono="🏫" titulo="Cursos" descripcion="Salones, estudiantes matriculados y cupo (RF-45, RF-46)." />}
        {(usuario?.roles ?? []).some((r) => r === 'DOCENTE' || r === 'COORDINADOR_ACADEMICO' || r === 'ADMINISTRADOR') && (
          <Tarjeta a="/notas" icono="✏️" titulo="Notas por salón" descripcion="Planilla de calificaciones de un salón completo (RF-31)." />
        )}
        <Tarjeta a="/horario" icono="🗓️" titulo="Horario" descripcion="Consulte el horario por salón o por docente (RF-24, RF-25)." />
        {(usuario?.roles ?? []).some((r) => r === 'COORDINADOR_ACADEMICO' || r === 'ADMINISTRADOR') && (
          <Tarjeta a="/asignaciones" icono="🧩" titulo="Asignación de clases" descripcion="Carga académica y dirección de grupo (RF-14, RF-16)." />
        )}
        {(usuario?.roles ?? []).some((r) => r === 'COORDINADOR_ACADEMICO' || r === 'RECTOR' || r === 'ADMINISTRADOR') && (
          <Tarjeta a="/reportes" icono="📊" titulo="Reportes" descripcion="Indicadores institucionales y rendimiento por salón (RF-47, RF-49, RF-50)." />
        )}
        {(usuario?.roles ?? []).some((r) => r === 'RECTOR' || r === 'ADMINISTRADOR') && (
          <Tarjeta a="/parametros" icono="⚙️" titulo="Parámetros institucionales" descripcion="Escala de calificación y mínimos institucionales (RF-59)." />
        )}
      </section>
    </Layout>
  );
}

function Dato({ etiqueta, valor }) {
  return (
    <div>
      <dt className="text-xs font-medium uppercase tracking-wide text-gray-500">{etiqueta}</dt>
      <dd className="mt-0.5 text-gray-900">{valor}</dd>
    </div>
  );
}

function Indicador({ etiqueta, valor, tono = 'azul' }) {
  const tonos = {
    azul: 'from-educk-500 to-educk-700',
    violeta: 'from-acento-400 to-acento-600',
    ambar: 'from-amber-400 to-amber-600',
    verde: 'from-emerald-400 to-emerald-600',
  };
  return (
    <div className={`rounded-2xl bg-gradient-to-br ${tonos[tono]} p-5 text-white shadow-tarjeta`}>
      <p className="text-xs font-medium uppercase tracking-wide text-white/80">{etiqueta}</p>
      <p className="mt-1 text-3xl font-extrabold tabular-nums">{valor}</p>
    </div>
  );
}

function Tarjeta({ a, icono, titulo, descripcion }) {
  return (
    <Link
      to={a}
      className="group rounded-2xl border border-gray-100 bg-white p-6 shadow-tarjeta transition
                 hover:-translate-y-0.5 hover:shadow-lg focus:outline-none focus:ring-2 focus:ring-educk-500"
    >
      <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-educk-50 text-xl">{icono}</span>
      <h3 className="mt-3 font-semibold text-gray-900 group-hover:text-educk-700">{titulo}</h3>
      <p className="mt-1 text-sm text-gray-600">{descripcion}</p>
    </Link>
  );
}
