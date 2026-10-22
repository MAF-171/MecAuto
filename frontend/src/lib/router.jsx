import { useEffect, useState } from 'react';

// Router mínimo basado en el hash (#/ruta). Evita configurar el servidor para rutas del lado del cliente.
export function rutaActual() {
  const h = window.location.hash.replace(/^#/, '') || '/';
  const [path, query = ''] = h.split('?');
  return { path: path || '/', query: new URLSearchParams(query) };
}
export function useRuta() {
  const [ruta, setRuta] = useState(rutaActual());
  useEffect(() => {
    const f = () => { setRuta(rutaActual()); window.scrollTo(0, 0); };
    window.addEventListener('hashchange', f);
    return () => window.removeEventListener('hashchange', f);
  }, []);
  return ruta;
}
export const navegar = (path) => { window.location.hash = path; };

export function Link({ to, children, ...resto }) {
  return <a href={'#' + to} {...resto}>{children}</a>;
}

/** Compara "/panel/ordenes/:id" con una ruta real y devuelve los parámetros o null. */
export function coincide(patron, path) {
  const a = patron.split('/').filter(Boolean);
  const b = path.split('/').filter(Boolean);
  if (a.length !== b.length) return null;
  const params = {};
  for (let i = 0; i < a.length; i++) {
    if (a[i].startsWith(':')) params[a[i].slice(1)] = decodeURIComponent(b[i]);
    else if (a[i] !== b[i]) return null;
  }
  return params;
}
