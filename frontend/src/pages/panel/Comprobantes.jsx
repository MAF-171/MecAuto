import { useState } from 'react';
import { api } from '../../lib/api';
import { useAuth } from '../../lib/auth.jsx';
import { useCarga } from '../../lib/hooks';
import { Campo, Confirmar, Insignia, PaginaCab, Paginacion, Tabla, Cargando, ErrorCaja, useAviso } from '../../components/ui.jsx';
import { useListado, BuscadorInput } from './comun.jsx';
import { fechaHora, soles } from '../../lib/format';
import { Link, navegar } from '../../lib/router.jsx';

const PAGO = { EFECTIVO: 'Efectivo', TRANSFERENCIA: 'Transferencia', TARJETA: 'Tarjeta', YAPE_PLIN: 'Yape / Plin' };

export function ComprobantesLista() {
  const [tipo, setTipo] = useState('');
  const [estado, setEstado] = useState('');
  const [desde, setDesde] = useState('');
  const [hasta, setHasta] = useState('');
  const lista = useListado('/comprobantes', { tipo, estado, desde, hasta });
  const { datos, cargando, error } = lista;
  return (
    <>
      <PaginaCab titulo="Comprobantes internos" subtitulo="Documentos internos del taller. No son comprobantes electrónicos de SUNAT." />
      <div className="filtros">
        <BuscadorInput valor={lista.q} onCambio={lista.setQ} placeholder="Número, cliente, documento o placa" />
        <Campo etiqueta="Tipo"><select value={tipo} onChange={(e) => { setTipo(e.target.value); lista.reiniciar(); }}><option value="">Todos</option><option value="BOLETA">Boleta</option><option value="FACTURA">Factura</option></select></Campo>
        <Campo etiqueta="Estado"><select value={estado} onChange={(e) => { setEstado(e.target.value); lista.reiniciar(); }}><option value="">Todos</option><option value="EMITIDO">Emitido</option><option value="ANULADO">Anulado</option></select></Campo>
        <Campo etiqueta="Desde"><input type="date" value={desde} onChange={(e) => { setDesde(e.target.value); lista.reiniciar(); }} /></Campo>
        <Campo etiqueta="Hasta"><input type="date" value={hasta} onChange={(e) => { setHasta(e.target.value); lista.reiniciar(); }} /></Campo>
      </div>
      {error && <ErrorCaja mensaje={error} onReintentar={lista.recargar} />}
      {cargando && !datos ? <Cargando /> : datos && (
        <>
          <Tabla vacio="Aún no se han emitido comprobantes. Se emiten desde una orden completada." onFila={(c) => navegar('/panel/comprobantes/' + c.id)} columnas={[
            { titulo: 'Número', ver: (c) => <strong>{c.numeroCompleto}</strong> }, { titulo: 'Tipo', ver: (c) => (c.tipo === 'BOLETA' ? 'Boleta' : 'Factura') },
            { titulo: 'Fecha', ver: (c) => fechaHora(c.fechaEmision) }, { titulo: 'Cliente', ver: (c) => c.clienteNombre }, { titulo: 'Orden', ver: (c) => c.ordenCodigo },
            { titulo: 'Estado', ver: (c) => <Insignia estado={c.estado} /> }, { titulo: 'Total', clase: 'num', ver: (c) => soles(c.total) },
          ]} filas={datos.contenido} />
          <Paginacion datos={datos} onCambiar={lista.setPagina} />
        </>
      )}
    </>
  );
}

export function ComprobanteDetalle({ params }) {
  const { tieneRol } = useAuth();
  const aviso = useAviso();
  const { datos: c, cargando, error, recargar, setDatos } = useCarga('/comprobantes/' + params.id);
  const [anular, setAnular] = useState(false);
  if (cargando && !c) return <Cargando />;
  if (error) return <ErrorCaja mensaje={error} onReintentar={recargar} />;
  if (!c) return null;
  const t = c.taller || {};
  return (
    <>
      <PaginaCab titulo={c.numeroCompleto} subtitulo={c.tipo === 'BOLETA' ? 'Boleta interna' : 'Factura interna'}>
        <Link to="/panel/comprobantes" className="btn btn-sec">← Volver</Link>
        <button className="btn btn-oscuro" onClick={() => window.print()}>Imprimir</button>
        {c.estado === 'EMITIDO' && tieneRol('ADMINISTRADOR') && <button className="btn btn-sec" onClick={() => setAnular(true)}>Anular</button>}
      </PaginaCab>
      <article className="documento">
        <div className="cab">
          <div><h2 style={{ marginBottom: 2 }}>{t.nombre_taller || 'Taller Mecánico MecAuto'}</h2>
            {t.ruc && <div>RUC {t.ruc}</div>}{t.direccion && <div>{t.direccion}</div>}{t.telefono && <div>Tel. {t.telefono}</div>}{t.correo && <div>{t.correo}</div>}</div>
          <div style={{ textAlign: 'right' }}><strong>{c.tipo === 'BOLETA' ? 'BOLETA INTERNA' : 'FACTURA INTERNA'}</strong><div style={{ fontSize: '1.2rem', fontWeight: 700 }}>{c.numeroCompleto}</div>
            <div>{fechaHora(c.fechaEmision)}</div>{c.estado === 'ANULADO' && <Insignia estado="ANULADO" />}</div>
        </div>
        <p><strong>Cliente:</strong> {c.clienteNombre}<br /><strong>{c.clienteTipoDoc}:</strong> {c.clienteDocumento}{c.vehiculoPlaca && <><br /><strong>Placa:</strong> {c.vehiculoPlaca}</>}<br /><strong>Orden:</strong> {c.ordenCodigo} · <strong>Pago:</strong> {PAGO[c.metodoPago] || c.metodoPago}</p>
        <table>
          <thead><tr><th>Descripción</th><th className="num">Cant.</th><th className="num">P. unit.</th><th className="num">Importe</th></tr></thead>
          <tbody>{c.detalles.map((l, i) => <tr key={i}><td>{l.descripcion}</td><td className="num">{l.cantidad}</td><td className="num">{soles(l.precioUnitario)}</td><td className="num">{soles(l.importe)}</td></tr>)}</tbody>
        </table>
        <div className="totales" style={{ marginTop: '1rem' }}>
          <div><span>Base imponible</span><span>{soles(c.baseImponible)}</span></div><div><span>IGV (18%)</span><span>{soles(c.igv)}</span></div>
          <div className="total"><span>Total</span><span>{soles(c.total)}</span></div>
        </div>
        {c.estado === 'ANULADO' && <p><strong>Motivo de anulación:</strong> {c.motivoAnulacion}</p>}
        <p className="aviso-legal">Documento interno de control del taller. No constituye comprobante de pago electrónico autorizado por SUNAT. Emitido por: {c.emitidoPor || '—'}.</p>
      </article>
      {anular && <Confirmar titulo="Anular comprobante" peligro textoOk="Anular" pedirMotivo etiquetaMotivo="Motivo de la anulación"
        mensaje={`Se anulará ${c.numeroCompleto}. El número no se reutiliza. Esta acción no se puede deshacer.`} onCancelar={() => setAnular(false)}
        onOk={async (motivo) => { const d = await api.post(`/comprobantes/${c.id}/anular`, { motivo }); setDatos(d); setAnular(false); aviso('Comprobante anulado'); }} />}
    </>
  );
}
