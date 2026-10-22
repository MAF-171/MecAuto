import { useState } from 'react';
import { api } from '../../lib/api';
import { useAuth } from '../../lib/auth.jsx';
import { useCarga } from '../../lib/hooks';
import { Campo, Insignia, Modal, PaginaCab, Tabla, Cargando, ErrorCaja, useAviso, useFormulario, nulo, num } from '../../components/ui.jsx';
import { soles } from '../../lib/format';
import { BuscadorInput } from './comun.jsx';

export default function Servicios() {
  const { tieneRol } = useAuth();
  const admin = tieneRol('ADMINISTRADOR');
  const [q, setQ] = useState('');
  const { datos, cargando, error, recargar } = useCarga('/servicios', { q });
  const aviso = useAviso();
  const [editando, setEditando] = useState(null);
  async function alternar(s) {
    try { await api.patch(`/servicios/${s.id}/estado`, { activo: !s.activo }); aviso(s.activo ? 'Servicio desactivado' : 'Servicio activado'); recargar(); }
    catch (e) { aviso(e.message, 'error'); }
  }
  return (
    <>
      <PaginaCab titulo="Servicios" subtitulo="Catálogo de servicios del taller (precios con IGV incluido)">
        {admin && <button className="btn btn-pri" onClick={() => setEditando('nuevo')}>+ Nuevo servicio</button>}
      </PaginaCab>
      <div className="filtros"><BuscadorInput valor={q} onCambio={setQ} placeholder="Buscar servicio" /></div>
      {error && <ErrorCaja mensaje={error} onReintentar={recargar} />}
      {cargando && !datos ? <Cargando /> : datos && (
        <Tabla vacio="No hay servicios." columnas={[
          { titulo: 'Servicio', ver: (s) => <><strong>{s.nombre}</strong><div style={{ color: 'var(--gris-500)', fontSize: '.82rem' }}>{s.descripcion}</div></> },
          { titulo: 'Precio ref.', clase: 'num', ver: (s) => (s.precioReferencial != null ? soles(s.precioReferencial) : 'Según diagnóstico') },
          { titulo: 'Duración', clase: 'num', ver: (s) => (s.duracionMinutos ? s.duracionMinutos + ' min' : '—') },
          { titulo: 'Estado', ver: (s) => <Insignia estado={s.activo ? 'ATENDIDA' : 'CANCELADA'} texto={s.activo ? 'Activo' : 'Inactivo'} /> },
          ...(admin ? [{ titulo: '', clase: 'acc', ver: (s) => (
            <span className="pie-acciones">
              <button className="btn btn-sec btn-sm" onClick={() => setEditando(s)}>Editar</button>
              <button className="btn btn-sec btn-sm" onClick={() => alternar(s)}>{s.activo ? 'Desactivar' : 'Activar'}</button>
            </span>) }] : []),
        ]} filas={datos} />
      )}
      {editando && <FormServicio servicio={editando === 'nuevo' ? null : editando} onCerrar={() => setEditando(null)}
        onGuardado={() => { setEditando(null); recargar(); aviso('Servicio guardado'); }} />}
    </>
  );
}

function FormServicio({ servicio, onCerrar, onGuardado }) {
  const f = useFormulario({
    nombre: servicio?.nombre || '', descripcion: servicio?.descripcion || '',
    precioReferencial: servicio?.precioReferencial ?? '', duracionMinutos: servicio?.duracionMinutos ?? '',
  }, async (v) => {
    const cuerpo = { nombre: v.nombre.trim(), descripcion: nulo(v.descripcion.trim()), precioReferencial: num(v.precioReferencial), duracionMinutos: num(v.duracionMinutos) };
    if (servicio) await api.put('/servicios/' + servicio.id, cuerpo); else await api.post('/servicios', cuerpo);
    onGuardado();
  });
  return (
    <Modal titulo={servicio ? 'Editar servicio' : 'Nuevo servicio'} onCerrar={onCerrar}>
      <form onSubmit={f.submit} noValidate>
        <Campo etiqueta="Nombre *" error={f.errores.nombre}><input value={f.valores.nombre} onChange={f.cambiar('nombre')} maxLength={100} /></Campo>
        <Campo etiqueta="Descripción" error={f.errores.descripcion}><textarea rows="3" value={f.valores.descripcion} onChange={f.cambiar('descripcion')} maxLength={400} /></Campo>
        <div className="rejilla">
          <Campo etiqueta="Precio referencial (S/, con IGV)" error={f.errores.precioReferencial} ayuda="Déjalo vacío si depende del diagnóstico"><input type="number" step="0.01" min="0" value={f.valores.precioReferencial} onChange={f.cambiar('precioReferencial')} /></Campo>
          <Campo etiqueta="Duración (minutos)" error={f.errores.duracionMinutos}><input type="number" min="1" value={f.valores.duracionMinutos} onChange={f.cambiar('duracionMinutos')} /></Campo>
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
