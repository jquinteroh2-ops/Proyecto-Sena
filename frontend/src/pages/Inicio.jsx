import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api/axios';
import { useAuthStore } from '../store/authStore';

// Pantalla posterior al inicio de sesion. Consume GET /api/identidad/yo (RS-03)
// para mostrar que perfil academico corresponde a la cuenta autenticada, sin
// que el cliente envie identificadores manipulables (RNF-07).
export default function Inicio() {
  const usuario = useAuthStore((s) => s.usuario);
  const logout = useAuthStore((s) => s.logout);
  const navigate = useNavigate();

  const [identidad, setIdentidad] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    api
      .get('/identidad/yo')
      .then((res) => setIdentidad(res.data))
      .catch((err) =>
        setError(err.response?.data?.mensaje ?? 'No fue posible consultar su identidad.')
      );
  }, []);

  // RF-61: cierra la sesion. El backend limpia su contexto y el cliente descarta
  // el token; al ser JWT sin estado, descartarlo es lo que termina la sesion.
  async function cerrarSesion() {
    try {
      await api.post('/auth/logout');
    } catch {
      // El cierre de sesion local no debe depender de que el backend responda.
    }
    logout();
    navigate('/login', { replace: true });
  }

  return (
    <div className="min-h-screen bg-educk-50">
      <header className="bg-white shadow-sm">
        <div className="mx-auto flex max-w-4xl flex-wrap items-center justify-between gap-3 px-4 py-4">
          <h1 className="text-xl font-bold text-educk-700">EduckTrack</h1>
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

      <main className="mx-auto max-w-4xl px-4 py-8">
        {/* HU-01: la contrasena inicial debe cambiarse en el primer ingreso. */}
        {usuario?.debeCambiarPassword && (
          <p className="mb-6 rounded-lg bg-amber-50 px-4 py-3 text-sm text-amber-900">
            Esta usando una contrasena inicial. El cambio de contrasena estara
            disponible cuando se implemente el modulo de recuperacion (RF-64).
          </p>
        )}

        <h2 className="text-2xl font-semibold text-gray-900">
          Hola, {usuario?.nombre}
        </h2>

        {error && (
          <p role="alert" className="mt-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700">
            {error}
          </p>
        )}

        {identidad && (
          <section className="mt-6 rounded-2xl bg-white p-6 shadow">
            <h3 className="text-sm font-semibold uppercase tracking-wide text-gray-500">
              Identidad resuelta por el servidor
            </h3>
            <dl className="mt-4 grid gap-4 sm:grid-cols-2">
              <Dato etiqueta="Usuario" valor={`#${identidad.usuarioId}`} />
              <Dato etiqueta="Correo" valor={identidad.correo} />
              <Dato etiqueta="Roles" valor={identidad.roles.join(', ') || '-'} />
              <Dato
                etiqueta="Perfil academico"
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
          </section>
        )}

        <p className="mt-8 text-xs text-gray-400">
          Fase 1 - identidad y ownership. Las vistas por rol (HU-01..HU-30) llegan en fases posteriores.
        </p>
      </main>
    </div>
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
