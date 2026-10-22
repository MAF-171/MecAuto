import { createContext, useCallback, useContext, useEffect, useRef, useState } from 'react';
import { etiqueta, TONO_ESTADO } from '../lib/format';

/* ------------------------------------------------------------ Avisos (toasts) */
const ToastCtx = createContext(() => {});
export const useAviso = () => useContext(ToastCtx);

export function AvisosProvider({ children }) {
  const [items, setItems] = useState([]);
  const aviso = useCallback((mensaje, tipo = 'ok') => {
    const id = Math.random().toString(36).slice(2);
    setItems((x) => [...x, { id, mensaje, tipo }]);
    setTimeout(() => setItems((x) => x.filter((i) => i.id !== id)), tipo === 'error' ? 6000 : 3500);
  }, []);
  return (
    <ToastCtx.Provider value={aviso}>
      {children}
      <div className="avisos" role="status" aria-live="polite">
        {items.map((i) => <div key={i.id} className={'aviso aviso-' + i.tipo}>{i.mensaje}</div>)}
      </div>
    </ToastCtx.Provider>
  );
}

/* ------------------------------------------------------------ Piezas básicas */
export const Insignia = ({ estado, texto }) => (
  <span className={'insignia insignia-' + (TONO_ESTADO[estado] || 'gris')}>{texto || etiqueta(estado)}</span>
);

export const Cargando = ({ texto = 'Cargando…' }) => <div className="estado-vacio" role="status">{texto}</div>;
export const ErrorCaja = ({ mensaje, onReintentar }) => (
  <div className="caja-error" role="alert">
    {mensaje}
    {onReintentar && <button className="btn btn-sec btn-sm" onClick={onReintentar}>Reintentar</button>}
  </div>
);
export const Vacio = ({ children }) => <div className="estado-vacio">{children}</div>;

export function Campo({ etiqueta: et, error, ayuda, children }) {
  return (
    <label className="campo">
      <span className="campo-et">{et}</span>
      {children}
      {ayuda && !error && <small className="campo-ayuda">{ayuda}</small>}
      {error && <small className="campo-error">{error}</small>}
    </label>
  );
}

export function PaginaCab({ titulo, subtitulo, children }) {
  return (
    <div className="pagina-cab">
      <div>
        <h1>{titulo}</h1>
        {subtitulo && <p className="sub">{subtitulo}</p>}
      </div>
      <div className="acciones">{children}</div>
    </div>
  );
}

/* ------------------------------------------------------------ Modal accesible */
export function Modal({ titulo, onCerrar, children, ancho }) {
  const ref = useRef(null);
  useEffect(() => {
    const previo = document.activeElement;
    const f = (e) => { if (e.key === 'Escape') onCerrar(); };
    document.addEventListener('keydown', f);
    ref.current?.querySelector('input,select,textarea,button')?.focus();
    return () => { document.removeEventListener('keydown', f); previo?.focus?.(); };
  }, [onCerrar]);
  return (
    <div className="modal-fondo" onMouseDown={(e) => { if (e.target === e.currentTarget) onCerrar(); }}>
      <div className="modal" role="dialog" aria-modal="true" aria-label={titulo} ref={ref} style={ancho ? { maxWidth: ancho } : null}>
        <header className="modal-cab">
          <h2>{titulo}</h2>
          <button className="btn-icono" aria-label="Cerrar" onClick={onCerrar}>×</button>
        </header>
        <div className="modal-cuerpo">{children}</div>
      </div>
    </div>
  );
}

/* ------------------------------------------------------------ Confirmación */
export function Confirmar({ titulo, mensaje, textoOk = 'Confirmar', peligro, onOk, onCancelar, pedirMotivo, etiquetaMotivo }) {
  const [motivo, setMotivo] = useState('');
  const [trabajando, setTrabajando] = useState(false);
  const [error, setError] = useState('');
  async function ok() {
    if (pedirMotivo && !motivo.trim()) { setError('Escribe el motivo'); return; }
    setTrabajando(true);
    try { await onOk(motivo.trim()); } catch (e) { setError(e.message); setTrabajando(false); }
  }
  return (
    <Modal titulo={titulo} onCerrar={onCancelar} ancho="440px">
      <p>{mensaje}</p>
      {pedirMotivo && (
        <Campo etiqueta={etiquetaMotivo || 'Motivo'} error={error}>
          <textarea rows="3" value={motivo} onChange={(e) => setMotivo(e.target.value)} maxLength={200} />
        </Campo>
      )}
      {!pedirMotivo && error && <div className="caja-error">{error}</div>}
      <div className="modal-pie">
        <button className="btn btn-sec" onClick={onCancelar} disabled={trabajando}>Cancelar</button>
        <button className={'btn ' + (peligro ? 'btn-peligro' : 'btn-pri')} onClick={ok} disabled={trabajando}>
          {trabajando ? 'Procesando…' : textoOk}
        </button>
      </div>
    </Modal>
  );
}

/* ------------------------------------------------------------ Tabla y paginación */
export function Tabla({ columnas, filas, vacio = 'No hay registros para mostrar.', clave = (f) => f.id, onFila }) {
  if (!filas || filas.length === 0) return <Vacio>{vacio}</Vacio>;
  return (
    <div className="tabla-wrap">
      <table className="tabla">
        <thead><tr>{columnas.map((c) => <th key={c.titulo} className={c.clase}>{c.titulo}</th>)}</tr></thead>
        <tbody>
          {filas.map((f) => (
            <tr key={clave(f)} className={onFila ? 'fila-click' : ''} onClick={onFila ? () => onFila(f) : undefined}>
              {columnas.map((c) => <td key={c.titulo} className={c.clase} data-et={c.titulo}>{c.ver(f)}</td>)}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export function Paginacion({ datos, onCambiar }) {
  if (!datos || datos.totalPaginas <= 1) {
    return datos ? <div className="paginacion"><span>{datos.totalElementos} registro(s)</span></div> : null;
  }
  return (
    <div className="paginacion">
      <span>{datos.totalElementos} registro(s) · página {datos.pagina + 1} de {datos.totalPaginas}</span>
      <div>
        <button className="btn btn-sec btn-sm" disabled={datos.pagina === 0} onClick={() => onCambiar(datos.pagina - 1)}>Anterior</button>
        <button className="btn btn-sec btn-sm" disabled={datos.pagina + 1 >= datos.totalPaginas} onClick={() => onCambiar(datos.pagina + 1)}>Siguiente</button>
      </div>
    </div>
  );
}

/* ------------------------------------------------------------ Logo */
/** Logo: «MECAUT» + una llanta roja como última letra. La imagen está en public/img/rueda.png. */
export function Logo() {
  return (
    <span className="marca" role="img" aria-label="MecAuto">
      <span className="marca-txt" aria-hidden="true">Mec<b>Aut</b></span>
      <img className="marca-rueda" src="/img/rueda.png" alt="" aria-hidden="true" width="34" height="34" />
    </span>
  );
}

/* ------------------------------------------------------------ WhatsApp flotante */
/** Botón fijo de WhatsApp. Solo aparece si el taller configuró su número (clave «whatsapp»). */
export function WhatsAppFlotante({ numero }) {
  const d = String(numero || '').replace(/\D/g, '');
  if (d.length < 9) return null;
  const tel = d.length === 9 ? '51' + d : d; // 9 dígitos = celular de Perú sin código de país
  return (
    <a className="wa-flotante" href={`https://wa.me/${tel}`} target="_blank" rel="noopener noreferrer" aria-label="Escribir por WhatsApp">
      <svg width="30" height="30" viewBox="0 0 24 24" aria-hidden="true">
        <path d="M12 2.5a9.5 9.5 0 0 0-8.1 14.4L2.5 21.5l4.7-1.3A9.5 9.5 0 1 0 12 2.5z" fill="#fff" />
        <path d="M8.6 7.8c0 3.8 3.1 6.9 6.9 6.9l1-1.7-2.1-1.1-1 .9a4.3 4.3 0 0 1-2.3-2.3l.9-1-1.1-2.1z" fill="#25D366" />
      </svg>
    </a>
  );
}

/* ------------------------------------------------------------ Formularios */
/** Hook sencillo de formulario: valores, errores por campo (del servidor) y envío. */
export function useFormulario(inicial, enviar) {
  const [valores, setValores] = useState(inicial);
  const [errores, setErrores] = useState({});
  const [errorGeneral, setErrorGeneral] = useState('');
  const [enviando, setEnviando] = useState(false);
  const cambiar = (campo) => (e) => {
    const v = e.target.type === 'checkbox' ? e.target.checked : e.target.value;
    setValores((x) => ({ ...x, [campo]: v }));
  };
  async function submit(e) {
    e?.preventDefault();
    setEnviando(true); setErrores({}); setErrorGeneral('');
    try { await enviar(valores); }
    catch (err) {
      setErrores(err.campos || {});
      setErrorGeneral(err.message);
    } finally { setEnviando(false); }
  }
  return { valores, setValores, errores, errorGeneral, enviando, cambiar, submit };
}

export const nulo = (v) => (v === '' || v === undefined ? null : v);
export const num = (v) => (v === '' || v === undefined || v === null ? null : Number(v));
