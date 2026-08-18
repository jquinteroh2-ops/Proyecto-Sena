import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import api from '../api/axios';
import Layout from '../components/Layout';
import { useAuthStore } from '../store/authStore';

// Pantalla posterior al inicio de sesion. Consume GET /api/identidad/yo (RS-03)
// para mostrar que perfil academico corresponde a la cuenta autenticada, sin
// que el cliente envie identificadores manipulables (RNF-07).
export default function Inicio() {
  const usuario = useAuthStore((s) => s.usuario);

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

  return (
    <Layout>
      {/* HU-01: la contrasena inicial debe cambiarse en el primer ingreso. Desde
          la Fase 10 el aviso lleva a alguna parte: antes anunciaba que el cambio
          "estara disponible", y para entonces RF-64 ya estaba implementado en el
          backend pero no habia pantalla. */}
      {usuario?.debeCambiarPassword && (
        <p className="mb-6 rounded-lg bg-amber-50 px-4 py-3 text-sm text-amber-900">
          Esta usando una contrasena inicial.{' '}
          <Link to="/recuperar-password" className="font-semibold underline">
            Cambiela ahora
          </Link>{' '}
          para asegurar su cuenta.
        </p>
      )}

      <h2 className="text-2xl font-semibold text-gray-900">Hola, {usuario?.nombre}</h2>

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

      <section className="mt-6 grid gap-4 sm:grid-cols-2">
        <Tarjeta
          a="/boletin"
          titulo="Boletin de calificaciones"
          descripcion="Consulte el boletin de un periodo cerrado (RF-35)."
        />
        {(usuario?.roles ?? []).some((r) => r === 'RECTOR' || r === 'ADMINISTRADOR') && (
          <Tarjeta
            a="/parametros"
            titulo="Parametros institucionales"
            descripcion="Escala de calificacion y minimos institucionales (RF-59)."
          />
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

function Tarjeta({ a, titulo, descripcion }) {
  return (
    <Link
      to={a}
      className="rounded-2xl bg-white p-6 shadow transition hover:shadow-md focus:outline-none
                 focus:ring-2 focus:ring-educk-500"
    >
      <h3 className="font-semibold text-educk-700">{titulo}</h3>
      <p className="mt-1 text-sm text-gray-600">{descripcion}</p>
    </Link>
  );
}
