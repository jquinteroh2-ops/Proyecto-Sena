import { Link, useLocation, useNavigate } from 'react-router-dom';
import api from '../api/axios';
import { useAuthStore } from '../store/authStore';

// Marco comun de las pantallas con sesion: cabecera, navegacion y cierre de
// sesion (RF-61). Existe desde la Fase 10 porque al pasar de una pantalla a
// cuatro, repetir la cabecera en cada una garantizaba que se fueran separando.
// Rediseñada cuando el numero de secciones crecio a nueve: la navegacion pasa
// a pildoras con icono para que siga siendo legible sin ocupar dos filas.
//
// DECISION DE DISENO: la navegacion se filtra por rol solo para no ofrecer
// enlaces que van a devolver 403. No es control de acceso: igual que
// RutaProtegida, la autorizacion la impone el backend en cada peticion.
export default function Layout({ children }) {
  const usuario = useAuthStore((s) => s.usuario);
  const logout = useAuthStore((s) => s.logout);
  const navigate = useNavigate();
  const { pathname } = useLocation();

  const roles = usuario?.roles ?? [];
  const esRectorOAdmin = roles.includes('RECTOR') || roles.includes('ADMINISTRADOR');
  const esEstudianteOAcudiente = roles.includes('ESTUDIANTE') || roles.includes('PADRE_FAMILIA');
  const tieneVisionAula =
    roles.includes('DOCENTE') ||
    roles.includes('COORDINADOR_ACADEMICO') ||
    roles.includes('RECTOR') ||
    roles.includes('ADMINISTRADOR');
  const puedeGestionarCarga = roles.includes('COORDINADOR_ACADEMICO') || roles.includes('ADMINISTRADOR');
  const puedeVerReportes =
    roles.includes('COORDINADOR_ACADEMICO') || roles.includes('RECTOR') || roles.includes('ADMINISTRADOR');
  const puedeCargarNotas = roles.includes('DOCENTE') || roles.includes('COORDINADOR_ACADEMICO') || roles.includes('ADMINISTRADOR');
  const puedeGestionarEstudiantes =
    roles.includes('COORDINADOR_ACADEMICO') || roles.includes('RECTOR') || roles.includes('ADMINISTRADOR');
  const puedeRegistrarAsistencia = roles.includes('DOCENTE') || roles.includes('COORDINADOR_ACADEMICO') || roles.includes('ADMINISTRADOR');

  const enlaces = [
    { a: '/', texto: 'Inicio', icono: IconoInicio, mostrar: true },
    { a: '/mis-notas', texto: 'Mis notas', icono: IconoGrafico, mostrar: esEstudianteOAcudiente },
    { a: '/boletin', texto: 'Boletín', icono: IconoBoletin, mostrar: true },
    { a: '/estudiantes', texto: 'Estudiantes', icono: IconoEstudiantes, mostrar: puedeGestionarEstudiantes },
    { a: '/cursos', texto: 'Cursos', icono: IconoCursos, mostrar: tieneVisionAula },
    { a: '/notas', texto: 'Notas', icono: IconoLapiz, mostrar: puedeCargarNotas },
    { a: '/asistencia', texto: 'Asistencia', icono: IconoAsistencia, mostrar: puedeRegistrarAsistencia },
    { a: '/horario', texto: 'Horario', icono: IconoReloj, mostrar: true },
    { a: '/asignaciones', texto: 'Asignaciones', icono: IconoAsignar, mostrar: puedeGestionarCarga },
    { a: '/reportes', texto: 'Reportes', icono: IconoReportes, mostrar: puedeVerReportes },
    { a: '/parametros', texto: 'Parámetros', icono: IconoAjustes, mostrar: esRectorOAdmin },
  ].filter((e) => e.mostrar);

  async function cerrarSesion() {
    try {
      await api.post('/auth/logout');
    } catch {
      // El cierre local no debe depender de que el backend responda: al ser JWT
      // sin estado (RS-04), descartar el token es lo que termina la sesion.
    }
    logout();
    navigate('/login', { replace: true });
  }

  const iniciales = (usuario?.nombre ?? '?')
    .split(' ')
    .map((p) => p[0])
    .slice(0, 2)
    .join('')
    .toUpperCase();

  return (
    <div className="fondo-educktrack min-h-screen">
      <header className="sticky top-0 z-10 border-b border-white/60 bg-white/80 backdrop-blur-md">
        <div className="mx-auto max-w-6xl px-4 py-3">
          <div className="flex items-center justify-between gap-4">
            <Link to="/" className="flex items-center gap-2.5">
              <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-marca text-base font-extrabold text-white shadow-suave">
                E
              </span>
              <span className="text-lg font-extrabold tracking-tight text-gray-900">
                Educk<span className="text-educk-600">Track</span>
              </span>
            </Link>

            <div className="flex items-center gap-3">
              <span className="hidden text-sm text-gray-600 sm:inline">{usuario?.correo}</span>
              <span className="flex h-9 w-9 items-center justify-center rounded-full bg-educk-100 text-sm font-bold text-educk-700">
                {iniciales}
              </span>
              <button
                type="button"
                onClick={cerrarSesion}
                className="rounded-lg border border-gray-200 bg-white px-3 py-1.5 text-sm font-medium text-gray-700
                           shadow-sm transition hover:border-gray-300 hover:bg-gray-50 focus:outline-none
                           focus:ring-2 focus:ring-educk-500"
              >
                Salir
              </button>
            </div>
          </div>

          <nav aria-label="Secciones" className="mt-3 flex flex-wrap gap-1.5">
            {enlaces.map(({ a, texto, icono: Icono }) => (
              <Enlace key={a} a={a} actual={pathname}>
                <Icono className="h-4 w-4" />
                {texto}
              </Enlace>
            ))}
          </nav>
        </div>
      </header>

      <main className="mx-auto max-w-6xl px-4 py-8">{children}</main>
    </div>
  );
}

function Enlace({ a, actual, children }) {
  const activo = actual === a;
  return (
    <Link
      to={a}
      aria-current={activo ? 'page' : undefined}
      className={
        activo
          ? 'flex items-center gap-1.5 rounded-full bg-educk-600 px-3 py-1.5 text-sm font-semibold text-white shadow-suave'
          : 'flex items-center gap-1.5 rounded-full px-3 py-1.5 text-sm font-medium text-gray-600 hover:bg-gray-100 hover:text-gray-900'
      }
    >
      {children}
    </Link>
  );
}

// Iconos propios, trazo simple (24x24, stroke actual). Evitan una dependencia
// nueva solo para un puñado de glifos de navegacion.
function trazo(props) {
  return { fill: 'none', stroke: 'currentColor', strokeWidth: 2, strokeLinecap: 'round', strokeLinejoin: 'round', viewBox: '0 0 24 24', ...props };
}

function IconoInicio(props) {
  return (
    <svg {...trazo(props)}>
      <path d="M3 11.5 12 4l9 7.5" />
      <path d="M5 10v9a1 1 0 0 0 1 1h4v-6h4v6h4a1 1 0 0 0 1-1v-9" />
    </svg>
  );
}
function IconoBoletin(props) {
  return (
    <svg {...trazo(props)}>
      <rect x="4" y="3" width="16" height="18" rx="2" />
      <path d="M8 8h8M8 12h8M8 16h5" />
    </svg>
  );
}
function IconoCursos(props) {
  return (
    <svg {...trazo(props)}>
      <path d="M12 3 2 8l10 5 10-5-10-5Z" />
      <path d="M6 10.5V16c0 1.5 3 3 6 3s6-1.5 6-3v-5.5" />
    </svg>
  );
}
function IconoLapiz(props) {
  return (
    <svg {...trazo(props)}>
      <path d="M12 20h9" />
      <path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4 12.5-12.5Z" />
    </svg>
  );
}
function IconoReloj(props) {
  return (
    <svg {...trazo(props)}>
      <circle cx="12" cy="12" r="9" />
      <path d="M12 7v5l3.5 2" />
    </svg>
  );
}
function IconoAsignar(props) {
  return (
    <svg {...trazo(props)}>
      <circle cx="9" cy="8" r="3" />
      <path d="M3 20c0-3 2.7-5 6-5s6 2 6 5" />
      <path d="M17 8h4M19 6v4" />
    </svg>
  );
}
function IconoReportes(props) {
  return (
    <svg {...trazo(props)}>
      <path d="M4 20V10M10 20V4M16 20v-7M4 20h16" />
    </svg>
  );
}
function IconoAjustes(props) {
  return (
    <svg {...trazo(props)}>
      <circle cx="12" cy="12" r="3" />
      <path d="M19.4 15a1.6 1.6 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.6 1.6 0 0 0-1.8-.3 1.6 1.6 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-.2a1.6 1.6 0 0 0-1-1.5 1.6 1.6 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.6 1.6 0 0 0 .3-1.8 1.6 1.6 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.2a1.6 1.6 0 0 0 1.5-1 1.6 1.6 0 0 0-.3-1.8l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.6 1.6 0 0 0 1.8.3H9a1.6 1.6 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.2a1.6 1.6 0 0 0 1 1.5 1.6 1.6 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.6 1.6 0 0 0-.3 1.8V9c0 .7.4 1.3 1 1.5h.2a2 2 0 1 1 0 4h-.2a1.6 1.6 0 0 0-1 1.5Z" />
    </svg>
  );
}
function IconoEstudiantes(props) {
  return (
    <svg {...trazo(props)}>
      <circle cx="9" cy="7" r="3" />
      <path d="M3 20c0-3 2.7-5 6-5s6 2 6 5" />
      <path d="M16 4.5c1.5.3 2.5 1.5 2.5 3S17.5 10.2 16 10.5" />
      <path d="M15 20c0-2 1-3.5 3-4" />
    </svg>
  );
}
function IconoAsistencia(props) {
  return (
    <svg {...trazo(props)}>
      <rect x="4" y="4" width="16" height="16" rx="2" />
      <path d="M8 2v4M16 2v4M4 10h16" />
      <path d="m9 15 2 2 4-4" />
    </svg>
  );
}
function IconoGrafico(props) {
  return (
    <svg {...trazo(props)}>
      <path d="M3 17l5-5 4 4 8-8" />
      <path d="M15 8h5v5" />
    </svg>
  );
}
