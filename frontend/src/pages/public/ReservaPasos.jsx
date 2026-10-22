import { useEffect, useMemo, useState } from 'react';
import { api } from '../../lib/api';
import { soles, hoyISO } from '../../lib/format';
import { Campo, useFormulario, nulo, num } from '../../components/ui.jsx';
import { categoriaDe, IconoCategoria } from '../../lib/servicios.jsx';

const PASOS = ['Servicio', 'Día y hora', 'Tus datos', 'Confirmar'];
const DIAS_VISTA = 8; // hoy + 7 días
const RE_TEL = /^[0-9+()\- ]{6,20}$/;
const RE_CORREO = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const INICIAL = { nombre: '', telefono: '', correo: '', placa: '', marca: '', modelo: '', motivo: '', sitioWeb: '' };

function sumarDias(iso, n) {
  const d = new Date(iso + 'T12:00:00');
  d.setDate(d.getDate() + n);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}
function etiquetaDia(iso) {
  const d = new Date(iso + 'T12:00:00');
  return {
    sem: d.toLocaleDateString('es-PE', { weekday: 'short' }).replace('.', ''),
    num: d.getDate(),
    mes: d.toLocaleDateString('es-PE', { month: 'short' }).replace('.', ''),
  };
}
const fechaLarga = (iso) => new Date(iso + 'T12:00:00').toLocaleDateString('es-PE', { weekday: 'long', day: 'numeric', month: 'long' });
const hhmm = (h) => String(h).slice(0, 5);

/**
 * Reserva de cita en 4 pasos: servicio → día y hora → tus datos → confirmar.
 * Usa los mismos endpoints públicos de siempre (/public/citas/disponibilidad y /public/citas).
 */
export default function ReservaPasos({ servicios, servicioElegido, horarioTexto }) {
  const [paso, setPaso] = useState(1);
  const [servicioId, setServicioId] = useState('');
  const [fecha, setFecha] = useState('');
  const [hora, setHora] = useState('');
  const [disp, setDisp] = useState({});
  const [version, setVersion] = useState(0);
  const [exito, setExito] = useState(null);
  const [intento, setIntento] = useState(false);
  const [verError, setVerError] = useState(false);

  const hoy = useMemo(() => hoyISO(), []);
  const dias = useMemo(() => Array.from({ length: DIAS_VISTA }, (_, i) => sumarDias(hoy, i)), [hoy]);

  // Disponibilidad de los próximos días (se vuelve a pedir si el horario elegido ya no estaba libre).
  useEffect(() => {
    let vivo = true;
    Promise.all(dias.map((d) => api.get('/public/citas/disponibilidad', { fecha: d }).then((r) => [d, r]).catch(() => [d, null])))
      .then((pares) => { if (vivo) setDisp(Object.fromEntries(pares)); });
    return () => { vivo = false; };
  }, [dias, version]);

  const ahora = new Date().toTimeString().slice(0, 5);
  const horaLibre = (d, fr) => fr.libres > 0 && (d !== hoy || hhmm(fr.hora) > ahora);
  const estadoDia = (d) => {
    const x = disp[d];
    if (x === undefined) return 'cargando';
    if (x === null) return 'abierto'; // no se pudo consultar: dejamos intentar
    if (!x.laborable) return 'cerrado';
    return x.franjas.some((fr) => horaLibre(d, fr)) ? 'abierto' : 'lleno';
  };

  const f = useFormulario(INICIAL, async (v) => {
    const hh = hora.length === 5 ? hora + ':00' : hora;
    const r = await api.post('/public/citas', {
      nombre: v.nombre.trim(), telefono: v.telefono.trim(), correo: nulo(v.correo.trim()),
      placa: v.placa.trim(), marca: nulo(v.marca.trim()), modelo: nulo(v.modelo.trim()),
      servicioId: num(servicioId), fechaHora: `${fecha}T${hh}`, motivo: nulo(v.motivo.trim()), sitioWeb: v.sitioWeb,
    });
    setExito(r);
  });
  const { setValores } = f;

  // «Reservar» en una tarjeta de servicio: se salta directo al paso 2 con ese servicio elegido.
  useEffect(() => {
    if (servicioElegido) { setServicioId(String(servicioElegido.id)); setPaso(2); setVerError(false); }
  }, [servicioElegido]);

  // Si el servidor rechaza algo, volvemos al paso donde se corrige.
  useEffect(() => {
    const campos = Object.keys(f.errores || {});
    if (campos.length === 0 && !f.errorGeneral) return;
    setVerError(true);
    if (campos.includes('servicioId')) setPaso(1);
    else if (campos.includes('fechaHora')) { setPaso(2); setHora(''); setVersion((x) => x + 1); }
    else if (campos.length) setPaso(3);
    else if (/horario|cupo|disponib|hora/i.test(f.errorGeneral || '')) { setPaso(2); setHora(''); setVersion((x) => x + 1); }
  }, [f.errores, f.errorGeneral]);

  const v = f.valores;
  const err3 = {
    nombre: v.nombre.trim() ? '' : 'Escribe tu nombre',
    telefono: RE_TEL.test(v.telefono.trim()) ? '' : 'Teléfono inválido (solo números, espacios, + o guiones)',
    placa: v.placa.trim() ? '' : 'Escribe la placa de tu vehículo',
    correo: !v.correo.trim() || RE_CORREO.test(v.correo.trim()) ? '' : 'Correo inválido',
  };
  const ok3 = !Object.values(err3).some(Boolean);
  const puede = [false, !!servicioId, !!(fecha && hora), ok3, true][paso];

  const ir = (n) => { setVerError(false); setIntento(false); setPaso(n); };
  const siguiente = () => {
    if (paso === 3 && !ok3) { setIntento(true); return; }
    if (puede && paso < 4) ir(paso + 1);
  };
  const enviar = (e) => {
    e.preventDefault();
    if (paso === 4) f.submit(e); else siguiente();
  };
  const reiniciar = () => {
    setExito(null); setPaso(1); setServicioId(''); setFecha(''); setHora(''); setIntento(false); setVerError(false);
    setValores(INICIAL); setVersion((x) => x + 1);
  };

  const servicio = servicios.find((s) => String(s.id) === servicioId);
  const franjas = (fecha && disp[fecha] && disp[fecha].franjas) || [];
  const mostrarErr = (campo) => (intento && err3[campo]) || f.errores[campo];

  if (exito) {
    return (
      <div className="rp rp-exito" role="status">
        <span className="rp-check" aria-hidden="true">✓</span>
        <h3>¡Solicitud registrada!</h3>
        <p>{exito.mensaje}</p>
        <p>Código de tu cita: <strong className="rp-codigo">{exito.codigo}</strong></p>
        <button type="button" className="btn btn-sec" onClick={reiniciar}>Registrar otra solicitud</button>
      </div>
    );
  }

  return (
    <form className="rp" onSubmit={enviar} noValidate>
      <ol className="rp-pasos" aria-label="Progreso de la reserva">
        {PASOS.map((n, i) => {
          const k = i + 1;
          return (
            <li key={n} className={k < paso ? 'hecho' : k === paso ? 'actual' : ''}>
              <button type="button" disabled={k > paso} onClick={() => ir(k)} aria-current={k === paso ? 'step' : undefined}>
                <span className="rp-num">{k < paso ? '✓' : k}</span><span className="rp-et">{n}</span>
              </button>
            </li>
          );
        })}
      </ol>
      <div className="rp-barra" aria-hidden="true"><i style={{ width: `${((paso - 1) / 3) * 100}%` }} /></div>

      <div className="rp-panel" key={paso}>
        {verError && f.errorGeneral && <div className="caja-error" role="alert">{f.errorGeneral}</div>}

        {paso === 1 && (
          <>
            <h3>¿Qué servicio necesitas?</h3>
            <p className="rp-sub">Elige uno. Si no estás seguro, el taller te orienta con el diagnóstico.</p>
            {servicios.length === 0 && <div className="estado-vacio">Pronto publicaremos nuestro catálogo de servicios.</div>}
            <div className="rp-servicios">
              {servicios.map((s) => (
                <button type="button" key={s.id} className="rp-serv" aria-pressed={servicioId === String(s.id)} onClick={() => setServicioId(String(s.id))}>
                  <span className="rp-serv-ico"><IconoCategoria categoria={categoriaDe(s.nombre)} /></span>
                  <span className="rp-serv-txt">
                    <strong>{s.nombre}</strong>
                    <small>{s.precioReferencial != null ? 'Desde ' + soles(s.precioReferencial) : 'Según diagnóstico'}</small>
                  </span>
                </button>
              ))}
            </div>
          </>
        )}

        {paso === 2 && (
          <>
            <h3>¿Qué día te conviene?</h3>
            <p className="rp-sub">{horarioTexto ? 'Horario: ' + horarioTexto + '.' : 'Elige un día y una hora disponibles.'}</p>
            <div className="rp-dias">
              {dias.map((d) => {
                const e = estadoDia(d);
                const l = etiquetaDia(d);
                return (
                  <button type="button" key={d} className={'rp-dia ' + e} disabled={e !== 'abierto'} aria-pressed={fecha === d}
                    onClick={() => { setFecha(d); setHora(''); }}>
                    <small>{l.sem}</small>
                    <b>{l.num}</b>
                    <small>{e === 'abierto' ? l.mes : e === 'cerrado' ? 'Cerrado' : e === 'lleno' ? 'Lleno' : '…'}</small>
                  </button>
                );
              })}
            </div>
            {fecha && (
              <div className="rp-horas">
                <span className="campo-et">Hora disponible</span>
                <div className="franjas" role="group" aria-label="Horarios disponibles">
                  {franjas.map((fr) => (
                    <button type="button" key={fr.hora} className="franja" aria-pressed={hora === fr.hora}
                      disabled={!horaLibre(fecha, fr)} onClick={() => setHora(fr.hora)}>{hhmm(fr.hora)}</button>
                  ))}
                </div>
              </div>
            )}
          </>
        )}

        {paso === 3 && (
          <>
            <h3>Tus datos</h3>
            <p className="rp-sub">Los usamos solo para confirmar tu cita.</p>
            <div className="rejilla">
              <Campo etiqueta="Nombre completo *" error={mostrarErr('nombre')}><input value={v.nombre} onChange={f.cambiar('nombre')} maxLength={100} autoComplete="name" /></Campo>
              <Campo etiqueta="Teléfono *" error={mostrarErr('telefono')}><input value={v.telefono} onChange={f.cambiar('telefono')} maxLength={20} inputMode="tel" autoComplete="tel" /></Campo>
              <Campo etiqueta="Correo (opcional)" error={mostrarErr('correo')}><input type="email" value={v.correo} onChange={f.cambiar('correo')} maxLength={100} autoComplete="email" /></Campo>
              <Campo etiqueta="Placa del vehículo *" error={mostrarErr('placa')}><input value={v.placa} onChange={f.cambiar('placa')} maxLength={10} placeholder="ABC-123" /></Campo>
              <Campo etiqueta="Marca" error={f.errores.marca}><input value={v.marca} onChange={f.cambiar('marca')} maxLength={40} /></Campo>
              <Campo etiqueta="Modelo" error={f.errores.modelo}><input value={v.modelo} onChange={f.cambiar('modelo')} maxLength={40} /></Campo>
              <div className="ancho">
                <Campo etiqueta="¿Qué le sucede a tu vehículo?" error={f.errores.motivo}>
                  <textarea rows="3" value={v.motivo} onChange={f.cambiar('motivo')} maxLength={300} />
                </Campo>
              </div>
              {/* Campo trampa anti-spam: las personas no lo ven; los bots suelen llenarlo. */}
              <div className="trampa" aria-hidden="true">
                <label>No llenar<input tabIndex={-1} autoComplete="off" value={v.sitioWeb} onChange={f.cambiar('sitioWeb')} /></label>
              </div>
            </div>
          </>
        )}

        {paso === 4 && (
          <>
            <h3>Revisa y confirma</h3>
            <p className="rp-sub">Tu solicitud queda registrada y el taller la confirmará.</p>
            <dl className="rp-resumen">
              <div><dt>Servicio</dt><dd>{servicio ? servicio.nombre : '—'}</dd></div>
              <div><dt>Día y hora</dt><dd>{fecha ? fechaLarga(fecha) : '—'} · {hora ? hhmm(hora) : '—'}</dd></div>
              <div><dt>Vehículo</dt><dd>{[v.placa.trim().toUpperCase(), v.marca.trim(), v.modelo.trim()].filter(Boolean).join(' · ') || '—'}</dd></div>
              <div><dt>Contacto</dt><dd>{v.nombre.trim()} · {v.telefono.trim()}{v.correo.trim() ? ' · ' + v.correo.trim() : ''}</dd></div>
              {v.motivo.trim() && <div className="ancho"><dt>Motivo</dt><dd>{v.motivo.trim()}</dd></div>}
            </dl>
          </>
        )}
      </div>

      <div className="rp-acciones">
        {paso > 1 ? <button type="button" className="btn btn-sec" onClick={() => ir(paso - 1)}>Atrás</button> : <span />}
        <button type="submit" className="btn btn-pri btn-lg" disabled={(paso < 3 && !puede) || f.enviando}>
          {paso === 4 ? (f.enviando ? 'Enviando…' : 'Enviar solicitud') : 'Continuar'}
        </button>
      </div>
    </form>
  );
}
