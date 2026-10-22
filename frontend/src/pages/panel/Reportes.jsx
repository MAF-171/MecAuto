import { useState } from 'react';
import { api } from '../../lib/api';
import { useCarga } from '../../lib/hooks';
import { Campo, PaginaCab, Cargando, ErrorCaja, Vacio, useAviso } from '../../components/ui.jsx';
import { soles } from '../../lib/format';

const REPORTES = [
  ['ordenes', 'Órdenes de trabajo'], ['citas', 'Citas'], ['clientes', 'Clientes nuevos'], ['vehiculos-atendidos', 'Vehículos atendidos'],
  ['inventario-movimientos', 'Movimientos de inventario'], ['stock-bajo', 'Stock bajo'], ['cotizaciones', 'Cotizaciones'], ['ingresos', 'Ingresos (comprobantes)'],
  ['servicios-solicitados', 'Servicios más solicitados'], ['repuestos-usados', 'Repuestos más usados'], ['rendimiento-mecanicos', 'Rendimiento por mecánico'],
];

const esDinero = (col) => /(total|importe|ingreso|monto|precio|igv|base)/i.test(col);

export default function Reportes() {
  const [tipo, setTipo] = useState('ordenes');
  const [desde, setDesde] = useState('');
  const [hasta, setHasta] = useState('');
  const aviso = useAviso();
  const { datos: r, cargando, error, recargar } = useCarga('/reportes/' + tipo, { desde, hasta });
  async function excel() {
    try {
      const blob = await api.blob(`/reportes/${tipo}/excel`, { desde, hasta });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a'); a.href = url; a.download = `mecauto-${tipo}.xlsx`; a.click(); URL.revokeObjectURL(url);
    } catch (e) { aviso(e.message, 'error'); }
  }
  const fmt = (col, v) => (v === null || v === undefined ? '—' : typeof v === 'number' && esDinero(col) && !/cantidad|unidades|total de/i.test(col) ? soles(v) : String(v));
  return (
    <>
      <PaginaCab titulo="Reportes" subtitulo="Datos reales del sistema. Sin filtro de fechas se usa el mes actual.">
        <button className="btn btn-sec" onClick={() => window.print()} disabled={!r}>Imprimir</button>
        <button className="btn btn-pri" onClick={excel} disabled={!r}>Exportar a Excel</button>
      </PaginaCab>
      <div className="filtros">
        <Campo etiqueta="Reporte"><select value={tipo} onChange={(e) => setTipo(e.target.value)}>{REPORTES.map(([k, n]) => <option key={k} value={k}>{n}</option>)}</select></Campo>
        <Campo etiqueta="Desde"><input type="date" value={desde} onChange={(e) => setDesde(e.target.value)} /></Campo>
        <Campo etiqueta="Hasta"><input type="date" value={hasta} onChange={(e) => setHasta(e.target.value)} /></Campo>
      </div>
      {error && <ErrorCaja mensaje={error} onReintentar={recargar} />}
      {cargando && !r ? <Cargando /> : r && (
        <section className="tarjeta">
          <h2>{r.titulo}</h2>
          <p className="campo-ayuda">{r.periodo}</p>
          {r.resumen && Object.keys(r.resumen).length > 0 && (
            <div className="kpis">{Object.entries(r.resumen).map(([k, v]) => <div className="kpi" key={k}><div className="valor">{typeof v === 'number' && esDinero(k) ? soles(v) : String(v)}</div><div className="et">{k}</div></div>)}</div>
          )}
          {r.filas.length === 0 ? <Vacio>No hay datos en el periodo seleccionado.</Vacio> : (
            <div className="tabla-wrap"><table className="tabla"><thead><tr>{r.columnas.map((c) => <th key={c}>{c}</th>)}</tr></thead>
              <tbody>{r.filas.map((fila, i) => <tr key={i}>{fila.map((v, j) => <td key={j} className={typeof v === 'number' ? 'num' : ''}>{fmt(r.columnas[j], v)}</td>)}</tr>)}</tbody></table></div>
          )}
        </section>
      )}
    </>
  );
}
