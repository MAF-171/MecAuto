import { useState } from 'react';
import { api } from '../../lib/api';
import { useAuth } from '../../lib/auth.jsx';
import { Campo, Confirmar, Insignia, Modal, PaginaCab, Paginacion, Tabla, Cargando, ErrorCaja, useAviso, useFormulario, nulo } from '../../components/ui.jsx';
import { useListado, BuscadorInput } from './comun.jsx';

const VACIO = { tipoDocumento: 'DNI', nroDocumento: '', nombre: '', telefono: '', correo: '', direccion: '' };

export default function Clientes() {
  const lista = useListado('/clientes');
  const aviso = useAviso();
  const [editando, setEditando] = useState(null); // null | 'nuevo' | cliente
  const [desactivar, setDesactivar] = useState(null);
  const { datos, cargando, error } = lista;

  async function alternar(c) {
    try { await api.patch(`/clientes/${c.id}/estado`, { activo: !c.activo }); aviso(c.activo ? 'Cliente desactivado' : 'Cliente reactivado'); lista.recargar(); }
    catch (e) { aviso(e.message, 'error'); }
  }
  return (
    <>
      <PaginaCab titulo="Clientes" subtitulo="Personas y empresas atendidas por el taller">
        <button className="btn btn-pri" onClick={() => setEditando('nuevo')}>+ Nuevo cliente</button>
      </PaginaCab>
      <div className="filtros"><BuscadorInput valor={lista.q} onCambio={lista.setQ} placeholder="Nombre, documento o teléfono" /></div>
      {error && <ErrorCaja mensaje={error} onReintentar={lista.recargar} />}
      {cargando && !datos ? <Cargando /> : datos && (
        <>
          <Tabla vacio="No se encontraron clientes." columnas={[
            { titulo: 'Documento', ver: (c) => `${c.tipoDocumento} ${c.nroDocumento}` },
            { titulo: 'Nombre', ver: (c) => c.nombre }, { titulo: 'Teléfono', ver: (c) => c.telefono || '—' },
            { titulo: 'Vehículos', clase: 'num', ver: (c) => c.cantidadVehiculos },
            { titulo: 'Estado', ver: (c) => <Insignia estado={c.activo ? 'ATENDIDA' : 'CANCELADA'} texto={c.activo ? 'Activo' : 'Inactivo'} /> },
            { titulo: '', clase: 'acc', ver: (c) => (
              <span className="pie-acciones">
                <button className="btn btn-sec btn-sm" onClick={() => setEditando(c)}>Editar</button>
                {c.activo ? <button className="btn btn-sec btn-sm" onClick={() => setDesactivar(c)}>Desactivar</button>
                  : <button className="btn btn-sec btn-sm" onClick={() => alternar(c)}>Reactivar</button>}
              </span>) },
          ]} filas={datos.contenido} />
          <Paginacion datos={datos} onCambiar={lista.setPagina} />
        </>
      )}
      {editando && <FormCliente cliente={editando === 'nuevo' ? null : editando} onCerrar={() => setEditando(null)}
        onGuardado={() => { setEditando(null); lista.recargar(); aviso('Cliente guardado'); }} />}
      {desactivar && <Confirmar titulo="Desactivar cliente" textoOk="Desactivar" peligro
        mensaje={`¿Desactivar a ${desactivar.nombre}? Su historial se conserva y podrás reactivarlo.`}
        onCancelar={() => setDesactivar(null)}
        onOk={async () => { await api.patch(`/clientes/${desactivar.id}/estado`, { activo: false }); setDesactivar(null); aviso('Cliente desactivado'); lista.recargar(); }} />}
    </>
  );
}

function FormCliente({ cliente, onCerrar, onGuardado }) {
  const f = useFormulario(cliente ? { ...VACIO, ...Object.fromEntries(Object.entries(cliente).map(([k, v]) => [k, v ?? ''])) } : VACIO, async (v) => {
    const cuerpo = { tipoDocumento: v.tipoDocumento, nroDocumento: v.nroDocumento.trim(), nombre: v.nombre.trim(),
      telefono: nulo(v.telefono.trim()), correo: nulo(v.correo.trim()), direccion: nulo(v.direccion.trim()) };
    if (cliente) await api.put('/clientes/' + cliente.id, cuerpo); else await api.post('/clientes', cuerpo);
    onGuardado();
  });
  return (
    <Modal titulo={cliente ? 'Editar cliente' : 'Nuevo cliente'} onCerrar={onCerrar}>
      <form onSubmit={f.submit} noValidate>
        <div className="rejilla">
          <Campo etiqueta="Tipo de documento *" error={f.errores.tipoDocumento}>
            <select value={f.valores.tipoDocumento} onChange={f.cambiar('tipoDocumento')}><option>DNI</option><option>RUC</option><option>CE</option></select>
          </Campo>
          <Campo etiqueta="N.º de documento *" error={f.errores.nroDocumento}><input value={f.valores.nroDocumento} onChange={f.cambiar('nroDocumento')} maxLength={20} /></Campo>
          <div className="ancho"><Campo etiqueta="Nombre o razón social *" error={f.errores.nombre}><input value={f.valores.nombre} onChange={f.cambiar('nombre')} maxLength={120} /></Campo></div>
          <Campo etiqueta="Teléfono" error={f.errores.telefono}><input value={f.valores.telefono} onChange={f.cambiar('telefono')} maxLength={20} /></Campo>
          <Campo etiqueta="Correo" error={f.errores.correo}><input type="email" value={f.valores.correo} onChange={f.cambiar('correo')} maxLength={100} /></Campo>
          <div className="ancho"><Campo etiqueta="Dirección" error={f.errores.direccion}><input value={f.valores.direccion} onChange={f.cambiar('direccion')} maxLength={200} /></Campo></div>
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
