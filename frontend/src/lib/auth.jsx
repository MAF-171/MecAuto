import { createContext, useContext, useEffect, useState, useCallback } from 'react';
import { api, borrarSesion, guardarSesion, leerSesion, onSesionExpirada } from './api';
import { navegar } from './router';

const Ctx = createContext(null);
export const useAuth = () => useContext(Ctx);

export function AuthProvider({ children }) {
  const [sesion, setSesion] = useState(leerSesion());

  const salir = useCallback(() => {
    borrarSesion();
    setSesion(null);
    navegar('/login');
  }, []);

  useEffect(() => { onSesionExpirada(() => { setSesion(null); navegar('/login'); }); }, []);

  async function entrar(username, password) {
    const r = await api.post('/auth/login', { username, password });
    const s = { token: r.token, usuario: r.usuario };
    guardarSesion(s);
    setSesion(s);
    return r.usuario;
  }

  const usuario = sesion?.usuario || null;
  const tieneRol = (...roles) => !!usuario && roles.includes(usuario.rol);
  return <Ctx.Provider value={{ usuario, entrar, salir, tieneRol }}>{children}</Ctx.Provider>;
}
