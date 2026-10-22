import { useState } from 'react';
import { api } from '../../lib/api';
import { Campo, Confirmar, Insignia, Modal, PaginaCab, Paginacion, Tabla, Cargando, ErrorCaja, useAviso, useFormulario, nulo, num } from '../../components/ui.jsx';
import { useListado, BuscadorInput, SelectorCliente, SelectorVehiculo } from './comun.jsx';
import { fechaHora, valorDeInput } from '../../lib/format';
import { navegar } from '../../lib/router.jsx';

export default function Citas() {
  const [estado, setEstado] = useState('');
  const [desde, setDesde] = useState('');
  const [hasta, setHasta] = useState('');
  const lista = useListado('/citas', { estado, desde, hasta });
  const aviso = useAviso();
  const [form, setForm] = useState(null);
  const [repro, setRepro] = useState(null);
  const [cancelar, setCancelar] = useState(null);
  const { datos, cargando, error } = lista;

  async function accion(ruta, ok) {
    try { await api.post(ruta); aviso(ok); lista.recargar(); } catch (e) { aviso(e.message, 'error'); }
  }
  async function crearOrden(c) {
    try { const o = await api.post('/ordenes/desde-cita/' + c.id); aviso('Orden ' + o.codigo + ' creada'); navegar('/panel/ordenes/' + o.id); }
    catch (e) { aviso(e.message, 'error'); }
  }
  return (
    <>
      <PaginaCab titulo="Citas" subtitulo="Solicitudes de la web y citas registradas por recepción">
        <button className="btn btn-pri" onClick={() => setForm('nueva')}>+ Nueva cita</button>
      </PaginaCab>
      <div className="filtros">
        <BuscadorInput valor={lista.q} onCambio={lista.setQ} placeholder="Código, nombre, placa o teléfono" />
        <Campo etiqueta="Estado"><select value={estado} onChange={(e) => { setEstado(e.target.value); lista.reiniciar(); }}>
          <option value="">Todos</option><option value="PENDIENTE">Pendiente</option><option value="CONFIRMADA">Confirmada</option><option value="COMPLETADA">Completada</option><option value="CANCELADA">Cancelada</option></select></Campo>
        <Campo etiqueta="Desde"><input type="date" value={desde} onChange={(e) => { setDesde(e.target.value); lista.reiniciar(); }} /></Campo>
        <Campo etiqueta="Hasta"><input type="date" value={hasta} onChange={(e) => { setHasta(e.target.value); lista.reiniciar(); }} /></Campo>
      </div>
      {error && <ErrorCaja mensaje={error} onReintentar={lista.recargar} />}
      {cargando && !datos ? <Cargando /> : datos && (
        <>
          <Tabla vacio="No hay citas con esos filtros." columnas={[
            { titulo: 'Código', ver: (c) => c.codigo }, { titulo: 'Fecha y hora', ver: (c) => fechaHora(c.fechaHora) },
            { titulo: 'Cliente', ver: (c) => c.clienteNombre || c.nombreContacto || '—' },
            { titulo: 'Contacto', ver: (c) => c.telefonoContacto || '—' },
            { titulo: 'Vehículo', ver: (c) => [c.placa, c.marca, c.modelo].filter(Boolean).join(' ') || '—' },
            { titulo: 'Servicio', ver: (c) => c.servicioNombre || '—' },
            { titulo: 'Origen', ver: (c) => (c.origen === 'PUBLICA' ? 'Web' : 'Recepción') },
            { titulo: 'Estado', ver: (c) => <Insignia estado={c.estado} /> },
            { titulo: '', clase: 'acc', ver: (c) => (c.estado === 'PENDIENTE' || c.estado === 'CONFIRMADA') ? (
              <span className="pie-acciones">
                {c.estado === 'PENDIENTE' && <button className="btn btn-sec btn-sm" onClick={() => accion(`/citas/${c.id}/confirmar`, 'Cita confirmada')}>Confirmar</button>}
                <button className="btn btn-sec btn-sm" onClick={() => setRepro(c)}>Reprogramar</button>
                <button className="btn btn-pri btn-sm" onClick={() => crearOrden(c)}>Crear orden</button>
                <button className="btn btn-sec btn-sm" onClick={() => setCancelar(c)}>Cancelar</button>
              </span>) : null },
          ]} filas={datos.contenido} />
          <Paginacion datos={datos} onCambiar={lista.setPagina} />
        </>
      )}
      {form && <FormCita onCerrar={() => setForm(null)} onGuardado={() => { setForm(null); lista.recargar(); aviso('Cita registrada'); }} />}
      {repro && <Reprogramar cita={repro} onCerrar={() => setRepro(null)} onGuardado={() => { setRepro(null); lista.recargar(); aviso('Cita reprogramada'); }} />}
      {cancelar && <Confirmar titulo="Cancelar cita" textoOk="Cancelar cita" peligro mensaje={`¿Cancelar la cita ${cancelar.codigo}?`}
        onCancelar={() => setCancelar(null)}
        onOk={async () => { await api.post(`/citas/${cancelar.id}/cancelar`); setCancelar(null); aviso('Cita cancelada'); lista.recargar(); }} />}
    </>
  );
}

function Reprogramar({ cita, onCerrar, onGuardado }) {
  const f = useFormulario({ fechaHora: valorDeInput(cita.fechaHora) }, async (v) => {
    await api.post(`/citas/${cita.id}/reprogramar`, { fechaHora: v.fechaHora + ':00' });
    onGuardado();
  });
  return (
    <Modal titulo={'Reprogramar ' + cita.codigo} onCerrar={onCerrar} ancho="440px">
      <form onSubmit={f.submit}>
        <Campo etiqueta="Nueva fecha y hora" error={f.errores.fechaHora}><input type="datetime-local" value={f.valores.fechaHora} onChange={f.cambiar('fechaHora')} required /></Campo>
        {f.errorGeneral && <div className="caja-error" role="alert">{f.errorGeneral}</div>}
        <div className="modal-pie"><button type="button" className="btn btn-sec" onClick={onCerrar}>Cancelar</button>
          <button className="btn btn-pri" disabled={f.enviando}>Reprogramar</button></div>
      </form>
    </Modal>
  );
}

function FormCita({ onCerrar, onGuardado }) {
  const [cliente, setCliente] = useState(null);
  const [vehiculoId, setVehiculoId] = useState(null);
  const [servicios, setServicios] = useState([]);
  const f = useFormulario({ servicioId: '', nombreContacto: '', telefonoContacto: '', correoContacto: '', placa: '', marca: '', modelo: '', fechaHora: '', motivo: '' }, async (v) => {
    const cuerpo = { clienteId: cliente?.id ?? null, vehiculoId, servicioId: num(v.servicioId),
      nombreContacto: v.nombreContacto.trim(), telefonoContacto: v.telefonoContacto.trim(), correoContacto: nulo(v.correoContacto.trim()),
      placa: v.placa.trim(), marca: nulo(v.marca.trim()), modelo: nulo(v.modelo.trim()),
      fechaHora: v.fechaHora ? v.fechaHora + ':00' : null, motivo: nulo(v.motivo.trim()) };
    await api.post('/citas', cuerpo);
    onGuardado();
  });
  useCargarServicios(setServicios);
  function elegirCliente(c) {
    setCliente(c); setVehiculoId(null);
    if (c) f.setValores((x) => ({ ...x, nombreContacto: c.nombre || x.nombreContacto, telefonoContacto: c.telefono || x.telefonoContacto, correoContacto: c.correo || x.correoContacto }));
  }
  function elegirVehiculo(id, v) {
    setVehiculoId(id);
    if (v) f.setValores((x) => ({ ...x, placa: v.placa, marca: v.marca || '', modelo: v.modelo || '' }));
  }
  return (
    <Modal titulo="Nueva cita" onCerrar={onCerrar} ancho="720px">
      <form onSubmit={f.submit} noValidate>
        <div className="caja-info">Si el cliente ya está registrado, búscalo para rellenar sus datos. Si no, escribe los datos de contacto y la placa.</div>
        <SelectorCliente etiqueta="Cliente registrado (opcional)" valor={cliente?.id} onElegir={elegirCliente} />
        {cliente && <SelectorVehiculo etiqueta="Vehículo registrado (opcional)" clienteId={cliente.id} valor={vehiculoId} onCambio={elegirVehiculo} error={f.errores.vehiculoId} />}
        <div className="rejilla">
          <Campo etiqueta="Nombre de contacto *" error={f.errores.nombreContacto}><input value={f.valores.nombreContacto} onChange={f.cambiar('nombreContacto')} maxLength={120} /></Campo>
          <Campo etiqueta="Teléfono *" error={f.errores.telefonoContacto}><input value={f.valores.telefonoContacto} onChange={f.cambiar('telefonoContacto')} maxLength={20} /></Campo>
          <Campo etiqueta="Correo" error={f.errores.correoContacto}><input type="email" value={f.valores.correoContacto} onChange={f.cambiar('correoContacto')} /></Campo>
          <Campo etiqueta="Placa *" error={f.errores.placa}><input value={f.valores.placa} onChange={f.cambiar('placa')} maxLength={10} /></Campo>
          <Campo etiqueta="Marca"><input value={f.valores.marca} onChange={f.cambiar('marca')} maxLength={40} /></Campo>
          <Campo etiqueta="Modelo"><input value={f.valores.modelo} onChange={f.cambiar('modelo')} maxLength={40} /></Campo>
          <Campo etiqueta="Fecha y hora *" error={f.errores.fechaHora}><input type="datetime-local" value={f.valores.fechaHora} onChange={f.cambiar('fechaHora')} /></Campo>
          <Campo etiqueta="Servicio *" error={f.errores.servicioId}><select value={f.valores.servicioId} onChange={f.cambiar('servicioId')}>
            <option value="">Elige un servicio</option>{servicios.map((s) => <option key={s.id} value={s.id}>{s.nombre}</option>)}</select></Campo>
          <div className="ancho"><Campo etiqueta="Motivo" error={f.errores.motivo}><textarea rows="2" value={f.valores.motivo} onChange={f.cambiar('motivo')} maxLength={300} /></Campo></div>
        </div>
        {f.errorGeneral && <div className="caja-error" role="alert">{f.errorGeneral}</div>}
        <div className="modal-pie"><button type="button" className="btn btn-sec" onClick={onCerrar}>Cancelar</button>
          <button className="btn btn-pri" disabled={f.enviando}>{f.enviando ? 'Guardando…' : 'Registrar cita'}</button></div>
      </form>
    </Modal>
  );
}

import { useEffect } from 'react';
function useCargarServicios(set) {
  useEffect(() => { api.get('/servicios', { soloActivos: true }).then(set).catch(() => {}); }, [set]);
}
