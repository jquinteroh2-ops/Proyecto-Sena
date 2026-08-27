import { useEffect, useState } from 'react';
import api from '../api/axios';

// Catalogos que varias pantallas de gestion necesitan para poblar selectores
// (cursos, materias, docentes, periodos). Se centralizan aqui porque
// Asignaciones, Horario, Notas y Reportes los piden todos.
export function useCatalogos({ cursos, materias, docentes, periodos } = {}) {
  const [datos, setDatos] = useState({ cursos: [], materias: [], docentes: [], periodos: [] });
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const pedidos = [];
    const claves = [];
    if (cursos) { pedidos.push(api.get('/cursos')); claves.push('cursos'); }
    if (materias) { pedidos.push(api.get('/materias')); claves.push('materias'); }
    if (docentes) { pedidos.push(api.get('/docentes')); claves.push('docentes'); }
    if (periodos) { pedidos.push(api.get('/periodos')); claves.push('periodos'); }

    Promise.all(pedidos)
      .then((respuestas) => {
        const nuevo = {};
        respuestas.forEach((res, i) => { nuevo[claves[i]] = res.data; });
        setDatos((prev) => ({ ...prev, ...nuevo }));
      })
      .catch(() => setError('No fue posible cargar los catalogos institucionales.'))
      .finally(() => setCargando(false));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return { ...datos, cargandoCatalogos: cargando, errorCatalogos: error };
}

export const NOMBRES_TIPO_EVALUACION = {
  QUIZ: 'Quiz',
  EXAMEN: 'Examen',
  TALLER: 'Taller',
  PROYECTO: 'Proyecto',
  AUTOEVALUACION: 'Autoevaluacion',
};

export const NOMBRES_DIA = {
  LUNES: 'Lunes',
  MARTES: 'Martes',
  MIERCOLES: 'Miercoles',
  JUEVES: 'Jueves',
  VIERNES: 'Viernes',
  SABADO: 'Sabado',
  DOMINGO: 'Domingo',
};

export const ORDEN_DIAS = ['LUNES', 'MARTES', 'MIERCOLES', 'JUEVES', 'VIERNES', 'SABADO', 'DOMINGO'];
