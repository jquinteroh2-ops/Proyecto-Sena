import { useState, useEffect, useRef } from "react";

// ─── DATA ────────────────────────────────────────────────────────────────────
const USER = {
  name: "Valentina Ríos",
  role: "Estudiante",
  avatar: "VR",
  grade: "Grado 10 - A",
};

const MATERIAS = [
  {
    id: 1, name: "Matemáticas", teacher: "Prof. Carlos Mendoza", icon: "📐",
    color: "#1565C0", promedio: 4.2,
    actividades: [
      { nombre: "Parcial 1", fecha: "Mar 12", porcentaje: 30, nota: 4.5 },
      { nombre: "Taller Álgebra", fecha: "Mar 20", porcentaje: 20, nota: 3.8 },
      { nombre: "Quiz Geometría", fecha: "Abr 3", porcentaje: 15, nota: 4.0 },
      { nombre: "Parcial 2", fecha: "Abr 18", porcentaje: 35, nota: 4.4 },
    ],
  },
  {
    id: 2, name: "Física", teacher: "Prof. Ana Gómez", icon: "⚛️",
    color: "#0d47a1", promedio: 3.6,
    actividades: [
      { nombre: "Lab. Movimiento", fecha: "Mar 10", porcentaje: 25, nota: 3.5 },
      { nombre: "Taller Ondas", fecha: "Mar 25", porcentaje: 25, nota: 3.8 },
      { nombre: "Parcial 1", fecha: "Abr 8", porcentaje: 50, nota: 3.4 },
    ],
  },
  {
    id: 3, name: "Inglés", teacher: "Prof. Sarah Williams", icon: "🌐",
    color: "#1976D2", promedio: 4.7,
    actividades: [
      { nombre: "Speaking Test", fecha: "Mar 15", porcentaje: 30, nota: 4.8 },
      { nombre: "Writing Essay", fecha: "Mar 28", porcentaje: 30, nota: 4.6 },
      { nombre: "Grammar Quiz", fecha: "Abr 10", porcentaje: 40, nota: 4.7 },
    ],
  },
  {
    id: 4, name: "Historia", teacher: "Prof. Julio Torres", icon: "📜",
    color: "#1565C0", promedio: 2.9,
    actividades: [
      { nombre: "Ensayo S.XIX", fecha: "Mar 18", porcentaje: 35, nota: 2.8 },
      { nombre: "Parcial 1", fecha: "Abr 5", porcentaje: 40, nota: 2.9 },
      { nombre: "Exposición", fecha: "Abr 20", porcentaje: 25, nota: 3.1 },
    ],
  },
  {
    id: 5, name: "Programación", teacher: "Prof. Diego Vargas", icon: "💻",
    color: "#0d47a1", promedio: 4.9,
    actividades: [
      { nombre: "Proyecto Web", fecha: "Mar 22", porcentaje: 40, nota: 5.0 },
      { nombre: "Quiz Algoritmos", fecha: "Abr 2", porcentaje: 20, nota: 4.8 },
      { nombre: "App React", fecha: "Abr 19", porcentaje: 40, nota: 4.9 },
    ],
  },
];

const TAREAS = [
  { id: 1, materia: "Matemáticas", titulo: "Ejercicios Cálculo Cap. 5", vence: "Hoy", urgente: true, icon: "📐" },
  { id: 2, materia: "Física", titulo: "Informe Lab. Termodinámica", vence: "Mañana", urgente: true, icon: "⚛️" },
  { id: 3, materia: "Inglés", titulo: "Reading Comprehension Unit 7", vence: "Vie 26 Abr", urgente: false, icon: "🌐" },
  { id: 4, materia: "Historia", titulo: "Línea de tiempo S.XX", vence: "Lun 28 Abr", urgente: false, icon: "📜" },
  { id: 5, materia: "Programación", titulo: "API REST con Node.js", vence: "Mar 29 Abr", urgente: false, icon: "💻" },
];

const EVENTOS = [
  { fecha: "Hoy", hora: "2:00 PM", titulo: "Clase extra Matemáticas", tipo: "clase" },
  { fecha: "Vie 26", hora: "10:00 AM", titulo: "Entrega Proyecto Física", tipo: "entrega" },
  { fecha: "Lun 29", hora: "8:00 AM", titulo: "Parcial de Historia", tipo: "parcial" },
  { fecha: "Mar 30", hora: "3:00 PM", titulo: "Reunión de padres", tipo: "reunion" },
];

const ASISTENCIA_MATERIAS = [
  { materia: "Matemáticas", presentes: 18, total: 20, icon: "📐" },
  { materia: "Física", presentes: 16, total: 20, icon: "⚛️" },
  { materia: "Inglés", presentes: 20, total: 20, icon: "🌐" },
  { materia: "Historia", presentes: 15, total: 20, icon: "📜" },
  { materia: "Programación", presentes: 19, total: 20, icon: "💻" },
];

// ─── HELPERS ─────────────────────────────────────────────────────────────────
const noteColor = (n) => n >= 3.5 ? "#22c55e" : n >= 3.0 ? "#f59e0b" : "#ef4444";
const noteBg = (n) => n >= 3.5 ? "#dcfce7" : n >= 3.0 ? "#fef3c7" : "#fee2e2";
const noteLabel = (n) => n >= 3.5 ? "Aprobado" : n >= 3.0 ? "En riesgo" : "Reprobado";
const promedioGeneral = (MATERIAS.reduce((a, m) => a + m.promedio, 0) / MATERIAS.length).toFixed(1);
const asistenciaGlobal = Math.round(
  ASISTENCIA_MATERIAS.reduce((a, m) => a + (m.presentes / m.total) * 100, 0) / ASISTENCIA_MATERIAS.length
);

// ─── STYLES ──────────────────────────────────────────────────────────────────
const css = `
  @import url('https://fonts.googleapis.com/css2?family=DM+Sans:ital,opsz,wght@0,9..40,300;0,9..40,400;0,9..40,500;0,9..40,600;0,9..40,700;1,9..40,400&family=Space+Grotesk:wght@400;500;600;700&display=swap');

  *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }

  :root {
    --blue: #1565C0;
    --blue-dark: #0d47a1;
    --blue-light: #1976D2;
    --blue-50: #e3f2fd;
    --blue-100: #bbdefb;
    --blue-200: #90caf9;
    --white: #ffffff;
    --gray-50: #f8faff;
    --gray-100: #f0f4ff;
    --gray-200: #e2eaf8;
    --gray-400: #94a3b8;
    --gray-600: #475569;
    --gray-800: #1e293b;
    --sidebar-w: 240px;
    --nav-h: 64px;
    --radius: 14px;
    --shadow: 0 2px 16px rgba(21,101,192,.10);
    --shadow-md: 0 4px 24px rgba(21,101,192,.16);
    --transition: all .22s cubic-bezier(.4,0,.2,1);
  }

  body { font-family: 'DM Sans', sans-serif; background: var(--gray-50); color: var(--gray-800); }

  /* ── LAYOUT ── */
  .app { display: flex; min-height: 100vh; }

  /* ── SIDEBAR ── */
  .sidebar {
    width: var(--sidebar-w); min-height: 100vh; background: var(--blue-dark);
    display: flex; flex-direction: column; position: fixed; left: 0; top: 0; z-index: 100;
    transition: var(--transition);
    box-shadow: 4px 0 24px rgba(13,71,161,.18);
  }
  .sidebar-logo {
    padding: 22px 20px 18px; display: flex; align-items: center; gap: 10px;
    border-bottom: 1px solid rgba(255,255,255,.10);
  }
  .logo-icon {
    width: 36px; height: 36px; background: var(--blue-light); border-radius: 10px;
    display: flex; align-items: center; justify-content: center; font-size: 18px;
    box-shadow: 0 2px 8px rgba(21,101,192,.4);
  }
  .logo-text { font-family: 'Space Grotesk', sans-serif; font-size: 18px; font-weight: 700; color: white; letter-spacing: -.3px; }
  .logo-sub { font-size: 10px; color: rgba(255,255,255,.5); font-weight: 400; display: block; }

  .sidebar-nav { flex: 1; padding: 16px 12px; display: flex; flex-direction: column; gap: 2px; }
  .nav-section { font-size: 10px; font-weight: 600; color: rgba(255,255,255,.35);
    letter-spacing: 1.2px; text-transform: uppercase; padding: 12px 10px 6px; }

  .nav-item {
    display: flex; align-items: center; gap: 12px; padding: 10px 12px; border-radius: 10px;
    color: rgba(255,255,255,.65); font-size: 14px; font-weight: 500; cursor: pointer;
    transition: var(--transition); position: relative; border: none; background: none; width: 100%; text-align: left;
  }
  .nav-item:hover { background: rgba(255,255,255,.08); color: white; }
  .nav-item.active { background: rgba(255,255,255,.15); color: white; }
  .nav-item.active::before {
    content: ''; position: absolute; left: 0; top: 50%; transform: translateY(-50%);
    width: 3px; height: 60%; background: var(--blue-200); border-radius: 0 3px 3px 0;
  }
  .nav-icon { font-size: 17px; width: 22px; text-align: center; flex-shrink: 0; }
  .nav-badge {
    margin-left: auto; background: #ef4444; color: white; font-size: 10px; font-weight: 700;
    padding: 2px 6px; border-radius: 20px; min-width: 18px; text-align: center;
  }
  .sidebar-footer {
    padding: 16px 12px; border-top: 1px solid rgba(255,255,255,.08);
  }
  .sidebar-user {
    display: flex; align-items: center; gap: 10px; padding: 10px 12px; border-radius: 10px;
    cursor: pointer; transition: var(--transition);
  }
  .sidebar-user:hover { background: rgba(255,255,255,.08); }
  .avatar-sm {
    width: 32px; height: 32px; border-radius: 10px; background: var(--blue-light);
    display: flex; align-items: center; justify-content: center; font-size: 12px;
    font-weight: 700; color: white; flex-shrink: 0;
  }
  .user-name-sm { font-size: 13px; font-weight: 600; color: white; line-height: 1.2; }
  .user-role-sm { font-size: 11px; color: rgba(255,255,255,.4); }

  /* ── TOPBAR ── */
  .topbar {
    position: fixed; top: 0; left: var(--sidebar-w); right: 0; height: var(--nav-h); z-index: 90;
    background: rgba(255,255,255,.92); backdrop-filter: blur(12px);
    border-bottom: 1px solid var(--gray-200);
    display: flex; align-items: center; justify-content: space-between; padding: 0 28px;
    box-shadow: 0 1px 12px rgba(21,101,192,.07);
  }
  .topbar-left { display: flex; flex-direction: column; }
  .topbar-title { font-family: 'Space Grotesk', sans-serif; font-size: 18px; font-weight: 700; color: var(--gray-800); }
  .topbar-sub { font-size: 12px; color: var(--gray-400); margin-top: 1px; }
  .topbar-right { display: flex; align-items: center; gap: 12px; }
  .topbar-btn {
    width: 38px; height: 38px; border-radius: 10px; border: 1px solid var(--gray-200);
    background: white; display: flex; align-items: center; justify-content: center;
    cursor: pointer; transition: var(--transition); font-size: 16px; position: relative;
    color: var(--gray-600);
  }
  .topbar-btn:hover { background: var(--blue-50); border-color: var(--blue-100); color: var(--blue); }
  .notif-dot {
    position: absolute; top: 6px; right: 6px; width: 8px; height: 8px; border-radius: 50%;
    background: #ef4444; border: 2px solid white;
  }
  .topbar-avatar {
    width: 38px; height: 38px; border-radius: 10px; background: var(--blue);
    display: flex; align-items: center; justify-content: center; font-size: 13px;
    font-weight: 700; color: white; cursor: pointer;
    box-shadow: 0 2px 8px rgba(21,101,192,.3);
  }
  .topbar-name { font-size: 14px; font-weight: 600; color: var(--gray-800); }
  .topbar-grade { font-size: 11px; color: var(--gray-400); }

  /* ── MAIN ── */
  .main { margin-left: var(--sidebar-w); margin-top: var(--nav-h); padding: 28px; min-height: calc(100vh - var(--nav-h)); }

  /* ── CARDS ── */
  .card {
    background: white; border-radius: var(--radius); box-shadow: var(--shadow);
    border: 1px solid var(--gray-200); transition: var(--transition);
    overflow: hidden;
  }
  .card:hover { box-shadow: var(--shadow-md); transform: translateY(-1px); }
  .card-header {
    padding: 18px 20px 14px; display: flex; align-items: center; justify-content: space-between;
    border-bottom: 1px solid var(--gray-100);
  }
  .card-title { font-family: 'Space Grotesk', sans-serif; font-size: 14px; font-weight: 600; color: var(--gray-800); }
  .card-body { padding: 18px 20px; }

  /* ── STAT CARDS ── */
  .stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 18px; margin-bottom: 24px; }

  .stat-card {
    background: white; border-radius: var(--radius); padding: 20px;
    border: 1px solid var(--gray-200); box-shadow: var(--shadow);
    transition: var(--transition); position: relative; overflow: hidden;
  }
  .stat-card:hover { box-shadow: var(--shadow-md); transform: translateY(-2px); }
  .stat-card::after {
    content: ''; position: absolute; bottom: 0; left: 0; right: 0; height: 3px;
  }
  .stat-card.blue::after { background: linear-gradient(90deg, var(--blue), var(--blue-light)); }
  .stat-card.green::after { background: linear-gradient(90deg, #22c55e, #4ade80); }
  .stat-card.amber::after { background: linear-gradient(90deg, #f59e0b, #fcd34d); }
  .stat-card.purple::after { background: linear-gradient(90deg, #8b5cf6, #a78bfa); }

  .stat-icon {
    width: 44px; height: 44px; border-radius: 12px; display: flex;
    align-items: center; justify-content: center; font-size: 20px; margin-bottom: 14px;
  }
  .stat-icon.blue { background: var(--blue-50); }
  .stat-icon.green { background: #dcfce7; }
  .stat-icon.amber { background: #fef3c7; }
  .stat-icon.purple { background: #ede9fe; }

  .stat-value { font-family: 'Space Grotesk', sans-serif; font-size: 28px; font-weight: 700; color: var(--gray-800); line-height: 1; }
  .stat-label { font-size: 13px; color: var(--gray-400); margin-top: 4px; font-weight: 400; }
  .stat-sub { font-size: 12px; color: var(--gray-600); margin-top: 8px; display: flex; align-items: center; gap: 4px; }
  .stat-badge { padding: 2px 8px; border-radius: 20px; font-size: 11px; font-weight: 600; }
  .badge-green { background: #dcfce7; color: #16a34a; }
  .badge-amber { background: #fef3c7; color: #d97706; }
  .badge-red { background: #fee2e2; color: #dc2626; }
  .badge-blue { background: var(--blue-50); color: var(--blue); }

  /* ── PROGRESS BAR ── */
  .progress-wrap { background: var(--gray-100); border-radius: 99px; overflow: hidden; }
  .progress-bar { height: 100%; border-radius: 99px; transition: width 1s cubic-bezier(.4,0,.2,1); }

  /* ── GRID LAYOUTS ── */
  .grid-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin-bottom: 24px; }
  .grid-3 { display: grid; grid-template-columns: 2fr 1fr; gap: 20px; margin-bottom: 24px; }

  /* ── TABLE ── */
  .data-table { width: 100%; border-collapse: collapse; }
  .data-table th {
    text-align: left; font-size: 11px; font-weight: 600; color: var(--gray-400);
    text-transform: uppercase; letter-spacing: .8px; padding: 10px 14px;
    background: var(--gray-50); border-bottom: 1px solid var(--gray-200);
  }
  .data-table td {
    padding: 12px 14px; font-size: 13.5px; border-bottom: 1px solid var(--gray-100);
    color: var(--gray-800);
  }
  .data-table tr:last-child td { border-bottom: none; }
  .data-table tr:hover td { background: var(--blue-50); }
  .nota-chip {
    display: inline-flex; align-items: center; gap: 4px; padding: 4px 10px;
    border-radius: 99px; font-size: 12px; font-weight: 600;
  }

  /* ── TASKS ── */
  .task-item {
    display: flex; align-items: center; gap: 12px; padding: 12px 0;
    border-bottom: 1px solid var(--gray-100); cursor: pointer; transition: var(--transition);
  }
  .task-item:last-child { border-bottom: none; }
  .task-item:hover { padding-left: 6px; }
  .task-dot { width: 10px; height: 10px; border-radius: 50%; flex-shrink: 0; }
  .task-info { flex: 1; min-width: 0; }
  .task-title { font-size: 13.5px; font-weight: 500; color: var(--gray-800); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
  .task-meta { font-size: 11.5px; color: var(--gray-400); margin-top: 2px; }
  .task-due { font-size: 11.5px; font-weight: 600; padding: 3px 9px; border-radius: 99px; flex-shrink: 0; }
  .task-due.urgent { background: #fee2e2; color: #dc2626; }
  .task-due.normal { background: var(--blue-50); color: var(--blue); }

  /* ── EVENTS ── */
  .event-item { display: flex; gap: 14px; padding: 10px 0; border-bottom: 1px solid var(--gray-100); }
  .event-item:last-child { border-bottom: none; }
  .event-date {
    width: 40px; height: 40px; border-radius: 10px; background: var(--blue-50);
    display: flex; flex-direction: column; align-items: center; justify-content: center; flex-shrink: 0;
  }
  .event-day { font-family: 'Space Grotesk', sans-serif; font-size: 15px; font-weight: 700; color: var(--blue); line-height: 1; }
  .event-mon { font-size: 9px; color: var(--blue-light); font-weight: 500; text-transform: uppercase; }
  .event-info { flex: 1; }
  .event-title { font-size: 13.5px; font-weight: 500; color: var(--gray-800); }
  .event-hora { font-size: 11.5px; color: var(--gray-400); margin-top: 2px; }
  .event-tipo { font-size: 10px; font-weight: 600; padding: 2px 8px; border-radius: 99px; float: right; }
  .tipo-clase { background: var(--blue-50); color: var(--blue); }
  .tipo-entrega { background: #fef3c7; color: #d97706; }
  .tipo-parcial { background: #fee2e2; color: #dc2626; }
  .tipo-reunion { background: #f3e8ff; color: #7c3aed; }

  /* ── BAR CHART ── */
  .chart-wrap { display: flex; align-items: flex-end; gap: 14px; height: 130px; padding: 0 4px; }
  .chart-bar-wrap { display: flex; flex-direction: column; align-items: center; gap: 6px; flex: 1; }
  .chart-bar {
    width: 100%; border-radius: 8px 8px 0 0; transition: height 1s cubic-bezier(.4,0,.2,1);
    position: relative; cursor: pointer; min-width: 28px;
  }
  .chart-bar:hover { filter: brightness(1.1); }
  .chart-label { font-size: 10px; color: var(--gray-400); font-weight: 500; text-align: center; white-space: nowrap; }
  .chart-val { font-size: 11px; font-weight: 700; text-align: center; }

  /* ── LOGIN ── */
  .login-wrap {
    min-height: 100vh; display: flex; align-items: center; justify-content: center;
    background: linear-gradient(135deg, var(--blue-dark) 0%, var(--blue) 50%, var(--blue-light) 100%);
    position: relative; overflow: hidden;
  }
  .login-bg-circle {
    position: absolute; border-radius: 50%;
    background: rgba(255,255,255,.04);
    pointer-events: none;
  }
  .login-card {
    background: white; border-radius: 20px; padding: 44px 40px;
    width: 100%; max-width: 420px; box-shadow: 0 24px 80px rgba(13,71,161,.35);
    position: relative; z-index: 2;
    animation: slideUp .5s cubic-bezier(.4,0,.2,1) both;
  }
  @keyframes slideUp { from { opacity: 0; transform: translateY(24px); } to { opacity: 1; transform: translateY(0); } }

  .login-logo { text-align: center; margin-bottom: 32px; }
  .login-logo-icon {
    width: 60px; height: 60px; background: var(--blue); border-radius: 16px;
    display: flex; align-items: center; justify-content: center; font-size: 28px;
    margin: 0 auto 12px; box-shadow: 0 8px 24px rgba(21,101,192,.35);
  }
  .login-title { font-family: 'Space Grotesk', sans-serif; font-size: 26px; font-weight: 700; color: var(--gray-800); }
  .login-sub { font-size: 14px; color: var(--gray-400); margin-top: 4px; }

  .form-group { margin-bottom: 18px; }
  .form-label { display: block; font-size: 13px; font-weight: 600; color: var(--gray-600); margin-bottom: 7px; }
  .form-input {
    width: 100%; padding: 12px 16px; border: 1.5px solid var(--gray-200); border-radius: 10px;
    font-size: 14px; font-family: 'DM Sans', sans-serif; color: var(--gray-800);
    transition: var(--transition); background: var(--gray-50); outline: none;
  }
  .form-input:focus { border-color: var(--blue); background: white; box-shadow: 0 0 0 3px rgba(21,101,192,.12); }
  .form-input.error { border-color: #ef4444; box-shadow: 0 0 0 3px rgba(239,68,68,.10); }
  .input-wrap { position: relative; }
  .input-eye {
    position: absolute; right: 14px; top: 50%; transform: translateY(-50%);
    cursor: pointer; font-size: 16px; color: var(--gray-400); background: none; border: none;
    padding: 0; display: flex; align-items: center;
  }
  .input-eye:hover { color: var(--blue); }
  .form-error { color: #ef4444; font-size: 12px; margin-top: 6px; display: flex; align-items: center; gap: 4px; }
  .form-forgot { text-align: right; margin-top: -10px; margin-bottom: 18px; }
  .form-forgot a { font-size: 13px; color: var(--blue); text-decoration: none; font-weight: 500; }
  .form-forgot a:hover { text-decoration: underline; }

  .btn-primary {
    width: 100%; padding: 13px; background: var(--blue); color: white; border: none;
    border-radius: 10px; font-size: 15px; font-weight: 600; font-family: 'DM Sans', sans-serif;
    cursor: pointer; transition: var(--transition); box-shadow: 0 4px 16px rgba(21,101,192,.3);
  }
  .btn-primary:hover { background: var(--blue-dark); box-shadow: 0 6px 20px rgba(21,101,192,.4); transform: translateY(-1px); }
  .btn-primary:active { transform: translateY(0); }

  /* ── CALIFICACIONES ── */
  .materia-item {
    border: 1px solid var(--gray-200); border-radius: var(--radius); overflow: hidden;
    margin-bottom: 12px; transition: var(--transition);
  }
  .materia-item:hover { border-color: var(--blue-100); box-shadow: var(--shadow); }
  .materia-header {
    display: flex; align-items: center; gap: 14px; padding: 16px 18px;
    cursor: pointer; background: white; transition: var(--transition);
  }
  .materia-header:hover { background: var(--gray-50); }
  .materia-icon { font-size: 22px; }
  .materia-info { flex: 1; }
  .materia-name { font-size: 15px; font-weight: 600; color: var(--gray-800); }
  .materia-teacher { font-size: 12px; color: var(--gray-400); margin-top: 2px; }
  .materia-nota {
    font-family: 'Space Grotesk', sans-serif; font-size: 22px; font-weight: 700;
    width: 52px; text-align: center;
  }
  .materia-chevron { color: var(--gray-400); font-size: 14px; transition: transform .25s; }
  .materia-chevron.open { transform: rotate(180deg); }
  .materia-body { padding: 0 18px 18px; background: var(--gray-50); }
  .materia-prog-wrap { margin-bottom: 14px; padding-top: 14px; }
  .materia-prog-label { display: flex; justify-content: space-between; font-size: 12px; color: var(--gray-400); margin-bottom: 6px; }

  /* ── ASISTENCIA PAGE ── */
  .asist-card {
    background: white; border-radius: var(--radius); border: 1px solid var(--gray-200);
    padding: 18px 20px; display: flex; align-items: center; gap: 16px;
    box-shadow: var(--shadow); transition: var(--transition); margin-bottom: 12px;
  }
  .asist-card:hover { box-shadow: var(--shadow-md); transform: translateX(3px); }
  .asist-icon { font-size: 24px; }
  .asist-info { flex: 1; }
  .asist-materia { font-size: 15px; font-weight: 600; color: var(--gray-800); }
  .asist-detail { font-size: 12px; color: var(--gray-400); margin-top: 2px; }
  .asist-pct { font-family: 'Space Grotesk', sans-serif; font-size: 22px; font-weight: 700; }
  .asist-prog { flex: 1; }
  .asist-bar-wrap { display: flex; align-items: center; gap: 10px; margin-top: 8px; }
  .asist-bar-track { flex: 1; height: 8px; background: var(--gray-100); border-radius: 99px; overflow: hidden; }
  .asist-bar-fill { height: 100%; border-radius: 99px; transition: width 1.2s cubic-bezier(.4,0,.2,1); }

  /* ── RESPONSIVE ── */
  @media (max-width: 1100px) {
    .stats-grid { grid-template-columns: repeat(2, 1fr); }
    .grid-3 { grid-template-columns: 1fr; }
  }
  @media (max-width: 768px) {
    :root { --sidebar-w: 0px; }
    .sidebar { transform: translateX(-240px); }
    .sidebar.open { transform: translateX(0); --sidebar-w: 240px; }
    .main { padding: 16px; }
    .topbar { left: 0; padding: 0 16px; }
    .stats-grid { grid-template-columns: 1fr 1fr; }
    .grid-2 { grid-template-columns: 1fr; }
  }
  @media (max-width: 480px) {
    .stats-grid { grid-template-columns: 1fr; }
    .login-card { padding: 32px 24px; margin: 16px; }
  }

  /* ── MISC ── */
  .section-title { font-family: 'Space Grotesk', sans-serif; font-size: 22px; font-weight: 700; color: var(--gray-800); margin-bottom: 6px; }
  .section-sub { font-size: 14px; color: var(--gray-400); margin-bottom: 24px; }
  .btn-sm {
    padding: 7px 14px; border-radius: 8px; font-size: 13px; font-weight: 600;
    border: 1.5px solid; cursor: pointer; transition: var(--transition); font-family: 'DM Sans', sans-serif;
    display: inline-flex; align-items: center; gap: 6px;
  }
  .btn-outline { border-color: var(--blue-100); color: var(--blue); background: var(--blue-50); }
  .btn-outline:hover { background: var(--blue); color: white; border-color: var(--blue); }
  .chip { display: inline-flex; align-items: center; gap: 5px; padding: 4px 10px; border-radius: 99px; font-size: 12px; font-weight: 600; }

  .welcome-banner {
    background: linear-gradient(120deg, var(--blue-dark) 0%, var(--blue) 60%, var(--blue-light) 100%);
    border-radius: var(--radius); padding: 24px 28px; margin-bottom: 24px;
    display: flex; align-items: center; justify-content: space-between;
    color: white; box-shadow: 0 4px 24px rgba(21,101,192,.3); position: relative; overflow: hidden;
  }
  .welcome-banner::before {
    content: ''; position: absolute; right: -30px; top: -30px;
    width: 160px; height: 160px; border-radius: 50%; background: rgba(255,255,255,.07);
  }
  .welcome-banner::after {
    content: ''; position: absolute; right: 60px; bottom: -40px;
    width: 100px; height: 100px; border-radius: 50%; background: rgba(255,255,255,.05);
  }
  .welcome-title { font-family: 'Space Grotesk', sans-serif; font-size: 20px; font-weight: 700; }
  .welcome-sub { font-size: 13px; opacity: .75; margin-top: 4px; }
  .welcome-emoji { font-size: 48px; position: relative; z-index: 2; }
  .empty-state { text-align: center; padding: 40px 20px; color: var(--gray-400); font-size: 14px; }
`;

// ─── COMPONENTS ──────────────────────────────────────────────────────────────

function ProgressBar({ value, max = 5, height = 8, color }) {
  const pct = Math.min(100, (value / max) * 100);
  const barColor = color || (value >= 3.5 ? "#22c55e" : value >= 3.0 ? "#f59e0b" : "#ef4444");
  return (
    <div className="progress-wrap" style={{ height }}>
      <div className="progress-bar" style={{ width: `${pct}%`, background: barColor, height: "100%" }} />
    </div>
  );
}

function StatCard({ icon, value, label, sub, badge, badgeType = "blue", color = "blue" }) {
  return (
    <div className={`stat-card ${color}`}>
      <div className={`stat-icon ${color}`}>{icon}</div>
      <div className="stat-value">{value}</div>
      <div className="stat-label">{label}</div>
      {(sub || badge) && (
        <div className="stat-sub">
          {sub}
          {badge && <span className={`stat-badge badge-${badgeType}`}>{badge}</span>}
        </div>
      )}
    </div>
  );
}

// ─── PAGES ───────────────────────────────────────────────────────────────────

function LoginPage({ onLogin }) {
  const [email, setEmail] = useState("");
  const [pass, setPass] = useState("");
  const [show, setShow] = useState(false);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const handle = (e) => {
    e.preventDefault();
    setError("");
    if (!email || !pass) { setError("Por favor completa todos los campos."); return; }
    if (!email.includes("@")) { setError("Ingresa un correo electrónico válido."); return; }
    setLoading(true);
    setTimeout(() => {
      if (pass === "error123") { setError("Credenciales incorrectas. Intenta de nuevo."); setLoading(false); return; }
      onLogin();
    }, 900);
  };

  return (
    <div className="login-wrap">
      <div className="login-bg-circle" style={{ width: 400, height: 400, top: -100, left: -100 }} />
      <div className="login-bg-circle" style={{ width: 300, height: 300, bottom: -80, right: -60 }} />
      <div className="login-bg-circle" style={{ width: 200, height: 200, top: "40%", right: "15%" }} />
      <div className="login-card">
        <div className="login-logo">
          <div className="login-logo-icon">🎓</div>
          <div className="login-title">EduTrack</div>
          <div className="login-sub">Plataforma de Gestión Académica</div>
        </div>
        <form onSubmit={handle}>
          <div className="form-group">
            <label className="form-label">Correo electrónico</label>
            <input
              className={`form-input ${error ? "error" : ""}`}
              type="text" placeholder="estudiante@sena.edu.co"
              value={email} onChange={e => setEmail(e.target.value)}
            />
          </div>
          <div className="form-group">
            <label className="form-label">Contraseña</label>
            <div className="input-wrap">
              <input
                className={`form-input ${error ? "error" : ""}`}
                type={show ? "text" : "password"} placeholder="••••••••"
                value={pass} onChange={e => setPass(e.target.value)}
                style={{ paddingRight: 44 }}
              />
              <button type="button" className="input-eye" onClick={() => setShow(!show)}>
                {show ? "🙈" : "👁️"}
              </button>
            </div>
          </div>
          {error && <div className="form-error">⚠️ {error}</div>}
          <div className="form-forgot"><a href="#">¿Olvidaste tu contraseña?</a></div>
          <button type="submit" className="btn-primary" disabled={loading}>
            {loading ? "Verificando..." : "Ingresar →"}
          </button>
        </form>
        <p style={{ textAlign: "center", fontSize: 12, color: "var(--gray-400)", marginTop: 20 }}>
          💡 Ingresa cualquier correo y contraseña para acceder
        </p>
      </div>
    </div>
  );
}

function DashboardPage() {
  const [animated, setAnimated] = useState(false);
  useEffect(() => { setTimeout(() => setAnimated(true), 100); }, []);

  const now = new Date();
  const hora = now.getHours();
  const greeting = hora < 12 ? "Buenos días" : hora < 18 ? "Buenas tardes" : "Buenas noches";

  return (
    <div className="main">
      <div className="welcome-banner">
        <div>
          <div className="welcome-title">{greeting}, {USER.name.split(" ")[0]}! 👋</div>
          <div className="welcome-sub">Tienes {TAREAS.filter(t => t.urgente).length} tareas urgentes y {EVENTOS.length} eventos esta semana.</div>
        </div>
        <div className="welcome-emoji">🎓</div>
      </div>

      <div className="stats-grid">
        <StatCard icon="📊" value={promedioGeneral} label="Promedio General" sub="Semestre 2025" badge={promedioGeneral >= 3.5 ? "Aprobado" : "En Riesgo"} badgeType={promedioGeneral >= 3.5 ? "green" : "amber"} color="blue" />
        <StatCard icon="📋" value={TAREAS.length} label="Tareas Pendientes" sub={`${TAREAS.filter(t=>t.urgente).length} urgentes hoy`} badge="Ver todas" badgeType="blue" color="amber" />
        <StatCard icon="✅" value={`${asistenciaGlobal}%`} label="Asistencia Global" sub="De 20 clases por materia" badge={asistenciaGlobal >= 80 ? "Buena" : "Atención"} badgeType={asistenciaGlobal >= 80 ? "green" : "red"} color="green" />
        <StatCard icon="📅" value={EVENTOS.length} label="Próximos Eventos" sub="Esta semana" badge="Ver calendario" badgeType="blue" color="purple" />
      </div>

      <div className="grid-3">
        {/* Chart */}
        <div className="card">
          <div className="card-header">
            <span className="card-title">Rendimiento por Materia</span>
            <span className="chip" style={{ background: "var(--blue-50)", color: "var(--blue)" }}>Semestre actual</span>
          </div>
          <div className="card-body">
            <div className="chart-wrap">
              {MATERIAS.map((m, i) => {
                const h = animated ? Math.round((m.promedio / 5) * 110) : 0;
                const c = m.promedio >= 3.5 ? `linear-gradient(180deg, #60a5fa, #1565C0)` : m.promedio >= 3.0 ? `linear-gradient(180deg, #fde68a, #f59e0b)` : `linear-gradient(180deg, #fca5a5, #ef4444)`;
                return (
                  <div key={m.id} className="chart-bar-wrap">
                    <div className="chart-val" style={{ color: noteColor(m.promedio) }}>{m.promedio}</div>
                    <div className="chart-bar" style={{ height: h, background: c, boxShadow: "0 4px 12px rgba(21,101,192,.2)" }} title={m.name} />
                    <div className="chart-label">{m.name.split(" ")[0]}</div>
                  </div>
                );
              })}
            </div>
            <div style={{ marginTop: 16 }}>
              <div style={{ display: "flex", gap: 12, flexWrap: "wrap", fontSize: 11, color: "var(--gray-400)" }}>
                <span>🟢 Aprobado (≥3.5)</span><span>🟡 Riesgo (3.0–3.4)</span><span>🔴 Bajo (&lt;3.0)</span>
              </div>
            </div>
          </div>
        </div>

        {/* Events */}
        <div className="card">
          <div className="card-header">
            <span className="card-title">Próximos Eventos</span>
          </div>
          <div className="card-body">
            {EVENTOS.map((ev, i) => (
              <div key={i} className="event-item">
                <div className="event-date">
                  <div className="event-day">{ev.fecha.split(" ")[1] || ev.fecha}</div>
                  <div className="event-mon">{ev.fecha.split(" ")[0]}</div>
                </div>
                <div className="event-info">
                  <span className={`event-tipo tipo-${ev.tipo}`}>{ev.tipo}</span>
                  <div className="event-title">{ev.titulo}</div>
                  <div className="event-hora">🕐 {ev.hora}</div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>

      <div className="grid-2">
        {/* Tareas */}
        <div className="card">
          <div className="card-header">
            <span className="card-title">Tareas Pendientes</span>
            <span className="chip badge-red">{TAREAS.filter(t=>t.urgente).length} urgentes</span>
          </div>
          <div className="card-body">
            {TAREAS.map(t => (
              <div key={t.id} className="task-item">
                <div className="task-dot" style={{ background: t.urgente ? "#ef4444" : "var(--blue)" }} />
                <div style={{ fontSize: 18 }}>{t.icon}</div>
                <div className="task-info">
                  <div className="task-title">{t.titulo}</div>
                  <div className="task-meta">{t.materia}</div>
                </div>
                <div className={`task-due ${t.urgente ? "urgent" : "normal"}`}>{t.vence}</div>
              </div>
            ))}
          </div>
        </div>

        {/* Last grades */}
        <div className="card">
          <div className="card-header">
            <span className="card-title">Últimas Calificaciones</span>
          </div>
          <div style={{ overflowX: "auto" }}>
            <table className="data-table">
              <thead><tr>
                <th>Materia</th><th>Actividad</th><th>Nota</th>
              </tr></thead>
              <tbody>
                {MATERIAS.slice(0, 5).map(m => {
                  const last = m.actividades[m.actividades.length - 1];
                  return (
                    <tr key={m.id}>
                      <td style={{ fontWeight: 500 }}>{m.icon} {m.name}</td>
                      <td style={{ color: "var(--gray-600)", fontSize: 13 }}>{last.nombre}</td>
                      <td>
                        <span className="nota-chip" style={{ background: noteBg(last.nota), color: noteColor(last.nota) }}>
                          {last.nota.toFixed(1)}
                        </span>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
}

function CalificacionesPage() {
  const [open, setOpen] = useState(null);
  const toggle = (id) => setOpen(open === id ? null : id);

  return (
    <div className="main">
      <div className="section-title">Mis Calificaciones</div>
      <div className="section-sub">Semestre 2025 — {MATERIAS.length} materias activas</div>

      <div style={{ display: "flex", gap: 12, marginBottom: 24, flexWrap: "wrap" }}>
        <div className="stat-card blue" style={{ flex: 1, minWidth: 140, padding: 16 }}>
          <div style={{ fontSize: 12, color: "var(--gray-400)", marginBottom: 4 }}>Promedio General</div>
          <div style={{ fontFamily: "'Space Grotesk', sans-serif", fontSize: 32, fontWeight: 700, color: noteColor(+promedioGeneral) }}>{promedioGeneral}</div>
          <div style={{ marginTop: 8 }}>
            <ProgressBar value={+promedioGeneral} height={6} />
          </div>
        </div>
        <div className="stat-card green" style={{ flex: 1, minWidth: 140, padding: 16 }}>
          <div style={{ fontSize: 12, color: "var(--gray-400)", marginBottom: 4 }}>Materias Aprobadas</div>
          <div style={{ fontFamily: "'Space Grotesk', sans-serif", fontSize: 32, fontWeight: 700, color: "#22c55e" }}>
            {MATERIAS.filter(m => m.promedio >= 3.5).length}/{MATERIAS.length}
          </div>
        </div>
        <div className="stat-card amber" style={{ flex: 1, minWidth: 140, padding: 16 }}>
          <div style={{ fontSize: 12, color: "var(--gray-400)", marginBottom: 4 }}>En Riesgo</div>
          <div style={{ fontFamily: "'Space Grotesk', sans-serif", fontSize: 32, fontWeight: 700, color: "#f59e0b" }}>
            {MATERIAS.filter(m => m.promedio >= 3.0 && m.promedio < 3.5).length}
          </div>
        </div>
        <div className="stat-card" style={{ flex: 1, minWidth: 140, padding: 16 }}>
          <div style={{ fontSize: 12, color: "var(--gray-400)", marginBottom: 4 }}>Reprobadas</div>
          <div style={{ fontFamily: "'Space Grotesk', sans-serif", fontSize: 32, fontWeight: 700, color: "#ef4444" }}>
            {MATERIAS.filter(m => m.promedio < 3.0).length}
          </div>
        </div>
      </div>

      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 16 }}>
        <span style={{ fontSize: 14, color: "var(--gray-600)" }}>📚 {MATERIAS.length} materias</span>
        <button className="btn-sm btn-outline">⬇️ Descargar Boletín PDF</button>
      </div>

      {MATERIAS.map(m => (
        <div key={m.id} className="materia-item">
          <div className="materia-header" onClick={() => toggle(m.id)}>
            <div className="materia-icon">{m.icon}</div>
            <div className="materia-info">
              <div className="materia-name">{m.name}</div>
              <div className="materia-teacher">{m.teacher} · {m.actividades.length} actividades</div>
            </div>
            <div style={{ textAlign: "right", marginRight: 12 }}>
              <div className="materia-nota" style={{ color: noteColor(m.promedio) }}>
                {m.promedio.toFixed(1)}
              </div>
              <span className="nota-chip" style={{ background: noteBg(m.promedio), color: noteColor(m.promedio), fontSize: 11 }}>
                {noteLabel(m.promedio)}
              </span>
            </div>
            <div style={{ width: 80, marginRight: 12 }}>
              <ProgressBar value={m.promedio} height={6} />
            </div>
            <div className={`materia-chevron ${open === m.id ? "open" : ""}`}>▼</div>
          </div>

          {open === m.id && (
            <div className="materia-body">
              <div className="materia-prog-wrap">
                <div className="materia-prog-label">
                  <span>Progreso de actividades</span>
                  <span>{m.actividades.length} de 5 completadas</span>
                </div>
                <ProgressBar value={m.actividades.length} max={5} height={8} color="var(--blue)" />
              </div>
              <div style={{ overflowX: "auto" }}>
                <table className="data-table">
                  <thead><tr>
                    <th>Actividad</th><th>Fecha</th><th>Porcentaje</th><th>Nota</th><th>Estado</th>
                  </tr></thead>
                  <tbody>
                    {m.actividades.map((a, i) => (
                      <tr key={i}>
                        <td style={{ fontWeight: 500 }}>{a.nombre}</td>
                        <td style={{ color: "var(--gray-400)", fontSize: 12 }}>{a.fecha}</td>
                        <td>
                          <span className="chip badge-blue">{a.porcentaje}%</span>
                        </td>
                        <td>
                          <span className="nota-chip" style={{ background: noteBg(a.nota), color: noteColor(a.nota), fontSize: 13, fontWeight: 700 }}>
                            {a.nota.toFixed(1)}
                          </span>
                        </td>
                        <td>
                          <span className="nota-chip" style={{ background: noteBg(a.nota), color: noteColor(a.nota), fontSize: 11 }}>
                            {noteLabel(a.nota)}
                          </span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              <div style={{ marginTop: 14, padding: 12, background: "white", borderRadius: 10, border: "1px solid var(--gray-200)", display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                <span style={{ fontSize: 13, color: "var(--gray-600)" }}>Promedio ponderado final:</span>
                <span style={{ fontFamily: "'Space Grotesk',sans-serif", fontSize: 20, fontWeight: 700, color: noteColor(m.promedio) }}>
                  {m.promedio.toFixed(1)} / 5.0
                </span>
              </div>
            </div>
          )}
        </div>
      ))}
    </div>
  );
}

function AsistenciaPage() {
  return (
    <div className="main">
      <div className="section-title">Mi Asistencia</div>
      <div className="section-sub">Período académico 2025 — 20 clases por materia</div>

      <div className="stats-grid" style={{ marginBottom: 28 }}>
        <StatCard icon="✅" value={`${asistenciaGlobal}%`} label="Asistencia Global" badge="Buena" badgeType="green" color="green" />
        <StatCard icon="📅" value={ASISTENCIA_MATERIAS.reduce((a,m)=>a+m.presentes,0)} label="Total Asistencias" color="blue" />
        <StatCard icon="❌" value={ASISTENCIA_MATERIAS.reduce((a,m)=>a+(m.total-m.presentes),0)} label="Total Ausencias" badge="Atención" badgeType="amber" color="amber" />
        <StatCard icon="🏆" value="Inglés" label="Mejor Asistencia" badge="100%" badgeType="green" color="blue" />
      </div>

      <div className="card" style={{ marginBottom: 24 }}>
        <div className="card-header">
          <span className="card-title">Asistencia por Materia</span>
          <span className="chip badge-blue">Semestre 2025</span>
        </div>
        <div className="card-body">
          {ASISTENCIA_MATERIAS.map((m, i) => {
            const pct = Math.round((m.presentes / m.total) * 100);
            const color = pct >= 90 ? "#22c55e" : pct >= 75 ? "#f59e0b" : "#ef4444";
            return (
              <div key={i} className="asist-card">
                <div className="asist-icon">{m.icon}</div>
                <div style={{ flex: 1 }}>
                  <div className="asist-materia">{m.materia}</div>
                  <div className="asist-detail">{m.presentes} de {m.total} clases asistidas</div>
                  <div className="asist-bar-wrap">
                    <div className="asist-bar-track">
                      <div className="asist-bar-fill" style={{ width: `${pct}%`, background: color }} />
                    </div>
                  </div>
                </div>
                <div className="asist-pct" style={{ color }}>{pct}%</div>
                <span className="nota-chip" style={{ background: pct >= 90 ? "#dcfce7" : pct >= 75 ? "#fef3c7" : "#fee2e2", color }}>
                  {pct >= 90 ? "Excelente" : pct >= 75 ? "Regular" : "Crítico"}
                </span>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}

function TareasPage() {
  const [done, setDone] = useState([]);
  const toggle = (id) => setDone(d => d.includes(id) ? d.filter(x => x !== id) : [...d, id]);

  return (
    <div className="main">
      <div className="section-title">Mis Tareas</div>
      <div className="section-sub">{TAREAS.length - done.length} tareas pendientes · {done.length} completadas</div>
      <div className="card">
        <div className="card-header">
          <span className="card-title">Pendientes</span>
          <span className="chip badge-red">{TAREAS.filter(t=>t.urgente && !done.includes(t.id)).length} urgentes</span>
        </div>
        <div className="card-body">
          {TAREAS.map(t => (
            <div key={t.id} className="task-item" onClick={() => toggle(t.id)}
              style={{ opacity: done.includes(t.id) ? .5 : 1, textDecoration: done.includes(t.id) ? "line-through" : "none", cursor: "pointer" }}>
              <div style={{ width: 18, height: 18, borderRadius: 5, border: done.includes(t.id) ? "none" : "2px solid var(--blue)", background: done.includes(t.id) ? "var(--blue)" : "transparent", display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0, color: "white", fontSize: 11, fontWeight: 700 }}>
                {done.includes(t.id) ? "✓" : ""}
              </div>
              <div style={{ fontSize: 18 }}>{t.icon}</div>
              <div className="task-info">
                <div className="task-title">{t.titulo}</div>
                <div className="task-meta">{t.materia}</div>
              </div>
              <div className={`task-due ${t.urgente ? "urgent" : "normal"}`}>{t.vence}</div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

function MensajesPage() {
  const msgs = [
    { from: "Prof. Carlos Mendoza", sub: "Parcial 3 - Matemáticas", preview: "El próximo parcial será el martes 29 de abril...", time: "Hoy 9:14 AM", unread: true, icon: "📐" },
    { from: "Prof. Ana Gómez", sub: "Laboratorio pendiente", preview: "Recuerda entregar el informe del lab antes del...", time: "Ayer 3:45 PM", unread: true, icon: "⚛️" },
    { from: "Dirección Académica", sub: "Reunión de padres", preview: "Se informa que la reunión de padres se realizará el...", time: "Mar 22", unread: false, icon: "🏫" },
    { from: "Prof. Sarah Williams", sub: "¡Excelente trabajo!", preview: "Quería felicitarte por tu desempeño en el Speaking...", time: "Mar 20", unread: false, icon: "🌐" },
  ];
  return (
    <div className="main">
      <div className="section-title">Mensajes</div>
      <div className="section-sub">{msgs.filter(m=>m.unread).length} mensajes sin leer</div>
      <div className="card">
        {msgs.map((m, i) => (
          <div key={i} style={{ display: "flex", gap: 14, padding: "14px 20px", borderBottom: i < msgs.length-1 ? "1px solid var(--gray-100)" : "none", cursor: "pointer", background: m.unread ? "var(--blue-50)" : "white", transition: "background .2s" }}
            onMouseEnter={e=>e.currentTarget.style.background="var(--gray-100)"}
            onMouseLeave={e=>e.currentTarget.style.background=m.unread?"var(--blue-50)":"white"}>
            <div style={{ width: 42, height: 42, borderRadius: 12, background: "var(--blue)", display: "flex", alignItems: "center", justifyContent: "center", fontSize: 20, flexShrink: 0 }}>{m.icon}</div>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                <span style={{ fontSize: 14, fontWeight: m.unread ? 700 : 500, color: "var(--gray-800)" }}>{m.from}</span>
                <span style={{ fontSize: 11.5, color: "var(--gray-400)" }}>{m.time}</span>
              </div>
              <div style={{ fontSize: 13, fontWeight: m.unread ? 600 : 400, color: m.unread ? "var(--gray-800)" : "var(--gray-600)", marginTop: 2 }}>{m.sub}</div>
              <div style={{ fontSize: 12.5, color: "var(--gray-400)", marginTop: 2, whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>{m.preview}</div>
            </div>
            {m.unread && <div style={{ width: 8, height: 8, borderRadius: "50%", background: "var(--blue)", flexShrink: 0, marginTop: 8 }} />}
          </div>
        ))}
      </div>
    </div>
  );
}

function CalendarioPage() {
  const todos = [
    ...EVENTOS.map(e => ({ ...e, type: e.tipo })),
    { fecha: "Mié 30", hora: "8:00 AM", titulo: "Entrega Línea de Tiempo Historia", tipo: "entrega" },
    { fecha: "Jue 1 May", hora: "2:00 PM", titulo: "Proyecto Final Programación", tipo: "entrega" },
  ];
  return (
    <div className="main">
      <div className="section-title">Calendario Académico</div>
      <div className="section-sub">Abril — Mayo 2025</div>
      <div className="card">
        <div className="card-header"><span className="card-title">Próximos compromisos</span></div>
        <div className="card-body">
          {todos.map((ev, i) => (
            <div key={i} className="event-item">
              <div className="event-date">
                <div className="event-day">{ev.fecha.split(" ")[1] || ev.fecha}</div>
                <div className="event-mon">{ev.fecha.split(" ")[0]}</div>
              </div>
              <div className="event-info">
                <span className={`event-tipo tipo-${ev.tipo}`}>{ev.tipo}</span>
                <div className="event-title" style={{ marginTop: 4 }}>{ev.titulo}</div>
                <div className="event-hora">🕐 {ev.hora}</div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

// ─── APP ─────────────────────────────────────────────────────────────────────
const NAV = [
  { id: "dashboard", label: "Inicio", icon: "🏠" },
  { id: "calificaciones", label: "Mis Notas", icon: "📊", badge: null },
  { id: "tareas", label: "Tareas", icon: "📋", badge: TAREAS.filter(t=>t.urgente).length },
  { id: "asistencia", label: "Asistencia", icon: "✅" },
  { id: "calendario", label: "Calendario", icon: "📅" },
  { id: "mensajes", label: "Mensajes", icon: "💬", badge: 2 },
];

const PAGE_TITLES = {
  dashboard: { title: "Dashboard", sub: `Bienvenido, ${USER.name}` },
  calificaciones: { title: "Mis Calificaciones", sub: "Semestre 2025" },
  tareas: { title: "Tareas Pendientes", sub: `${TAREAS.length} tareas activas` },
  asistencia: { title: "Mi Asistencia", sub: "Control de asistencia por materia" },
  calendario: { title: "Calendario", sub: "Eventos y compromisos académicos" },
  mensajes: { title: "Mensajes", sub: "2 mensajes sin leer" },
};

export default function App() {
  const [logged, setLogged] = useState(false);
  const [page, setPage] = useState("dashboard");

  if (!logged) return (
    <>
      <style>{css}</style>
      <LoginPage onLogin={() => setLogged(true)} />
    </>
  );

  const pt = PAGE_TITLES[page];

  return (
    <>
      <style>{css}</style>
      <div className="app">
        {/* Sidebar */}
        <aside className="sidebar">
          <div className="sidebar-logo">
            <div className="logo-icon">🎓</div>
            <div>
              <div className="logo-text">EduTrack</div>
              <span className="logo-sub">Gestión Académica</span>
            </div>
          </div>
          <nav className="sidebar-nav">
            <div className="nav-section">Principal</div>
            {NAV.map(n => (
              <button key={n.id} className={`nav-item ${page === n.id ? "active" : ""}`} onClick={() => setPage(n.id)}>
                <span className="nav-icon">{n.icon}</span>
                {n.label}
                {n.badge ? <span className="nav-badge">{n.badge}</span> : null}
              </button>
            ))}
            <div className="nav-section" style={{ marginTop: 8 }}>Cuenta</div>
            <button className="nav-item" onClick={() => setLogged(false)}>
              <span className="nav-icon">🚪</span>
              Cerrar Sesión
            </button>
          </nav>
          <div className="sidebar-footer">
            <div className="sidebar-user">
              <div className="avatar-sm">{USER.avatar}</div>
              <div>
                <div className="user-name-sm">{USER.name}</div>
                <div className="user-role-sm">{USER.grade}</div>
              </div>
            </div>
          </div>
        </aside>

        {/* Topbar */}
        <header className="topbar">
          <div className="topbar-left">
            <div className="topbar-title">{pt.title}</div>
            <div className="topbar-sub">{pt.sub}</div>
          </div>
          <div className="topbar-right">
            <button className="topbar-btn" title="Notificaciones" onClick={() => setPage("mensajes")}>
              🔔<span className="notif-dot" />
            </button>
            <button className="topbar-btn" title="Buscar">🔍</button>
            <div>
              <div className="topbar-name">{USER.name}</div>
              <div className="topbar-grade">{USER.grade}</div>
            </div>
            <div className="topbar-avatar">{USER.avatar}</div>
          </div>
        </header>

        {/* Page */}
        {page === "dashboard" && <DashboardPage />}
        {page === "calificaciones" && <CalificacionesPage />}
        {page === "asistencia" && <AsistenciaPage />}
        {page === "tareas" && <TareasPage />}
        {page === "mensajes" && <MensajesPage />}
        {page === "calendario" && <CalendarioPage />}
      </div>
    </>
  );
}
