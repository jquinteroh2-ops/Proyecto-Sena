import { Link, useLocation, useNavigate } from 'react-router-dom';
import api from '../api/axios';
import { useAuthStore } from '../store/authStore';

// Marco comun de las pantallas con sesion: cabecera, navegacion y cierre de
// sesion (RF-61). Existe desde la Fase 10 porque al pasar de una pantalla a
// cuatro, repetir la cabecera en cada una garantizaba que se fueran separando.
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

  return (
    <div className="min-h-screen bg-educk-50">
      <header className="bg-white shadow-sm">
        <div className="mx-auto flex max-w-5xl flex-wrap items-center justify-between gap-3 px-4 py-4">
          <div className="flex flex-wrap items-center gap-6">
            <Link to="/" className="text-xl font-bold text-educk-700">
              EduckTrack
            </Link>
            <nav aria-label="Secciones" className="flex flex-wrap gap-4">
              <Enlace a="/" actual={pathname}>
                Inicio
              </Enlace>
              <Enlace a="/boletin" actual={pathname}>
                Boletin
              </Enlace>
              {esRectorOAdmin && (
                <Enlace a="/parametros" actual={pathname}>
                  Parametros
                </Enlace>
              )}
            </nav>
          </div>

          <div className="flex items-center gap-4">
            <span className="text-sm text-gray-600">{usuario?.correo}</span>
            <button
              type="button"
              onClick={cerrarSesion}
              className="rounded-lg border border-gray-300 px-3 py-1.5 text-sm font-medium text-gray-800
                         hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-educk-500"
            >
              Cerrar sesion
            </button>
          </div>
        </div>
      </header>

      <main className="mx-auto max-w-5xl px-4 py-8">{children}</main>
    </div>
  );
}

function Enlace({ a, actual, children }) {
  const activo = actual === a;
  return (
    <Link
      to={a}
      // RNF-22: el estado activo no se senala solo con color.
      aria-current={activo ? 'page' : undefined}
      className={
        activo
          ? 'border-b-2 border-educk-600 pb-0.5 text-sm font-semibold text-educk-700'
          : 'pb-0.5 text-sm font-medium text-gray-600 hover:text-educk-700'
      }
    >
      {children}
    </Link>
  );
}
