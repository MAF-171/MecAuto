# MecAuto — Sistema de información para la gestión de servicios del taller mecánico

Proyecto académico (UTP, Ingeniería de Sistemas). Aplicación web para gestionar clientes, vehículos, citas, órdenes de trabajo, inventario, cotizaciones y comprobantes internos de un taller mecánico, con un sitio público para solicitar citas.

> **Estado del proyecto:** el sistema se compiló, se arrancó y se recorrió de punta a punta (cliente → vehículo → orden → repuesto → comprobante) en una PC con Windows, JDK 21, Maven y SQL Server; las 141 pruebas automáticas (24 clases) pasan con `mvn test`. Qué se verificó, qué se corrigió y qué **falta** verificar está detallado en [`docs/VERIFICACION.md`](docs/VERIFICACION.md).

## 1. Tecnologías

| Capa | Tecnología |
|---|---|
| Backend | Java 21, Spring Boot 3.4, Maven, Spring Web (REST), Spring Data JPA/Hibernate, Spring Security (JWT sin estado), Jakarta Bean Validation, springdoc-openapi (Swagger UI), Apache POI (Excel) |
| Base de datos | Microsoft SQL Server (administrada con SSMS), driver oficial Microsoft JDBC, scripts T-SQL versionados |
| Frontend | React 18 + Vite (JavaScript), CSS propio, enrutador por hash y cliente `fetch` propios |
| Pruebas | JUnit 5, Mockito, Spring Boot Test (+ H2 en memoria solo para pruebas) |
| Editor | Visual Studio Code |

Desviaciones respecto al documento académico (ver [`docs/ARQUITECTURA.md`](docs/ARQUITECTURA.md)): el CRUD usa Spring Data JPA y las consultas SQL directas (por ejemplo, documento duplicado de clientes) pasan por un DAO con JdbcTemplate; SQL Server en vez de MySQL (lo exige el enunciado del proyecto); Guava y Apache Commons Lang 3 se usan en validaciones y utilidades; y un enrutador/cliente HTTP propios sustituyen a React Router/Axios para no depender de más librerías.

### 1.1 Librerías del backend (`backend/pom.xml`)

Spring Boot administra las versiones de las dependencias que no llevan versión propia; las demás se declaran como propiedades en el `pom.xml`.

| Librería (artifact) | Versión | Para qué se usa en MecAuto |
|---|---|---|
| `spring-boot-starter-web` | 3.4.5 | API REST (`@RestController`, prefijo `/api/v1`), Jackson para JSON y Tomcat embebido. |
| `spring-boot-starter-data-jpa` | 3.4.5 | Entidades y repositorios (Spring Data JPA con Hibernate). Hibernate **no** modifica el esquema (`ddl-auto=none`). |
| `mssql-jdbc` | gestionada por Spring Boot | Driver oficial de Microsoft para conectarse a SQL Server. Lo usan JPA y el DAO con `JdbcTemplate`. |
| `spring-boot-starter-security` | 3.4.5 | Autenticación, autorización por rol con `@PreAuthorize` y cifrado de contraseñas con BCrypt. |
| `jjwt-api` / `jjwt-impl` / `jjwt-jackson` | 0.12.6 | Creación y verificación de tokens JWT firmados (inicio de sesión sin estado). |
| `spring-boot-starter-validation` | 3.4.5 | Validación de datos de entrada con `@Valid`, `@NotBlank`, `@Size`, `@Pattern`, `@Email`. |
| `spring-boot-starter-mail` | 3.4.5 | Envío del código de recuperación de contraseña por correo (SMTP). |
| `spring-boot-starter-actuator` | 3.4.5 | Endpoint de salud `/actuator/health`. Solo se expone `health` e `info`. |
| `springdoc-openapi-starter-webmvc-ui` | 2.8.6 | Documentación interactiva de la API (Swagger UI) en `/swagger-ui.html`. |
| `poi-ooxml` (Apache POI) | 5.4.0 | Exportación de reportes a Excel (`ExcelExporter`, solo administrador): franja con el logo (`resources/branding/logo-reporte.png`), encabezado en rojo, filtros e impresión lista. |
| **`guava`** (Google Guava) | 33.4.8-jre | `Preconditions` valida cantidad y stock en `InventarioService`; `Sets` compara colecciones de vehículos en `ClienteService`. |
| **`commons-lang3`** (Apache Commons Lang) | gestionada por Spring Boot | `StringUtils` normaliza placas y textos de búsqueda (`PlacaUtil`, `TextoUtil`, `SeguimientoService`). |
| **Logback** (viene con Spring Boot) | gestionada por Spring Boot | Registro de eventos en `logs/mecauto.log`: 5 MB por archivo y hasta 14 archivos. Se configura en `application.properties`. |
| `lombok` | gestionada por Spring Boot | Reduce código repetitivo (opcional, solo en compilación). |
| `spring-boot-starter-test` (JUnit 5, Mockito, AssertJ) | 3.4.5 | Pruebas unitarias y de integración (`test`). |
| `spring-security-test` | gestionada por Spring Boot | Simula usuarios con rol en las pruebas de seguridad (`test`). |
| `h2` | gestionada por Spring Boot | Base en memoria **solo para pruebas de integración**; la aplicación usa SQL Server. |

Las cuatro librerías pedidas en el curso son **Guava, Apache POI, Apache Commons Lang y Logback**.

### 1.2 Librerías del frontend (`frontend/package.json`)

| Librería | Versión | Uso |
|---|---|---|
| `react` / `react-dom` | ^18.3.1 | Interfaz de usuario por componentes. |
| `vite` (desarrollo) | ^6.0.0 | Servidor de desarrollo (con proxy `/api` → backend) y empaquetado (`npm run build`). |
| `@vitejs/plugin-react` (desarrollo) | ^4.3.4 | Soporte de JSX y recarga rápida en Vite. |

No se usan React Router ni Axios: el enrutador por hash y el cliente `fetch` son propios (`frontend/src/lib`), para depender de menos librerías.

### 1.3 Cómo se agregan o actualizan librerías

1. Edita `backend/pom.xml` (nueva `<dependency>`; si necesita versión, defínela en `<properties>`).
2. Ejecuta `mvn -q dependency:tree` para ver el árbol completo, incluidas las dependencias transitivas (por ejemplo, Logback llega con `spring-boot-starter`).
3. Ejecuta `mvn test` antes de subir el cambio.
4. En el frontend usa `npm install <paquete>` y sube `package.json` y `package-lock.json`.

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
## Equipo y responsabilidades

Proyecto del curso de Ingeniería de Sistemas — UTP.

| Integrante | Código | GitHub | Responsabilidad principal |
|---|---|---|---|
| Miguel Angel Flores Hualinga (jefe de proyecto) | U19202961 | [@MAF-171](https://github.com/MAF-171) | Contexto y problema, Lean Canvas, alcance, integración del repositorio y del documento final |
| Jairo Ernesto Ramos Hidalgo | U19311177 | [@Romeo2111](https://github.com/Romeo2111) | Alternativas de solución, especificación de requisitos (SRS) |
| Fabian Adriano Chafio Chumioque | U24220385 | [@adrianochafiochumioque-commits](https://github.com/adrianochafiochumioque-commits) | Proceso de desarrollo, arquitectura, patrones, base de datos, diagrama de clases, acta de constitución |
| Renzo Gerardo Butron Nina | U24203388 | [@rgbn120897](https://github.com/rgbn120897) | Mockups y pantallas, base de datos conectada, comprobantes, conclusiones |
| Freddy Adbre Berru Sanchez | U22220696 | [@freddykae15-sketch](https://github.com/freddykae15-sketch) | GitHub, librerías, pruebas TDD (`mvn test`) |

Cada integrante trabajó en su propia rama (`feature/...`) y se integró mediante Pull Request. El historial real está en la pestaña *Contributors* y en `git shortlog -sn`.

## Documentación por parte
- [Frontend](frontend/README.md) — React + Vite, pantallas y rutas
- [Backend](backend/README.md) — API Spring Boot, seguridad y reglas de negocio
- [Base de datos](database/README.md) — scripts, tablas e integridad


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
| `MECAUTO_MAIL_HOST` / `MECAUTO_MAIL_PORT` / `MECAUTO_MAIL_USER` / `MECAUTO_MAIL_PASSWORD` | para recuperar contraseña por correo | Servidor SMTP que envía el código de recuperación. Con Gmail: `smtp.gmail.com`, puerto `587`, tu correo y una **contraseña de aplicación** (no la clave normal). `MECAUTO_MAIL_REMITENTE` cambia el remitente (por defecto, el usuario SMTP). |
| `MECAUTO_TWILIO_SID` / `MECAUTO_TWILIO_TOKEN` / `MECAUTO_TWILIO_NUMERO` | para recuperar contraseña por SMS | Cuenta de [Twilio](https://www.twilio.com) (servicio de pago) para enviar el código por SMS al celular. Sin ellas, la recuperación por celular no envía nada. |
| `MECAUTO_CODIGO_EN_LOG` | no | `true` escribe el código de recuperación en el log cuando no hay correo/SMS configurado. **Solo para probar en tu PC**, nunca en un servidor real. |

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
4. En **Usuarios** crea al personal de recepción y a los mecánicos. Registra su **correo y/o celular**: con ellos cada persona puede recuperar su contraseña desde «¿Olvidaste tu contraseña?» en la pantalla de acceso (recibe un código de 6 dígitos que vence en 15 minutos).
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

## 10. Seguridad

La seguridad se aplica en el **servidor**; ocultar un botón en el frontend nunca se considera protección. Las clases están en `backend/src/main/java/pe/mecauto/security` y `config/SecurityConfig.java`.

| Aspecto | Qué hace MecAuto | Dónde se ve |
|---|---|---|
| **Autenticación** | Inicio de sesión con usuario y contraseña. El servidor devuelve un **JWT** firmado (HMAC) con el usuario, su id y su rol; dura 480 minutos por defecto (`MECAUTO_JWT_MINUTOS`). | `JwtService`, `JwtAuthFilter` |
| **Sin sesiones** | La API es *stateless*: no guarda sesión ni cookies. El token viaja en el encabezado `Authorization: Bearer …`. Por eso CSRF está desactivado a propósito. | `SecurityConfig` |
| **Secreto del token** | `MECAUTO_JWT_SECRET` de **32+ caracteres**, sin valor por defecto: si falta o es corta, la aplicación no arranca. | `application.properties` |
| **Contraseñas** | Se guardan solo como hash **BCrypt (costo 12)**. Entre 10 y 72 caracteres, con letras y números. No hay contraseña de administrador por defecto. | `SecurityConfig`, `UsuarioService` |
| **Autorización por rol** | Tres roles (Administrador, Recepción, Mecánico). Cada endpoint protegido lleva `@PreAuthorize` (más de 50 en los controladores); además el servicio revisa reglas como «el mecánico solo ve sus órdenes». Ver la tabla de la sección 9. | Controladores y servicios |
| **Rutas públicas** | Solo `/api/v1/public/**`, el login, la recuperación de contraseña, `/actuator/health` y Swagger. Todo lo demás exige token. Una petición sin token o sin permiso recibe 401 o 403 con un mensaje uniforme. | `SecurityConfig`, `RestSecurityHandlers` |
| **Bloqueo por intentos** | 5 intentos fallidos seguidos de un usuario bloquean su acceso por **15 minutos**. El estado vive en memoria (se reinicia con el servidor). | `LoginAttemptService` |
| **Recuperación de contraseña** | Código de **6 dígitos** generado con `SecureRandom`, enviado al correo o celular registrado. Vence en **15 minutos**, admite **5 intentos** y se guarda solo como hash. | `RecuperacionPasswordService` |
| **Validación de entrada** | Los DTO usan Bean Validation (`@Valid`, `@NotBlank`, `@Size`, `@Pattern`, `@Email`); además hay validación de documento, placa y celular en clases de utilidad. | `dto/`, `util/` |
| **SQL seguro** | JPA y `JdbcTemplate` con parámetros (`?`); nunca se concatenan datos del usuario en una consulta (evita inyección SQL). | `dao/ClienteDAOImpl` |
| **CORS** | Solo los orígenes de `MECAUTO_CORS_ORIGENES` (por defecto `http://localhost:5173`); métodos y encabezados limitados; sin credenciales. | `SecurityConfig` |
| **Sitio público** | Citas con límite por IP (5 solicitudes cada 10 minutos por defecto) y un campo trampa (`sitioWeb`) que descarta robots. El seguimiento de una orden exige el código de la orden y los últimos caracteres de la placa, y devuelve solo un resumen (estado, placa, servicio). | `PublicController`, `CitaService`, `SeguimientoService` |
| **Datos en tránsito** | La conexión a SQL Server se cifra por defecto (`encrypt=true;trustServerCertificate=false`). `trustServerCertificate=true` solo se permite en la PC de desarrollo. | `MECAUTO_DB_URL` |
| **Mínimo privilegio en la base** | El login `mecauto_app` solo tiene lectura y escritura sobre la base `MECAUTO`; no es administrador del servidor. | `database/crear-usuario-app.sql` |
| **Secretos fuera del repositorio** | Claves, JWT y SMTP llegan por variables de entorno; `backend/.env.local` queda fuera de Git (`.gitignore` ignora `.env` y `*.local`). Solo se versiona `env.example`. | `.gitignore` |
| **Errores y logs** | Un manejador global devuelve mensajes uniformes sin detalles internos (sin *stack traces*). Los logs no guardan contraseñas ni tokens; el código de recuperación solo se escribe si activas `MECAUTO_CODIGO_EN_LOG` (desarrollo). | `GlobalExceptionHandler` |
| **Integridad de datos** | Stock sin negativos (validación, bloqueo de fila y `CHECK` en SQL Server); estados de la orden sin saltos; borrado lógico para conservar el historial. | Servicios y `schema.sql` |
| **Frontend** | El token se guarda en `sessionStorage` (se borra al cerrar la pestaña) y React escapa el contenido, por lo que no se usa `dangerouslySetInnerHTML`. | `frontend/src/lib/api.js` |
| **Pruebas** | `JwtServiceTest`, `LoginAttemptServiceTest`, `RecuperacionPasswordServiceTest`, pruebas de permisos por rol y `SeguridadEndpointsIT` (rutas protegidas). | `backend/src/test` |

### Límites conocidos (conviene decirlos)

* El bloqueo por intentos y los códigos de recuperación viven en **memoria**: se pierden al reiniciar el backend y no funcionan con varias instancias a la vez.
* El token no se puede revocar antes de que venza (vence en 8 horas); cerrar sesión solo lo borra del navegador.
* La aplicación no fuerza HTTPS por sí sola: en un servidor real debe ir detrás de un proxy o servidor con certificado.
* Las credenciales de `seed-demo-completo.sql` (`Demo2026!`) son solo de demostración: cámbialas o desactívalas antes de usar datos reales.
* No son comprobantes electrónicos de SUNAT.

### Lista de comprobación antes de publicar

1. Cambiar `MECAUTO_JWT_SECRET`, la clave de `mecauto_app` y la contraseña del administrador.
2. Quitar `MECAUTO_ADMIN_*` después del primer arranque.
3. `MECAUTO_CODIGO_EN_LOG=false` y `trustServerCertificate=false`.
4. `MECAUTO_CORS_ORIGENES` con el dominio real y HTTPS delante del sistema.
5. Opcional: `MECAUTO_DOCS=false` para ocultar Swagger.

## 11. Módulos y reglas principales

* **Sitio público:** servicios y datos de contacto leídos de la base; formulario de cita con horarios reales (capacidad por franja); protección anti-spam (límite por IP y campo trampa).
* **Órdenes de trabajo:** estados `Recibida → En diagnóstico → En reparación → Completada → Entregada` (o `Cancelada`), sin saltos ni retrocesos; historial de cada cambio.
* **Inventario:** el stock **nunca queda negativo** (validación + bloqueo de fila + `CHECK` en SQL Server). Los repuestos de una orden se descuentan **una sola vez** al completarla (clave de idempotencia) y se devuelven si se cancela. Cada cambio deja un movimiento con el stock resultante. Alerta de stock bajo.
* **IGV:** los precios ya incluyen 18 %. La base imponible se calcula como `total / 1.18` y el IGV como diferencia (nunca se suma dos veces).
* **Cotizaciones:** pendiente → aprobada → convertida en orden (operación atómica). Vigencia de 15 días por defecto.
* **Comprobantes internos:** boleta (DNI/CE) o factura (RUC) con numeración correlativa sin saltos duplicados; imprimible. **No son comprobantes electrónicos de SUNAT** y así se indica en el documento.
* **Reportes:** 11 reportes con filtro de fechas, impresión y exportación a Excel (solo administrador).
* **Borrado lógico:** clientes, vehículos, servicios, repuestos y usuarios se desactivan; no se eliminan para conservar el historial.

## 12. Pruebas

```powershell
cd backend
mvn test
```

* **Pruebas unitarias** (no necesitan base de datos): IGV, estados de orden, permisos por rol, horarios, validación de documentos, placas y celulares, y las reglas de negocio de cada servicio con Mockito: clientes, vehículos, citas, órdenes de trabajo (flujo de estados e inventario), cotizaciones, comprobantes, inventario, usuarios y recuperación de contraseña, además de la exportación de reportes a Excel (`ExcelExporterTest`: logo, importes numéricos y texto seguro). También JWT, bloqueo de inicio de sesión y límite anti-spam.
* **Pruebas de integración** (`*IT.java`, H2 en memoria): protección de rutas por rol. **No usan SQL Server real.**
* No hay pruebas automáticas contra SQL Server; se verifica con `consultas.sql` y uso manual.

Qué se ejecutó realmente y qué falta: [`docs/VERIFICACION.md`](docs/VERIFICACION.md).

## 13. Solución de problemas

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

## 14. Documentación

* [`docs/ARQUITECTURA.md`](docs/ARQUITECTURA.md) — capas, modelo de datos, casos de uso, diagramas de clases y secuencia.
* [`docs/API.md`](docs/API.md) — endpoints principales y permisos.
* [`docs/PATRONES.md`](docs/PATRONES.md) — patrones y principios realmente usados.
* [`docs/VERIFICACION.md`](docs/VERIFICACION.md) — qué se probó y qué no.
