# API REST — resumen

Base: `/api/v1`. Autenticación: `Authorization: Bearer <token>` (se obtiene en `POST /auth/login`). Documentación interactiva generada del código: `http://localhost:8080/swagger-ui.html`.

Formato de error (siempre el mismo):

```json
{ "fecha": "...", "estado": 400, "error": "Bad Request", "mensaje": "Revisa los datos enviados", "ruta": "/api/v1/clientes", "campos": { "nombre": "El nombre es obligatorio" } }
```

Códigos: `200/201/204` éxito · `400` datos inválidos · `401` sin sesión · `403` sin permiso · `404` no existe · `409` conflicto (duplicado, concurrencia) · `422` regla de negocio (p. ej. stock insuficiente) · `429` demasiados intentos.

Roles: **A** = Administrador, **R** = Recepción, **M** = Mecánico. Los listados devuelven `{contenido, pagina, tamano, totalElementos, totalPaginas}`.

## Públicos (sin token)
| Método y ruta | Descripción |
|---|---|
| `GET /public/taller` | Datos de contacto configurados |
| `GET /public/servicios` | Catálogo activo |
| `GET /public/citas/disponibilidad?fecha=YYYY-MM-DD` | Franjas y cupos libres (próximos 90 días) |
| `POST /public/citas` | Solicita una cita (límite por IP, campo trampa) |
| `POST /public/ordenes/consulta` | Estado de una orden. Body `{codigo, placa}` (últimos 3 caracteres de la placa como segundo dato). Devuelve solo estado, servicio, placa enmascarada y fechas; mismo error 404 si algo no coincide; límite 15 consultas / 10 min por IP (429) |
| `POST /auth/login` | Devuelve token y usuario (bloqueo tras 5 fallos en 15 min) |

## Con sesión
| Ruta | Métodos | Roles |
|---|---|---|
| `/auth/me` | GET | A R M |
| `/usuarios` (`/{id}`, `/{id}/estado`, `/{id}/password`) | GET POST PUT PATCH | A |
| `/usuarios/mecanicos` | GET | A R |
| `/clientes` (`/{id}`, `/{id}/vehiculos`, `/{id}/estado`) | GET POST PUT PATCH | A R |
| `/vehiculos` (`/{id}`, `/{id}/historial`) | GET | A R M |
| `/vehiculos`, `/vehiculos/{id}`, `/vehiculos/{id}/estado` | POST PUT PATCH | A R |
| `/servicios` | GET | A R M |
| `/servicios`, `/servicios/{id}`, `/{id}/estado` | POST PUT PATCH | A |
| `/proveedores` | GET | A R |
| `/proveedores`, `/{id}`, `/{id}/estado` | POST PUT PATCH | A |
| `/repuestos`, `/repuestos/{id}`, `/repuestos/stock-bajo` | GET | A R M |
| `/repuestos`, `/repuestos/{id}`, `/{id}/estado` | POST PUT PATCH | A |
| `/repuestos/{id}/movimientos` | POST | A R M (M solo salidas; R sin ajustes) |
| `/movimientos` | GET | A R |
| `/citas` (+`/disponibilidad`, `/{id}`, `/confirmar`, `/cancelar`, `/reprogramar`) | GET POST PUT | A R |
| `/ordenes`, `/ordenes/{id}` | GET | A R M (M solo las suyas) |
| `/ordenes`, `/ordenes/desde-cita/{citaId}`, `/ordenes/{id}/cotizacion` | POST | A R |
| `/ordenes/{id}` | PUT | A R M (M solo diagnóstico y trabajos) |
| `/ordenes/{id}/mecanico` | PATCH | A R |
| `/ordenes/{id}/estado` | POST | A R M (según `OrdenEstadoPolicy`) |
| `/ordenes/{id}/servicios[/{linea}]`, `/ordenes/{id}/repuestos[/{linea}]` | POST DELETE | A R M |
| `/cotizaciones` (+`/aprobar`, `/rechazar`, `/convertir`) | GET POST PUT | A R |
| `/comprobantes` (`/{id}`) | GET, POST (emitir) | A R |
| `/comprobantes/{id}/anular` | POST | A |
| `/dashboard` | GET | A R M (datos según rol) |
| `/reportes`, `/reportes/{tipo}?desde&hasta`, `/reportes/{tipo}/excel` | GET | A |
| `/configuracion` | GET / PUT | A R / A |

Tipos de reporte: `ordenes`, `citas`, `clientes`, `vehiculos-atendidos`, `inventario-movimientos`, `stock-bajo`, `cotizaciones`, `ingresos`, `servicios-solicitados`, `repuestos-usados`, `rendimiento-mecanicos`.

El mecánico solo puede ver/modificar órdenes asignadas a él: esa regla la aplica `OrdenService`, además de `@PreAuthorize` en el controlador.
