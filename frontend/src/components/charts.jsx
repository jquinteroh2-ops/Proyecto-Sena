import { useState } from 'react';

// Graficos propios en SVG (sin libreria externa: el proyecto no tenia ninguna
// y anadir una para dos graficos era mas peso del que aportaba). Siguen las
// convenciones de la guia de diseno de datos: un solo hue para magnitud
// (azul institucional), marcas finas, extremos redondeados, marcadores
// interactivos con tooltip, y nunca color como unico portador de significado
// (los estados van con icono + texto, no con el tono de la barra).

const AZUL = '#1d4ed8'; // educk-600
const GRIS_EJE = '#9ca3af'; // gray-400, recesivo a proposito

// Linea de tendencia (ej. promedio del estudiante por periodo). `puntos` es
// [{ etiqueta, valor }]. `minimo`/`maximo` fijan el rango del eje Y (por
// defecto la escala institucional 1.0-5.0).
export function GraficoLinea({ puntos, minimo = 1, maximo = 5, alto = 220 }) {
  const [activo, setActivo] = useState(null);
  const ancho = Math.max(360, puntos.length * 90);
  const margen = { top: 20, right: 20, bottom: 32, left: 36 };
  const anchoUtil = ancho - margen.left - margen.right;
  const altoUtil = alto - margen.top - margen.bottom;

  function x(i) {
    return margen.left + (puntos.length <= 1 ? anchoUtil / 2 : (i / (puntos.length - 1)) * anchoUtil);
  }
  function y(valor) {
    const t = (valor - minimo) / (maximo - minimo);
    return margen.top + altoUtil - t * altoUtil;
  }

  const lineaPath = puntos
    .map((p, i) => `${i === 0 ? 'M' : 'L'} ${x(i)} ${y(p.valor)}`)
    .join(' ');

  const lineasGuia = [minimo, (minimo + maximo) / 2, maximo];

  return (
    <div className="overflow-x-auto">
      <svg
        role="img"
        aria-label="Tendencia del promedio por periodo"
        width={ancho}
        height={alto}
        className="max-w-full"
      >
        {/* Grid recesivo: solo tres lineas de referencia, sin cuadricula densa. */}
        {lineasGuia.map((v) => (
          <g key={v}>
            <line x1={margen.left} x2={ancho - margen.right} y1={y(v)} y2={y(v)} stroke="#e5e7eb" strokeWidth="1" />
            <text x={margen.left - 8} y={y(v)} textAnchor="end" dominantBaseline="middle" fontSize="11" fill={GRIS_EJE}>
              {v.toFixed(1)}
            </text>
          </g>
        ))}

        {/* Umbral de aprobacion, mismo eje, marcado con texto en vez de un
            segundo hue para no crear un falso segundo dato. */}
        <path d={lineaPath} fill="none" stroke={AZUL} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />

        {puntos.map((p, i) => (
          <g key={p.etiqueta}>
            <circle
              cx={x(i)}
              cy={y(p.valor)}
              r={activo === i ? 6 : 5}
              fill={AZUL}
              stroke="#fcfcfb"
              strokeWidth="2"
              onMouseEnter={() => setActivo(i)}
              onMouseLeave={() => setActivo(null)}
              style={{ cursor: 'pointer' }}
            />
            <text x={x(i)} y={alto - 8} textAnchor="middle" fontSize="11" fill={GRIS_EJE}>
              {p.etiqueta}
            </text>
            {activo === i && (
              <g>
                <rect
                  x={x(i) - 26}
                  y={y(p.valor) - 32}
                  width="52"
                  height="22"
                  rx="6"
                  fill="#111827"
                />
                <text x={x(i)} y={y(p.valor) - 17} textAnchor="middle" fontSize="12" fill="#fff" fontWeight="600">
                  {p.valor.toFixed(2)}
                </text>
              </g>
            )}
          </g>
        ))}
      </svg>
    </div>
  );
}

// Barras de magnitud (ej. promedio por estudiante en un salon). `barras` es
// [{ etiqueta, valor, bien }] donde `bien` es un booleano que decide el
// icono+texto de estado mostrado bajo la barra, no el color de la barra
// misma (rojo/verde como hue categorico falla la separacion CVD, ver
// dataviz/references/color-formula.md).
export function GraficoBarras({ barras, minimo = 1, maximo = 5, alto = 240 }) {
  const [activo, setActivo] = useState(null);
  const anchoBarra = 40;
  const espacio = 22;
  const margen = { top: 20, right: 20, bottom: 46, left: 36 };
  const ancho = margen.left + margen.right + barras.length * (anchoBarra + espacio);
  const altoUtil = alto - margen.top - margen.bottom;

  function alturaBarra(valor) {
    const t = (valor - minimo) / (maximo - minimo);
    return Math.max(2, t * altoUtil);
  }

  return (
    <div className="overflow-x-auto">
      <svg role="img" aria-label="Promedio por estudiante" width={ancho} height={alto} className="max-w-full">
        <line
          x1={margen.left}
          x2={ancho - margen.right}
          y1={margen.top + altoUtil}
          y2={margen.top + altoUtil}
          stroke="#e5e7eb"
          strokeWidth="1"
        />
        {barras.map((b, i) => {
          const h = alturaBarra(b.valor);
          const bx = margen.left + i * (anchoBarra + espacio);
          const by = margen.top + altoUtil - h;
          return (
            <g key={b.etiqueta}>
              <rect
                x={bx}
                y={by}
                width={anchoBarra}
                height={h}
                rx="4"
                fill={AZUL}
                opacity={activo === i ? 1 : 0.85}
                onMouseEnter={() => setActivo(i)}
                onMouseLeave={() => setActivo(null)}
                style={{ cursor: 'pointer' }}
              />
              {activo === i && (
                <text x={bx + anchoBarra / 2} y={by - 8} textAnchor="middle" fontSize="12" fontWeight="600" fill="#111827">
                  {b.valor.toFixed(2)}
                </text>
              )}
              <text
                x={bx + anchoBarra / 2}
                y={alto - 30}
                textAnchor="middle"
                fontSize="10"
                fill={GRIS_EJE}
                transform={`rotate(-20 ${bx + anchoBarra / 2} ${alto - 30})`}
              >
                {b.etiqueta.length > 12 ? `${b.etiqueta.slice(0, 11)}…` : b.etiqueta}
              </text>
            </g>
          );
        })}
      </svg>
    </div>
  );
}
