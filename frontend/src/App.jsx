import { Navigate, Route, Routes } from 'react-router-dom';
import RutaProtegida from './components/RutaProtegida';
import Boletin from './pages/Boletin';
import Inicio from './pages/Inicio';
import Login from './pages/Login';
import Parametros from './pages/Parametros';
import RecuperarPassword from './pages/RecuperarPassword';
import RestablecerPassword from './pages/RestablecerPassword';

// Enrutado de la SPA. El interceptor de axios redirige al login ante un 401
// (RNF-06).
//
// Las rutas de recuperacion (RF-64) son publicas por necesidad: quien recupera
// su contrasena no tiene sesion, y lo que autoriza la operacion es el enlace de
// un solo uso que recibio por correo. Coinciden con las rutas publicas que
// declara SecurityConfig en el backend.
export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/recuperar-password" element={<RecuperarPassword />} />
      <Route path="/restablecer-password" element={<RestablecerPassword />} />

      <Route
        path="/"
        element={
          <RutaProtegida>
            <Inicio />
          </RutaProtegida>
        }
      />
      <Route
        path="/boletin"
        element={
          <RutaProtegida>
            <Boletin />
          </RutaProtegida>
        }
      />
      <Route
        path="/parametros"
        element={
          <RutaProtegida>
            <Parametros />
          </RutaProtegida>
        }
      />

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
