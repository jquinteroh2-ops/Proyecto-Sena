import { Navigate, Route, Routes } from 'react-router-dom';
import RutaProtegida from './components/RutaProtegida';
import Inicio from './pages/Inicio';
import Login from './pages/Login';

// Enrutado de la SPA. La ruta /login es la unica publica; el resto exige sesion
// activa (RS-03). El interceptor de axios redirige aqui ante un 401 (RNF-06).
export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route
        path="/"
        element={
          <RutaProtegida>
            <Inicio />
          </RutaProtegida>
        }
      />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
