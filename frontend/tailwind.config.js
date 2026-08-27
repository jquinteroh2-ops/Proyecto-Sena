/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      fontFamily: {
        sans: ['"Plus Jakarta Sans"', 'system-ui', 'Avenir', 'Helvetica', 'Arial', 'sans-serif'],
      },
      colors: {
        // Paleta institucional EduckTrack. `educk` es el azul primario (accion,
        // enlaces, marca); `acento` es un violeta secundario reservado para
        // acentos de marca (gradientes de cabecera, hero) y nunca compite con
        // `educk` como color de accion.
        educk: {
          50: '#eef6ff',
          100: '#d9eaff',
          200: '#b8d9ff',
          300: '#8ac0ff',
          400: '#559eff',
          500: '#2563eb',
          600: '#1d4ed8',
          700: '#1e40af',
          800: '#1e3a8a',
          900: '#172a5e',
        },
        acento: {
          400: '#a78bfa',
          500: '#7c3aed',
          600: '#6d28d9',
        },
      },
      boxShadow: {
        suave: '0 1px 2px rgba(15, 23, 42, 0.04), 0 8px 24px -12px rgba(15, 23, 42, 0.12)',
        tarjeta: '0 1px 2px rgba(15, 23, 42, 0.03), 0 12px 32px -16px rgba(29, 78, 216, 0.18)',
      },
      backgroundImage: {
        marca: 'linear-gradient(135deg, #1d4ed8 0%, #4338ca 55%, #6d28d9 100%)',
      },
    },
  },
  plugins: [],
};
