# Qué se verificó y qué no

Este documento separa lo que **se ejecutó realmente** de lo que **solo se escribió**. Hay dos entornos distintos y se indica cuál produjo cada resultado.

* **Entorno de desarrollo (sin red a Maven Central, npm ni SQL Server):** solo permitió pruebas parciales.
* **PC del equipo (Windows, JDK 21, Maven 3.10.0, Node 24, SQL Server local):** ahí se ejecutó el sistema completo el **19/10/2026**. Los resultados de esa jornada los **observó y reportó el equipo** al seguir la guía del README; no hay capturas automáticas ni informes de pruebas guardados en el repositorio, salvo los que se indiquen.

## A. Verificado en la PC del equipo (19/10/2026)
| Qué | Cómo | Resultado |
|---|---|---|
| Compilación del backend | `mvn -DskipTests compile` | `BUILD SUCCESS` |
| Pruebas unitarias del backend | `mvn test` | 35 pruebas, 0 fallos, `BUILD SUCCESS` |
| Esquema de base de datos | `database/schema.sql` en SSMS, sobre una base `MECAUTO` vacía | 18 tablas creadas |
| Datos iniciales | `database/seed.sql` | Secuencias, configuración y catálogo de servicios cargados |
| Usuario de aplicación | `database/crear-usuario-app.sql` (login `mecauto_app`, solo lectura/escritura) | El backend se conecta con ese usuario |
| Arranque del backend | `mvn spring-boot:run` con `MECAUTO_DDL_AUTO=validate` | Arranca; las entidades coinciden con las tablas; `/actuator/health` en `UP`; se crea el administrador inicial |
| Arranque del frontend | `npm install` y `npm run dev` | Sitio disponible en `http://localhost:5173` |
| Flujo de taller de punta a punta | Cliente → vehículo → orden de trabajo (`OT-0002`) → servicios → repuesto → asignar mecánico → diagnóstico → En reparación → Completada | Funciona; el stock del repuesto pasó de 10 a 9 al completar |
| Comprobante | Emisión de boleta interna para la orden | `B001-00000001`, total S/ 565.00 = base S/ 478.81 + IGV S/ 86.19 (cuadra con las líneas, incluido un descuento de S/ 20.00) |
| Persistencia en SQL Server | Consultas `SELECT` en SSMS sobre comprobante, detalle, orden, movimientos y repuesto | Los datos coinciden con lo visto en pantalla (comprobante, 7 líneas, movimientos de entrada y salida, stock 9) |
| Permisos por rol | Ingreso con usuario Mecánico | Reportado como correcto |
| Cita pública | Reserva desde el sitio público y revisión en Citas como administrador | Reportado como correcto |

### Defecto encontrado y corregido en esa jornada
* **Selector de repuesto en la orden de trabajo:** la lista de varias filas (`<select size=5>`) no registraba la selección en el navegador del equipo y el botón *Agregar* quedaba desactivado. Se reemplazó por una lista de botones con la fila elegida resaltada (`OrdenDetalle.jsx`, `styles.css`). Tras el cambio, el equipo confirmó que el repuesto se agrega. No se añadió una prueba automatizada para este caso.
* **Esquema antiguo:** una base `MECAUTO` previa con tablas de un intento anterior hacía fallar `schema.sql` (el script solo crea lo que no existe). Se resolvió eliminando esa base de pruebas y recreándola. Si tu instancia ya tiene una base `MECAUTO` con otras tablas, haz lo mismo **solo si no necesitas esos datos**.

## A.1. Cambios posteriores (20/10/2026): Guava, Commons Lang 3 y DAO
Se agregaron `guava` y `commons-lang3` al `pom.xml`, `dao/ClienteDAO` + `ClienteDAOImpl` (JdbcTemplate) y su uso en `ClienteService`, `InventarioService`, `TextoUtil` y `PlacaUtil`.
* **Ejecutado en el entorno de desarrollo:** `TextoUtilTest` y `PlacaUtilTest` (8 pruebas) con el JAR real de Commons Lang 3 y un lanzador mínimo de JUnit: 8 de 8 pasan. Las llamadas a Guava (`Preconditions`, `Sets.immutableEnumSet`) se probaron con el JAR real de Guava.
* **Pendiente de ejecutar en la PC del equipo:** `mvn test` completo (incluye `ClienteDAOImplTest` sobre H2 y la nueva prueba de `InventarioServiceTest`), y la prueba manual de crear/editar un cliente con documento duplicado. En el entorno de desarrollo esos archivos solo se revisaron por sintaxis, sin las librerías de Spring.

## B. Verificado en el entorno de desarrollo (anterior, parcial)
| Qué | Cómo | Resultado |
|---|---|---|
| Lógica pura del dominio y utilidades (26 pruebas JUnit 5): IGV (216 → 183.05 + 32.95; 236 → 200 + 36), cotización con descuento, estados de orden, permisos por rol, franjas horarias, validación de DNI/RUC/CE y placas | `javac` + lanzador mínimo propio de JUnit (no es el motor oficial; no va en el proyecto) | 26 de 26 pasan |
| Frontend: empaquetado sin errores de sintaxis ni de importación | `esbuild` (no Vite) | OK |
| Frontend: 25 comprobaciones en Chromium (Playwright): sitio público, cita con franjas, redirección sin sesión, login, menú y rutas por rol, crear cliente, cambio de estado, reporte, vista móvil, sin errores de consola | Contra una **API simulada escrita por mí** a partir de los DTO, no contra el backend real | 25 de 25 pasan |
| Contrato frontend ↔ backend | Revisión manual de cada campo de los DTO; se corrigieron desajustes (servicio obligatorio en citas, RUC obligatorio en proveedores, origen `PUBLICA`) | Revisado a mano |

## C. Todavía NO verificado
| Qué | Motivo | Riesgo |
|---|---|---|
| `SeguridadEndpointsIT` (5 pruebas de seguridad con H2) | El nombre termina en `IT`, así que `mvn test` (Surefire) no lo ejecuta; haría falta `mvn verify` con el plugin Failsafe configurado | Podría fallar por diferencias de H2 con alguna columna; sería un problema de la prueba, no del sistema |
| Segundo comprobante para la misma orden (debe rechazarse) | No se reportó el resultado | Revisar a mano |
| Paso de la orden a *Entregada*, anulación de comprobante, cotizaciones | No se reportó | Revisar a mano |
| Rol Recepción (permisos y menú) | Solo se probó el rol Mecánico | Probar con un usuario de recepción |
| Exportación a Excel, reportes, bloqueo de login tras intentos fallidos, límite de citas por franja | No se reportó | Revisar a mano |
| `seed-demo.sql` y el archivo completo `consultas.sql` | Solo se ejecutaron consultas `SELECT` propias de comprobación | Ejecutarlos en SSMS |
| Concurrencia (dos descuentos simultáneos sobre el mismo repuesto) | Requiere prueba de carga | El diseño (bloqueo pesimista + clave de idempotencia + `CHECK`) está implementado pero no se probó bajo carga |
| Pruebas automáticas del frontend contra el backend real | No existen | Las 25 comprobaciones de la sección B usan una API simulada |

## Cómo seguir verificando en tu PC
1. `cd backend && mvn verify` para intentar incluir las pruebas `*IT`.
2. Recorrer los puntos de la sección C con usuarios de los tres roles y contrastar con la tabla de permisos del README.
3. Ejecutar `database/consultas.sql` completo y `database/seed-demo.sql` si quieres datos de demostración.

## Limitaciones conocidas del producto
* Los comprobantes son **internos**; no se integran con SUNAT.
* No hay envío de correos ni notificaciones (no se pidió y no se simuló).
* El token vive en `sessionStorage` (ver compromisos en `docs/ARQUITECTURA.md`).
* No hay recuperación de contraseña por correo: el administrador asigna una nueva.
* `trustServerCertificate=true` en la URL de ejemplo es **solo para desarrollo local**; en un servidor real debe instalarse un certificado válido y quitarse.
* El sitio público no tiene imágenes ni textos de certificaciones, testimonios ni dirección: se muestran solo los datos que el administrador configure.
* Un intento previo de otra sesión dejó archivos de la primera versión (`pom.xml`, `application.properties`, `schema.sql`, `seed.sql`) que fueron reemplazados; los restos se archivaron en `docs/_intento-previo/`.
