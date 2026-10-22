import { useEffect, useMemo, useState } from 'react';
import { api } from '../../lib/api';
import { soles, hoyISO } from '../../lib/format';
import { Logo, WhatsAppFlotante } from '../../components/ui.jsx';
import { Link } from '../../lib/router.jsx';
import { CATEGORIAS, categoriaDe, IconoCategoria } from '../../lib/servicios.jsx';
import { conReferencia } from '../../lib/referencia.js';
import ReservaPasos from './ReservaPasos.jsx';
import ContactoSeccion from './ContactoSeccion.jsx';
import SeguimientoOrden from './SeguimientoOrden.jsx';

// Las fotos de /public/img son de ejemplo: se reemplazan copiando otro archivo con el mismo nombre.
const PASOS = [
  ['Solicita tu cita', 'Elige el servicio, el día y la hora que más te convenga.', '/img/auto-1.jpg'],
  ['Recepción del vehículo', 'Registramos tu vehículo y la falla que reportas.', '/img/auto-2.jpg'],
  ['Diagnóstico y presupuesto', 'Revisamos el vehículo y te informamos antes de reparar.', '/img/auto-3.jpg'],
  ['Reparación y entrega', 'Realizamos el trabajo aprobado y te avisamos para el recojo.', '/img/auto-4.jpg'],
];

/** Hace aparecer con suavidad las secciones marcadas con la clase «revelar» cuando entran en pantalla. */
function useRevelar() {
  useEffect(() => {
    const els = document.querySelectorAll('.revelar');
    if (!('IntersectionObserver' in window)) { els.forEach((e) => e.classList.add('visto')); return undefined; }
    const io = new IntersectionObserver((entradas) => {
      entradas.forEach((x) => { if (x.isIntersecting) { x.target.classList.add('visto'); io.unobserve(x.target); } });
    }, { threshold: 0.12 });
    els.forEach((e) => io.observe(e));
    return () => io.disconnect();
  }, []);
}

/** Próximos horarios libres de hoy, según la disponibilidad real del taller (hasta 3). */
function useHorariosHoy() {
  const [horas, setHoras] = useState(null);
  useEffect(() => {
    let vivo = true;
    api.get('/public/citas/disponibilidad', { fecha: hoyISO() })
      .then((d) => {
        if (!vivo) return;
        if (!d.laborable) { setHoras([]); return; }
        const ahora = new Date().toTimeString().slice(0, 5);
        setHoras(d.franjas.filter((f) => f.libres > 0 && f.hora.slice(0, 5) > ahora).slice(0, 3).map((f) => f.hora.slice(0, 5)));
      })
      .catch(() => vivo && setHoras([]));
    return () => { vivo = false; };
  }, []);
  return horas;
}

export default function Inicio() {
  const [tallerApi, setTaller] = useState({});
  const taller = useMemo(() => conReferencia(tallerApi), [tallerApi]);
  const [servicios, setServicios] = useState(null);
  const [errServ, setErrServ] = useState('');
  const [categoria, setCategoria] = useState('Todos');
  const [servicioElegido, setServicioElegido] = useState(null);
  const horasHoy = useHorariosHoy();
  useRevelar();

  useEffect(() => {
    api.get('/public/taller').then(setTaller).catch(() => {});
    api.get('/public/servicios').then(setServicios).catch((e) => setErrServ(e.message));
  }, []);

  const nombre = taller.nombre_taller || 'Taller Mecánico MecAuto';

  const conCategoria = useMemo(() => (servicios || []).map((s) => ({ ...s, categoria: categoriaDe(s.nombre) })), [servicios]);
  const categorias = useMemo(() => CATEGORIAS.filter((c) => conCategoria.some((s) => s.categoria === c)), [conCategoria]);
  const visibles = categoria === 'Todos' ? conCategoria : conCategoria.filter((s) => s.categoria === categoria);
  const reservar = (id) => { setServicioElegido({ id, t: Date.now() }); document.getElementById('cita')?.scrollIntoView({ behavior: 'smooth' }); };

  return (
    <>
      <header className="pub-nav">
        <div className="in">
          <Logo />
          <nav aria-label="Principal">
            <a href="#servicios" onClick={ir('servicios')}>Servicios</a>
            <a href="#proceso" onClick={ir('proceso')}>Cómo trabajamos</a>
            <a href="#cita" onClick={ir('cita')}>Solicitar cita</a>
            <a href="#seguimiento" onClick={ir('seguimiento')}>Mi orden</a>
            <a href="#contacto" onClick={ir('contacto')}>Contacto</a>
            <Link to="/login" className="btn btn-sec btn-sm">Acceso personal</Link>
          </nav>
        </div>
      </header>

      <section className="hero">
        <div className="in hero-rejilla">
          <div className="hero-texto">
            <span className="etiqueta etiqueta-clara">Taller mecánico</span>
            <h1>Cuidamos tu vehículo con <b>diagnóstico claro</b></h1>
            <p>{nombre}. Elige el día y la hora que más te convenga; te informamos el costo antes de iniciar cualquier trabajo.</p>
            <div className="hero-botones">
              <a className="btn btn-pri btn-lg" href="#cita" onClick={ir('cita')}>Reservar mi cita</a>
              <a className="btn btn-borde btn-lg" href="#servicios" onClick={ir('servicios')}>Ver servicios</a>
            </div>
          </div>
          <div className="hero-foto">
            <img src="/img/taller.jpg" alt="Interior del taller con mecánicos trabajando" />
            <div className="hero-horarios">
              <small>Horarios libres hoy</small>
              {horasHoy === null && <span className="hero-nota">Consultando…</span>}
              {horasHoy && horasHoy.length > 0 && (
                <div className="hero-chips">
                  {horasHoy.map((h) => <a key={h} href="#cita" onClick={ir('cita')}>{h}</a>)}
                </div>
              )}
              {horasHoy && horasHoy.length === 0 && <span className="hero-nota">Reserva tu cita para el próximo día disponible.</span>}
            </div>
          </div>
        </div>
      </section>

      <section className="serv-banda" id="servicios">
        <img className="serv-fondo" src="/img/silueta.jpg" alt="" />
        <div className="in">
          <span className="etiqueta etiqueta-clara">Nuestros servicios</span>
          <h2>Todo lo que tu vehículo <b>necesita</b></h2>
          <p>Los precios referenciales incluyen IGV. Cuando dice «según diagnóstico», te informamos el costo antes de iniciar el trabajo.</p>
        </div>
      </section>

      <section className="seccion seccion-servicios revelar" aria-label="Catálogo de servicios">
        {errServ && <div className="caja-error">{errServ}</div>}
        {!servicios && !errServ && <div className="estado-vacio">Cargando servicios…</div>}
        {servicios && servicios.length === 0 && <div className="estado-vacio">Pronto publicaremos nuestro catálogo de servicios.</div>}
        {conCategoria.length > 0 && (
          <div className="chips" role="group" aria-label="Filtrar por categoría">
            {['Todos', ...categorias].map((c) => (
              <button key={c} type="button" className="chip" aria-pressed={categoria === c} onClick={() => setCategoria(c)}>
                {c} <span>{c === 'Todos' ? conCategoria.length : conCategoria.filter((s) => s.categoria === c).length}</span>
              </button>
            ))}
          </div>
        )}
        <div className="servicios-grid">
          {visibles.map((s, i) => (
            <article key={s.id + categoria} className="servicio-card" style={{ animationDelay: `${Math.min(i, 10) * 45}ms` }}>
              <div className="sv-cab">
                <span className="sv-icono"><IconoCategoria categoria={s.categoria} /></span>
                <span className="sv-grupo">{s.categoria}</span>
              </div>
              <h3>{s.nombre}</h3>
              <p>{s.descripcion}</p>
              <div className="sv-pie">
                <div className="sv-precio">
                  <strong>{s.precioReferencial != null ? 'Desde ' + soles(s.precioReferencial) : 'Según diagnóstico'}</strong>
                  {s.duracionMinutos ? <small>≈ {s.duracionMinutos} min</small> : null}
                </div>
                <button type="button" className="sv-reservar" onClick={() => reservar(s.id)} aria-label={'Reservar ' + s.nombre}>Reservar</button>
              </div>
            </article>
          ))}
        </div>
      </section>

      <section className="seccion revelar" id="proceso" style={{ paddingTop: 0 }}>
        <h2>Así <b>trabajamos</b></h2>
        <div className="pasos">
          {PASOS.map(([t, d, img], i) => (
            <div className="paso" key={t}>
              <div className="paso-img">
                <img src={img} alt="" loading="lazy" />
                <span className="paso-num">0{i + 1}</span>
              </div>
              <h3>{t}</h3>
              <p>{d}</p>
            </div>
          ))}
        </div>
      </section>

      <SeguimientoOrden />

      <section className="seccion revelar" id="cita" style={{ paddingTop: 0 }}>
        <h2>Solicita tu <b>cita</b></h2>
        <p className="lead">Cuatro pasos sencillos. Tu solicitud queda registrada y el taller la confirmará.</p>
        <ReservaPasos servicios={servicios || []} servicioElegido={servicioElegido} horarioTexto={taller.horario_texto} />
      </section>

      <ContactoSeccion taller={taller} />

      <section className="cierre">
        <img className="serv-fondo" src="/img/silueta.jpg" alt="" />
        <div className="in">
          <h2>¿Listo para cuidar tu <b>vehículo</b>?</h2>
          <p>Elige el día y la hora. Te informamos el costo antes de iniciar el trabajo.</p>
          <div className="hero-botones">
            <a className="btn btn-pri btn-lg" href="#cita" onClick={ir('cita')}>Reservar mi cita</a>
            <a className="btn btn-borde btn-lg" href="#servicios" onClick={ir('servicios')}>Ver servicios</a>
          </div>
        </div>
      </section>

      <footer className="pub-pie">
        <div className="in pie-rejilla">
          <div className="pie-marca">
            <Logo />
            <p>Mantenimiento y reparación vehicular con diagnóstico claro y trabajo transparente.</p>
          </div>
          <nav aria-label="Servicios"><strong>Servicios</strong>
            {CATEGORIAS.filter((c) => c !== 'Otros').map((c) => <a key={c} href="#servicios" onClick={(e) => { ir('servicios')(e); setCategoria(c); }}>{c}</a>)}
          </nav>
          <nav aria-label="El taller"><strong>El taller</strong>
            <a href="#proceso" onClick={ir('proceso')}>Cómo trabajamos</a>
            <a href="#cita" onClick={ir('cita')}>Solicitar cita</a>
            <a href="#seguimiento" onClick={ir('seguimiento')}>Consultar mi orden</a>
            <Link to="/login">Acceso personal</Link>
          </nav>
          <div className="pie-contacto"><strong>Contacto</strong>
            {taller.correo && <span>{taller.correo}</span>}
            {taller.whatsapp && <span>WhatsApp {taller.whatsapp}{taller._ref.whatsapp ? ' (referencia)' : ''}</span>}
            {taller.horario_texto && <span>{taller.horario_texto}</span>}
          </div>
        </div>
        <div className="in pie-linea"><span>© {new Date().getFullYear()} {nombre}</span><span>{taller.ruc ? 'RUC ' + taller.ruc + (taller._ref.ruc ? ' (referencia)' : '') : ''}</span></div>
      </footer>

      <WhatsAppFlotante numero={taller.whatsapp} />
    </>
  );
}

function ir(id) {
  return (e) => { e.preventDefault(); document.getElementById(id)?.scrollIntoView({ behavior: 'smooth' }); };
}
