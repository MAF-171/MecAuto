import { useState } from 'react';
import { api } from '../../lib/api';
import { useAuth } from '../../lib/auth.jsx';
import { Campo, Confirmar, Insignia, Modal, PaginaCab, Paginacion, Tabla, Cargando, ErrorCaja, Vacio, useAviso, useFormulario, nulo, num } from '../../components/ui.jsx';
import { useCarga } from '../../lib/hooks';
import { useListado, BuscadorInput, SelectorCliente } from './comun.jsx';
import { fechaHora, soles } from '../../lib/format';
import { Link } from '../../lib/router.jsx';

export default function Vehiculos() {
  const { tieneRol } = useAuth();
  const puedeEditar = tieneRol('ADMINISTRADOR', 'RECEPCION');
  const lista = useListado('/vehiculos');
  const aviso = useAviso();
  const [editando, setEditando] = useState(null);
  const [historial, setHistorial] = useState(null);
  const [desactivar, setDesactivar] = useState(null);
  const { datos, cargando, error } = lista;
  async function reactivar(v) {
    try { await api.patch(`/vehiculos/${v.id}/estado`, { activo: true }); aviso('Vehículo reactivado'); lista.recargar(); } catch (e) { aviso(e.message, 'error'); }
  }
  return (
    <>
      <PaginaCab titulo="Vehículos" subtitulo="Vehículos registrados y su historial de mantenimiento">
        {puedeEditar && <button className="btn btn-pri" onClick={() => setEditando('nuevo')}>+ Nuevo vehículo</button>}
      </PaginaCab>
      <div className="filtros"><BuscadorInput valor={lista.q} onCambio={lista.setQ} placeholder="Placa, marca, modelo o cliente" /></div>
      {error && <ErrorCaja mensaje={error} onReintentar={lista.recargar} />}
      {cargando && !datos ? <Cargando /> : datos && (
        <>
          <Tabla vacio="No se encontraron vehículos." columnas={[
            { titulo: 'Placa', ver: (v) => <strong>{v.placa}</strong> }, { titulo: 'Vehículo', ver: (v) => `${v.marca} ${v.modelo}${v.anio ? ' ' + v.anio : ''}` },
            { titulo: 'Cliente', ver: (v) => v.clienteNombre }, { titulo: 'Km', clase: 'num', ver: (v) => v.kilometraje.toLocaleString('es-PE') },
            { titulo: 'Estado', ver: (v) => <Insignia estado={v.activo ? 'ATENDIDA' : 'CANCELADA'} texto={v.activo ? 'Activo' : 'Inactivo'} /> },
            { titulo: '', clase: 'acc', ver: (v) => (
              <span className="pie-acciones">
                <button className="btn btn-sec btn-sm" onClick={() => setHistorial(v)}>Historial</button>
                {puedeEditar && <button className="btn btn-sec btn-sm" onClick={() => setEditando(v)}>Editar</button>}
                {puedeEditar && (v.activo ? <button className="btn btn-sec btn-sm" onClick={() => setDesactivar(v)}>Desactivar</button>
                  : <button className="btn btn-sec btn-sm" onClick={() => reactivar(v)}>Reactivar</button>)}
              </span>) },
          ]} filas={datos.contenido} />
          <Paginacion datos={datos} onCambiar={lista.setPagina} />
        </>
      )}
      {editando && <FormVehiculo vehiculo={editando === 'nuevo' ? null : editando} onCerrar={() => setEditando(null)}
        onGuardado={() => { setEditando(null); lista.recargar(); aviso('Vehículo guardado'); }} />}
      {historial && <Historial vehiculo={historial} onCerrar={() => setHistorial(null)} />}
      {desactivar && <Confirmar titulo="Desactivar vehículo" textoOk="Desactivar" peligro
        mensaje={`¿Desactivar el vehículo ${desactivar.placa}? Su historial se conserva.`} onCancelar={() => setDesactivar(null)}
        onOk={async () => { await api.patch(`/vehiculos/${desactivar.id}/estado`, { activo: false }); setDesactivar(null); aviso('Vehículo desactivado'); lista.recargar(); }} />}
    </>
  );
}

function FormVehiculo({ vehiculo, onCerrar, onGuardado }) {
  const [cliente, setCliente] = useState(vehiculo ? { id: vehiculo.clienteId } : null);
  const f = useFormulario({
    placa: vehiculo?.placa || '', marca: vehiculo?.marca || '', modelo: vehiculo?.modelo || '',
    anio: vehiculo?.anio ?? '', color: vehiculo?.color || '', kilometraje: vehiculo?.kilometraje ?? 0,
  }, async (v) => {
    if (!cliente) throw Object.assign(new Error('Elige el cliente propietario'), { campos: {} });
    const cuerpo = { clienteId: cliente.id, placa: v.placa.trim(), marca: v.marca.trim(), modelo: v.modelo.trim(),
      anio: num(v.anio), color: nulo(v.color.trim()), kilometraje: num(v.kilometraje) ?? 0 };
    if (vehiculo) await api.put('/vehiculos/' + vehiculo.id, cuerpo); else await api.post('/vehiculos', cuerpo);
    onGuardado();
  });
  return (
    <Modal titulo={vehiculo ? 'Editar vehículo' : 'Nuevo vehículo'} onCerrar={onCerrar}>
      <form onSubmit={f.submit} noValidate>
        <SelectorCliente valor={cliente?.id} nombreInicial={vehiculo?.clienteNombre || ''} onElegir={setCliente} error={f.errores.clienteId} />
        <div className="rejilla">
          <Campo etiqueta="Placa *" error={f.errores.placa}><input value={f.valores.placa} onChange={f.cambiar('placa')} maxLength={10} placeholder="ABC-123" /></Campo>
          <Campo etiqueta="Año" error={f.errores.anio}><input type="number" value={f.valores.anio} onChange={f.cambiar('anio')} /></Campo>
          <Campo etiqueta="Marca *" error={f.errores.marca}><input value={f.valores.marca} onChange={f.cambiar('marca')} maxLength={40} /></Campo>
          <Campo etiqueta="Modelo *" error={f.errores.modelo}><input value={f.valores.modelo} onChange={f.cambiar('modelo')} maxLength={40} /></Campo>
          <Campo etiqueta="Color" error={f.errores.color}><input value={f.valores.color} onChange={f.cambiar('color')} maxLength={30} /></Campo>
          <Campo etiqueta="Kilometraje" error={f.errores.kilometraje}><input type="number" min="0" value={f.valores.kilometraje} onChange={f.cambiar('kilometraje')} /></Campo>
        </div>
        {f.errorGeneral && <div className="caja-error" role="alert">{f.errorGeneral}</div>}
        <div className="modal-pie">
          <button type="button" className="btn btn-sec" onClick={onCerrar}>Cancelar</button>
          <button className="btn btn-pri" disabled={f.enviando}>{f.enviando ? 'Guardando…' : 'Guardar'}</button>
        </div>
      </form>
    </Modal>
  );
}

function Historial({ vehiculo, onCerrar }) {
  const { datos, cargando, error } = useCarga(`/vehiculos/${vehiculo.id}/historial`);
  return (
    <Modal titulo={`Historial de ${vehiculo.placa}`} onCerrar={onCerrar} ancho="820px">
      {cargando && <Cargando />}
      {error && <ErrorCaja mensaje={error} />}
      {datos && datos.length === 0 && <Vacio>Este vehículo aún no tiene órdenes de trabajo.</Vacio>}
      {datos && datos.map((h) => (
        <article className="tarjeta" key={h.ordenId}>
          <div style={{ display: 'flex', justifyContent: 'space-between', gap: '1rem', flexWrap: 'wrap' }}>
            <strong><Link to={'/panel/ordenes/' + h.ordenId}>{h.codigo}</Link> · {fechaHora(h.fechaIngreso)}</strong>
            <span><Insignia estado={h.estado} /> {soles(h.total)}</span>
          </div>
          <dl className="datos" style={{ marginTop: '.6rem' }}>
            <div><dt>Km</dt><dd>{h.kilometraje ?? '—'}</dd></div>
            <div><dt>Mecánico</dt><dd>{h.mecanico || '—'}</dd></div>
            <div><dt>Falla</dt><dd>{h.falla || '—'}</dd></div>
            <div><dt>Diagnóstico</dt><dd>{h.diagnostico || '—'}</dd></div>
            <div><dt>Trabajos realizados</dt><dd>{h.trabajosRealizados || '—'}</dd></div>
          </dl>
        </article>
      ))}
    </Modal>
  );
}
