import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import api from '../api/axios';
import { useAuthStore } from '../store/authStore';

// Pantalla de inicio de sesion (RF-60, RF-61).
// La autenticacion real la resuelve el backend: este formulario solo envia las
// credenciales y guarda el JWT emitido (RS-04). El control de acceso nunca se
// decide aqui (RS-03: la autorizacion la impone siempre el servidor).
export default function Login() {
  const [correo, setCorreo] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState(null);
  const [enviando, setEnviando] = useState(false);

  const login = useAuthStore((s) => s.login);
  const navigate = useNavigate();
  const location = useLocation();

  // Ruta que el usuario intentaba abrir antes de ser redirigido al login.
  const destino = location.state?.from ?? '/';

  async function onSubmit(evento) {
    evento.preventDefault();
    setError(null);
    setEnviando(true);
    try {
      const { data } = await api.post('/auth/login', { correo, password });
      login({ token: data.token, usuario: data.usuario });
      navigate(destino, { replace: true });
    } catch (err) {
      // RNF-10: se muestra el mensaje en espanol que envia el backend (ApiError).
      setError(
        err.response?.data?.mensaje ??
          'No fue posible conectar con el servidor. Inténtelo de nuevo.'
      );
    } finally {
      setEnviando(false);
    }
  }

  return (
    <main className="fondo-educktrack relative flex min-h-screen items-center justify-center overflow-hidden px-4 py-8">
      {/* Panel de marca: solo visible desde sm, para no restar espacio al
          formulario en movil. Puramente decorativo, no lleva contenido que la
          pantalla necesite para funcionar. */}
      <div className="pointer-events-none absolute inset-0 hidden sm:block">
        <div className="absolute -left-24 top-1/2 h-[32rem] w-[32rem] -translate-y-1/2 rounded-full bg-marca opacity-[0.07] blur-3xl" />
      </div>

      <div className="relative grid w-full max-w-4xl overflow-hidden rounded-3xl bg-white shadow-tarjeta sm:grid-cols-2">
        <div className="hidden flex-col justify-between bg-marca p-10 text-white sm:flex">
          <div>
            <span className="flex h-11 w-11 items-center justify-center rounded-xl bg-white/15 text-lg font-extrabold ring-1 ring-white/25">
              E
            </span>
            <h1 className="mt-6 text-3xl font-extrabold leading-tight">
              Educk<span className="text-white/70">Track</span>
            </h1>
            <p className="mt-3 max-w-xs text-sm text-white/80">
              Notas, horarios, asignaciones y reportes de todo el colegio en un
              solo lugar.
            </p>
          </div>
          <p className="text-xs text-white/60">Sistema de gestión académica</p>
        </div>

        <div className="p-8 sm:p-10">
          <header className="sm:hidden">
            <h1 className="text-2xl font-extrabold text-educk-700">EduckTrack</h1>
            <p className="mt-1 text-sm text-gray-600">Sistema de gestión académica</p>
          </header>
          <h2 className="hidden text-xl font-bold text-gray-900 sm:block">Inicia sesión</h2>
          <p className="mt-1 hidden text-sm text-gray-500 sm:block">Ingresa con tu cuenta institucional.</p>

        <form onSubmit={onSubmit} className="mt-6 space-y-5 sm:mt-8" noValidate>
          <div>
            {/* RNF-22: etiquetas semanticas asociadas al control. */}
            <label htmlFor="correo" className="block text-sm font-medium text-gray-800">
              Correo institucional
            </label>
            <input
              id="correo"
              name="correo"
              type="email"
              required
              autoComplete="username"
              autoFocus
              value={correo}
              onChange={(e) => setCorreo(e.target.value)}
              placeholder="usuario@colegio.edu.co"
              className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2 text-gray-900
                         focus:border-educk-500 focus:outline-none focus:ring-2 focus:ring-educk-500"
            />
          </div>

          <div>
            <label htmlFor="password" className="block text-sm font-medium text-gray-800">
              Contraseña
            </label>
            <input
              id="password"
              name="password"
              type="password"
              required
              autoComplete="current-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2 text-gray-900
                         focus:border-educk-500 focus:outline-none focus:ring-2 focus:ring-educk-500"
            />
          </div>

          {/* RNF-10: el error se anuncia a lectores de pantalla al aparecer. */}
          {error && (
            <p role="alert" className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
              {error}
            </p>
          )}

          <button
            type="submit"
            disabled={enviando}
            className="w-full rounded-xl bg-marca px-4 py-2.5 font-semibold text-white shadow-suave
                       transition hover:brightness-110 focus:outline-none focus:ring-2 focus:ring-educk-500
                       focus:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-60 disabled:hover:brightness-100"
          >
            {enviando ? 'Verificando...' : 'Iniciar sesión'}
          </button>

          {/* RF-64: sin este enlace la recuperacion existia pero no habia por
              donde llegar a ella. */}
          <Link
            to="/recuperar-password"
            className="block text-center text-sm font-medium text-educk-700 hover:underline"
          >
            Olvidé mi contraseña
          </Link>
        </form>
        </div>
      </div>
    </main>
  );
}
