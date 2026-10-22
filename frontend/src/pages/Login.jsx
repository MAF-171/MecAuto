import { useAuth } from '../lib/auth.jsx';
import { navegar, Link } from '../lib/router.jsx';
import { Campo, Logo, useFormulario } from '../components/ui.jsx';

export default function Login() {
  const { entrar } = useAuth();
  const f = useFormulario({ username: '', password: '' }, async (v) => {
    await entrar(v.username.trim(), v.password);
    navegar('/panel');
  });
  return (
    <div className="login-pagina">
      <form className="login-caja" onSubmit={f.submit} noValidate>
        <Logo />
        <h1>Acceso del personal</h1>
        <p className="sub" style={{ color: 'var(--gris-500)' }}>Ingresa con tu usuario del taller.</p>
        <Campo etiqueta="Usuario" error={f.errores.username}>
          <input value={f.valores.username} onChange={f.cambiar('username')} autoComplete="username" autoFocus required />
        </Campo>
        <Campo etiqueta="Contraseña" error={f.errores.password}>
          <input type="password" value={f.valores.password} onChange={f.cambiar('password')} autoComplete="current-password" required />
        </Campo>
        {f.errorGeneral && <div className="caja-error" role="alert">{f.errorGeneral}</div>}
        <button className="btn btn-pri" style={{ width: '100%' }} disabled={f.enviando || !f.valores.username || !f.valores.password}>
          {f.enviando ? 'Ingresando…' : 'Ingresar'}
        </button>
        <p style={{ textAlign: 'center', marginBottom: 0 }}><Link to="/">← Volver al sitio</Link></p>
      </form>
    </div>
  );
}
