import { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import api from '../api/axios';

// Consumo del enlace de recuperacion (RF-64, HU-04).
//
// Es la pantalla a la que apunta el correo que envia el backend
// (educktrack.seguridad.recuperacion.url-base). Hasta la Fase 10 esa ruta no
// existia, de modo que el enlace llevaba a ninguna parte.
//
// La ruta es publica a proposito: quien restablece su contrasena no tiene
// sesion, y lo que autoriza la operacion es el enlace de un solo uso.
export default function RestablecerPassword() {
  const [params] = useSearchParams();
  const token = params.get('token');

  const [password, setPassword] = useState('');
  const [confirmacion, setConfirmacion] = useState('');
  const [error, setError] = useState(null);
  const [listo, setListo] = useState(false);
  const [enviando, setEnviando] = useState(false);
  const navigate = useNavigate();

  async function onSubmit(evento) {
    evento.preventDefault();
    setError(null);

    // La confirmacion se comprueba aqui y no en el servidor porque es un error
    // de tecleo, no una regla de negocio: enviarlo gastaria el enlace, que es de
    // un solo uso, por haber escrito mal la contrasena dos veces.
    if (password !== confirmacion) {
      setError('Las dos contrasenas no coinciden.');
      return;
    }

    setEnviando(true);
    try {
      await api.post('/auth/restablecer-password', { token, nuevaPassword: password });
      setListo(true);
    } catch (err) {
      // La politica minima (HU-04) y la validez del enlace las decide el
      // backend; aqui solo se muestra su mensaje (RNF-10).
      setError(
        err.response?.data?.mensaje ??
          'No fue posible conectar con el servidor. Intentelo de nuevo.'
      );
    } finally {
      setEnviando(false);
    }
  }

  return (
    <main className="flex min-h-screen items-center justify-center bg-educk-50 px-4 py-8">
      <div className="w-full max-w-md rounded-2xl bg-white p-8 shadow-lg sm:p-10">
        <header className="text-center">
          <h1 className="text-3xl font-bold text-educk-700">EduckTrack</h1>
          <p className="mt-1 text-gray-600">Nueva contrasena</p>
        </header>

        {!token ? (
          // Se entra sin token cuando alguien abre la ruta a mano o copia mal el
          // enlace. Sin esto el formulario se enviaria para fallar despues.
          <div className="mt-8 space-y-4">
            <p role="alert" className="rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700">
              Este enlace no es valido. Solicite uno nuevo desde la pantalla de
              recuperacion.
            </p>
            <Link
              to="/recuperar-password"
              className="block text-center text-sm font-medium text-educk-700 hover:underline"
            >
              Solicitar un enlace nuevo
            </Link>
          </div>
        ) : listo ? (
          <div className="mt-8 space-y-4">
            <p role="status" className="rounded-lg bg-green-50 px-4 py-3 text-sm text-green-800">
              Contrasena actualizada correctamente. Ya puede iniciar sesion.
            </p>
            <button
              type="button"
              onClick={() => navigate('/login', { replace: true })}
              className="w-full rounded-lg bg-educk-600 px-4 py-2.5 font-semibold text-white
                         hover:bg-educk-700 focus:outline-none focus:ring-2 focus:ring-educk-500
                         focus:ring-offset-2"
            >
              Ir al inicio de sesion
            </button>
          </div>
        ) : (
          <form onSubmit={onSubmit} className="mt-8 space-y-5" noValidate>
            <div>
              <label htmlFor="password" className="block text-sm font-medium text-gray-800">
                Nueva contrasena
              </label>
              <input
                id="password"
                name="password"
                type="password"
                required
                minLength={8}
                autoComplete="new-password"
                autoFocus
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2 text-gray-900
                           focus:border-educk-500 focus:outline-none focus:ring-2 focus:ring-educk-500"
                aria-describedby="ayuda-password"
              />
              <p id="ayuda-password" className="mt-1 text-xs text-gray-500">
                Al menos 8 caracteres.
              </p>
            </div>

            <div>
              <label htmlFor="confirmacion" className="block text-sm font-medium text-gray-800">
                Repita la contrasena
              </label>
              <input
                id="confirmacion"
                name="confirmacion"
                type="password"
                required
                autoComplete="new-password"
                value={confirmacion}
                onChange={(e) => setConfirmacion(e.target.value)}
                className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2 text-gray-900
                           focus:border-educk-500 focus:outline-none focus:ring-2 focus:ring-educk-500"
              />
            </div>

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
              {enviando ? 'Guardando...' : 'Cambiar contrasena'}
            </button>
          </form>
        )}
      </div>
    </main>
  );
}
