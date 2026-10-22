import { useEffect, useState } from 'react';
import { api } from '../../lib/api';
import { useAuth } from '../../lib/auth.jsx';
import { useCarga } from '../../lib/hooks';
import { Campo, Insignia, Modal, PaginaCab, Paginacion, Tabla, Cargando, ErrorCaja, useAviso, useFormulario, nulo, num } from '../../components/ui.jsx';
import { useListado, BuscadorInput, SelectorCliente, SelectorVehiculo } from './comun.jsx';
import { etiqueta, fechaHora, soles, fecha } from '../../lib/format';
import { Link, navegar } from '../../lib/router.jsx';

const ESTADOS = ['RECIBIDA', 'EN_DIAGNOSTICO', 'EN_REPARACION', 'COMPLETADA', 'ENTREGADA', 'CANCELADA'];

export default function Ordenes() {
  const { tieneRol } = useAuth();
  const [vista, setVista] = useState('lista');
  const [estado, setEstado] = useState('');
  const [desde, setDesde] = useState('');
  const [hasta, setHasta] = useState('');
  const lista = useListado('/ordenes', { estado, desde, hasta }, vista === 'tablero' ? 100 : 15);
  const [nueva, setNueva] = useState(false);
  const { datos, cargando, error } = lista;
  const cols = (o) => [
    { titulo: 'Código', ver: (x) => <Link to={'/panel/ordenes/' + x.id}><strong>{x.codigo}</strong></Link> },
    { titulo: 'Cliente', ver: (x) => x.clienteNombre }, { titulo: 'Vehículo', ver: (x) => `${x.placa} · ${x.vehiculo}` },
    { titulo: 'Mecánico', ver: (x) => x.mecanicoNombre || 'Sin asignar' }, { titulo: 'Ingreso', ver: (x) => fechaHora(x.fechaIngreso) },
    { titulo: 'Estado', ver: (x) => <Insignia estado={x.estado} /> }, { titulo: 'Total', clase: 'num', ver: (x) => soles(x.total) },
  ];
  return (
    <>
      <PaginaCab titulo="Órdenes de trabajo" subtitulo={tieneRol('MECANICO') ? 'Órdenes asignadas a ti' : 'Seguimiento de los trabajos del taller'}>
        <div className="pestanas" style={{ margin: 0, border: 0 }}>
          <button className={vista === 'lista' ? 'activo' : ''} onClick={() => setVista('lista')}>Lista</button>
          <button className={vista === 'tablero' ? 'activo' : ''} onClick={() => setVista('tablero')}>Tablero</button>
        </div>
        {tieneRol('ADMINISTRADOR', 'RECEPCION') && <button className="btn btn-pri" onClick={() => setNueva(true)}>+ Nueva orden</button>}
      </PaginaCab>
      <div className="filtros">
        <BuscadorInput valor={lista.q} onCambio={lista.setQ} placeholder="Código, placa o cliente" />
        {vista === 'lista' && <Campo etiqueta="Estado"><select value={estado} onChange={(e) => { setEstado(e.target.value); lista.reiniciar(); }}>
          <option value="">Todos</option>{ESTADOS.map((e) => <option key={e} value={e}>{etiqueta(e)}</option>)}</select></Campo>}
        <Campo etiqueta="Desde"><input type="date" value={desde} onChange={(e) => { setDesde(e.target.value); lista.reiniciar(); }} /></Campo>
        <Campo etiqueta="Hasta"><input type="date" value={hasta} onChange={(e) => { setHasta(e.target.value); lista.reiniciar(); }} /></Campo>
      </div>
      {error && <ErrorCaja mensaje={error} onReintentar={lista.recargar} />}
      {cargando && !datos ? <Cargando /> : datos && (vista === 'lista' ? (
        <>
          <Tabla vacio="No hay órdenes con esos filtros." columnas={cols()} filas={datos.contenido} onFila={(o) => navegar('/panel/ordenes/' + o.id)} />
          <Paginacion datos={datos} onCambiar={lista.setPagina} />
        </>
      ) : (
        <div className="tablero">
          {ESTADOS.filter((e) => e !== 'CANCELADA').map((e) => {
            const items = datos.contenido.filter((o) => o.estado === e);
            return (
              <section className="columna" key={e} aria-label={etiqueta(e)}>
                <h3>{etiqueta(e)} <span>{items.length}</span></h3>
                {items.map((o) => (
                  <Link key={o.id} to={'/panel/ordenes/' + o.id} className="ficha">
                    <strong>{o.codigo} · {o.placa}</strong>{o.clienteNombre}<br />
                    <small>{o.vehiculo} · {o.mecanicoNombre || 'Sin mecánico'}</small>
                    {o.fechaEntregaEstimada && <small><br />Entrega est.: {fecha(o.fechaEntregaEstimada)}</small>}
                  </Link>
                ))}
              </section>
            );
          })}
        </div>
      ))}
      {datos && vista === 'tablero' && datos.totalElementos > datos.contenido.length && <p className="campo-ayuda">Se muestran las {datos.contenido.length} órdenes más recientes. Usa los filtros para acotar.</p>}
      {nueva && <FormOrden onCerrar={() => setNueva(false)} onCreada={(o) => { setNueva(false); navegar('/panel/ordenes/' + o.id); }} />}
    </>
  );
}

function FormOrden({ onCerrar, onCreada }) {
  const [cliente, setCliente] = useState(null);
  const [vehiculoId, setVehiculoId] = useState(null);
  const { datos: mecanicos } = useCarga('/usuarios/mecanicos');
  const f = useFormulario({ falla: '', mecanicoId: '', kilometrajeIngreso: '', fechaEntregaEstimada: '' }, async (v) => {
    if (!vehiculoId) throw Object.assign(new Error('Elige cliente y vehículo'), { campos: {} });
    const o = await api.post('/ordenes', { vehiculoId, falla: v.falla.trim(), mecanicoId: num(v.mecanicoId),
      kilometrajeIngreso: num(v.kilometrajeIngreso), fechaEntregaEstimada: nulo(v.fechaEntregaEstimada), citaId: null });
    onCreada(o);
  });
  return (
    <Modal titulo="Nueva orden de trabajo" onCerrar={onCerrar} ancho="680px">
      <form onSubmit={f.submit} noValidate>
        <SelectorCliente valor={cliente?.id} onElegir={(c) => { setCliente(c); setVehiculoId(null); }} />
        <SelectorVehiculo clienteId={cliente?.id} valor={vehiculoId} onCambio={setVehiculoId} error={f.errores.vehiculoId} />
        <Campo etiqueta="Falla reportada *" error={f.errores.falla}><textarea rows="3" value={f.valores.falla} onChange={f.cambiar('falla')} maxLength={500} /></Campo>
        <div className="rejilla">
          <Campo etiqueta="Mecánico" error={f.errores.mecanicoId}><select value={f.valores.mecanicoId} onChange={f.cambiar('mecanicoId')}>
            <option value="">Sin asignar</option>{(mecanicos || []).map((m) => <option key={m.id} value={m.id}>{m.nombre}</option>)}</select></Campo>
          <Campo etiqueta="Kilometraje de ingreso" error={f.errores.kilometrajeIngreso}><input type="number" min="0" value={f.valores.kilometrajeIngreso} onChange={f.cambiar('kilometrajeIngreso')} /></Campo>
          <Campo etiqueta="Entrega estimada" error={f.errores.fechaEntregaEstimada}><input type="date" value={f.valores.fechaEntregaEstimada} onChange={f.cambiar('fechaEntregaEstimada')} /></Campo>
        </div>
        {f.errorGeneral && <div className="caja-error" role="alert">{f.errorGeneral}</div>}
        <div className="modal-pie"><button type="button" className="btn btn-sec" onClick={onCerrar}>Cancelar</button>
          <button className="btn btn-pri" disabled={f.enviando}>{f.enviando ? 'Creando…' : 'Crear orden'}</button></div>
      </form>
    </Modal>
  );
}
