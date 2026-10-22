import { useState } from 'react';
import { useAuth } from '../../lib/auth.jsx';
import { Link } from '../../lib/router.jsx';
import { Logo } from '../../components/ui.jsx';
import { ROLES } from '../../lib/format';
import { RUTAS_PANEL } from './rutas.jsx';

export default function PanelLayout({ path, children }) {
  const { usuario, salir } = useAuth();
  const [abierto, setAbierto] = useState(false);
  const menu = RUTAS_PANEL.filter((r) => r.menu && (!r.roles || r.roles.includes(usuario.rol)));
  const activo = (r) => (r.path === '/panel' ? path === '/panel' : path.startsWith(r.path.split('/:')[0]));
  return (
    <div className="panel">
      {abierto && <div className="velo" onClick={() => setAbierto(false)} />}
      <aside className={'lateral' + (abierto ? ' abierto' : '')}>
        <Logo />
        <nav aria-label="Panel">
          {menu.map((r) => (
            <Link key={r.path} to={r.path} className={activo(r) ? 'activo' : ''} onClick={() => setAbierto(false)}>{r.menu}</Link>
          ))}
        </nav>
        <div className="usuario-caja">
          <strong>{usuario.nombre}</strong>
          <span>{ROLES[usuario.rol]}</span>
          <div style={{ marginTop: '.6rem', display: 'flex', gap: '.4rem' }}>
            <Link to="/" className="btn btn-sec btn-sm">Sitio</Link>
            <button className="btn btn-sec btn-sm" onClick={salir}>Salir</button>
          </div>
        </div>
      </aside>
      <div className="contenido-panel">
        <div className="barra">
          <button aria-label="Abrir menú" onClick={() => setAbierto(true)}>☰</button>
          <Logo />
        </div>
        <main className="principal">{children}</main>
      </div>
    </div>
  );
}
