import * as XLSX from 'xlsx';
import { useRef, useState } from 'react';
import { Alerta, Boton } from './ui';

// Importador generico desde un archivo Excel/CSV. No sabe nada de estudiantes
// ni de notas: recibe `procesarFila(fila, indice)` y la llama una vez por
// cada fila de la primera hoja (encabezados de la fila 1 como claves). Quien
// lo usa decide que hacer con cada fila (a que endpoint POSTear) y que cuenta
// como exito o error; este componente solo orquesta el archivo, el progreso y
// el resumen final.
//
// DECISION DE DISENO: fila por fila, no en lote. El backend no tiene un
// endpoint de alta masiva (cada registro pasa por sus propias reglas de
// negocio: RB-01, RB-17, RB-07, etc.), asi que "importar" aqui es "automatizar
// el mismo POST que se haria a mano, muchas veces seguidas", con un reporte de
// que filas fallaron y por que. Una fila mal formada no aborta las demas.
export default function ImportadorExcel({ titulo, columnas, procesarFila }) {
  const inputRef = useRef(null);
  const [procesando, setProcesando] = useState(false);
  const [progreso, setProgreso] = useState(null); // { hecho, total }
  const [resultado, setResultado] = useState(null); // { exitos, errores: [{fila, mensaje}] }
  const [errorArchivo, setErrorArchivo] = useState(null);

  async function onFile(evento) {
    const archivo = evento.target.files?.[0];
    if (!archivo) return;
    setErrorArchivo(null);
    setResultado(null);

    let filas;
    try {
      const buffer = await archivo.arrayBuffer();
      const libro = XLSX.read(buffer, { type: 'array' });
      const hoja = libro.Sheets[libro.SheetNames[0]];
      filas = XLSX.utils.sheet_to_json(hoja, { defval: '' });
    } catch {
      setErrorArchivo('No fue posible leer el archivo. Debe ser un .xlsx, .xls o .csv valido.');
      if (inputRef.current) inputRef.current.value = '';
      return;
    }

    if (filas.length === 0) {
      setErrorArchivo('El archivo no tiene filas de datos.');
      if (inputRef.current) inputRef.current.value = '';
      return;
    }

    setProcesando(true);
    setProgreso({ hecho: 0, total: filas.length });
    const errores = [];
    let exitos = 0;

    for (let i = 0; i < filas.length; i++) {
      try {
        await procesarFila(filas[i], i);
        exitos++;
      } catch (err) {
        errores.push({
          fila: i + 2, // +1 por indice base 1, +1 por la fila de encabezado
          mensaje: err.response?.data?.mensaje ?? err.message ?? 'Error desconocido',
        });
      }
      setProgreso({ hecho: i + 1, total: filas.length });
    }

    setResultado({ exitos, errores });
    setProcesando(false);
    if (inputRef.current) inputRef.current.value = '';
  }

  return (
    <div className="rounded-xl border border-dashed border-gray-300 bg-gray-50/60 p-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <p className="text-sm font-semibold text-gray-900">{titulo}</p>
          <p className="text-xs text-gray-500">
            Columnas esperadas: <code className="text-gray-700">{columnas.join(', ')}</code>
          </p>
        </div>
        <label>
          <input
            ref={inputRef}
            type="file"
            accept=".xlsx,.xls,.csv"
            onChange={onFile}
            disabled={procesando}
            className="hidden"
          />
          <span
            role="button"
            tabIndex={0}
            className={`inline-block rounded-xl px-4 py-2 text-sm font-semibold shadow-suave transition
                        ${procesando ? 'cursor-not-allowed bg-gray-300 text-gray-500' : 'cursor-pointer bg-marca text-white hover:brightness-110'}`}
            onKeyDown={(e) => {
              if ((e.key === 'Enter' || e.key === ' ') && !procesando) inputRef.current?.click();
            }}
            onClick={() => !procesando && inputRef.current?.click()}
          >
            {procesando ? `Procesando ${progreso?.hecho ?? 0}/${progreso?.total ?? 0}...` : 'Elegir archivo Excel'}
          </span>
        </label>
      </div>

      {errorArchivo && <div className="mt-3"><Alerta>{errorArchivo}</Alerta></div>}

      {resultado && (
        <div className="mt-3 space-y-2">
          <Alerta tipo={resultado.errores.length === 0 ? 'exito' : 'advertencia'}>
            {resultado.exitos} fila(s) importada(s) correctamente
            {resultado.errores.length > 0 && `, ${resultado.errores.length} con error`}.
          </Alerta>
          {resultado.errores.length > 0 && (
            <div className="max-h-40 overflow-y-auto rounded-lg bg-red-50 p-3 text-xs text-red-800">
              {resultado.errores.map((e, i) => (
                <p key={i}>
                  Fila {e.fila}: {e.mensaje}
                </p>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}

export function BotonPlantilla({ nombre, columnas, filaEjemplo }) {
  function descargar() {
    const hoja = XLSX.utils.json_to_sheet([filaEjemplo]);
    const libro = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(libro, hoja, 'Plantilla');
    XLSX.writeFile(libro, `${nombre}.xlsx`);
  }
  return (
    <Boton type="button" variante="secundario" onClick={descargar}>
      Descargar plantilla ({columnas.length} columnas)
    </Boton>
  );
}
