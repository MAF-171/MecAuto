/**
 * MODO DEMOSTRACIÓN: mientras el taller no guarde sus datos reales en «Configuración del taller»,
 * la web pública muestra estos datos DE EJEMPLO (marcados como «referencia»).
 * Para la entrega final, cambia MODO_DEMO a false: solo se mostrará lo que el taller haya guardado.
 *
 * Teléfono y WhatsApp: número de contacto de prueba del equipo (944 058 789). Cámbialos por los del taller.
 */
export const MODO_DEMO = true;

const REFERENCIA = {
  direccion: 'Parque Kennedy, Miraflores, Lima',
  telefono: '(51) 944 058 789',
  whatsapp: '+51 944 058 789',
  correo: 'mecauto_sac@hotmail.com',
  ruc: '20613050699',
};

/** Devuelve los datos del taller completando lo que falte con la referencia; `_ref` indica qué campos son de ejemplo. */
export function conReferencia(taller) {
  const t = { ...taller };
  const ref = {};
  if (MODO_DEMO) {
    Object.keys(REFERENCIA).forEach((k) => {
      if (!String(t[k] || '').trim()) { t[k] = REFERENCIA[k]; ref[k] = true; }
    });
  }
  return { ...t, _ref: ref };
}
