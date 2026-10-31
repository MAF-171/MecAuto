# MecAuto — Backend (API REST)

API REST en **Java 21 + Spring Boot 3.4.5** con SQL Server. Prefijo de todas las rutas: `/api/v1`. Documentación interactiva (Swagger UI, springdoc): `http://localhost:8080/swagger-ui.html`.

## Arquitectura por capas (paquete `pe.mecauto`)
```
controller  →  service  →  repository / dao  →  SQL Server
    │             │
   dto         entity, domain (reglas puras), mapper
```
| Paquete | Contenido |
|---|---|
| `controller` | 14 controladores REST + `Roles` (expresiones de permisos). Solo reciben/validan y delegan. |
| `service` | Reglas de negocio: Auth, Cita, Cliente, Vehiculo, Orden, Cotizacion, Comprobante, Inventario, Usuario, Dashboard, Seguimiento, Reporte, etc. |
| `service/reporte` | 11 generadores de reporte (ingresos, órdenes, citas, clientes, stock bajo, rendimiento de mecánicos…) + `ExcelExporter` (Apache POI). |
| `repository` | 16 repositorios Spring Data JPA (con bloqueos pesimistas donde hay concurrencia). |
| `dao` | `ClienteDAO` con **JdbcTemplate** (acceso SQL directo, para demostrar el patrón DAO). |
| `entity` | 18 entidades JPA, una por tabla. |
| `domain` | Enumeraciones y reglas puras: `EstadoOrden`, `OrdenEstadoPolicy`, `CalculadoraIgv`, `FranjasHorarias`, `Rol`, `TipoComprobante`… |
| `dto`, `mapper` | Objetos de entrada/salida y conversión entidad ↔ DTO. |
| `security` | `JwtService`, `JwtAuthFilter`, `LoginAttemptService`, `UsuarioDetailsService`, manejadores 401/403. |
| `config` | `SecurityConfig`, `AppProperties`, `DataInitializer` (crea el primer admin), `OpenApiConfig`. |
| `exception` | Excepciones de negocio y `GlobalExceptionHandler` (respuestas de error uniformes). |
| `util` | Validación de documento, placa, teléfono y texto. |

## Librerías principales
Spring Web, Spring Security, Spring Data JPA, Bean Validation, Spring Mail, **jjwt 0.12.6** (JWT), **springdoc 2.8.6** (Swagger), **Apache POI 5.4.0** (Excel), Guava, commons-lang3, `mssql-jdbc`; en pruebas: JUnit 5, Mockito, H2.

## Seguridad
- Autenticación **stateless con JWT** (expira en 480 min por defecto, `MECAUTO_JWT_MINUTOS`).
- Contraseñas con **BCrypt (costo 12)**.
- Roles: `ADMINISTRADOR`, `RECEPCION`, `MECANICO`, verificados con `@PreAuthorize` en cada endpoint.
- Bloqueo temporal tras intentos fallidos de login (`LoginAttemptService`) y límite de solicitudes de cita pública por IP.
- Recuperación de contraseña con código de 6 dígitos enviado por correo/celular.
- CORS restringido a `MECAUTO_CORS_ORIGENES`. Secretos solo por variables de entorno.
- Consultas con parámetros (JPA/JdbcTemplate): sin concatenar SQL.
- Excel: las celdas de texto que empiezan con `= + - @` se guardan como texto (evita inyección de fórmulas).

## Reglas de negocio importantes
- **Orden de trabajo:** `RECIBIDA → EN_DIAGNOSTICO → EN_REPARACION → COMPLETADA → ENTREGADA` (se puede `CANCELAR` antes de entregar). Solo se editan líneas mientras está en recibida/diagnóstico/reparación (`OrdenEstadoPolicy`).
- **Inventario:** el stock no puede ser negativo; los movimientos usan clave de idempotencia (no se duplican) y el repuesto tiene `version` + bloqueo pesimista.
- **Numeración** de comprobantes/cotizaciones/órdenes con tabla `secuencia` bloqueada para evitar duplicados.
- **IGV** calculado en `CalculadoraIgv`. Un comprobante emitido por orden (índice único).
- **Citas:** franjas horarias y disponibilidad en `FranjasHorarias`.

## Endpoints (≈96 mapeos)
| Módulo | Ruta base | Acceso |
|---|---|---|
| Autenticación | `/auth` (login, recuperar, restablecer, me) | público / autenticado |
| Público | `/public` (taller, servicios, disponibilidad, citas, consulta de orden) | público |
| Servicios y proveedores | `/servicios`, `/proveedores` | ver: según rol; crear/editar: ADMIN |
| Clientes | `/clientes` | ADMIN, RECEPCION |
| Vehículos | `/vehiculos` | ver: todos; editar: ADMIN, RECEPCION |
| Citas | `/citas` | ADMIN, RECEPCION |
| Órdenes | `/ordenes` | ver: todos; crear/asignar: ADMIN, RECEPCION |
| Inventario | `/repuestos`, `/movimientos` | ver: todos; crear/editar repuesto: ADMIN |
| Cotizaciones | `/cotizaciones` | ADMIN, RECEPCION |
| Comprobantes | `/comprobantes` | ADMIN, RECEPCION; anular: ADMIN |
| Reportes | `/reportes` (+ `/{tipo}/excel`) | ADMIN |
| Usuarios | `/usuarios` | ADMIN (`/mecanicos`: ADMIN, RECEPCION) |
| Configuración, Dashboard | `/configuracion`, `/dashboard` | ver: ADMIN/RECEPCION · Dashboard: todos |

## Variables de entorno
`MECAUTO_DB_USER`, `MECAUTO_DB_PASSWORD`, `MECAUTO_JWT_SECRET` (≥ 32 caracteres), `MECAUTO_ADMIN_USER`, `MECAUTO_ADMIN_PASSWORD`; opcionales: `MECAUTO_DB_URL`, `MECAUTO_PORT`, `MECAUTO_CORS_ORIGENES`, `MECAUTO_MAIL_*`. Valores reales **nunca** en Git.

## Ejecutar y probar
```powershell
cd C:\proyectos\MecAuto\backend
mvn spring-boot:run     # arranca en http://localhost:8080
mvn test                # 141 pruebas en 24 clases
```
Pruebas por área: dominio (IGV, estados, franjas), seguridad (JWT, intentos de login, acceso a endpoints), servicios (citas, órdenes, inventario, cotizaciones, comprobantes, usuarios, recuperación), utilidades, DAO y `ExcelExporterTest`.
