import { useRuta, coincide, navegar } from './lib/router.jsx';
import { useAuth } from './lib/auth.jsx';
import Inicio from './pages/public/Inicio.jsx';
import Login from './pages/Login.jsx';
import PanelLayout from './pages/panel/PanelLayout.jsx';
import { RUTAS_PANEL } from './pages/panel/rutas.jsx';
import { useEffect } from 'react';

export default function App() {
  const { path } = useRuta();
  const { usuario } = useAuth();
  const enPanel = path.startsWith('/panel');

  useEffect(() => {
    if (enPanel && !usuario) navegar('/login');
  }, [enPanel, usuario]);

  if (path === '/login') return usuario ? <Redirigir a="/panel" /> : <Login />;
  if (enPanel) {
    if (!usuario) return null;
    let pagina = null;
    for (const r of RUTAS_PANEL) {
      const p = coincide(r.path, path);
      if (p) {
        pagina = r.roles && !r.roles.includes(usuario.rol)
          ? <div className="caja-error">No tienes permiso para ver esta sección.</div>
          : <r.componente params={p} />;
        break;
      }
    }
    return <PanelLayout path={path}>{pagina || <div className="caja-error">Página no encontrada.</div>}</PanelLayout>;
  }
  return <Inicio />;
}

function Redirigir({ a }) {
  useEffect(() => { navegar(a); }, [a]);
  return null;
}
