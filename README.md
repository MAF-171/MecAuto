# MecAuto — Sistema de información para la gestión de servicios del taller mecánico

Proyecto académico (UTP, Ingeniería de Sistemas). Aplicación web para gestionar clientes, vehículos, citas, órdenes de trabajo, inventario, cotizaciones y comprobantes internos de un taller mecánico, con un sitio público para solicitar citas.

> **Estado del proyecto:** el sistema se compiló, se arrancó y se recorrió de punta a punta (cliente → vehículo → orden → repuesto → comprobante) en una PC con Windows, JDK 21, Maven y SQL Server; las 35 pruebas unitarias pasan. Qué se verificó, qué se corrigió y qué **falta** verificar está detallado en [`docs/VERIFICACION.md`](docs/VERIFICACION.md).

## 1. Tecnologías

| Capa | Tecnología |
|---|---|
| Backend | Java 21, Spring Boot 3.4, Maven, Spring Web (REST), Spring Data JPA/Hibernate, Spring Security (JWT sin estado), Jakarta Bean Validation, springdoc-openapi (Swagger UI), Apache POI (Excel) |
| Base de datos | Microsoft SQL Server (administrada con SSMS), driver oficial Microsoft JDBC, scripts T-SQL versionados |
| Frontend | React 18 + Vite (JavaScript), CSS propio, enrutador por hash y cliente `fetch` propios |
| Pruebas | JUnit 5, Mockito, Spring Boot Test (+ H2 en memoria solo para pruebas) |
| Editor | Visual Studio Code |

Desviaciones respecto al documento académico (ver [`docs/ARQUITECTURA.md`](docs/ARQUITECTURA.md)): el CRUD usa Spring Data JPA y las consultas SQL directas (por ejemplo, documento duplicado de clientes) pasan por un DAO con JdbcTemplate; SQL Server en vez de MySQL (lo exige el enunciado del proyecto); Guava y Apache Commons Lang 3 se usan en validaciones y utilidades; y un enrutador/cliente HTTP propios sustituyen a React Router/Axios para no depender de más librerías.

## 2. Estructura del repositorio

```
MecAuto/
├── backend/            API REST (Spring Boot)
│   └── src/main/java/pe/mecauto/{controller,service,dao,repository,entity,dto,mapper,config,security,exception,domain,util}
├── frontend/           React + Vite (sitio público y panel interno)
├── database/           schema.sql · seed.sql · seed-demo.sql · seed-demo-completo.sql · crear-usuario-app.sql · consultas.sql
├── docs/               arquitectura, API, patrones, verificación
└── .vscode/            extensiones, lanzador, tareas
```

## 3. Requisitos previos (Windows)

1. **JDK 21** (por ejemplo Temurin 21). Verifica en una terminal: `java -version` debe decir `21`. Si hay otra versión, instala la 21 y define `JAVA_HOME` apuntando a ella.
2. **Maven 3.9+**: `mvn -version` debe mostrar Java 21. (La extensión de Maven de VS Code también puede usarse, pero el comando `mvn` en la terminal es lo más simple.)
3. **Node.js 20 o superior** y npm: `node -v`.
4. **SQL Server** (Express o Developer) y **SSMS** (SQL Server Management Studio).
5. **VS Code** con las extensiones recomendadas: al abrir la carpeta `MecAuto`, acepta «Instalar extensiones recomendadas» (Extension Pack for Java, Spring Boot Extension Pack, SQL Server (mssql), ESLint, Prettier, REST Client).

## 4. Configurar SQL Server

### 4.1 Averiguar el nombre de tu instancia y el puerto
No asumas `localhost` ni `SQLEXPRESS`: depende de tu instalación.

* **Nombre de la instancia:** abre SSMS; en la ventana *Conectar al servidor* verás algo como `MIPC\SQLEXPRESS` o solo `MIPC`. Esa es tu instancia (`MIPC\SQLEXPRESS` = servidor `MIPC`, instancia `SQLEXPRESS`; si no hay `\`, es la instancia predeterminada). También puedes ejecutar en una consulta: `SELECT @@SERVERNAME;`
* **Habilitar TCP/IP:** abre *SQL Server Configuration Manager* → *Configuración de red de SQL Server* → *Protocolos de …* → clic derecho en **TCP/IP** → *Habilitar*. Luego reinicia el servicio de SQL Server.
* **Puerto:** en TCP/IP → pestaña *Direcciones IP* → sección **IPAll**. La instancia predeterminada usa `1433`. Las instancias con nombre (como SQLEXPRESS) suelen usar un puerto **dinámico**: anota el valor de *Puertos dinámicos TCP* de IPAll y escríbelo como puerto fijo en *Puerto TCP* (déjalo vacío en *Puertos dinámicos*), después reinicia el servicio.
* **Autenticación mixta:** en SSMS, clic derecho sobre el servidor → *Propiedades* → *Seguridad* → *Modo de autenticación de SQL Server y Windows*. Reinicia el servicio.

### 4.2 Crear la base de datos y las tablas
En SSMS abre y ejecuta, en este orden (cada script se puede repetir sin borrar datos):

1. `database/schema.sql` — crea la base `MECAUTO` y las tablas (solo crea lo que falta; no elimina nada).
2. `database/seed.sql` — correlativos, configuración del taller y catálogo de 14 servicios. **No crea usuarios.**
3. *(opcional, solo para ensayar)* `database/seed-demo.sql` — algunos clientes, vehículos y repuestos **ficticios**.
   *(opcional, para la demostración)* `database/seed-demo-completo.sql` — clientes, vehículos, mecánicos, 10 órdenes de trabajo, inventario, boletas y una factura de ejemplo. Crea los usuarios `recepcion`, `jperez`, `dhans` y `mfeels` (contraseña de demostración `Demo2026!`; cámbiala o desactívalos antes de usar el sistema con datos reales). Se ejecuta una sola vez, después de iniciar el backend por primera vez con el administrador.
4. `database/crear-usuario-app.sql` — crea un login de SQL Server `mecauto_app` con permisos de lectura/escritura **solo** sobre `MECAUTO`. Antes de ejecutarlo cambia la contraseña que aparece en el script (el script se detiene si no la cambias).

### 4.3 Verificar
`database/consultas.sql` contiene consultas de **solo lectura** para comprobar en SSMS lo que guarda la aplicación (citas, órdenes, movimientos, comprobantes) y comprobaciones de consistencia.

## 5. Variables de entorno del backend

Los secretos **no** van en archivos versionados. Copia `backend/env.example` como `backend/.env.local` (ignorado por Git) y complétalo:

| Variable | Obligatoria | Descripción |
|---|---|---|
| `MECAUTO_DB_URL` | no | Cadena JDBC. Por defecto `jdbc:sqlserver://localhost:1433;databaseName=MECAUTO;encrypt=true;trustServerCertificate=false;...`. Para una instancia con nombre usa `jdbc:sqlserver://MIPC;instanceName=SQLEXPRESS;databaseName=MECAUTO;...` o el puerto fijo que configuraste. |
| `MECAUTO_DB_USER` / `MECAUTO_DB_PASSWORD` | sí | Usuario y clave de SQL Server (p. ej. `mecauto_app`). |
| `MECAUTO_JWT_SECRET` | sí | Cadena aleatoria de **32+ caracteres** para firmar los tokens. La aplicación no arranca si falta o es corta. |
| `MECAUTO_ADMIN_USER` / `MECAUTO_ADMIN_PASSWORD` | solo la 1.ª vez | Crean el primer administrador **solo si la tabla `usuario` está vacía**. Contraseña: mínimo 10 caracteres con letras y números. Luego puedes quitarlas. |
| `MECAUTO_CORS_ORIGENES` | no | Origen del frontend permitido (por defecto `http://localhost:5173`). |
| `MECAUTO_DDL_AUTO` | no | `none` por defecto. Pon `validate` si quieres que Hibernate compare entidades y tablas al arrancar. Nunca se usa `update`/`create`. |

**Certificado de SQL Server:** por defecto se exige certificado válido (`encrypt=true;trustServerCertificate=false`). SQL Server de escritorio usa un certificado autofirmado, por lo que **solo en tu PC de desarrollo** puedes poner `trustServerCertificate=true` en `MECAUTO_DB_URL`. No lo uses en un servidor real.

**No hay contraseña de administrador por defecto.** Tú eliges la del primer administrador con las variables anteriores; el hash BCrypt es lo único que se guarda.

## 6. Ejecutar el backend

Con VS Code: abre *Run and Debug* y ejecuta **MecAuto backend (Spring Boot)** (lee `backend/.env.local`). O desde una terminal de PowerShell:

```powershell
cd backend
# cargar variables de la sesión actual (ejemplo)
$env:MECAUTO_DB_USER="mecauto_app"; $env:MECAUTO_DB_PASSWORD="tu_clave"
$env:MECAUTO_JWT_SECRET="una_cadena_aleatoria_de_mas_de_32_caracteres"
$env:MECAUTO_ADMIN_USER="admin"; $env:MECAUTO_ADMIN_PASSWORD="UnaClaveSegura123"
$env:MECAUTO_DB_URL="jdbc:sqlserver://localhost:1433;databaseName=MECAUTO;encrypt=true;trustServerCertificate=true;sendStringParametersAsUnicode=false"
mvn spring-boot:run
```

* Comprobación de salud: <http://localhost:8080/actuator/health> → `{"status":"UP"}`
* Documentación interactiva (Swagger UI): <http://localhost:8080/swagger-ui.html> (botón *Authorize* con el token del login).
* Logs: `backend/logs/mecauto.log`.

## 7. Ejecutar el frontend

```powershell
cd frontend
npm install
npm run dev
```

Abre <http://localhost:5173>. Vite reenvía `/api` al backend (`http://localhost:8080`), así que no hay problemas de CORS en desarrollo. Para producir archivos estáticos: `npm run build` (carpeta `frontend/dist`).

## 8. Primer uso

1. Arranca el backend con `MECAUTO_ADMIN_USER` y `MECAUTO_ADMIN_PASSWORD`; el log dirá «Administrador inicial … creado».
2. Entra a <http://localhost:5173/#/login> con ese usuario.
3. En **Configuración** completa nombre, RUC, dirección, teléfono y horario del taller. Mientras estén vacíos, el sitio público no muestra datos de contacto (no se inventan).
4. En **Usuarios** crea al personal de recepción y a los mecánicos.
5. En **Servicios** revisa el catálogo; en **Inventario** registra repuestos (el stock inicial queda como movimiento).
6. Verifica en SSMS con `database/consultas.sql` que lo que hiciste quedó guardado.

## 9. Roles y permisos (se validan en el servidor)

| Módulo | Administrador | Recepción | Mecánico |
|---|:-:|:-:|:-:|
| Dashboard | completo (con ingresos) | operativo | solo sus órdenes |
| Clientes | ✔ | ✔ | — |
| Vehículos / historial | ✔ | ✔ | consulta |
| Citas | ✔ | ✔ | — |
| Órdenes | todas | todas | solo las asignadas; registra diagnóstico y trabajos |
| Cambios de estado | todos (incluye cancelar) | completar y entregar | diagnóstico, reparación, completar |
| Inventario (consulta) | ✔ | ✔ | ✔ |
| Inventario (entradas) | ✔ | ✔ | — |
| Inventario (salidas) | ✔ | ✔ | ✔ |
| Inventario (ajustes, altas, precios) | ✔ | — | — |
| Cotizaciones | ✔ | ✔ | — |
| Comprobantes (emitir) | ✔ | ✔ | — |
| Comprobantes (anular) | ✔ | — | — |
| Reportes / Excel | ✔ | — | — |
| Usuarios | ✔ | — | — |
| Configuración | editar | ver | — |

## 10. Módulos y reglas principales

* **Sitio público:** servicios y datos de contacto leídos de la base; formulario de cita con horarios reales (capacidad por franja); protección anti-spam (límite por IP y campo trampa).
* **Órdenes de trabajo:** estados `Recibida → En diagnóstico → En reparación → Completada → Entregada` (o `Cancelada`), sin saltos ni retrocesos; historial de cada cambio.
* **Inventario:** el stock **nunca queda negativo** (validación + bloqueo de fila + `CHECK` en SQL Server). Los repuestos de una orden se descuentan **una sola vez** al completarla (clave de idempotencia) y se devuelven si se cancela. Cada cambio deja un movimiento con el stock resultante. Alerta de stock bajo.
* **IGV:** los precios ya incluyen 18 %. La base imponible se calcula como `total / 1.18` y el IGV como diferencia (nunca se suma dos veces).
* **Cotizaciones:** pendiente → aprobada → convertida en orden (operación atómica). Vigencia de 15 días por defecto.
* **Comprobantes internos:** boleta (DNI/CE) o factura (RUC) con numeración correlativa sin saltos duplicados; imprimible. **No son comprobantes electrónicos de SUNAT** y así se indica en el documento.
* **Reportes:** 11 reportes con filtro de fechas, impresión y exportación a Excel (solo administrador).
* **Borrado lógico:** clientes, vehículos, servicios, repuestos y usuarios se desactivan; no se eliminan para conservar el historial.

## 11. Pruebas

```powershell
cd backend
mvn test
```

* **Pruebas unitarias** (no necesitan base de datos): IGV, estados de orden, permisos por rol, horarios, validación de documentos y placas, reglas de inventario (Mockito).
* **Pruebas de integración** (`*IT.java`, H2 en memoria): protección de rutas por rol. **No usan SQL Server real.**
* No hay pruebas automáticas contra SQL Server; se verifica con `consultas.sql` y uso manual.

Qué se ejecutó realmente y qué falta: [`docs/VERIFICACION.md`](docs/VERIFICACION.md).

## 12. Solución de problemas

| Síntoma | Causa probable y solución |
|---|---|
| `release version 21 not supported` / `invalid target release` | Maven usa otro JDK. Revisa `mvn -version` y `JAVA_HOME`. |
| `The driver could not establish a secure connection … PKIX path building failed` | Certificado autofirmado. En desarrollo local usa `trustServerCertificate=true` en `MECAUTO_DB_URL`. |
| `Connection refused` / `TCP/IP connection failed` | TCP/IP deshabilitado, puerto equivocado o servicio detenido. Revisa 4.1. |
| `Login failed for user 'mecauto_app'` | Autenticación mixta desactivada, usuario sin crear o clave distinta. |
| `Cannot open database "MECAUTO"` | No ejecutaste `schema.sql` o el usuario no tiene permisos (`crear-usuario-app.sql`). |
| La app no arranca: «mecauto.jwt.secret» | Falta `MECAUTO_JWT_SECRET` o tiene menos de 32 caracteres. |
| `Schema-validation: missing table` | Pusiste `MECAUTO_DDL_AUTO=validate` y falta un script SQL. Ejecuta `schema.sql`. |
| No existe ningún usuario para entrar | La tabla `usuario` estaba vacía y no definiste `MECAUTO_ADMIN_*`. Define ambas y reinicia el backend. |
| El frontend muestra «No se pudo conectar con el servidor» | El backend no está levantado o no está en el puerto 8080 (`MECAUTO_API` cambia el destino del proxy de Vite). |
| Error de compilación en `mvn` | Cópialo completo (archivo y línea). El backend sí compiló en la PC del equipo (ver `docs/VERIFICACION.md`); revisa que uses JDK 21 y que Maven use ese JDK (`mvn -version`). |

## 13. Documentación

* [`docs/ARQUITECTURA.md`](docs/ARQUITECTURA.md) — capas, modelo de datos, casos de uso, diagramas de clases y secuencia.
* [`docs/API.md`](docs/API.md) — endpoints principales y permisos.
* [`docs/PATRONES.md`](docs/PATRONES.md) — patrones y principios realmente usados.
* [`docs/VERIFICACION.md`](docs/VERIFICACION.md) — qué se probó y qué no.
