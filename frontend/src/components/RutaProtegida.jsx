import { Navigate, useLocation } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';

// Guard de ruta: exige una sesion activa para renderizar la pagina (RS-03).
//
// DECISION DE DISENO: esta comprobacion es solo de navegacion, para no mostrar
// pantallas vacias a quien no ha iniciado sesion. La autorizacion real la impone
// el backend en cada peticion (@PreAuthorize + ownership); un usuario que
// manipule el estado del cliente no obtiene datos, obtiene 401/403.
export default function RutaProtegida({ children }) {
  const token = useAuthStore((s) => s.token);
  const location = useLocation();

  if (!token) {
    // Se recuerda el destino para volver a el tras iniciar sesion.
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }
  return children;
}
