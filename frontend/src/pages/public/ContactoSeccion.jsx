import { useState } from 'react';

// Respuestas generales. Revísalas y ajústalas a la forma real de trabajar del taller.
const PREGUNTAS = [
  ['¿Necesito cita para que me atiendan?', 'Puedes reservar en línea eligiendo un día y una hora disponibles. Así el taller prepara tu atención y evitas esperas.'],
  ['¿Cómo sé cuánto va a costar?', 'Los servicios con precio referencial lo muestran con IGV incluido. En los demás, te informamos el costo después del diagnóstico y antes de iniciar el trabajo.'],
  ['¿Qué comprobante recibo?', 'Una boleta si eres persona natural o una factura si tienes RUC, con la base imponible y el IGV detallados. Son comprobantes internos del taller.'],
  ['¿Puedo cambiar o cancelar mi cita?', 'Sí. Comunícate con el taller indicando el código de tu cita y la reprogramamos o la cancelamos.'],
];

// Ubicación de EJEMPLO para el mapa mientras el taller no guarde su dirección real en Configuración.
const REFERENCIA = 'Parque Kennedy, Miraflores, Lima, Perú';

const soloDigitos = (s) => String(s || '').replace(/\D/g, '');

/** Preguntas frecuentes + datos de contacto del taller (vienen de la Configuración del taller). */
export default function ContactoSeccion({ taller }) {
  const [abierta, setAbierta] = useState(0);
  const dir = (taller.direccion || '').trim();
  const ref = taller._ref || {};
  const esRef = !dir || !!ref.direccion;
  const dirMapa = esRef ? REFERENCIA : dir;
  const wa = soloDigitos(taller.whatsapp);
  const waTel = wa.length === 9 ? '51' + wa : wa;
  const tel = String(taller.telefono || '').replace(/[^\d+]/g, '');

  return (
    <section className="seccion revelar" id="contacto" style={{ paddingTop: 0 }}>
      <div className="ct-rejilla">
        <div>
          <h2>Preguntas <b>frecuentes</b></h2>
          <div className="faq">
            {PREGUNTAS.map(([q, a], i) => (
              <div key={q} className={'faq-item' + (abierta === i ? ' abierta' : '')}>
                <button type="button" aria-expanded={abierta === i} onClick={() => setAbierta(abierta === i ? -1 : i)}>
                  <span>{q}</span><i aria-hidden="true">{abierta === i ? '−' : '+'}</i>
                </button>
                <div className="faq-resp"><div><p>{a}</p></div></div>
              </div>
            ))}
          </div>
        </div>

        <div>
          <h2>Visítanos o <b>escríbenos</b></h2>
          <div className="mapa">
            <iframe title="Ubicación del taller" loading="lazy" referrerPolicy="no-referrer-when-downgrade"
              src={'https://maps.google.com/maps?q=' + encodeURIComponent(dirMapa) + '&z=16&output=embed'} />
            {esRef && <span className="mapa-ref">Ubicación de referencia</span>}
            <a className="mapa-llegar" href={'https://www.google.com/maps/search/?api=1&query=' + encodeURIComponent(dirMapa)} target="_blank" rel="noopener noreferrer">Cómo llegar</a>
          </div>

          <dl className="ct-datos">
              <div><dt>{esRef ? 'Dirección (referencia)' : 'Dirección'}</dt><dd>{dir || 'Parque Kennedy, Miraflores, Lima'}</dd></div>
              {taller.horario_texto && <div><dt>Horario</dt><dd>{taller.horario_texto}</dd></div>}
              {taller.telefono && <div><dt>Teléfono{ref.telefono ? ' (referencia)' : ''}</dt><dd>{taller.telefono}</dd></div>}
              {taller.whatsapp && <div><dt>WhatsApp{ref.whatsapp ? ' (referencia)' : ''}</dt><dd>{taller.whatsapp}</dd></div>}
              {taller.correo && <div className="ancho"><dt>Correo{ref.correo ? ' (referencia)' : ''}</dt><dd>{taller.correo}</dd></div>}
            </dl>

          <div className="ct-botones">
            <a className="btn btn-pri" href="#cita" onClick={(e) => { e.preventDefault(); document.getElementById('cita')?.scrollIntoView({ behavior: 'smooth' }); }}>Reservar mi cita</a>
            {waTel.length >= 9 && <a className="btn btn-oscuro" href={'https://wa.me/' + waTel} target="_blank" rel="noopener noreferrer">WhatsApp</a>}
            {tel.length >= 6 && <a className="btn btn-sec" href={'tel:' + tel}>Llamar</a>}
          </div>
        </div>
      </div>
    </section>
  );
}
