import { useEffect, useState } from 'react';
import { api } from '../../lib/api';
import { useAuth } from '../../lib/auth.jsx';
import { useCarga } from '../../lib/hooks';
import { Campo, PaginaCab, Cargando, ErrorCaja, useAviso } from '../../components/ui.jsx';

const GRUPOS = [
  ['Datos del taller (se muestran en el sitio y en los comprobantes)', [
    ['nombre_taller', 'Nombre del taller'], ['ruc', 'RUC'], ['direccion', 'Dirección'], ['telefono', 'Teléfono'], ['whatsapp', 'WhatsApp'], ['correo', 'Correo'], ['horario_texto', 'Horario (texto para el sitio)']]],
  ['Citas', [['hora_apertura', 'Hora de apertura (HH:mm)'], ['hora_cierre', 'Hora de cierre (HH:mm)'], ['duracion_franja_min', 'Duración de cada franja (min)'],
    ['citas_por_franja', 'Citas simultáneas por franja'], ['dias_laborables', 'Días laborables (1=lunes … 7=domingo, separados por coma)']]],
  ['Comprobantes internos', [['serie_boleta', 'Serie de boletas (ej. B001)'], ['serie_factura', 'Serie de facturas (ej. F001)']]],
];

export default function Configuracion() {
  const { tieneRol } = useAuth();
  const admin = tieneRol('ADMINISTRADOR');
  const aviso = useAviso();
  const { datos, cargando, error, recargar } = useCarga('/configuracion');
  const [v, setV] = useState({});
  const [guardando, setGuardando] = useState(false);
  const [err, setErr] = useState('');
  useEffect(() => { if (datos) setV(datos); }, [datos]);
  async function guardar(e) {
    e.preventDefault(); setGuardando(true); setErr('');
    try { const r = await api.put('/configuracion', v); setV(r); aviso('Configuración guardada'); } catch (x) { setErr(x.message); } finally { setGuardando(false); }
  }
  if (cargando && !datos) return <Cargando />;
  if (error) return <ErrorCaja mensaje={error} onReintentar={recargar} />;
  return (
    <>
      <PaginaCab titulo="Configuración" subtitulo={admin ? 'Datos del taller y reglas de citas y comprobantes' : 'Solo el administrador puede modificar la configuración'} />
      <form onSubmit={guardar}>
        {GRUPOS.map(([titulo, campos]) => (
          <section className="tarjeta" key={titulo}>
            <h2>{titulo}</h2>
            <div className="rejilla">
              {campos.map(([k, et]) => (
                <Campo etiqueta={et} key={k}><input value={v[k] ?? ''} onChange={(e) => setV({ ...v, [k]: e.target.value })} disabled={!admin} /></Campo>
              ))}
            </div>
          </section>
        ))}
        {err && <div className="caja-error" role="alert">{err}</div>}
        {admin && <button className="btn btn-pri" disabled={guardando}>{guardando ? 'Guardando…' : 'Guardar configuración'}</button>}
      </form>
    </>
  );
}
