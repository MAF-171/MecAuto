import Dashboard from './Dashboard.jsx';
import Clientes from './Clientes.jsx';
import Vehiculos from './Vehiculos.jsx';
import Servicios from './Servicios.jsx';
import Citas from './Citas.jsx';
import Ordenes from './Ordenes.jsx';
import OrdenDetalle from './OrdenDetalle.jsx';
import Inventario from './Inventario.jsx';
import Cotizaciones from './Cotizaciones.jsx';
import { ComprobantesLista, ComprobanteDetalle } from './Comprobantes.jsx';
import Reportes from './Reportes.jsx';
import Usuarios from './Usuarios.jsx';
import Configuracion from './Configuracion.jsx';

const GESTION = ['ADMINISTRADOR', 'RECEPCION'];
const ADMIN = ['ADMINISTRADOR'];

// `menu` = texto del menú lateral (sin menu = ruta interna). `roles` = quién puede entrar; el backend lo vuelve a verificar.
export const RUTAS_PANEL = [
  { path: '/panel', componente: Dashboard, menu: 'Inicio' },
  { path: '/panel/clientes', componente: Clientes, menu: 'Clientes', roles: GESTION },
  { path: '/panel/vehiculos', componente: Vehiculos, menu: 'Vehículos' },
  { path: '/panel/citas', componente: Citas, menu: 'Citas', roles: GESTION },
  { path: '/panel/ordenes', componente: Ordenes, menu: 'Órdenes de trabajo' },
  { path: '/panel/ordenes/:id', componente: OrdenDetalle },
  { path: '/panel/inventario', componente: Inventario, menu: 'Inventario' },
  { path: '/panel/cotizaciones', componente: Cotizaciones, menu: 'Cotizaciones', roles: GESTION },
  { path: '/panel/comprobantes', componente: ComprobantesLista, menu: 'Comprobantes', roles: GESTION },
  { path: '/panel/comprobantes/:id', componente: ComprobanteDetalle, roles: GESTION },
  { path: '/panel/servicios', componente: Servicios, menu: 'Servicios' },
  { path: '/panel/reportes', componente: Reportes, menu: 'Reportes', roles: ADMIN },
  { path: '/panel/usuarios', componente: Usuarios, menu: 'Usuarios', roles: ADMIN },
  { path: '/panel/configuracion', componente: Configuracion, menu: 'Configuración', roles: GESTION },
];
