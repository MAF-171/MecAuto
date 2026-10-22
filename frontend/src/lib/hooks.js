import { useCallback, useEffect, useRef, useState } from 'react';
import { api } from './api';

/** Carga un recurso y expone {datos, cargando, error, recargar}. */
export function useCarga(ruta, params, activo = true) {
  const [datos, setDatos] = useState(null);
  const [cargando, setCargando] = useState(activo);
  const [error, setError] = useState('');
  const clave = ruta + JSON.stringify(params || {});
  const ultima = useRef(0);

  const recargar = useCallback(async () => {
    if (!activo) return;
    const id = ++ultima.current;
    setCargando(true);
    setError('');
    try {
      const d = await api.get(ruta, params);
      if (id === ultima.current) setDatos(d);
    } catch (e) {
      if (id === ultima.current) setError(e.message);
    } finally {
      if (id === ultima.current) setCargando(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [clave, activo]);

  useEffect(() => { recargar(); }, [recargar]);
  return { datos, cargando, error, recargar, setDatos };
}

/** Retrasa un valor (para no consultar en cada tecla). */
export function useRetraso(valor, ms = 350) {
  const [v, setV] = useState(valor);
  useEffect(() => { const t = setTimeout(() => setV(valor), ms); return () => clearTimeout(t); }, [valor, ms]);
  return v;
}
