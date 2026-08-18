import { useState } from 'react';
import { Link } from 'react-router-dom';
import api from '../api/axios';

// Solicitud del enlace de recuperacion (RF-64, HU-04).
//
// DECISION DE DISENO: la pantalla muestra el MISMO mensaje exista o no la
// cuenta, porque el backend responde lo mismo en ambos casos a proposito. Si
// aqui se distinguiera ("ese correo no esta registrado"), el cliente desharia
// la propiedad que el servidor se cuida de mantener: que este endpoint publico
// no sirva para averiguar que correos tienen cuenta.
export default function RecuperarPassword() {
  const [correo, setCorreo] = useState('');
  const [enviado, setEnviado] = useState(false);
  const [error, setError] = useState(null);
  const [enviando, setEnviando] = useState(false);

  async function onSubmit(evento) {
    evento.preventDefault();
    setError(null);
    setEnviando(true);
    try {
      await api.post('/auth/recuperar-password', { correo });
      setEnviado(true);
    } catch (err) {
      // Solo puede fallar por formato invalido o por caida del servidor: el
      // backend no falla por que la cuenta no exista.
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
          <p className="mt-1 text-gray-600">Recuperar contrasena</p>
        </header>

        {enviado ? (
          <div className="mt-8 space-y-4">
            <p role="status" className="rounded-lg bg-green-50 px-4 py-3 text-sm text-green-800">
              Si el correo esta registrado, recibira un enlace para restablecer
              la contrasena. El enlace caduca en 30 minutos y solo puede usarse
              una vez.
            </p>
            <Link
              to="/login"
              className="block text-center text-sm font-medium text-educk-700 hover:underline"
            >
              Volver al inicio de sesion
            </Link>
          </div>
        ) : (
          <form onSubmit={onSubmit} className="mt-8 space-y-5" noValidate>
            <p className="text-sm text-gray-600">
              Indique su correo institucional y le enviaremos un enlace para
              elegir una contrasena nueva.
            </p>

            <div>
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
              {enviando ? 'Enviando...' : 'Enviar enlace'}
            </button>

            <Link
              to="/login"
              className="block text-center text-sm font-medium text-educk-700 hover:underline"
            >
              Volver al inicio de sesion
            </Link>
          </form>
        )}
      </div>
    </main>
  );
}
