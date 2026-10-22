/**
 * Categorías de servicios para los filtros de la web pública.
 *
 * NOTA: la tabla `servicio` todavía no tiene una columna de categoría, así que se deduce
 * del nombre con palabras clave. Sirve para el catálogo inicial; un servicio nuevo cuyo
 * nombre no coincida con ninguna palabra cae en «Otros». Más adelante se puede agregar
 * una columna `categoria` a la tabla y quitar esta deducción.
 */
export const CATEGORIAS = ['Mantenimiento', 'Frenos y transmisión', 'Ruedas', 'Motor y electricidad', 'Otros'];

const REGLAS = [
  ['Ruedas', ['neumatic', 'alineacion', 'balanceo', 'llanta', 'rueda']],
  ['Frenos y transmisión', ['freno', 'suspension', 'direccion', 'transmision', 'embrague']],
  ['Motor y electricidad', ['diagnostico', 'electric', 'bateria', 'refrigeracion', 'aire acondicionado', 'motor', 'escaner']],
  ['Mantenimiento', ['mantenimiento', 'aceite', 'filtro', 'revision', 'inspeccion', 'preventivo']],
];

const sinTildes = (s) => String(s || '').normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase();

export function categoriaDe(nombre) {
  const n = sinTildes(nombre);
  for (const [cat, claves] of REGLAS) if (claves.some((c) => n.includes(c))) return cat;
  return 'Otros';
}

const TRAZOS = {
  Mantenimiento: <><path d="M12 3s6 6.5 6 11a6 6 0 0 1-12 0c0-4.5 6-11 6-11z" /><path d="M9.5 14.5a2.7 2.7 0 0 0 2.5 2.5" /></>,
  'Frenos y transmisión': <><circle cx="12" cy="12" r="8.5" /><circle cx="12" cy="12" r="3" /><path d="M12 3.5v2.2M12 18.3v2.2M3.5 12h2.2M18.3 12h2.2" /></>,
  Ruedas: <><circle cx="12" cy="12" r="9" /><circle cx="12" cy="12" r="4.5" /><circle cx="12" cy="12" r="1" /><path d="M12 3v4.5M12 16.5V21M3 12h4.5M16.5 12H21" /></>,
  'Motor y electricidad': <path d="M13 2L4.5 13.5H11L10 22l9-12h-6.5z" />,
  Otros: <path d="M14.7 6.3a4 4 0 0 0-5.4 5.4L3 18l3 3 6.3-6.3a4 4 0 0 0 5.4-5.4l-2.6 2.6-2.4-.6-.6-2.4z" />,
};

export function IconoCategoria({ categoria }) {
  return (
    <svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      {TRAZOS[categoria] || TRAZOS.Otros}
    </svg>
  );
}
