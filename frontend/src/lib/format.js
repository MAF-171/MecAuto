const moneda = new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN' });
export const soles = (n) => (n === null || n === undefined || n === '' ? '—' : moneda.format(Number(n)));

export function fecha(iso) {
  if (!iso) return '—';
  const d = new Date(iso);
  return isNaN(d) ? '—' : d.toLocaleDateString('es-PE', { day: '2-digit', month: '2-digit', year: 'numeric' });
}
export function fechaHora(iso) {
  if (!iso) return '—';
  const d = new Date(iso);
  return isNaN(d) ? '—' : d.toLocaleString('es-PE', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' });
}
export const hoyISO = () => {
  const d = new Date();
  return new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 10);
};
export const valorDeInput = (dt) => (dt ? dt.slice(0, 16) : '');

export const ETIQUETA_ESTADO = {
  RECIBIDA: 'Recibida', EN_DIAGNOSTICO: 'En diagnóstico', EN_REPARACION: 'En reparación',
  COMPLETADA: 'Completada', ENTREGADA: 'Entregada', CANCELADA: 'Cancelada',
  PENDIENTE: 'Pendiente', CONFIRMADA: 'Confirmada', ATENDIDA: 'Atendida',
  APROBADA: 'Aprobada', RECHAZADA: 'Rechazada', CONVERTIDA: 'Convertida',
  EMITIDO: 'Emitido', ANULADO: 'Anulado',
  ENTRADA: 'Entrada', SALIDA: 'Salida', AJUSTE: 'Ajuste',
};
export const etiqueta = (e) => ETIQUETA_ESTADO[e] || e;

export const TONO_ESTADO = {
  RECIBIDA: 'azul', EN_DIAGNOSTICO: 'amarillo', EN_REPARACION: 'amarillo', COMPLETADA: 'verde',
  ENTREGADA: 'verde', CANCELADA: 'rojo', PENDIENTE: 'amarillo', CONFIRMADA: 'azul', ATENDIDA: 'verde',
  APROBADA: 'verde', RECHAZADA: 'rojo', CONVERTIDA: 'azul', EMITIDO: 'verde', ANULADO: 'rojo',
  ENTRADA: 'verde', SALIDA: 'rojo', AJUSTE: 'amarillo',
};

export const ROLES = { ADMINISTRADOR: 'Administrador', RECEPCION: 'Recepción', MECANICO: 'Mecánico' };
