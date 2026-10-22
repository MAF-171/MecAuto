# Arquitectura de MecAuto

Los diagramas usan [Mermaid](https://mermaid.js.org/) (VS Code los muestra con la extensión «Markdown Preview Mermaid Support», y GitHub los dibuja directamente). Reflejan lo que está implementado en el código.

## 1. Vista general

```mermaid
flowchart LR
  N[Navegador<br/>React + Vite] -- "HTTPS/JSON<br/>/api/v1 + JWT" --> C
  subgraph B[Backend Spring Boot]
    C[controller<br/>@PreAuthorize por rol] --> S[service<br/>@Transactional]
    S --> R[repository<br/>Spring Data JPA]
    S --> D[domain<br/>reglas puras]
    C -.usa.-> DTO[dto + mapper]
    SEC[security<br/>JwtAuthFilter] --> C
  end
  R -- "JDBC (mssql-jdbc)" --> DB[(SQL Server<br/>MECAUTO)]
```

Capas y responsabilidades:

| Paquete | Responsabilidad |
|---|---|
| `controller` | Endpoints REST `/api/v1`, códigos HTTP, permisos por rol. Sin lógica de negocio. |
| `service` | Casos de uso y transacciones. Único lugar donde se orquestan varias tablas. |
| `domain` | Enumeraciones y reglas puras sin Spring: máquina de estados de la orden, cálculo de IGV, franjas horarias, política de permisos de estado. |
| `repository` | Acceso a datos (Spring Data JPA, consultas con bloqueo pesimista donde hace falta). |
| `entity` | Mapeo JPA de las tablas. Nunca se devuelven por la API. |
| `dto` / `mapper` | Contratos de entrada/salida (records) y conversión. Los hashes y datos internos no salen. |
| `security` / `config` | JWT, filtro de autenticación, CORS, BCrypt, inicializador del primer administrador. |
| `exception` | Excepciones de negocio y `GlobalExceptionHandler` (formato de error único). |

## 2. Modelo de datos (SQL Server)

```mermaid
erDiagram
  USUARIO ||--o{ ORDEN_TRABAJO : "mecánico asignado"
  CLIENTE ||--o{ VEHICULO : posee
  CLIENTE ||--o{ ORDEN_TRABAJO : solicita
  VEHICULO ||--o{ ORDEN_TRABAJO : ingresa
  CLIENTE ||--o{ COTIZACION : recibe
  CITA }o--|| SERVICIO : "servicio pedido"
  CITA |o--o| ORDEN_TRABAJO : origina
  ORDEN_TRABAJO ||--o{ DETALLE_ORDEN_SERVICIO : contiene
  ORDEN_TRABAJO ||--o{ DETALLE_ORDEN_REPUESTO : contiene
  ORDEN_TRABAJO ||--o{ ORDEN_HISTORIAL : registra
  SERVICIO ||--o{ DETALLE_ORDEN_SERVICIO : ""
  REPUESTO ||--o{ DETALLE_ORDEN_REPUESTO : ""
  PROVEEDOR ||--o{ REPUESTO : suministra
  REPUESTO ||--o{ MOVIMIENTO_INVENTARIO : "historial de stock"
  COTIZACION ||--o{ DETALLE_COTIZACION : contiene
  COTIZACION |o--o| ORDEN_TRABAJO : "se convierte en"
  ORDEN_TRABAJO ||--o{ COMPROBANTE : "máx. 1 emitido"
  COMPROBANTE ||--o{ DETALLE_COMPROBANTE : contiene
```

Tablas de apoyo: `secuencia` (correlativos con bloqueo de fila) y `configuracion_taller` (clave/valor). Restricciones relevantes: `CHECK (stock_actual >= 0)`, único filtrado `UX_movimiento_idempotencia` (evita descuentos duplicados), único filtrado `UX_comprobante_orden_emitido` (un solo comprobante emitido por orden), `UNIQUE(tipo, serie, numero)`. Detalle completo en `database/schema.sql`.

El **historial de mantenimiento** de un vehículo se obtiene de las órdenes (`orden_trabajo` + `orden_historial`), sin duplicar datos en otra tabla.

## 3. Casos de uso

```mermaid
flowchart LR
  V((Visitante)) --> U1[Ver servicios y contacto]
  V --> U2[Solicitar cita]
  R((Recepción)) --> U3[Gestionar clientes y vehículos]
  R --> U4[Gestionar citas]
  R --> U5[Crear orden / desde cita]
  R --> U6[Cotizar y convertir en orden]
  R --> U7[Emitir comprobante interno]
  R --> U8[Registrar entradas/salidas de inventario]
  M((Mecánico)) --> U9[Ver sus órdenes]
  M --> U10[Registrar diagnóstico y trabajos]
  M --> U11[Cambiar estado de reparación]
  M --> U12[Registrar salida de repuestos]
  A((Administrador)) --> U13[Todo lo anterior]
  A --> U14[Reportes y Excel]
  A --> U15[Usuarios y configuración]
  A --> U16[Ajustes de inventario, anular comprobantes, cancelar órdenes]
```

## 4. Clases principales (dominio y servicios)

```mermaid
classDiagram
  class OrdenTrabajo { +estado: EstadoOrden +recalcularTotal() }
  class EstadoOrden { <<enum>> +siguientes() +puedePasarA(destino) +admiteCambiosEnDetalle() }
  class OrdenEstadoPolicy { +puedeCambiarA(rol, destino)$ }
  class CalculadoraIgv { +desglosar(total)$ +importe(cant, precio)$ }
  class OrdenService { +crear() +cambiarEstado() +agregarRepuesto() }
  class InventarioService { +registrar() +salidaPorOrden() +devolucionPorOrden() -aplicarMovimiento() }
  class CotizacionService { +convertirEnOrden() +generarDesdeOrden() }
  class ComprobanteService { +emitir() +anular() }
  class ComprobanteFactory { +tipoPara(orden) +crear(orden, pago, emisor, serie, numero) }
  class SecuenciaService { +siguiente(nombre) }
  class ReporteService { +generar(tipo, desde, hasta, estado) }
  class ReporteGenerador { <<interface>> +tipo() +generar(filtro) }
  class AlertaStockListener { +alAlertar(StockBajoEvent) }
  OrdenService --> EstadoOrden
  OrdenService --> OrdenEstadoPolicy
  OrdenService --> InventarioService
  OrdenService --> SecuenciaService
  CotizacionService --> OrdenService
  CotizacionService --> CalculadoraIgv
  ComprobanteService --> ComprobanteFactory
  ComprobanteService --> SecuenciaService
  ComprobanteService --> CalculadoraIgv
  InventarioService ..> AlertaStockListener : publica StockBajoEvent
  ReporteService o-- ReporteGenerador
  OrdenTrabajo --> EstadoOrden
```

## 5. Secuencias

### 5.1 Completar una orden (descuento de inventario)

```mermaid
sequenceDiagram
  actor U as Mecánico/Recepción
  participant C as OrdenController
  participant O as OrdenService
  participant P as OrdenEstadoPolicy
  participant I as InventarioService
  participant DB as SQL Server
  U->>C: POST /ordenes/{id}/estado {COMPLETADA}
  C->>O: cambiarEstado(id, req, actor)
  O->>DB: SELECT orden (bloqueo de fila)
  O->>P: ¿el rol puede pasar a COMPLETADA? + ¿transición válida?
  loop cada repuesto no descontado
    O->>DB: SELECT repuesto (bloqueo)
    O->>I: salidaPorOrden(clave "OT{id}-L{línea}")
    I->>DB: ¿existe la clave? no → UPDATE stock + INSERT movimiento
  end
  O->>DB: UPDATE estado + INSERT orden_historial
  Note over O,DB: Todo en UNA transacción: si falla algo, nada se guarda
  O-->>C: OrdenDetalleDto
```

### 5.2 Cita pública

```mermaid
sequenceDiagram
  actor V as Visitante
  participant F as React
  participant P as PublicController
  participant L as LimitadorSolicitudes
  participant S as CitaService
  participant DB as SQL Server
  V->>F: elige día
  F->>P: GET /public/citas/disponibilidad?fecha
  P->>S: disponibilidad(fecha)
  S-->>F: franjas con cupos libres
  V->>F: completa y envía
  F->>P: POST /public/citas
  P->>L: verificar(IP) → 429 si excede
  P->>S: crearPublica (descarta si el campo trampa viene lleno)
  S->>DB: valida franja/capacidad, genera CITA-000n, INSERT cita
  S-->>F: 201 {codigo}
```

### 5.3 Emitir comprobante

```mermaid
sequenceDiagram
  actor R as Recepción
  participant CS as ComprobanteService
  participant SQ as SecuenciaService
  participant DB as SQL Server
  R->>CS: emitir(ordenId, metodoPago)
  CS->>DB: SELECT orden (bloqueo); exige COMPLETADA y sin comprobante emitido
  CS->>SQ: siguiente(serie) (fila bloqueada)
  CS->>DB: INSERT comprobante + detalles (IGV desglosado del total)
  CS-->>R: 201 número correlativo
```

## 6. Decisiones relevantes

* **Sesión sin estado con JWT** (HS256, expira a las 8 h por defecto). El filtro vuelve a leer el usuario en cada petición: si se desactiva, su token deja de servir de inmediato.
* **El frontend guarda el token en `sessionStorage`** (se pierde al cerrar la pestaña). Es un compromiso: más simple que cookies `HttpOnly`, a cambio de ser accesible por JavaScript si hubiera XSS; React escapa el contenido por defecto y no se usa `dangerouslySetInnerHTML`.
* **`open-in-view=false`**: los servicios arman los DTO dentro de la transacción; no hay carga perezosa fuera de ella.
* **Concurrencia:** bloqueo pesimista (`PESSIMISTIC_WRITE`) en repuestos, órdenes, cotizaciones y secuencias; `@Version` como red de seguridad.
* **Desviaciones del documento académico:** Spring Data JPA para el CRUD más un DAO con JdbcTemplate para las consultas SQL directas (`dao/ClienteDAO`); SQL Server en lugar de MySQL; Guava y Commons Lang 3 usados en validaciones y utilidades; React Router/Axios sustituidos por un enrutador por hash y `fetch` propios; el historial de mantenimiento se deriva de las órdenes; la consulta pública de historial por placa se eliminó para no exponer datos de terceros.
