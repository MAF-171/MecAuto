# MecAuto — Base de datos (SQL Server)

Base `MECAUTO` en **Microsoft SQL Server**, scripts en T-SQL para ejecutar con SSMS o la extensión *SQL Server (mssql)* de VS Code. Hibernate **no** modifica el esquema (`ddl-auto=none`): este esquema versionado es la fuente de verdad.

## Scripts y orden de ejecución
| # | Archivo | Qué hace |
|---|---|---|
| 1 | `schema.sql` | Crea la base y las 18 tablas, restricciones e índices. No usa `DROP`; se puede repetir. |
| 2 | `seed.sql` | Datos iniciales necesarios (configuración del taller, secuencias, catálogos). No crea usuarios. |
| 3 | `seed-demo.sql` | *(Opcional)* proveedores, servicios y repuestos de ejemplo. |
| 3 | `seed-demo-completo.sql` | *(Opcional)* demo completa: clientes, vehículos, mecánicos, órdenes, inventario, boletas y factura. Usuarios demo: `recepcion`, `jperez`, `dhans`, `mfeels` (clave de demostración indicada en el script). |
| 4 | `crear-usuario-app.sql` | *(Recomendado)* crea un login con permisos solo sobre `MECAUTO` para que el backend no use `sa`. **Cambie la clave antes de ejecutar y no suba la real a Git.** |
| — | `consultas.sql` | Consultas de **solo lectura** para comprobar en SSMS lo que se guardó desde la aplicación. |

El primer administrador lo crea el backend al iniciar (variables `MECAUTO_ADMIN_USER` y `MECAUTO_ADMIN_PASSWORD`).

## Tablas (18)
| Grupo | Tablas |
|---|---|
| Personas | `usuario`, `cliente`, `vehiculo` |
| Catálogo | `servicio`, `proveedor`, `repuesto` |
| Inventario | `movimiento_inventario` |
| Operación | `cita`, `orden_trabajo`, `detalle_orden_servicio`, `detalle_orden_repuesto`, `orden_historial` |
| Ventas | `cotizacion`, `detalle_cotizacion`, `comprobante`, `detalle_comprobante` |
| Soporte | `secuencia` (numeración), `configuracion_taller` |

### Relaciones principales
- `cliente` 1—N `vehiculo`; `vehiculo` 1—N `orden_trabajo`; `cita` puede originar una `orden_trabajo`.
- `usuario` (mecánico) 1—N `orden_trabajo`.
- `orden_trabajo` 1—N `detalle_orden_servicio` y `detalle_orden_repuesto`; 1—N `orden_historial` (auditoría de cambios de estado).
- `repuesto` 1—N `movimiento_inventario`; `proveedor` 1—N `repuesto`.
- `cotizacion` 1—N `detalle_cotizacion`; `comprobante` 1—N `detalle_comprobante`.

## Integridad de datos
- **Llaves foráneas** entre todas las tablas relacionadas.
- **UNIQUE:** usuario, documento, placa, código y RUC; correo y teléfono de usuario con **índice único filtrado** (permite varios NULL).
- **CHECK:** rol de usuario, stock y precios no negativos, año y kilometraje válidos, estados permitidos.
- **Idempotencia de inventario:** `UX_movimiento_idempotencia` evita registrar dos veces el mismo movimiento.
- **Un comprobante emitido por orden:** `UX_comprobante_orden_emitido`.
- **Concurrencia:** columna `version` en `repuesto` y bloqueos pesimistas desde el backend (`repuesto`, `orden_trabajo`, `cotizacion`, `secuencia`).
- **Índices de consulta:** citas por fecha/estado, órdenes por estado/vehículo/mecánico, historial por orden, movimientos por repuesto y fecha.

## Seguridad
- Las contraseñas se guardan con **hash BCrypt**, nunca en texto.
- La aplicación usa un login con permisos mínimos (`db_datareader` / `db_datawriter`), no `sa`.
- Conexión con `encrypt=true` y certificado validado (ver README principal para entornos locales con certificado autofirmado).

## Verificar rápido
```sql
USE MECAUTO;
SELECT COUNT(*) AS tablas FROM sys.tables;   -- debe dar 18
```
