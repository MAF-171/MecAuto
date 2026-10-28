import { useState } from 'react';
import { api } from '../../lib/api';
import { useAuth } from '../../lib/auth.jsx';
import { nulo, Campo, Insignia, Modal, PaginaCab, Paginacion, Tabla, Cargando, ErrorCaja, useAviso, useFormulario } from '../../components/ui.jsx';
import { useListado, BuscadorInput } from './comun.jsx';
import { ROLES, fechaHora } from '../../lib/format';

export default function Usuarios() {
  const { usuario: yo } = useAuth();
  const lista = useListado('/usuarios');
  const aviso = useAviso();
  const [form, setForm] = useState(null);
  const [clave, setClave] = useState(null);
  const { datos, cargando, error } = lista;
  async function alternar(u) {
    try { await api.patch(`/usuarios/${u.id}/estado`, { activo: !u.activo }); aviso(u.activo ? 'Usuario desactivado' : 'Usuario activado'); lista.recargar(); } catch (e) { aviso(e.message, 'error'); }
  }
  return (
    <>
      <PaginaCab titulo="Usuarios" subtitulo="Personal con acceso al sistema">
        <button className="btn btn-pri" onClick={() => setForm('nuevo')}>+ Nuevo usuario</button>
      </PaginaCab>
      <div className="filtros"><BuscadorInput valor={lista.q} onCambio={lista.setQ} placeholder="Usuario o nombre" /></div>
      {error && <ErrorCaja mensaje={error} onReintentar={lista.recargar} />}
      {cargando && !datos ? <Cargando /> : datos && (
        <>
          <Tabla vacio="No hay usuarios." columnas={[
            { titulo: 'Usuario', ver: (u) => <strong>{u.username}</strong> }, { titulo: 'Nombre', ver: (u) => u.nombre }, { titulo: 'Rol', ver: (u) => ROLES[u.rol] },
            { titulo: 'Contacto', ver: (u) => (u.correo || u.telefono ? <>{u.correo}{u.correo && u.telefono && <br />}{u.telefono}</> : <small style={{ color: 'var(--gris-500)' }}>Sin correo ni celular</small>) },
            { titulo: 'Creado', ver: (u) => fechaHora(u.creadoEn) },
            { titulo: 'Estado', ver: (u) => <Insignia estado={u.activo ? 'ATENDIDA' : 'CANCELADA'} texto={u.activo ? 'Activo' : 'Inactivo'} /> },
            { titulo: '', clase: 'acc', ver: (u) => (
              <span className="pie-acciones">
                <button className="btn btn-sec btn-sm" onClick={() => setForm(u)}>Editar</button>
                <button className="btn btn-sec btn-sm" onClick={() => setClave(u)}>Nueva clave</button>
                {u.id !== yo.id && <button className="btn btn-sec btn-sm" onClick={() => alternar(u)}>{u.activo ? 'Desactivar' : 'Activar'}</button>}
              </span>) },
          ]} filas={datos.contenido} />
          <Paginacion datos={datos} onCambiar={lista.setPagina} />
        </>
      )}
      {form && <FormUsuario usuario={form === 'nuevo' ? null : form} onCerrar={() => setForm(null)} onGuardado={() => { setForm(null); lista.recargar(); aviso('Usuario guardado'); }} />}
      {clave && <FormClave usuario={clave} onCerrar={() => setClave(null)} onGuardado={() => { setClave(null); aviso('Contraseña actualizada'); }} />}
    </>
  );
}

const AYUDA_CLAVE = 'Mínimo 10 caracteres, con letras y números';

function FormUsuario({ usuario, onCerrar, onGuardado }) {
  const f = useFormulario({ username: usuario?.username || '', nombre: usuario?.nombre || '', rol: usuario?.rol || 'RECEPCION', password: '', correo: usuario?.correo || '', telefono: usuario?.telefono || '' }, async (v) => {
    const contacto = { correo: nulo(v.correo.trim()), telefono: nulo(v.telefono.trim()) };
    if (usuario) await api.put('/usuarios/' + usuario.id, { nombre: v.nombre.trim(), rol: v.rol, ...contacto });
    else await api.post('/usuarios', { username: v.username.trim(), nombre: v.nombre.trim(), rol: v.rol, password: v.password, ...contacto });
    onGuardado();
  });
  return (
    <Modal titulo={usuario ? 'Editar usuario' : 'Nuevo usuario'} onCerrar={onCerrar} ancho="480px">
      <form onSubmit={f.submit} noValidate>
        <Campo etiqueta="Usuario *" error={f.errores.username}><input value={f.valores.username} onChange={f.cambiar('username')} disabled={!!usuario} maxLength={40} autoComplete="off" /></Campo>
        <Campo etiqueta="Nombre completo *" error={f.errores.nombre}><input value={f.valores.nombre} onChange={f.cambiar('nombre')} maxLength={100} /></Campo>
        <Campo etiqueta="Rol *" error={f.errores.rol}><select value={f.valores.rol} onChange={f.cambiar('rol')}>{Object.entries(ROLES).map(([k, n]) => <option key={k} value={k}>{n}</option>)}</select></Campo>
        <div className="rejilla">
          <Campo etiqueta="Correo" error={f.errores.correo}><input type="email" value={f.valores.correo} onChange={f.cambiar('correo')} maxLength={100} autoComplete="off" /></Campo>
          <Campo etiqueta="Celular" error={f.errores.telefono}><input type="tel" value={f.valores.telefono} onChange={f.cambiar('telefono')} maxLength={20} placeholder="987654321" autoComplete="off" /></Campo>
        </div>
        <small className="campo-ayuda" style={{ display: 'block', margin: '-.4rem 0 .85rem' }}>Con el correo o el celular, la persona puede recuperar su contraseña si la olvida.</small>
        {!usuario && <Campo etiqueta="Contraseña inicial *" error={f.errores.password} ayuda={AYUDA_CLAVE}><input type="password" value={f.valores.password} onChange={f.cambiar('password')} autoComplete="new-password" /></Campo>}
        {f.errorGeneral && <div className="caja-error" role="alert">{f.errorGeneral}</div>}
        <div className="modal-pie"><button type="button" className="btn btn-sec" onClick={onCerrar}>Cancelar</button><button className="btn btn-pri" disabled={f.enviando}>Guardar</button></div>
      </form>
    </Modal>
  );
}

function FormClave({ usuario, onCerrar, onGuardado }) {
  const f = useFormulario({ nuevaPassword: '' }, async (v) => { await api.post(`/usuarios/${usuario.id}/password`, { nuevaPassword: v.nuevaPassword }); onGuardado(); });
  return (
    <Modal titulo={'Nueva contraseña · ' + usuario.username} onCerrar={onCerrar} ancho="440px">
      <form onSubmit={f.submit} noValidate>
        <Campo etiqueta="Nueva contraseña *" error={f.errores.nuevaPassword} ayuda={AYUDA_CLAVE}><input type="password" value={f.valores.nuevaPassword} onChange={f.cambiar('nuevaPassword')} autoComplete="new-password" /></Campo>
        {f.errorGeneral && <div className="caja-error" role="alert">{f.errorGeneral}</div>}
        <div className="modal-pie"><button type="button" className="btn btn-sec" onClick={onCerrar}>Cancelar</button><button className="btn btn-pri" disabled={f.enviando}>Cambiar</button></div>
      </form>
    </Modal>
  );
}
