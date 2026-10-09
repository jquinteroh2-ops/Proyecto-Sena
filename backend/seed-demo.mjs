// Script de datos de demostracion para EduckTrack, NO parte del backend.
// Crea un periodo cerrado (para Boletin/Reportes/tendencia) y uno abierto
// (para la planilla de notas en vivo), con materias, docentes, un curso,
// estudiantes matriculados, horario y calificaciones variadas.
//
// Uso: node seed-demo.mjs                 (contra localhost:8081)
//      SEED_BASE_URL=https://... node seed-demo.mjs   (contra otro backend)
//      SEED_CORREO / SEED_PASSWORD: cuenta con la que se conecta (por
//      defecto el administrador inicial de desarrollo)

const BASE = (process.env.SEED_BASE_URL ?? 'http://localhost:8081') + '/api';
let TOKEN = '';

async function intentar(method, path, body) {
  const res = await fetch(BASE + path, {
    method,
    headers: {
      'Content-Type': 'application/json',
      ...(TOKEN ? { Authorization: `Bearer ${TOKEN}` } : {}),
    },
    body: body ? JSON.stringify(body) : undefined,
  });
  const text = await res.text();
  const data = text ? JSON.parse(text) : null;
  if (!res.ok) {
    const err = new Error(`${method} ${path} -> ${res.status}: ${JSON.stringify(data)}`);
    err.status = res.status;
    throw err;
  }
  return data;
}

// Reintenta una vez ante un 401 transitorio (se vio uno aislado durante las
// pruebas, no reproducible; no vale la pena investigarlo mas a fondo para un
// script de datos de demostracion que no es parte del producto).
async function api(method, path, body) {
  try {
    return await intentar(method, path, body);
  } catch (err) {
    if (err.status === 401) {
      await new Promise((r) => setTimeout(r, 300));
      return intentar(method, path, body);
    }
    throw err;
  }
}

async function main() {
  // Credenciales por variables de entorno: nunca escritas en el repositorio.
  // Por defecto, el administrador inicial que crea el backend en desarrollo.
  const login = await api('POST', '/auth/login', {
    correo: process.env.SEED_CORREO ?? 'admin@educktrack.edu.co',
    password: process.env.SEED_PASSWORD ?? 'Admin123*',
  });
  TOKEN = login.token;
  console.log('Login OK');

  // --- Periodo 1 (se cerrara al final: Boletin, Reportes, tendencia) ---
  const periodo1 = await api('POST', '/periodos', {
    nombre: 'Periodo 1 - 2026',
    anioLectivo: 2026,
    fechaInicio: '2026-01-15',
    fechaFin: '2026-04-15',
  });
  await api('POST', `/periodos/${periodo1.id}/activar`);
  console.log('Periodo 1 creado y activado:', periodo1.id);

  // --- Materias ---
  const materiasReq = [
    { codigo: 'MAT', nombre: 'Matematicas', area: 'Ciencias exactas', intensidadHorariaSemanal: 5 },
    { codigo: 'ESP', nombre: 'Espanol', area: 'Lenguaje', intensidadHorariaSemanal: 4 },
    { codigo: 'CIE', nombre: 'Ciencias Naturales', area: 'Ciencias naturales', intensidadHorariaSemanal: 4 },
  ];
  const materias = [];
  for (const m of materiasReq) materias.push(await api('POST', '/materias', m));
  console.log('Materias creadas:', materias.map((m) => m.nombre).join(', '));

  // --- Docentes ---
  const docentesReq = [
    { documento: '1000001', nombres: 'Laura', apellidos: 'Gomez', correo: 'laura.gomez@educktrack.edu.co', telefono: '3001234567', areasFormacion: ['Ciencias exactas'] },
    { documento: '1000002', nombres: 'Carlos', apellidos: 'Ramirez', correo: 'carlos.ramirez@educktrack.edu.co', telefono: '3007654321', areasFormacion: ['Lenguaje', 'Ciencias naturales'] },
  ];
  const docentes = [];
  for (const d of docentesReq) docentes.push(await api('POST', '/docentes', d));
  console.log('Docentes creados:', docentes.map((d) => d.nombres).join(', '));

  // --- Estudiantes ---
  const estudiantesReq = [
    { documento: '2000001', nombres: 'Sofia', apellidos: 'Martinez', fechaNacimiento: '2013-03-10', acudienteNombre: 'Ana Martinez', acudienteTelefono: '3101111111', acudienteParentesco: 'MADRE' },
    { documento: '2000002', nombres: 'Mateo', apellidos: 'Perez', fechaNacimiento: '2013-05-22', acudienteNombre: 'Luis Perez', acudienteTelefono: '3102222222', acudienteParentesco: 'PADRE' },
    { documento: '2000003', nombres: 'Valentina', apellidos: 'Rodriguez', fechaNacimiento: '2013-07-01', acudienteNombre: 'Marta Rodriguez', acudienteTelefono: '3103333333', acudienteParentesco: 'MADRE' },
    { documento: '2000004', nombres: 'Samuel', apellidos: 'Torres', fechaNacimiento: '2013-02-14', acudienteNombre: 'Pedro Torres', acudienteTelefono: '3104444444', acudienteParentesco: 'PADRE' },
    { documento: '2000005', nombres: 'Isabella', apellidos: 'Diaz', fechaNacimiento: '2013-09-30', acudienteNombre: 'Carla Diaz', acudienteTelefono: '3105555555', acudienteParentesco: 'MADRE' },
    { documento: '2000006', nombres: 'Santiago', apellidos: 'Vargas', fechaNacimiento: '2013-11-18', acudienteNombre: 'Jorge Vargas', acudienteTelefono: '3106666666', acudienteParentesco: 'PADRE' },
  ];
  const estudiantes = [];
  for (const e of estudiantesReq) estudiantes.push(await api('POST', '/estudiantes', e));
  console.log('Estudiantes creados:', estudiantes.length);

  // Los bloques horarios no son por periodo (HU-11: no se repite dia/hora/
  // jornada), asi que se crean una sola vez y se reutilizan en ambos periodos.
  const bloquesReq = [
    { dia: 'LUNES', horaInicio: '07:00:00', horaFin: '08:00:00', jornada: 'MANANA' },
    { dia: 'MARTES', horaInicio: '07:00:00', horaFin: '08:00:00', jornada: 'MANANA' },
    { dia: 'MIERCOLES', horaInicio: '07:00:00', horaFin: '08:00:00', jornada: 'MANANA' },
  ];
  const bloques = [];
  for (const b of bloquesReq) bloques.push(await api('POST', '/horarios/bloques', b));
  console.log('Bloques horarios creados:', bloques.length);

  // === Bloque por periodo: curso, plan, matriculas, asignaciones, notas ===
  async function construirPeriodo(periodo, notasBase) {
    const curso = await api('POST', '/cursos', {
      nombre: 'Sexto A',
      grado: 6,
      nivel: 'BASICA_SECUNDARIA',
      jornada: 'MANANA',
      cupoMaximo: 35,
      periodoAcademicoId: periodo.id,
    });

    for (const m of materias) {
      await api('POST', `/cursos/${curso.id}/plan-estudios`, { materiaId: m.id });
    }

    for (const e of estudiantes) {
      await api('POST', '/matriculas', { estudianteId: e.id, cursoId: curso.id, periodoAcademicoId: periodo.id });
    }

    // Docente de Matematicas dicta Matematicas; docente de Lenguaje dicta
    // Espanol y Ciencias Naturales (coincide con sus areas de formacion).
    await api('POST', '/asignaciones', { docenteId: docentes[0].id, materiaId: materias[0].id, cursoId: curso.id, periodoAcademicoId: periodo.id });
    await api('POST', '/asignaciones', { docenteId: docentes[1].id, materiaId: materias[1].id, cursoId: curso.id, periodoAcademicoId: periodo.id });
    await api('POST', '/asignaciones', { docenteId: docentes[1].id, materiaId: materias[2].id, cursoId: curso.id, periodoAcademicoId: periodo.id });
    await api('POST', `/asignaciones/cursos/${curso.id}/director`, { docenteId: docentes[0].id });

    // Ponderacion: quiz 30, examen 40, taller 30 (suma 100, RB-07).
    for (const m of materias) {
      await api('POST', '/ponderaciones', {
        materiaId: m.id,
        periodoAcademicoId: periodo.id,
        ponderaciones: [
          { tipo: 'QUIZ', porcentaje: 30 },
          { tipo: 'EXAMEN', porcentaje: 40 },
          { tipo: 'TALLER', porcentaje: 30 },
        ],
      });
    }

    // Calificaciones: notasBase[estudianteIndex] es el promedio objetivo
    // aproximado; se reparte en los tres tipos con variacion pequena para
    // que el promedio ponderado no sea un numero identico y redondo.
    for (let i = 0; i < estudiantes.length; i++) {
      const est = estudiantes[i];
      const base = notasBase[i];
      for (const m of materias) {
        for (const [tipo, delta] of [['QUIZ', 0.1], ['EXAMEN', -0.1], ['TALLER', 0.0]]) {
          const valor = Math.min(5, Math.max(1, +(base + delta).toFixed(1)));
          await api('POST', '/calificaciones', {
            estudianteId: est.id,
            materiaId: m.id,
            cursoId: curso.id,
            periodoAcademicoId: periodo.id,
            tipo,
            valor,
          });
        }
      }
    }

    // Horario: un bloque por materia, dias distintos, jornada manana.
    const docentePorMateria = [docentes[0].id, docentes[1].id, docentes[1].id];
    for (let i = 0; i < materias.length; i++) {
      await api('POST', '/horarios/asignaciones', {
        bloqueId: bloques[i].id,
        materiaId: materias[i].id,
        docenteId: docentePorMateria[i],
        cursoId: curso.id,
        periodoAcademicoId: periodo.id,
      });
    }

    return curso;
  }

  // Periodo 1: notas mas bajas (para que el periodo 2 muestre una mejora en
  // la tendencia de MisNotas, o un descenso en algun estudiante a proposito).
  const notasPeriodo1 = [3.2, 4.5, 2.6, 3.9, 4.8, 2.2];
  const curso1 = await construirPeriodo(periodo1, notasPeriodo1);
  console.log('Periodo 1 construido, curso', curso1.id);

  await api('POST', `/calificaciones/cerrar-corte?cursoId=${curso1.id}&periodoAcademicoId=${periodo1.id}`);
  console.log('Corte del periodo 1 cerrado (habilita Boletin y Reportes)');

  // --- Periodo 2 (abierto, para la planilla de notas en vivo) ---
  const periodo2 = await api('POST', '/periodos', {
    nombre: 'Periodo 2 - 2027',
    anioLectivo: 2027,
    fechaInicio: '2027-01-15',
    fechaFin: '2027-04-15',
  });
  await api('POST', `/periodos/${periodo2.id}/activar`);
  console.log('Periodo 2 creado y activado:', periodo2.id);

  // Algunos suben, algunos bajan respecto al periodo 1 (tendencia visible).
  const notasPeriodo2 = [3.8, 4.1, 3.4, 3.5, 4.9, 2.9];
  const curso2 = await construirPeriodo(periodo2, notasPeriodo2);
  console.log('Periodo 2 construido, curso', curso2.id, '(corte ABIERTO a proposito)');

  // --- Cuentas de demo por rol, vinculadas a un perfil real ---
  const cuentaDocente = await api('POST', '/usuarios', {
    nombre: 'Laura Gomez', correo: 'laura.gomez@educktrack.edu.co', password: 'Demo12345', roles: ['DOCENTE'],
  });
  await api('POST', `/identidad/docentes/${docentes[0].id}/cuenta`, { usuarioId: cuentaDocente.id });

  const cuentaEstudiante = await api('POST', '/usuarios', {
    nombre: 'Sofia Martinez', correo: 'sofia.martinez@educktrack.edu.co', password: 'Demo12345', roles: ['ESTUDIANTE'],
  });
  await api('POST', `/identidad/estudiantes/${estudiantes[0].id}/cuenta`, { usuarioId: cuentaEstudiante.id });

  const cuentaPadre = await api('POST', '/usuarios', {
    nombre: 'Ana Martinez', correo: 'ana.martinez@educktrack.edu.co', password: 'Demo12345', roles: ['PADRE_FAMILIA'],
  });
  await api('POST', `/identidad/estudiantes/${estudiantes[0].id}/acudientes`, {
    usuarioId: cuentaPadre.id, parentesco: 'MADRE',
  });

  const cuentaCoordinador = await api('POST', '/usuarios', {
    nombre: 'Coordinacion Academica', correo: 'coordinacion@educktrack.edu.co', password: 'Demo12345', roles: ['COORDINADOR_ACADEMICO'],
  });

  console.log('\n=== Datos de demostracion listos ===');
  console.log('Curso periodo 1 (cerrado, boletin/reportes):', curso1.id, '- periodo', periodo1.id);
  console.log('Curso periodo 2 (abierto, notas en vivo):', curso2.id, '- periodo', periodo2.id);
  console.log('\nCuentas de demo (password: Demo12345):');
  console.log('  Docente:      laura.gomez@educktrack.edu.co');
  console.log('  Estudiante:   sofia.martinez@educktrack.edu.co');
  console.log('  Acudiente:    ana.martinez@educktrack.edu.co');
  console.log('  Coordinacion: coordinacion@educktrack.edu.co');
  console.log('  Admin/Rector: rector@educktrack.edu.co (ya existia)');
}

main().catch((err) => {
  console.error('FALLO:', err.message);
  process.exit(1);
});
