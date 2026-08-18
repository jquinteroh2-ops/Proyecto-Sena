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
          'No fue posible conectar con el servidor. Intentelo de nuevo.'
      );
    } finally {
      setEnviando(false);
    }
  }

  return (
    <main className="min-h-screen flex items-center justify-center bg-educk-50 px-4 py-8">
      <div className="bg-white shadow-lg rounded-2xl p-8 sm:p-10 w-full max-w-md">
        <header className="text-center">
          <h1 className="text-3xl font-bold text-educk-700">EduckTrack</h1>
          <p className="mt-1 text-gray-600">Sistema de gestion academica</p>
        </header>

        <form onSubmit={onSubmit} className="mt-8 space-y-5" noValidate>
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
              Contrasena
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
            className="w-full rounded-lg bg-educk-600 px-4 py-2.5 font-semibold text-white
                       hover:bg-educk-700 focus:outline-none focus:ring-2 focus:ring-educk-500
                       focus:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {enviando ? 'Verificando...' : 'Iniciar sesion'}
          </button>

          {/* RF-64: sin este enlace la recuperacion existia pero no habia por
              donde llegar a ella. */}
          <Link
            to="/recuperar-password"
            className="block text-center text-sm font-medium text-educk-700 hover:underline"
          >
            Olvide mi contrasena
          </Link>
        </form>
      </div>
    </main>
  );
}
