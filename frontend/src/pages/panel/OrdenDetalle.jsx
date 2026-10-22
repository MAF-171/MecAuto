import { useEffect, useState } from 'react';
import { api } from '../../lib/api';
import { useAuth } from '../../lib/auth.jsx';
import { useCarga, useRetraso } from '../../lib/hooks';
import { Campo, Confirmar, Insignia, Modal, PaginaCab, Tabla, Cargando, ErrorCaja, useAviso, useFormulario, nulo, num } from '../../components/ui.jsx';
import { etiqueta, fecha, fechaHora, soles } from '../../lib/format';
import { Link, navegar } from '../../lib/router.jsx';

export default function OrdenDetalle({ params }) {
  const { tieneRol } = useAuth();
  const aviso = useAviso();
  const { datos: o, cargando, error, recargar, setDatos } = useCarga('/ordenes/' + params.id);
  const [estadoNuevo, setEstadoNuevo] = useState(null);
  const [modal, setModal] = useState(null); // 'servicio' | 'repuesto' | 'mecanico'
  const [quitar, setQuitar] = useState(null);
  if (cargando && !o) return <Cargando />;
  if (error) return <ErrorCaja mensaje={error} onReintentar={recargar} />;
  if (!o) return null;
  const gestion = tieneRol('ADMINISTRADOR', 'RECEPCION');

  async function ejecutar(promesa, ok) {
    try { const d = await promesa; setDatos(d); if (ok) aviso(ok); } catch (e) { aviso(e.message, 'error'); }
  }
  async function generarCotizacion() {
    try { const c = await api.post(`/ordenes/${o.id}/cotizacion`); aviso('Cotización ' + c.codigo + ' creada'); navegar('/panel/cotizaciones'); }
    catch (e) { aviso(e.message, 'error'); }
  }

  return (
    <>
      <PaginaCab titulo={`Orden ${o.codigo}`} subtitulo={`${o.clienteNombre} · ${o.placa} · ${o.vehiculo}`}>
        <Insignia estado={o.estado} />
        <Link to="/panel/ordenes" className="btn btn-sec">← Volver</Link>
      </PaginaCab>

      <section className="tarjeta">
        <h2>Cambiar estado</h2>
        {o.transicionesPermitidas.length === 0 ? <p style={{ margin: 0 }}>La orden está en un estado final ({etiqueta(o.estado)}).</p> : (
          <div className="pie-acciones">
            {o.transicionesPermitidas.map((e) => (
              <button key={e} className={'btn ' + (e === 'CANCELADA' ? 'btn-sec' : 'btn-pri')} onClick={() => setEstadoNuevo(e)}>Pasar a {etiqueta(e)}</button>
            ))}
          </div>
        )}
        {gestion && (
          <div className="pie-acciones" style={{ marginTop: '.8rem' }}>
            {o.estado === 'COMPLETADA' && !o.comprobanteId && <button className="btn btn-oscuro" onClick={() => setModal('comprobante')}>Emitir comprobante interno</button>}
            {o.comprobanteId && <Link className="btn btn-sec" to={'/panel/comprobantes/' + o.comprobanteId}>Ver comprobante {o.comprobanteNumero}</Link>}
            {o.estado !== 'CANCELADA' && <button className="btn btn-sec" onClick={generarCotizacion}>Generar cotización</button>}
          </div>
        )}
      </section>

      <section className="tarjeta">
        <h2>Datos de la orden</h2>
        <dl className="datos">
          <div><dt>Cliente</dt><dd>{o.clienteNombre} ({o.clienteDocumento})</dd></div>
          <div><dt>Mecánico</dt><dd>{o.mecanicoNombre || 'Sin asignar'} {gestion && o.editable && <button className="btn btn-sec btn-sm" onClick={() => setModal('mecanico')}>Cambiar</button>}</dd></div>
          <div><dt>Ingreso</dt><dd>{fechaHora(o.fechaIngreso)}</dd></div>
          <div><dt>Entrega estimada</dt><dd>{fecha(o.fechaEntregaEstimada)}</dd></div>
          <div><dt>Cierre</dt><dd>{fechaHora(o.fechaCierre)}</dd></div>
          <div><dt>Km de ingreso</dt><dd>{o.kilometrajeIngreso ?? '—'}</dd></div>
        </dl>
      </section>

      <FormEdicion key={[o.falla,o.diagnostico,o.trabajosRealizados,o.manoObra,o.descuento,o.estado,o.kilometrajeIngreso,o.fechaEntregaEstimada].join('|')} o={o} gestion={gestion} onGuardado={(d) => { setDatos(d); aviso('Orden actualizada'); }} />

      <section className="tarjeta">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <h2>Servicios</h2>
          {o.editable && <button className="btn btn-sec btn-sm" onClick={() => setModal('servicio')}>+ Agregar servicio</button>}
        </div>
        <Tabla vacio="Aún no hay servicios en esta orden." columnas={[
          { titulo: 'Descripción', ver: (l) => l.descripcion }, { titulo: 'Cant.', clase: 'num', ver: (l) => l.cantidad },
          { titulo: 'P. unit.', clase: 'num', ver: (l) => soles(l.precioUnitario) }, { titulo: 'Importe', clase: 'num', ver: (l) => soles(l.importe) },
          ...(o.editable ? [{ titulo: '', clase: 'acc', ver: (l) => <button className="btn btn-sec btn-sm" onClick={() => setQuitar({ tipo: 'servicios', l })}>Quitar</button> }] : []),
        ]} filas={o.servicios} />
      </section>

      <section className="tarjeta">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <h2>Repuestos</h2>
          {o.editable && <button className="btn btn-sec btn-sm" onClick={() => setModal('repuesto')}>+ Agregar repuesto</button>}
        </div>
        <p className="campo-ayuda">El stock se descuenta cuando la orden pasa a «Completada» y se devuelve si se cancela.</p>
        <Tabla vacio="Aún no hay repuestos en esta orden." columnas={[
          { titulo: 'Código', ver: (l) => l.codigo }, { titulo: 'Descripción', ver: (l) => l.descripcion }, { titulo: 'Cant.', clase: 'num', ver: (l) => l.cantidad },
          { titulo: 'Stock disp.', clase: 'num', ver: (l) => l.stockDisponible }, { titulo: 'P. unit.', clase: 'num', ver: (l) => soles(l.precioUnitario) },
          { titulo: 'Importe', clase: 'num', ver: (l) => soles(l.importe) },
          { titulo: 'Inventario', ver: (l) => (l.descontado ? <Insignia estado="ENTREGADA" texto="Descontado" /> : <Insignia estado="PENDIENTE" texto="Pendiente" />) },
          ...(o.editable ? [{ titulo: '', clase: 'acc', ver: (l) => <button className="btn btn-sec btn-sm" onClick={() => setQuitar({ tipo: 'repuestos', l })}>Quitar</button> }] : []),
        ]} filas={o.repuestos} />
        <div className="totales" style={{ marginTop: '1rem' }}>
          <div><span>Mano de obra</span><span>{soles(o.manoObra)}</span></div>
          {Number(o.descuento) > 0 && <div><span>Descuento</span><span>− {soles(o.descuento)}</span></div>}
          <div><span>Base imponible</span><span>{soles(o.baseImponible)}</span></div>
          <div><span>IGV (18%)</span><span>{soles(o.igv)}</span></div>
          <div className="total"><span>Total (IGV incluido)</span><span>{soles(o.total)}</span></div>
        </div>
      </section>

      <section className="tarjeta">
        <h2>Historial de estados</h2>
        <ul className="linea-tiempo">
          {o.historial.map((h, i) => (
            <li key={i}><strong>{h.estadoAnterior ? `${etiqueta(h.estadoAnterior)} → ` : ''}{etiqueta(h.estadoNuevo)}</strong>
              <br /><small>{fechaHora(h.fecha)} · {h.usuario || 'Sistema'}{h.comentario ? ' · ' + h.comentario : ''}</small></li>
          ))}
        </ul>
      </section>

      {estadoNuevo && <CambiarEstado o={o} estado={estadoNuevo} onCerrar={() => setEstadoNuevo(null)}
        onHecho={(d) => { setDatos(d); setEstadoNuevo(null); aviso('Estado actualizado'); }} />}
      {modal === 'servicio' && <AgregarServicio o={o} onCerrar={() => setModal(null)} onHecho={(d) => { setDatos(d); setModal(null); aviso('Servicio agregado'); }} />}
      {modal === 'repuesto' && <AgregarRepuesto o={o} onCerrar={() => setModal(null)} onHecho={(d) => { setDatos(d); setModal(null); aviso('Repuesto agregado'); }} />}
      {modal === 'comprobante' && <EmitirComprobante o={o} onCerrar={() => setModal(null)} />}
      {modal === 'mecanico' && <AsignarMecanico o={o} onCerrar={() => setModal(null)} onHecho={(d) => { setDatos(d); setModal(null); aviso('Mecánico asignado'); }} />}
      {quitar && <Confirmar titulo="Quitar línea" textoOk="Quitar" peligro mensaje={`¿Quitar «${quitar.l.descripcion}» de la orden?`} onCancelar={() => setQuitar(null)}
        onOk={async () => { const d = await api.del(`/ordenes/${o.id}/${quitar.tipo}/${quitar.l.id}`); setDatos(d); setQuitar(null); aviso('Línea eliminada'); }} />}
    </>
  );
}

function FormEdicion({ o, gestion, onGuardado }) {
  const soloMecanico = !gestion;
  const f = useFormulario({
    falla: o.falla || '', diagnostico: o.diagnostico || '', trabajosRealizados: o.trabajosRealizados || '',
    manoObra: o.manoObra ?? 0, descuento: o.descuento ?? 0, fechaEntregaEstimada: o.fechaEntregaEstimada || '', kilometrajeIngreso: o.kilometrajeIngreso ?? '',
  }, async (v) => {
    const d = await api.put('/ordenes/' + o.id, { falla: v.falla.trim(), diagnostico: nulo(v.diagnostico.trim()), trabajosRealizados: nulo(v.trabajosRealizados.trim()),
      manoObra: num(v.manoObra) ?? 0, descuento: num(v.descuento) ?? 0, fechaEntregaEstimada: nulo(v.fechaEntregaEstimada), kilometrajeIngreso: num(v.kilometrajeIngreso) });
    onGuardado(d);
  });
  const bloqueado = !o.editable;
  return (
    <form className="tarjeta" onSubmit={f.submit} noValidate>
      <h2>Falla, diagnóstico y trabajos</h2>
      {bloqueado && <div className="caja-info">La orden está {etiqueta(o.estado).toLowerCase()} y ya no admite cambios.</div>}
      <Campo etiqueta="Falla reportada" error={f.errores.falla}><textarea rows="2" value={f.valores.falla} onChange={f.cambiar('falla')} disabled={bloqueado || soloMecanico} maxLength={500} /></Campo>
      <Campo etiqueta="Diagnóstico" error={f.errores.diagnostico}><textarea rows="3" value={f.valores.diagnostico} onChange={f.cambiar('diagnostico')} disabled={bloqueado} maxLength={1000} /></Campo>
      <Campo etiqueta="Trabajos realizados" error={f.errores.trabajosRealizados}><textarea rows="3" value={f.valores.trabajosRealizados} onChange={f.cambiar('trabajosRealizados')} disabled={bloqueado} maxLength={1000} /></Campo>
      {gestion && (
        <div className="rejilla">
          <Campo etiqueta="Mano de obra (S/)" error={f.errores.manoObra}><input type="number" step="0.01" min="0" value={f.valores.manoObra} onChange={f.cambiar('manoObra')} disabled={bloqueado} /></Campo>
          <Campo etiqueta="Descuento (S/)" error={f.errores.descuento}><input type="number" step="0.01" min="0" value={f.valores.descuento} onChange={f.cambiar('descuento')} disabled={bloqueado} /></Campo>
          <Campo etiqueta="Entrega estimada" error={f.errores.fechaEntregaEstimada}><input type="date" value={f.valores.fechaEntregaEstimada} onChange={f.cambiar('fechaEntregaEstimada')} disabled={bloqueado} /></Campo>
          <Campo etiqueta="Kilometraje de ingreso" error={f.errores.kilometrajeIngreso}><input type="number" min="0" value={f.valores.kilometrajeIngreso} onChange={f.cambiar('kilometrajeIngreso')} disabled={bloqueado} /></Campo>
        </div>
      )}
      {f.errorGeneral && <div className="caja-error" role="alert">{f.errorGeneral}</div>}
      {!bloqueado && <button className="btn btn-pri" disabled={f.enviando}>{f.enviando ? 'Guardando…' : 'Guardar cambios'}</button>}
    </form>
  );
}

function CambiarEstado({ o, estado, onCerrar, onHecho }) {
  const critico = estado === 'CANCELADA';
  return (
    <Confirmar titulo={'Pasar a ' + etiqueta(estado)} peligro={critico} textoOk={critico ? 'Cancelar orden' : 'Confirmar'}
      pedirMotivo={critico} etiquetaMotivo="Motivo de la cancelación"
      mensaje={estado === 'COMPLETADA' ? 'Al completar la orden se descontará del inventario el stock de los repuestos. Verifica que las líneas sean correctas.'
        : critico ? 'La orden quedará cancelada y se devolverá al inventario lo que ya se haya descontado.' : `La orden ${o.codigo} pasará a «${etiqueta(estado)}».`}
      onCancelar={onCerrar}
      onOk={async (motivo) => onHecho(await api.post(`/ordenes/${o.id}/estado`, { estado, comentario: nulo(motivo) }))} />
  );
}

function AsignarMecanico({ o, onCerrar, onHecho }) {
  const { datos: mecs } = useCarga('/usuarios/mecanicos');
  const [id, setId] = useState(o.mecanicoId || '');
  const f = useFormulario({}, async () => onHecho(await api.patch(`/ordenes/${o.id}/mecanico`, { mecanicoId: num(id) })));
  return (
    <Modal titulo="Asignar mecánico" onCerrar={onCerrar} ancho="420px">
      <form onSubmit={f.submit}>
        <Campo etiqueta="Mecánico"><select value={id} onChange={(e) => setId(e.target.value)}>
          <option value="">Sin asignar</option>{(mecs || []).map((m) => <option key={m.id} value={m.id}>{m.nombre}</option>)}</select></Campo>
        {f.errorGeneral && <div className="caja-error">{f.errorGeneral}</div>}
        <div className="modal-pie"><button type="button" className="btn btn-sec" onClick={onCerrar}>Cancelar</button><button className="btn btn-pri" disabled={f.enviando}>Guardar</button></div>
      </form>
    </Modal>
  );
}

function AgregarServicio({ o, onCerrar, onHecho }) {
  const { datos: servicios } = useCarga('/servicios', { soloActivos: true });
  const f = useFormulario({ servicioId: '', cantidad: 1, precioUnitario: '', descripcion: '' }, async (v) =>
    onHecho(await api.post(`/ordenes/${o.id}/servicios`, { servicioId: num(v.servicioId), cantidad: num(v.cantidad), precioUnitario: num(v.precioUnitario), descripcion: nulo(v.descripcion.trim()) })));
  const elegido = (servicios || []).find((s) => String(s.id) === String(f.valores.servicioId));
  return (
    <Modal titulo="Agregar servicio" onCerrar={onCerrar} ancho="520px">
      <form onSubmit={f.submit} noValidate>
        <Campo etiqueta="Servicio *" error={f.errores.servicioId}><select value={f.valores.servicioId} onChange={f.cambiar('servicioId')}>
          <option value="">Elige…</option>{(servicios || []).map((s) => <option key={s.id} value={s.id}>{s.nombre}</option>)}</select></Campo>
        <div className="rejilla">
          <Campo etiqueta="Cantidad *" error={f.errores.cantidad}><input type="number" min="1" value={f.valores.cantidad} onChange={f.cambiar('cantidad')} /></Campo>
          <Campo etiqueta="Precio unitario (S/, IGV incl.)" error={f.errores.precioUnitario}
            ayuda={elegido?.precioReferencial != null ? 'Vacío = precio de referencia ' + soles(elegido.precioReferencial) : 'Este servicio es según diagnóstico: indica el precio'}>
            <input type="number" step="0.01" min="0" value={f.valores.precioUnitario} onChange={f.cambiar('precioUnitario')} /></Campo>
        </div>
        <Campo etiqueta="Descripción (opcional)" error={f.errores.descripcion}><input value={f.valores.descripcion} onChange={f.cambiar('descripcion')} maxLength={200} /></Campo>
        {f.errorGeneral && <div className="caja-error" role="alert">{f.errorGeneral}</div>}
        <div className="modal-pie"><button type="button" className="btn btn-sec" onClick={onCerrar}>Cancelar</button><button className="btn btn-pri" disabled={f.enviando}>Agregar</button></div>
      </form>
    </Modal>
  );
}

function AgregarRepuesto({ o, onCerrar, onHecho }) {
  const [q, setQ] = useState('');
  const t = useRetraso(q);
  const { datos } = useCarga('/repuestos', { q: t, soloActivos: true, tamano: 20 });
  const f = useFormulario({ repuestoId: '', cantidad: 1 }, async (v) =>
    onHecho(await api.post(`/ordenes/${o.id}/repuestos`, { repuestoId: num(v.repuestoId), cantidad: num(v.cantidad) })));
  return (
    <Modal titulo="Agregar repuesto" onCerrar={onCerrar} ancho="520px">
      <form onSubmit={f.submit} noValidate>
        <Campo etiqueta="Buscar repuesto"><input type="search" value={q} onChange={(e) => setQ(e.target.value)} placeholder="Código o nombre" /></Campo>
        <div className="campo">
          <span className="campo-et">Repuesto * <small>(haz clic en uno)</small></span>
          <div className="lista-elegir" role="listbox" aria-label="Repuestos disponibles">
            {(datos?.contenido || []).length === 0 && <div className="lista-vacia">No hay repuestos activos que coincidan.</div>}
            {(datos?.contenido || []).map((r) => (
              <button type="button" key={r.id} role="option" aria-selected={String(f.valores.repuestoId) === String(r.id)}
                className={'lista-item' + (String(f.valores.repuestoId) === String(r.id) ? ' elegido' : '')}
                onClick={() => f.setValores((x) => ({ ...x, repuestoId: String(r.id) }))}>
                <span>{r.codigo} · {r.nombre}</span>
                <small>stock {r.stockActual} — {soles(r.precioVenta)}</small>
              </button>
            ))}
          </div>
          {f.errores.repuestoId && <small className="campo-error">{f.errores.repuestoId}</small>}
          {f.valores.repuestoId && <small className="campo-ayuda">Elegido: {((datos?.contenido || []).find((r) => String(r.id) === String(f.valores.repuestoId)) || {}).nombre}</small>}
        </div>
        <Campo etiqueta="Cantidad *" error={f.errores.cantidad}><input type="number" min="1" value={f.valores.cantidad} onChange={f.cambiar('cantidad')} /></Campo>
        {f.errorGeneral && <div className="caja-error" role="alert">{f.errorGeneral}</div>}
        <div className="modal-pie"><button type="button" className="btn btn-sec" onClick={onCerrar}>Cancelar</button><button className="btn btn-pri" disabled={f.enviando || !f.valores.repuestoId}>Agregar</button></div>
      </form>
    </Modal>
  );
}

function EmitirComprobante({ o, onCerrar }) {
  const aviso = useAviso();
  const f = useFormulario({ metodoPago: 'EFECTIVO' }, async (v) => {
    const c = await api.post('/comprobantes', { ordenId: o.id, metodoPago: v.metodoPago });
    aviso('Comprobante ' + c.numeroCompleto + ' emitido');
    navegar('/panel/comprobantes/' + c.id);
  });
  return (
    <Modal titulo="Emitir comprobante interno" onCerrar={onCerrar} ancho="460px">
      <form onSubmit={f.submit}>
        <p>Total de la orden: <strong>{soles(o.total)}</strong> (IGV incluido). Se emitirá boleta o factura según el documento del cliente (RUC = factura).</p>
        <Campo etiqueta="Método de pago *" error={f.errores.metodoPago}>
          <select value={f.valores.metodoPago} onChange={f.cambiar('metodoPago')}>
            <option value="EFECTIVO">Efectivo</option><option value="TRANSFERENCIA">Transferencia</option><option value="TARJETA">Tarjeta</option><option value="YAPE_PLIN">Yape / Plin</option>
          </select>
        </Campo>
        {f.errorGeneral && <div className="caja-error" role="alert">{f.errorGeneral}</div>}
        <div className="modal-pie"><button type="button" className="btn btn-sec" onClick={onCerrar}>Cancelar</button><button className="btn btn-pri" disabled={f.enviando}>Emitir</button></div>
      </form>
    </Modal>
  );
}
