import { useEffect, useState } from 'react';
import api from './api/axios';

// Pantalla inicial de la Fase 0: verifica conectividad con el backend.
// Las vistas por rol (HU-01..HU-30) se construyen en la Fase 11.
export default function App() {
  const [estado, setEstado] = useState('consultando...');
  const [error, setError] = useState(null);

  useEffect(() => {
    api
      .get('/health')
      .then((res) => setEstado(res.data.status))
      .catch((err) => setError(err.message));
  }, []);

  return (
    <div className="min-h-screen flex items-center justify-center bg-educk-50">
      <div className="bg-white shadow-lg rounded-2xl p-10 max-w-md w-full text-center">
        <h1 className="text-3xl font-bold text-educk-700">EduckTrack</h1>
        <p className="mt-2 text-gray-600">Sistema de gestion academica</p>
        <div className="mt-6 text-sm">
          <span className="text-gray-500">Estado del backend: </span>
          {error ? (
            <span className="font-semibold text-red-600">sin conexion ({error})</span>
          ) : (
            <span className="font-semibold text-green-600">{estado}</span>
          )}
        </div>
        <p className="mt-6 text-xs text-gray-400">Fase 0 - scaffold del proyecto</p>
      </div>
    </div>
  );
}
