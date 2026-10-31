# MecAuto — Frontend

Interfaz web del sistema: **sitio público** (inicio, reserva de cita, seguimiento de orden) y **panel interno** para el personal del taller.

## Tecnologías
| Pieza | Versión | Para qué se usa |
|---|---|---|
| React | 18.3 | Interfaz por componentes |
| Vite | 6 | Servidor de desarrollo y empaquetado |
| @vitejs/plugin-react | 4.3 | Soporte JSX/React en Vite |

No se usan librerías de enrutamiento ni de peticiones HTTP: el router (`lib/router.jsx`) y el cliente de la API (`lib/api.js`) son propios y pequeños.

## Cómo ejecutarlo
```powershell
cd C:\proyectos\MecAuto\frontend
npm install
npm run dev        # http://localhost:5173
npm run build      # genera la carpeta dist/
```
El backend debe estar en `http://localhost:8080`. En desarrollo Vite reenvía `/api` al backend (proxy en `vite.config.js`), así el navegador habla con un solo origen y no hay problemas de CORS. Para otro destino: variable `MECAUTO_API`. En producción se puede definir `VITE_API_URL` (ver `.env.example`).

## Estructura
```
frontend/
├─ index.html, vite.config.js, package.json, .env.example
├─ public/            favicon e imágenes del sitio (img/)
└─ src/
   ├─ main.jsx        punto de entrada
   ├─ App.jsx         decide qué pantalla mostrar según la ruta y el rol
   ├─ styles.css      estilos globales
   ├─ components/ui.jsx   componentes reutilizables
   ├─ lib/
   │  ├─ api.js       cliente de la API (agrega el token JWT, maneja errores y 401)
   │  ├─ auth.jsx     contexto de sesión (usuario, login, logout)
   │  ├─ router.jsx   router por "hash" (#/panel/ordenes)
   │  ├─ hooks.js, format.js, referencia.js, servicios.jsx   utilidades
   └─ pages/
      ├─ Login.jsx, Recuperar.jsx
      ├─ public/      Inicio, ReservaPasos, SeguimientoOrden, ContactoSeccion
      └─ panel/       PanelLayout, rutas, Dashboard, Clientes, Vehiculos, Citas,
                      Ordenes, OrdenDetalle, Inventario, Cotizaciones,
                      Comprobantes, Reportes, Usuarios, Servicios, Configuracion
```

## Rutas del panel y quién puede entrar
Definidas en `pages/panel/rutas.jsx`. Si el rol no corresponde se muestra “No tienes permiso para ver esta sección”.

| Ruta | Pantalla |
|---|---|
| `/panel` | Dashboard |
| `/panel/clientes`, `/panel/vehiculos` | Clientes y vehículos |
| `/panel/citas` | Citas (agenda, confirmar, reprogramar, cancelar) |
| `/panel/ordenes`, `/panel/ordenes/:id` | Órdenes de trabajo y su detalle |
| `/panel/inventario` | Repuestos y movimientos |
| `/panel/cotizaciones` | Cotizaciones |
| `/panel/comprobantes`, `/panel/comprobantes/:id` | Boletas y facturas |
| `/panel/servicios` | Catálogo de servicios |
| `/panel/reportes` | Reportes y exportación a Excel (solo administrador) |
| `/panel/usuarios` | Usuarios y roles (solo administrador) |
| `/panel/configuracion` | Datos del taller |

## Seguridad en el frontend
- El token JWT se guarda en `sessionStorage` (se borra al cerrar la pestaña) y se envía en `Authorization: Bearer`.
- Si la API responde 401, la sesión se cierra y se vuelve al login.
- Ocultar un botón **no es seguridad**: la validación real de permisos se hace siempre en el backend (`@PreAuthorize`). El frontend solo mejora la experiencia.
- React escapa el texto por defecto; no se usa `dangerouslySetInnerHTML`.

## Parte pública (sin iniciar sesión)
- **Reserva de cita:** el cliente elige servicio, fecha/hora disponible (`GET /public/citas/disponibilidad`) y envía sus datos (`POST /public/citas`). El backend limita las solicitudes por IP.
- **Seguimiento:** con placa y número de documento consulta el estado de su orden (`POST /public/ordenes/consulta`).
