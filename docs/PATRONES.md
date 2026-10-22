# Patrones y principios realmente usados

Solo se listan los que existen en el código y resuelven un problema concreto.

| Patrón / principio | Dónde | Para qué |
|---|---|---|
| **Layered architecture** | todo el backend | Separar HTTP, negocio, datos y dominio. |
| **Repository** | `repository/*` (Spring Data JPA) | Abstraer el acceso a SQL Server. |
| **DAO (Data Access Object)** | `dao/ClienteDAO` (interfaz) y `dao/ClienteDAOImpl` (JdbcTemplate) | Consultas SQL parametrizadas propias, detrás de una interfaz que el servicio inyecta; hoy resuelve la verificación de documento duplicado de clientes. |
| **DTO + Mapper** | `dto/*`, `mapper/*` | No exponer entidades ni hashes; contrato estable de la API. |
| **State (enum con comportamiento)** | `EstadoOrden`, `OrdenEstadoPolicy` | Transiciones válidas de la orden y quién puede hacerlas, en un solo sitio y probado sin Spring. |
| **Strategy + registro** | `ReporteGenerador` (11 implementaciones) y `ReporteService` | Añadir un reporte nuevo = una clase nueva, sin tocar `ReporteService` ni el controlador. |
| **Factory** | `ComprobanteFactory` | Decidir boleta/factura y construir el comprobante con su desglose de IGV. |
| **Observer (eventos de Spring)** | `StockBajoEvent` + `AlertaStockListener` (`AFTER_COMMIT`) | Avisar de stock bajo solo cuando la transacción de inventario se confirmó. |
| **Unit of Work (transacciones)** | `@Transactional` en servicios | Operaciones multi-tabla atómicas (conversión de cotización, completar orden). |
| **Idempotencia** | `clave_idempotencia` + índice único filtrado | Evitar doble descuento de stock ante reintentos. |
| **SRP** | `CalculadoraIgv`, `FranjasHorarias`, `SecuenciaService` | Cada clase tiene una razón para cambiar. |
| **OCP** | estrategias de reporte | Extender sin modificar. |
| **DIP** | servicios reciben repositorios por constructor | Permite probar con Mockito. |

No se usó Singleton manual, Builder ni Decorator porque no había un problema que justificara agregarlos.

## Librerías de apoyo y dónde se usan
| Librería | Uso real en el código |
|---|---|
| **Google Guava** | `Preconditions` en `InventarioService.salidaPorOrden/devolucionPorOrden` (contratos internos: repuesto bloqueado y cantidad positiva); `Sets.immutableEnumSet` para los estados «abiertos» en `ClienteService`. |
| **Apache Commons Lang 3** | `StringUtils` en `TextoUtil` (recortar, nulo si vacío, normalizar espacios) y en `PlacaUtil` (mayúsculas sin espacios). |
| **Apache POI** | Exportación a Excel (`ExcelExporter`). |
| **Logback** (incluido en Spring Boot) | Registro en consola y en `logs/mecauto.log`, con rotación por tamaño y días (`application.properties`). |

