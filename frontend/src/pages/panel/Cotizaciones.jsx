import { useEffect, useState } from 'react';
import { api } from '../../lib/api';
import { useCarga } from '../../lib/hooks';
import { Campo, Confirmar, Insignia, Modal, PaginaCab, Paginacion, Tabla, Cargando, ErrorCaja, useAviso, useFormulario, nulo, num } from '../../components/ui.jsx';
import { useListado, BuscadorInput, SelectorCliente, SelectorVehiculo } from './comun.jsx';
import { fecha, soles } from '../../lib/format';
import { navegar } from '../../lib/router.jsx';

export default function Cotizaciones() {
  const [estado, setEstado] = useState('');
  const lista = useListado('/cotizaciones', { estado });
  const aviso = useAviso();
  const [ver, setVer] = useState(null);
  const [editor, setEditor] = useState(null);
  const { datos, cargando, error } = lista;
  return (
    <>
      <PaginaCab titulo="Cotizaciones" subtitulo="Presupuestos para clientes. Los precios incluyen IGV (18%).">
        <button className="btn btn-pri" onClick={() => setEditor('nueva')}>+ Nueva cotización</button>
      </PaginaCab>
      <div className="filtros">
        <BuscadorInput valor={lista.q} onCambio={lista.setQ} placeholder="Código, cliente o placa" />
        <Campo etiqueta="Estado"><select value={estado} onChange={(e) => { setEstado(e.target.value); lista.reiniciar(); }}>
          <option value="">Todos</option><option value="PENDIENTE">Pendiente</option><option value="APROBADA">Aprobada</option><option value="RECHAZADA">Rechazada</option><option value="CONVERTIDA">Convertida</option></select></Campo>
      </div>
      {error && <ErrorCaja mensaje={error} onReintentar={lista.recargar} />}
      {cargando && !datos ? <Cargando /> : datos && (
        <>
          <Tabla vacio="No hay cotizaciones." onFila={setVer} columnas={[
            { titulo: 'Código', ver: (c) => <strong>{c.codigo}</strong> }, { titulo: 'Cliente', ver: (c) => c.clienteNombre }, { titulo: 'Placa', ver: (c) => c.placa || '—' },
            { titulo: 'Emisión', ver: (c) => fecha(c.fechaEmision) }, { titulo: 'Vigencia', ver: (c) => <>{fecha(c.vigenciaHasta)} {c.vencida && c.estado === 'PENDIENTE' && <Insignia estado="CANCELADA" texto="Vencida" />}</> },
            { titulo: 'Estado', ver: (c) => <Insignia estado={c.estado} /> }, { titulo: 'Total', clase: 'num', ver: (c) => soles(c.total) },
          ]} filas={datos.contenido} />
          <Paginacion datos={datos} onCambiar={lista.setPagina} />
        </>
      )}
      {ver && <Detalle id={ver.id} onCerrar={() => setVer(null)} onCambio={lista.recargar} onEditar={(c) => { setVer(null); setEditor(c); }} />}
      {editor && <Editor cotizacion={editor === 'nueva' ? null : editor} onCerrar={() => setEditor(null)} onGuardado={() => { setEditor(null); lista.recargar(); aviso('Cotización guardada'); }} />}
    </>
  );
}

function Detalle({ id, onCerrar, onCambio, onEditar }) {
  const { datos: c, cargando, error, recargar, setDatos } = useCarga('/cotizaciones/' + id);
  const aviso = useAviso();
  const [conf, setConf] = useState(null);
  async function accion(ruta, ok) {
    try { const d = await api.post(ruta); setDatos(d); aviso(ok); onCambio(); } catch (e) { aviso(e.message, 'error'); }
  }
  async function convertir() {
    try { const o = await api.post(`/cotizaciones/${id}/convertir`); aviso('Orden ' + o.codigo + ' creada'); onCambio(); navegar('/panel/ordenes/' + o.id); }
    catch (e) { aviso(e.message, 'error'); }
  }
  return (
    <Modal titulo={c ? 'Cotización ' + c.codigo : 'Cotización'} onCerrar={onCerrar} ancho="820px">
      {cargando && !c && <Cargando />}
      {error && <ErrorCaja mensaje={error} onReintentar={recargar} />}
      {c && (
        <div className="no-imprimir-cont">
          <dl className="datos">
            <div><dt>Cliente</dt><dd>{c.clienteNombre}</dd></div><div><dt>Vehículo</dt><dd>{c.placa || '—'}</dd></div>
            <div><dt>Emisión</dt><dd>{fecha(c.fechaEmision)}</dd></div><div><dt>Vigencia</dt><dd>{fecha(c.vigenciaHasta)}</dd></div>
            <div><dt>Estado</dt><dd><Insignia estado={c.estado} /></dd></div>
          </dl>
          <Tabla columnas={[
            { titulo: 'Descripción', ver: (l) => l.descripcion }, { titulo: 'Cant.', clase: 'num', ver: (l) => l.cantidad },
            { titulo: 'P. unit.', clase: 'num', ver: (l) => soles(l.precioUnitario) }, { titulo: 'Importe', clase: 'num', ver: (l) => soles(l.importe) },
          ]} filas={c.detalles} clave={(l) => l.id} />
          <div className="totales" style={{ marginTop: '1rem' }}>
            <div><span>Subtotal</span><span>{soles(c.subtotal)}</span></div>
            {Number(c.descuento) > 0 && <div><span>Descuento</span><span>− {soles(c.descuento)}</span></div>}
            <div><span>Base imponible</span><span>{soles(c.baseImponible)}</span></div>
            <div><span>IGV (18%)</span><span>{soles(c.igv)}</span></div>
            <div className="total"><span>Total</span><span>{soles(c.total)}</span></div>
          </div>
          {c.observaciones && <p><strong>Observaciones:</strong> {c.observaciones}</p>}
          <div className="modal-pie" style={{ flexWrap: 'wrap' }}>
            {c.estado === 'PENDIENTE' && <>
              <button className="btn btn-sec" onClick={() => onEditar(c)}>Editar</button>
              <button className="btn btn-sec" onClick={() => setConf('rechazar')}>Rechazar</button>
              <button className="btn btn-pri" onClick={() => accion(`/cotizaciones/${id}/aprobar`, 'Cotización aprobada')} disabled={c.vencida}>Aprobar</button></>}
            {c.estado === 'APROBADA' && <button className="btn btn-pri" onClick={convertir}>Convertir en orden de trabajo</button>}
            {c.ordenId && <button className="btn btn-sec" onClick={() => navegar('/panel/ordenes/' + c.ordenId)}>Ver orden</button>}
            <button className="btn btn-sec" onClick={() => window.print()}>Imprimir</button>
          </div>
        </div>
      )}
      {conf === 'rechazar' && <Confirmar titulo="Rechazar cotización" textoOk="Rechazar" peligro mensaje="La cotización quedará rechazada." onCancelar={() => setConf(null)}
        onOk={async () => { const d = await api.post(`/cotizaciones/${id}/rechazar`); setDatos(d); setConf(null); aviso('Cotización rechazada'); onCambio(); }} />}
    </Modal>
  );
}

function Editor({ cotizacion, onCerrar, onGuardado }) {
  const [cliente, setCliente] = useState(cotizacion ? { id: cotizacion.clienteId } : null);
  const [vehiculoId, setVehiculoId] = useState(cotizacion?.vehiculoId ?? null);
  const [lineas, setLineas] = useState(cotizacion
    ? cotizacion.detalles.map((l) => ({ tipo: l.tipo, servicioId: l.servicioId, repuestoId: l.repuestoId, descripcion: l.descripcion, cantidad: l.cantidad, precioUnitario: l.precioUnitario }))
    : []);
  const { datos: servicios } = useCarga('/servicios', { soloActivos: true });
  const { datos: repuestos } = useCarga('/repuestos', { soloActivos: true, tamano: 100 });
  const f = useFormulario({ vigenciaHasta: cotizacion?.vigenciaHasta || '', descuento: cotizacion?.descuento ?? 0, observaciones: cotizacion?.observaciones || '' }, async (v) => {
    if (!cliente) throw Object.assign(new Error('Elige el cliente'), { campos: {} });
    if (lineas.length === 0) throw Object.assign(new Error('Agrega al menos una línea'), { campos: {} });
    const cuerpo = { clienteId: cliente.id, vehiculoId, vigenciaHasta: nulo(v.vigenciaHasta), descuento: num(v.descuento) ?? 0, observaciones: nulo(v.observaciones.trim()),
      lineas: lineas.map((l) => ({ tipo: l.tipo, servicioId: l.servicioId ?? null, repuestoId: l.repuestoId ?? null, descripcion: nulo((l.descripcion || '').trim()), cantidad: Number(l.cantidad), precioUnitario: num(l.precioUnitario) })) };
    if (cotizacion) await api.put('/cotizaciones/' + cotizacion.id, cuerpo); else await api.post('/cotizaciones', cuerpo);
    onGuardado();
  });
  const [nuevo, setNuevo] = useState({ tipo: 'SERVICIO', refId: '', cantidad: 1, precio: '', descripcion: '' });

  function agregar() {
    const l = { tipo: nuevo.tipo, servicioId: null, repuestoId: null, cantidad: Number(nuevo.cantidad) || 1, precioUnitario: nuevo.precio === '' ? null : Number(nuevo.precio), descripcion: nuevo.descripcion };
    if (nuevo.tipo === 'SERVICIO') {
      const s = (servicios || []).find((x) => String(x.id) === String(nuevo.refId)); if (!s) return;
      l.servicioId = s.id; l.descripcion = l.descripcion || s.nombre; if (l.precioUnitario == null) l.precioUnitario = s.precioReferencial;
    } else if (nuevo.tipo === 'REPUESTO') {
      const r = (repuestos?.contenido || []).find((x) => String(x.id) === String(nuevo.refId)); if (!r) return;
      l.repuestoId = r.id; l.descripcion = l.descripcion || r.nombre; if (l.precioUnitario == null) l.precioUnitario = r.precioVenta;
    } else if (!l.descripcion.trim()) return;
    if (l.precioUnitario == null) return;
    setLineas((x) => [...x, l]);
    setNuevo({ tipo: nuevo.tipo, refId: '', cantidad: 1, precio: '', descripcion: '' });
  }
  const subtotal = lineas.reduce((a, l) => a + Number(l.cantidad) * Number(l.precioUnitario || 0), 0);
  const total = Math.max(0, subtotal - (Number(f.valores.descuento) || 0));

  return (
    <Modal titulo={cotizacion ? 'Editar ' + cotizacion.codigo : 'Nueva cotización'} onCerrar={onCerrar} ancho="860px">
      <form onSubmit={f.submit} noValidate>
        <SelectorCliente valor={cliente?.id} nombreInicial={cotizacion?.clienteNombre || ''} onElegir={(c) => { setCliente(c); setVehiculoId(null); }} />
        <SelectorVehiculo etiqueta="Vehículo (opcional)" clienteId={cliente?.id} valor={vehiculoId} onCambio={setVehiculoId} />
        <h3>Líneas</h3>
        <div className="tarjeta" style={{ background: 'var(--gris-100)' }}>
          <div className="rejilla">
            <Campo etiqueta="Tipo"><select value={nuevo.tipo} onChange={(e) => setNuevo({ ...nuevo, tipo: e.target.value, refId: '' })}>
              <option value="SERVICIO">Servicio</option><option value="REPUESTO">Repuesto</option><option value="MANO_OBRA">Mano de obra</option></select></Campo>
            {nuevo.tipo === 'SERVICIO' && <Campo etiqueta="Servicio"><select value={nuevo.refId} onChange={(e) => setNuevo({ ...nuevo, refId: e.target.value })}>
              <option value="">Elige…</option>{(servicios || []).map((s) => <option key={s.id} value={s.id}>{s.nombre}</option>)}</select></Campo>}
            {nuevo.tipo === 'REPUESTO' && <Campo etiqueta="Repuesto"><select value={nuevo.refId} onChange={(e) => setNuevo({ ...nuevo, refId: e.target.value })}>
              <option value="">Elige…</option>{(repuestos?.contenido || []).map((r) => <option key={r.id} value={r.id}>{r.codigo} · {r.nombre}</option>)}</select></Campo>}
            {nuevo.tipo === 'MANO_OBRA' && <Campo etiqueta="Descripción"><input value={nuevo.descripcion} onChange={(e) => setNuevo({ ...nuevo, descripcion: e.target.value })} /></Campo>}
            <Campo etiqueta="Cantidad"><input type="number" min="1" value={nuevo.cantidad} onChange={(e) => setNuevo({ ...nuevo, cantidad: e.target.value })} /></Campo>
            <Campo etiqueta="Precio unitario (S/, IGV incl.)" ayuda="Vacío = precio del catálogo"><input type="number" step="0.01" min="0" value={nuevo.precio} onChange={(e) => setNuevo({ ...nuevo, precio: e.target.value })} /></Campo>
          </div>
          <button type="button" className="btn btn-sec btn-sm" onClick={agregar}>+ Agregar línea</button>
        </div>
        <Tabla vacio="Aún no hay líneas." clave={(l) => l.__k || JSON.stringify(l)} columnas={[
          { titulo: 'Descripción', ver: (l) => l.descripcion }, { titulo: 'Cant.', clase: 'num', ver: (l) => l.cantidad },
          { titulo: 'P. unit.', clase: 'num', ver: (l) => soles(l.precioUnitario) }, { titulo: 'Importe', clase: 'num', ver: (l) => soles(Number(l.cantidad) * Number(l.precioUnitario)) },
          { titulo: '', clase: 'acc', ver: (l) => <button type="button" className="btn btn-sec btn-sm" onClick={() => setLineas((x) => x.filter((y) => y !== l))}>Quitar</button> },
        ]} filas={lineas} />
        <div className="rejilla" style={{ marginTop: '1rem' }}>
          <Campo etiqueta="Válida hasta" error={f.errores.vigenciaHasta} ayuda="Vacío = 15 días"><input type="date" value={f.valores.vigenciaHasta} onChange={f.cambiar('vigenciaHasta')} /></Campo>
          <Campo etiqueta="Descuento (S/)" error={f.errores.descuento}><input type="number" step="0.01" min="0" value={f.valores.descuento} onChange={f.cambiar('descuento')} /></Campo>
          <div className="ancho"><Campo etiqueta="Observaciones" error={f.errores.observaciones}><textarea rows="2" value={f.valores.observaciones} onChange={f.cambiar('observaciones')} maxLength={400} /></Campo></div>
        </div>
        <p className="totales" style={{ textAlign: 'right' }}>Total estimado (IGV incluido): <strong>{soles(total)}</strong></p>
        {f.errorGeneral && <div className="caja-error" role="alert">{f.errorGeneral}</div>}
        {Object.keys(f.errores).length > 0 && <div className="caja-error">{Object.entries(f.errores).map(([k, v]) => <div key={k}>{k}: {v}</div>)}</div>}
        <div className="modal-pie"><button type="button" className="btn btn-sec" onClick={onCerrar}>Cancelar</button><button className="btn btn-pri" disabled={f.enviando}>{f.enviando ? 'Guardando…' : 'Guardar'}</button></div>
      </form>
    </Modal>
  );
}
