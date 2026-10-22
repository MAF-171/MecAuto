import { useState } from 'react';
import { api } from '../../lib/api';
import { useAuth } from '../../lib/auth.jsx';
import { useCarga } from '../../lib/hooks';
import { Campo, Insignia, Modal, PaginaCab, Paginacion, Tabla, Cargando, ErrorCaja, useAviso, useFormulario, nulo, num } from '../../components/ui.jsx';
import { useListado, BuscadorInput } from './comun.jsx';
import { etiqueta, fechaHora, soles } from '../../lib/format';
import { Link } from '../../lib/router.jsx';

export default function Inventario() {
  const { tieneRol } = useAuth();
  const [tab, setTab] = useState('repuestos');
  const verMov = tieneRol('ADMINISTRADOR', 'RECEPCION');
  return (
    <>
      <PaginaCab titulo="Inventario" subtitulo="Repuestos, movimientos de stock y proveedores" />
      <div className="pestanas" role="tablist">
        <button role="tab" className={tab === 'repuestos' ? 'activo' : ''} onClick={() => setTab('repuestos')}>Repuestos</button>
        {verMov && <button role="tab" className={tab === 'movimientos' ? 'activo' : ''} onClick={() => setTab('movimientos')}>Movimientos</button>}
        {verMov && <button role="tab" className={tab === 'proveedores' ? 'activo' : ''} onClick={() => setTab('proveedores')}>Proveedores</button>}
      </div>
      {tab === 'repuestos' && <Repuestos />}
      {tab === 'movimientos' && verMov && <Movimientos />}
      {tab === 'proveedores' && verMov && <Proveedores />}
    </>
  );
}

function Repuestos() {
  const { tieneRol } = useAuth();
  const admin = tieneRol('ADMINISTRADOR');
  const [bajo, setBajo] = useState(false);
  const lista = useListado('/repuestos', { soloBajoStock: bajo });
  const aviso = useAviso();
  const [form, setForm] = useState(null);
  const [mov, setMov] = useState(null);
  const { datos, cargando, error } = lista;
  async function alternar(r) {
    try { await api.patch(`/repuestos/${r.id}/estado`, { activo: !r.activo }); aviso('Estado actualizado'); lista.recargar(); } catch (e) { aviso(e.message, 'error'); }
  }
  return (
    <>
      <div className="filtros">
        <BuscadorInput valor={lista.q} onCambio={lista.setQ} placeholder="Código, nombre o categoría" />
        <Campo etiqueta="Mostrar"><select value={bajo ? '1' : ''} onChange={(e) => { setBajo(!!e.target.value); lista.reiniciar(); }}><option value="">Todos</option><option value="1">Solo stock bajo</option></select></Campo>
        {admin && <button className="btn btn-pri" onClick={() => setForm('nuevo')}>+ Nuevo repuesto</button>}
      </div>
      {error && <ErrorCaja mensaje={error} onReintentar={lista.recargar} />}
      {cargando && !datos ? <Cargando /> : datos && (
        <>
          <Tabla vacio="No hay repuestos." columnas={[
            { titulo: 'Código', ver: (r) => r.codigo }, { titulo: 'Repuesto', ver: (r) => <>{r.nombre}<div style={{ color: 'var(--gris-500)', fontSize: '.8rem' }}>{r.categoria}</div></> },
            { titulo: 'Stock', clase: 'num', ver: (r) => <>{r.stockActual} {r.bajoStock && <Insignia estado="CANCELADA" texto="Stock bajo" />}</> },
            { titulo: 'Mínimo', clase: 'num', ver: (r) => r.stockMinimo }, { titulo: 'P. venta', clase: 'num', ver: (r) => soles(r.precioVenta) },
            { titulo: 'Proveedor', ver: (r) => r.proveedorNombre || '—' },
            { titulo: 'Estado', ver: (r) => <Insignia estado={r.activo ? 'ATENDIDA' : 'CANCELADA'} texto={r.activo ? 'Activo' : 'Inactivo'} /> },
            { titulo: '', clase: 'acc', ver: (r) => (
              <span className="pie-acciones">
                {r.activo && <button className="btn btn-sec btn-sm" onClick={() => setMov(r)}>Movimiento</button>}
                {admin && <button className="btn btn-sec btn-sm" onClick={() => setForm(r)}>Editar</button>}
                {admin && <button className="btn btn-sec btn-sm" onClick={() => alternar(r)}>{r.activo ? 'Desactivar' : 'Activar'}</button>}
              </span>) },
          ]} filas={datos.contenido} />
          <Paginacion datos={datos} onCambiar={lista.setPagina} />
        </>
      )}
      {form && <FormRepuesto repuesto={form === 'nuevo' ? null : form} onCerrar={() => setForm(null)} onGuardado={() => { setForm(null); lista.recargar(); aviso('Repuesto guardado'); }} />}
      {mov && <FormMovimiento repuesto={mov} onCerrar={() => setMov(null)} onGuardado={() => { setMov(null); lista.recargar(); aviso('Movimiento registrado'); }} />}
    </>
  );
}

function FormRepuesto({ repuesto, onCerrar, onGuardado }) {
  const { datos: provs } = useCarga('/proveedores', { soloActivos: true });
  const f = useFormulario({
    codigo: repuesto?.codigo || '', nombre: repuesto?.nombre || '', categoria: repuesto?.categoria || '', proveedorId: repuesto?.proveedorId ?? '',
    precioCompra: repuesto?.precioCompra ?? 0, precioVenta: repuesto?.precioVenta ?? '', stockMinimo: repuesto?.stockMinimo ?? 0, stockInicial: 0,
  }, async (v) => {
    const cuerpo = { proveedorId: num(v.proveedorId), codigo: v.codigo.trim(), nombre: v.nombre.trim(), categoria: nulo(v.categoria.trim()),
      precioCompra: num(v.precioCompra) ?? 0, precioVenta: num(v.precioVenta), stockMinimo: num(v.stockMinimo) ?? 0, stockInicial: repuesto ? null : (num(v.stockInicial) ?? 0) };
    if (repuesto) await api.put('/repuestos/' + repuesto.id, cuerpo); else await api.post('/repuestos', cuerpo);
    onGuardado();
  });
  return (
    <Modal titulo={repuesto ? 'Editar repuesto' : 'Nuevo repuesto'} onCerrar={onCerrar} ancho="680px">
      <form onSubmit={f.submit} noValidate>
        <div className="rejilla">
          <Campo etiqueta="Código *" error={f.errores.codigo}><input value={f.valores.codigo} onChange={f.cambiar('codigo')} maxLength={20} /></Campo>
          <Campo etiqueta="Categoría" error={f.errores.categoria}><input value={f.valores.categoria} onChange={f.cambiar('categoria')} maxLength={40} /></Campo>
          <div className="ancho"><Campo etiqueta="Nombre *" error={f.errores.nombre}><input value={f.valores.nombre} onChange={f.cambiar('nombre')} maxLength={100} /></Campo></div>
          <Campo etiqueta="Precio de compra (S/)" error={f.errores.precioCompra}><input type="number" step="0.01" min="0" value={f.valores.precioCompra} onChange={f.cambiar('precioCompra')} /></Campo>
          <Campo etiqueta="Precio de venta (S/, IGV incl.) *" error={f.errores.precioVenta}><input type="number" step="0.01" min="0" value={f.valores.precioVenta} onChange={f.cambiar('precioVenta')} /></Campo>
          <Campo etiqueta="Stock mínimo" error={f.errores.stockMinimo}><input type="number" min="0" value={f.valores.stockMinimo} onChange={f.cambiar('stockMinimo')} /></Campo>
          {!repuesto && <Campo etiqueta="Stock inicial" error={f.errores.stockInicial} ayuda="Se registra como movimiento de entrada"><input type="number" min="0" value={f.valores.stockInicial} onChange={f.cambiar('stockInicial')} /></Campo>}
          <Campo etiqueta="Proveedor" error={f.errores.proveedorId}><select value={f.valores.proveedorId} onChange={f.cambiar('proveedorId')}><option value="">Sin proveedor</option>
            {(provs || []).map((p) => <option key={p.id} value={p.id}>{p.razonSocial}</option>)}</select></Campo>
        </div>
        {repuesto && <p className="campo-ayuda">El stock solo cambia con movimientos (entrada, salida o ajuste) para dejar historial.</p>}
        {f.errorGeneral && <div className="caja-error" role="alert">{f.errorGeneral}</div>}
        <div className="modal-pie"><button type="button" className="btn btn-sec" onClick={onCerrar}>Cancelar</button><button className="btn btn-pri" disabled={f.enviando}>{f.enviando ? 'Guardando…' : 'Guardar'}</button></div>
      </form>
    </Modal>
  );
}

function FormMovimiento({ repuesto, onCerrar, onGuardado }) {
  const { tieneRol } = useAuth();
  const tipos = tieneRol('ADMINISTRADOR') ? ['ENTRADA', 'SALIDA', 'AJUSTE'] : tieneRol('RECEPCION') ? ['ENTRADA', 'SALIDA'] : ['SALIDA'];
  const [clave] = useState(() => (crypto.randomUUID ? crypto.randomUUID() : String(Date.now()) + Math.random()));
  const f = useFormulario({ tipo: tipos[0], cantidad: '', motivo: '' }, async (v) => {
    await api.post(`/repuestos/${repuesto.id}/movimientos`, { tipo: v.tipo, cantidad: num(v.cantidad), motivo: nulo(v.motivo.trim()), claveIdempotencia: clave });
    onGuardado();
  });
  return (
    <Modal titulo={`Movimiento · ${repuesto.codigo}`} onCerrar={onCerrar} ancho="460px">
      <p>{repuesto.nombre} — stock actual: <strong>{repuesto.stockActual}</strong></p>
      <form onSubmit={f.submit} noValidate>
        <Campo etiqueta="Tipo *"><select value={f.valores.tipo} onChange={f.cambiar('tipo')}>{tipos.map((t) => <option key={t} value={t}>{etiqueta(t)}</option>)}</select></Campo>
        <Campo etiqueta={f.valores.tipo === 'AJUSTE' ? 'Stock contado *' : 'Cantidad *'} error={f.errores.cantidad}
          ayuda={f.valores.tipo === 'AJUSTE' ? 'El stock quedará exactamente en este valor' : undefined}>
          <input type="number" min="0" value={f.valores.cantidad} onChange={f.cambiar('cantidad')} /></Campo>
        <Campo etiqueta={f.valores.tipo === 'AJUSTE' ? 'Motivo del ajuste *' : 'Motivo'} error={f.errores.motivo}><input value={f.valores.motivo} onChange={f.cambiar('motivo')} maxLength={200} /></Campo>
        {f.errorGeneral && <div className="caja-error" role="alert">{f.errorGeneral}</div>}
        <div className="modal-pie"><button type="button" className="btn btn-sec" onClick={onCerrar}>Cancelar</button><button className="btn btn-pri" disabled={f.enviando}>Registrar</button></div>
      </form>
    </Modal>
  );
}

function Movimientos() {
  const [tipo, setTipo] = useState('');
  const [desde, setDesde] = useState('');
  const [hasta, setHasta] = useState('');
  const [pagina, setPagina] = useState(0);
  const { datos, cargando, error, recargar } = useCarga('/movimientos', { tipo, desde, hasta, pagina, tamano: 20 });
  return (
    <>
      <div className="filtros">
        <Campo etiqueta="Tipo"><select value={tipo} onChange={(e) => { setTipo(e.target.value); setPagina(0); }}><option value="">Todos</option><option value="ENTRADA">Entrada</option><option value="SALIDA">Salida</option><option value="AJUSTE">Ajuste</option></select></Campo>
        <Campo etiqueta="Desde"><input type="date" value={desde} onChange={(e) => { setDesde(e.target.value); setPagina(0); }} /></Campo>
        <Campo etiqueta="Hasta"><input type="date" value={hasta} onChange={(e) => { setHasta(e.target.value); setPagina(0); }} /></Campo>
      </div>
      {error && <ErrorCaja mensaje={error} onReintentar={recargar} />}
      {cargando && !datos ? <Cargando /> : datos && (
        <>
          <Tabla vacio="No hay movimientos." columnas={[
            { titulo: 'Fecha', ver: (m) => fechaHora(m.fecha) }, { titulo: 'Repuesto', ver: (m) => `${m.repuestoCodigo} · ${m.repuestoNombre}` },
            { titulo: 'Tipo', ver: (m) => <Insignia estado={m.tipo} /> }, { titulo: 'Cantidad', clase: 'num', ver: (m) => (m.cantidad > 0 ? '+' : '') + m.cantidad },
            { titulo: 'Stock resultante', clase: 'num', ver: (m) => m.stockResultante }, { titulo: 'Motivo', ver: (m) => m.motivo || '—' },
            { titulo: 'Orden', ver: (m) => (m.ordenId ? <Link to={'/panel/ordenes/' + m.ordenId}>Ver</Link> : '—') }, { titulo: 'Usuario', ver: (m) => m.usuarioNombre || '—' },
          ]} filas={datos.contenido} />
          <Paginacion datos={datos} onCambiar={setPagina} />
        </>
      )}
    </>
  );
}

function Proveedores() {
  const { tieneRol } = useAuth();
  const admin = tieneRol('ADMINISTRADOR');
  const { datos, cargando, error, recargar } = useCarga('/proveedores');
  const aviso = useAviso();
  const [form, setForm] = useState(null);
  async function alternar(p) {
    try { await api.patch(`/proveedores/${p.id}/estado`, { activo: !p.activo }); aviso('Estado actualizado'); recargar(); } catch (e) { aviso(e.message, 'error'); }
  }
  return (
    <>
      {admin && <div className="filtros"><button className="btn btn-pri" onClick={() => setForm('nuevo')}>+ Nuevo proveedor</button></div>}
      {error && <ErrorCaja mensaje={error} onReintentar={recargar} />}
      {cargando && !datos ? <Cargando /> : datos && (
        <Tabla vacio="No hay proveedores." columnas={[
          { titulo: 'Razón social', ver: (p) => p.razonSocial }, { titulo: 'RUC', ver: (p) => p.ruc || '—' }, { titulo: 'Teléfono', ver: (p) => p.telefono || '—' },
          { titulo: 'Estado', ver: (p) => <Insignia estado={p.activo ? 'ATENDIDA' : 'CANCELADA'} texto={p.activo ? 'Activo' : 'Inactivo'} /> },
          ...(admin ? [{ titulo: '', clase: 'acc', ver: (p) => <span className="pie-acciones"><button className="btn btn-sec btn-sm" onClick={() => setForm(p)}>Editar</button>
            <button className="btn btn-sec btn-sm" onClick={() => alternar(p)}>{p.activo ? 'Desactivar' : 'Activar'}</button></span> }] : []),
        ]} filas={datos} />
      )}
      {form && <FormProveedor proveedor={form === 'nuevo' ? null : form} onCerrar={() => setForm(null)} onGuardado={() => { setForm(null); recargar(); aviso('Proveedor guardado'); }} />}
    </>
  );
}

function FormProveedor({ proveedor, onCerrar, onGuardado }) {
  const f = useFormulario({ razonSocial: proveedor?.razonSocial || '', ruc: proveedor?.ruc || '', telefono: proveedor?.telefono || '' }, async (v) => {
    const c = { razonSocial: v.razonSocial.trim(), ruc: v.ruc.trim(), telefono: nulo(v.telefono.trim()) };
    if (proveedor) await api.put('/proveedores/' + proveedor.id, c); else await api.post('/proveedores', c);
    onGuardado();
  });
  return (
    <Modal titulo={proveedor ? 'Editar proveedor' : 'Nuevo proveedor'} onCerrar={onCerrar} ancho="480px">
      <form onSubmit={f.submit} noValidate>
        <Campo etiqueta="Razón social *" error={f.errores.razonSocial}><input value={f.valores.razonSocial} onChange={f.cambiar('razonSocial')} maxLength={120} /></Campo>
        <Campo etiqueta="RUC *" error={f.errores.ruc}><input value={f.valores.ruc} onChange={f.cambiar('ruc')} maxLength={11} /></Campo>
        <Campo etiqueta="Teléfono" error={f.errores.telefono}><input value={f.valores.telefono} onChange={f.cambiar('telefono')} maxLength={20} /></Campo>
        {f.errorGeneral && <div className="caja-error" role="alert">{f.errorGeneral}</div>}
        <div className="modal-pie"><button type="button" className="btn btn-sec" onClick={onCerrar}>Cancelar</button><button className="btn btn-pri" disabled={f.enviando}>Guardar</button></div>
      </form>
    </Modal>
  );
}
