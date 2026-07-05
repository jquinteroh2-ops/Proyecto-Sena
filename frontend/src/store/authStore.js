import { create } from 'zustand';
import { persist } from 'zustand/middleware';

// Estado global de sesion/rol (RS-03 RBAC en cliente). Persistido en localStorage
// para sobrevivir recargas. La autorizacion real siempre la impone el backend.
export const useAuthStore = create(
  persist(
    (set) => ({
      token: null,
      usuario: null, // { id, nombre, correo, roles: [] }

      login: ({ token, usuario }) => set({ token, usuario }),
      logout: () => set({ token: null, usuario: null }),

      tieneRol: (rol) => {
        const roles = (useAuthStore.getState().usuario?.roles) || [];
        return roles.includes(rol);
      },
    }),
    { name: 'educktrack-auth' }
  )
);
