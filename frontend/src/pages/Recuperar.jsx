import { useState } from 'react';
import { api } from '../lib/api';
import { Link } from '../lib/router.jsx';
import { Campo, Logo, useFormulario } from '../components/ui.jsx';

const AYUDA_CLAVE = 'Mínimo 10 caracteres, con letras y números';

// Recuperación de contraseña del personal: 1) correo o celular → 2) código + nueva contraseña → 3) listo.
export default function Recuperar() {
  const [paso, setPaso] = useState(1);
  const [identificador, setIdentificador] = useState('');
  return (
    <div className="login-pagina">
      <div className="login-caja">
        <Logo />
        <h1>Recuperar contraseña</h1>
        <div className="login-pasos" aria-hidden="true">{[1, 2, 3].map((n) => <span key={n} className={n <= paso ? 'activo' : ''} />)}</div>
        {paso === 1 && <PasoSolicitar inicial={identificador} onEnviado={(id) => { setIdentificador(id); setPaso(2); }} />}
        {paso === 2 && <PasoCodigo identificador={identificador} onCambiarDato={() => setPaso(1)} onListo={() => setPaso(3)} />}
        {paso === 3 && (
          <>
            <div className="login-ok" role="status">Tu contraseña se cambió correctamente. Ya puedes ingresar con la nueva.</div>
            <Link to="/login" className="btn btn-pri" style={{ width: '100%' }}>Ir a iniciar sesión</Link>
          </>
        )}
        {paso !== 3 && <Link to="/login" className="login-volver">← Volver al inicio de sesión</Link>}
      </div>
    </div>
  );
}

function PasoSolicitar({ inicial, onEnviado }) {
  const f = useFormulario({ identificador: inicial }, async (v) => {
    await api.post('/auth/recuperar/solicitar', { identificador: v.identificador.trim() });
    onEnviado(v.identificador.trim());
  });
  return (
    <form onSubmit={f.submit} noValidate>
      <p className="sub" style={{ color: 'var(--gris-500)' }}>Escribe el correo o el número de celular registrado en tu usuario. Te enviaremos un código de 6 dígitos.</p>
      <Campo etiqueta="Correo o celular" error={f.errores.identificador}>
        <input value={f.valores.identificador} onChange={f.cambiar('identificador')} autoComplete="email" placeholder="tucorreo@ejemplo.com o 987654321" autoFocus />
      </Campo>
      {f.errorGeneral && !f.errores.identificador && <div className="caja-error" role="alert">{f.errorGeneral}</div>}
      <button className="btn btn-pri" style={{ width: '100%' }} disabled={f.enviando || !f.valores.identificador.trim()}>
        {f.enviando ? 'Enviando…' : 'Enviar código'}
      </button>
    </form>
  );
}

function PasoCodigo({ identificador, onCambiarDato, onListo }) {
  const [reenviado, setReenviado] = useState('');
  const f = useFormulario({ codigo: '', nuevaPassword: '', confirmar: '' }, async (v) => {
    if (v.nuevaPassword !== v.confirmar) {
      throw Object.assign(new Error('Las contraseñas no coinciden'), { campos: { confirmar: 'Las contraseñas no coinciden' } });
    }
    await api.post('/auth/recuperar/restablecer', { identificador, codigo: v.codigo.trim(), nuevaPassword: v.nuevaPassword });
    onListo();
  });
  async function reenviar() {
    try { await api.post('/auth/recuperar/solicitar', { identificador }); setReenviado('Te enviamos un código nuevo.'); }
    catch (e) { setReenviado(e.message); }
  }
  return (
    <form onSubmit={f.submit} noValidate>
      <p className="sub" style={{ color: 'var(--gris-500)' }}>
        Si <strong>{identificador}</strong> está registrado, te llegó un código. Vence en 15 minutos.{' '}
        <a href="#/recuperar" onClick={(e) => { e.preventDefault(); onCambiarDato(); }}>Cambiar dato</a>
      </p>
      <Campo etiqueta="Código de 6 dígitos" error={f.errores.codigo}>
        <input value={f.valores.codigo} onChange={(e) => f.setValores((x) => ({ ...x, codigo: e.target.value.replace(/\D/g, '').slice(0, 6) }))}
          inputMode="numeric" autoComplete="one-time-code" placeholder="000000" autoFocus />
      </Campo>
      <Campo etiqueta="Nueva contraseña" error={f.errores.nuevaPassword} ayuda={AYUDA_CLAVE}>
        <input type="password" value={f.valores.nuevaPassword} onChange={f.cambiar('nuevaPassword')} autoComplete="new-password" />
      </Campo>
      <Campo etiqueta="Repite la nueva contraseña" error={f.errores.confirmar}>
        <input type="password" value={f.valores.confirmar} onChange={f.cambiar('confirmar')} autoComplete="new-password" />
      </Campo>
      {f.errorGeneral && Object.keys(f.errores).length === 0 && <div className="caja-error" role="alert">{f.errorGeneral}</div>}
      <button className="btn btn-pri" style={{ width: '100%' }} disabled={f.enviando || f.valores.codigo.length !== 6 || !f.valores.nuevaPassword || !f.valores.confirmar}>
        {f.enviando ? 'Guardando…' : 'Cambiar contraseña'}
      </button>
      <p className="login-olvido" style={{ textAlign: 'center', margin: '.8rem 0 0' }}>
        <a href="#/recuperar" onClick={(e) => { e.preventDefault(); reenviar(); }}>¿No te llegó? Reenviar código</a>
        {reenviado && <><br /><small style={{ color: 'var(--gris-500)' }}>{reenviado}</small></>}
      </p>
    </form>
  );
}
