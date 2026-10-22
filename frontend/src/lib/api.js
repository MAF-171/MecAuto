// Cliente HTTP mínimo. El token JWT vive en sessionStorage (se borra al cerrar la pestaña).
const BASE = (import.meta.env.VITE_API_URL || '') + '/api/v1';
const CLAVE = 'mecauto.sesion';

export function leerSesion() {
  try { return JSON.parse(sessionStorage.getItem(CLAVE)); } catch { return null; }
}
export function guardarSesion(s) {
  try { sessionStorage.setItem(CLAVE, JSON.stringify(s)); } catch { /* almacenamiento bloqueado */ }
}
export function borrarSesion() {
  try { sessionStorage.removeItem(CLAVE); } catch { /* nada */ }
}

export class ApiError extends Error {
  constructor(estado, mensaje, campos) {
    super(mensaje);
    this.estado = estado;
    this.campos = campos || {};
  }
}

let alExpirar = () => {};
export function onSesionExpirada(fn) { alExpirar = fn; }

function qs(params) {
  const p = new URLSearchParams();
  Object.entries(params || {}).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== '') p.append(k, v);
  });
  const s = p.toString();
  return s ? '?' + s : '';
}

async function pedir(metodo, ruta, { params, cuerpo, blob } = {}) {
  const sesion = leerSesion();
  const headers = {};
  if (cuerpo !== undefined) headers['Content-Type'] = 'application/json';
  if (sesion?.token) headers.Authorization = 'Bearer ' + sesion.token;
  let resp;
  try {
    resp = await fetch(BASE + ruta + qs(params), {
      method: metodo,
      headers,
      body: cuerpo !== undefined ? JSON.stringify(cuerpo) : undefined,
    });
  } catch {
    throw new ApiError(0, 'No se pudo conectar con el servidor. Verifica que el backend esté en ejecución.');
  }
  if (resp.status === 401 && sesion && !ruta.startsWith('/auth/login')) {
    borrarSesion();
    alExpirar();
    throw new ApiError(401, 'Tu sesión expiró. Inicia sesión nuevamente.');
  }
  if (!resp.ok) {
    let datos = null;
    try { datos = await resp.json(); } catch { /* sin cuerpo */ }
    throw new ApiError(resp.status, datos?.mensaje || 'Error inesperado (' + resp.status + ')', datos?.campos);
  }
  if (blob) return resp.blob();
  if (resp.status === 204) return null;
  return resp.json();
}

export const api = {
  get: (r, params) => pedir('GET', r, { params }),
  post: (r, cuerpo) => pedir('POST', r, { cuerpo: cuerpo ?? {} }),
  put: (r, cuerpo) => pedir('PUT', r, { cuerpo }),
  patch: (r, cuerpo) => pedir('PATCH', r, { cuerpo }),
  del: (r) => pedir('DELETE', r),
  blob: (r, params) => pedir('GET', r, { params, blob: true }),
};
