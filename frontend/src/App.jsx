import { Navigate, Route, Routes } from 'react-router-dom';
import RutaProtegida from './components/RutaProtegida';
import Asignaciones from './pages/Asignaciones';
import Asistencia from './pages/Asistencia';
import Boletin from './pages/Boletin';
import Cursos from './pages/Cursos';
import Estudiantes from './pages/Estudiantes';
import Horario from './pages/Horario';
import Inicio from './pages/Inicio';
import Login from './pages/Login';
import MisNotas from './pages/MisNotas';
import Notas from './pages/Notas';
import Parametros from './pages/Parametros';
import RecuperarPassword from './pages/RecuperarPassword';
import Reportes from './pages/Reportes';
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
      <Route
        path="/cursos"
        element={
          <RutaProtegida>
            <Cursos />
          </RutaProtegida>
        }
      />
      <Route
        path="/estudiantes"
        element={
          <RutaProtegida>
            <Estudiantes />
          </RutaProtegida>
        }
      />
      <Route
        path="/asistencia"
        element={
          <RutaProtegida>
            <Asistencia />
          </RutaProtegida>
        }
      />
      <Route
        path="/asignaciones"
        element={
          <RutaProtegida>
            <Asignaciones />
          </RutaProtegida>
        }
      />
      <Route
        path="/horario"
        element={
          <RutaProtegida>
            <Horario />
          </RutaProtegida>
        }
      />
      <Route
        path="/notas"
        element={
          <RutaProtegida>
            <Notas />
          </RutaProtegida>
        }
      />
      <Route
        path="/mis-notas"
        element={
          <RutaProtegida>
            <MisNotas />
          </RutaProtegida>
        }
      />
      <Route
        path="/reportes"
        element={
          <RutaProtegida>
            <Reportes />
          </RutaProtegida>
        }
      />

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
