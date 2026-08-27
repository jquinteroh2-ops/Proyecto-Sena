// Piezas de formulario y presentacion compartidas entre pantallas. Se
// extrajeron de Boletin.jsx (Fase 10) cuando el numero de pantallas nuevas
// empezo a repetir el mismo Campo/Etiqueta/Numero en cada archivo.

export function Campo({ etiqueta, htmlFor, children }) {
  return (
    <div>
      <label htmlFor={htmlFor} className="block text-sm font-medium text-gray-800">
        {etiqueta}
      </label>
      {children}
    </div>
  );
}

export function Numero({ id, valor, onChange, min = '1', required = true }) {
  return (
    <input
      id={id}
      type="number"
      min={min}
      required={required}
      value={valor}
      onChange={(e) => onChange(e.target.value)}
      className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2 text-gray-900
                 focus:border-educk-500 focus:outline-none focus:ring-2 focus:ring-educk-500"
    />
  );
}

export function Texto({ id, valor, onChange, required = true, tipo = 'text', placeholder }) {
  return (
    <input
      id={id}
      type={tipo}
      required={required}
      placeholder={placeholder}
      value={valor}
      onChange={(e) => onChange(e.target.value)}
      className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2 text-gray-900
                 focus:border-educk-500 focus:outline-none focus:ring-2 focus:ring-educk-500"
    />
  );
}

export function Selector({ id, valor, onChange, opciones, required = true, placeholder = 'Seleccione...' }) {
  return (
    <select
      id={id}
      value={valor}
      onChange={(e) => onChange(e.target.value)}
      required={required}
      className="mt-1 w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-gray-900
                 focus:border-educk-500 focus:outline-none focus:ring-2 focus:ring-educk-500"
    >
      <option value="">{placeholder}</option>
      {opciones.map((o) => (
        <option key={o.value} value={o.value}>
          {o.label}
        </option>
      ))}
    </select>
  );
}

export function Boton({ children, disabled, type = 'submit', variante = 'primario', onClick }) {
  const estilos = {
    primario:
      'bg-marca text-white shadow-suave hover:brightness-110 focus:ring-educk-500 disabled:opacity-60 disabled:hover:brightness-100',
    secundario:
      'border border-gray-200 bg-white text-gray-800 shadow-sm hover:border-gray-300 hover:bg-gray-50 focus:ring-educk-500 disabled:opacity-60',
  };
  return (
    <button
      type={type}
      disabled={disabled}
      onClick={onClick}
      className={`rounded-xl px-4 py-2.5 text-sm font-semibold transition focus:outline-none focus:ring-2
                  focus:ring-offset-2 disabled:cursor-not-allowed ${estilos[variante]}`}
    >
      {children}
    </button>
  );
}

export function Etiqueta({ tono, children }) {
  const tonos = {
    verde: 'bg-green-100 text-green-800',
    rojo: 'bg-red-100 text-red-800',
    ambar: 'bg-amber-100 text-amber-900',
    azul: 'bg-educk-100 text-educk-700',
  };
  return (
    <span className={`rounded-full px-2.5 py-0.5 text-xs font-medium ${tonos[tono]}`}>
      {children}
    </span>
  );
}

export function Alerta({ tipo = 'error', children }) {
  const estilos =
    tipo === 'error'
      ? 'bg-red-50 text-red-700'
      : tipo === 'exito'
        ? 'bg-green-50 text-green-800'
        : 'bg-amber-50 text-amber-900';
  return (
    <p role={tipo === 'error' ? 'alert' : 'status'} className={`rounded-lg px-4 py-3 text-sm ${estilos}`}>
      {children}
    </p>
  );
}

export function Modal({ titulo, onCerrar, children }) {
  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-gray-900/50 px-4 backdrop-blur-sm"
      role="dialog"
      aria-modal="true"
      aria-labelledby="modal-titulo"
      onClick={(e) => { if (e.target === e.currentTarget) onCerrar(); }}
    >
      <div className="max-h-[90vh] w-full max-w-lg overflow-y-auto rounded-2xl border border-gray-100 bg-white p-6 shadow-tarjeta">
        <div className="flex items-center justify-between">
          <h3 id="modal-titulo" className="text-lg font-semibold text-gray-900">{titulo}</h3>
          <button
            type="button"
            onClick={onCerrar}
            aria-label="Cerrar"
            className="rounded-lg p-1 text-gray-400 hover:bg-gray-100 hover:text-gray-700"
          >
            ✕
          </button>
        </div>
        <div className="mt-4">{children}</div>
      </div>
    </div>
  );
}

export function Tarjeta({ children, className = '' }) {
  return (
    <section className={`rounded-2xl border border-gray-100 bg-white p-6 shadow-tarjeta ${className}`}>
      {children}
    </section>
  );
}
