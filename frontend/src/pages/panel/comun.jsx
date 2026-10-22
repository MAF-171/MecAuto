import { useState } from 'react';
import { useCarga, useRetraso } from '../../lib/hooks';

/** Lista paginada con búsqueda: devuelve estado y helpers para una pantalla de tabla. */
export function useListado(ruta, filtrosExtra = {}, tamano = 15) {
  const [q, setQ] = useState('');
  const [pagina, setPagina] = useState(0);
  const qr = useRetraso(q);
  const carga = useCarga(ruta, { q: qr, pagina, tamano, ...filtrosExtra });
  return {
    ...carga, q, pagina,
    setQ: (v) => { setQ(v); setPagina(0); },
    setPagina,
    reiniciar: () => setPagina(0),
  };
}
export const BuscadorInput = ({ valor, onCambio, placeholder = 'Buscar…' }) => (
  <div className="buscar campo">
    <input type="search" aria-label="Buscar" placeholder={placeholder} value={valor} onChange={(e) => onCambio(e.target.value)} />
  </div>
);

import { useEffect } from 'react';
import { api } from '../../lib/api';
import { Campo } from '../../components/ui.jsx';

/** Buscador de cliente: escribe y elige de la lista. Devuelve el id vía onElegir(cliente|null). */
export function SelectorCliente({ valor, nombreInicial = '', onElegir, error, etiqueta = 'Cliente *' }) {
  const [texto, setTexto] = useState(nombreInicial);
  const [res, setRes] = useState([]);
  const [abierto, setAbierto] = useState(false);
  const t = useRetraso(texto, 300);
  useEffect(() => {
    if (!abierto || t.trim().length < 2) { setRes([]); return; }
    let vivo = true;
    api.get('/clientes', { q: t, soloActivos: true, tamano: 8 }).then((r) => vivo && setRes(r.contenido)).catch(() => {});
    return () => { vivo = false; };
  }, [t, abierto]);
  return (
    <Campo etiqueta={etiqueta} error={error} ayuda={valor ? 'Cliente seleccionado' : 'Escribe al menos 2 letras o dígitos del documento'}>
      <input value={texto} placeholder="Buscar cliente…" onChange={(e) => { setTexto(e.target.value); setAbierto(true); if (valor) onElegir(null); }} />
      {abierto && res.length > 0 && (
        <ul className="estado-lista" style={{ border: '1px solid var(--gris-300)', borderRadius: 8, padding: '.2rem .6rem', background: '#fff' }}>
          {res.map((c) => (
            <li key={c.id}><button type="button" className="btn-icono" style={{ fontSize: '.9rem', textAlign: 'left', width: '100%', color: 'inherit' }}
              onClick={() => { onElegir(c); setTexto(`${c.nombre} (${c.nroDocumento})`); setAbierto(false); setRes([]); }}>
              {c.nombre} · {c.nroDocumento}</button></li>
          ))}
        </ul>
      )}
    </Campo>
  );
}

/** Selector de vehículo de un cliente. */
export function SelectorVehiculo({ clienteId, valor, onCambio, error, etiqueta = 'Vehículo *' }) {
  const [lista, setLista] = useState([]);
  useEffect(() => {
    if (!clienteId) { setLista([]); return; }
    let vivo = true;
    api.get('/vehiculos', { clienteId, soloActivos: true, tamano: 50 }).then((r) => vivo && setLista(r.contenido)).catch(() => {});
    return () => { vivo = false; };
  }, [clienteId]);
  return (
    <Campo etiqueta={etiqueta} error={error} ayuda={clienteId && lista.length === 0 ? 'Este cliente no tiene vehículos activos. Regístralo en «Vehículos».' : undefined}>
      <select value={valor || ''} onChange={(e) => { const id = e.target.value ? Number(e.target.value) : null; onCambio(id, lista.find((v) => v.id === id) || null); }} disabled={!clienteId}>
        <option value="">{clienteId ? 'Elige un vehículo' : 'Primero elige un cliente'}</option>
        {lista.map((v) => <option key={v.id} value={v.id}>{v.placa} · {v.marca} {v.modelo}</option>)}
      </select>
    </Campo>
  );
}
