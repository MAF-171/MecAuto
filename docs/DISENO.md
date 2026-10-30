# Diseño: proceso, arquitectura, patrones y base de datos

**Responsable:** Fabian Adriano Chafio Chumioque
**Proyecto:** MecAuto

## 1. Proceso de atención y facturación

Recepción (RECIBIDA) → Diagnóstico (EN_DIAGNOSTICO) → Cotización → ¿Aprueba? → Reparación (EN_REPARACION) → Cierre y comprobante (COMPLETADA) → Entrega (ENTREGADA). Si el cliente no aprueba, la orden pasa a CANCELADA.

**Reglas:** boleta (DNI/CE) o factura (RUC), IGV 18 %, un comprobante por orden Completada, y el stock se descuenta al completar.

**Pasos que automatiza el sistema**
- Registro de cliente, vehículo y orden (código OT-0001).
- Control de estados con reglas por rol.
- Cotización y descuento automático de repuestos.
- Boleta o factura con IGV calculado.
- Alerta de stock mínimo y reportes.

El diagnóstico y la decisión del cliente (¿Aprueba?) son trabajo humano que se registra en el sistema.

## 2. Arquitectura por capas

| Capa | Descripción |
|---|---|
| Presentación | React 18 + Vite, `pages/`, `components/`, `lib/api.js`, token JWT |
| API REST | 15 controladores, prefijo `/api/v1`, `@PreAuthorize` por rol |
| Negocio | 24 clases de servicio: reglas de estado, IGV, stock y comprobantes |
| Acceso a datos | 16 repositorios Spring Data JPA + `ClienteDAO` con `JdbcTemplate` |
| Base de datos | SQL Server, 18 tablas, script `database/schema.sql` |

El controlador nunca accede a la base: siempre pasa por el servicio.

**Paquetes `pe.mecauto`:** config, controller, dto, mapper, service, repository, dao, entity (18), domain (enums y reglas), security, exception, util.

## 3. Patrones de diseño y principios

- **MVC:** modelo (entidades y servicios), vista (React) y controlador (`@RestController`). Ejemplo: `OrdenController → OrdenService → OrdenTrabajoRepository`.
- **DAO:** consultas SQL explícitas aisladas detrás de una interfaz (`ClienteDAO` / `ClienteDAOImpl` con `JdbcTemplate`).
- **SOLID:** responsabilidad única (un servicio por tema), abierto/cerrado (`ComprobanteFactory` e interfaces inyectadas por constructor), interfaces pequeñas.
- **TDD:** ciclo rojo → verde → refactorizar sobre las reglas de negocio (`EstadoOrdenTest`, `OrdenEstadoPolicyTest`).

## 4. Base de datos (SQL Server)

18 tablas normalizadas hasta 3FN, con integridad referencial (FK) y `CHECK`.

**Principales:** `cliente`, `vehiculo`, `orden_trabajo`, `usuario`, `repuesto`, `comprobante`.

**Otras 12:** `proveedor`, `servicio`, `cita`, `movimiento_inventario`, `detalle_orden_servicio`, `detalle_orden_repuesto`, `orden_historial`, `cotizacion`, `detalle_cotizacion`, `detalle_comprobante`, `secuencia`, `configuracion_taller`.

El script `database/schema.sql` es idempotente (se puede repetir sin borrar datos).

## 5. Project Charter y cronograma

| Dato | Valor |
|---|---|
| Inicio | 17/08/2026 |
| Fin | 30/11/2026 |
| Jefe de proyecto | Miguel Angel Flores Hualinga |
| Patrocinador | Propietario del taller MECAUTO |

| Fase | Fechas | Duración |
|---|---|---|
| Cap. 1 Planificación y análisis | 17/08 – 19/09 | 34 días |
| Cap. 2 Diseño | 20/09 – 24/10 | 35 días |
| Cap. 3 Desarrollo | 25/10 – 19/11 | 26 días |
| Cap. 4 Pruebas, despliegue y entrega | 20/11 – 30/11 | 11 días |

**Entregables:** documento de análisis y diseño, sistema web funcional, repositorio GitHub y script de base de datos, manuales de usuario y técnico, informe final.
