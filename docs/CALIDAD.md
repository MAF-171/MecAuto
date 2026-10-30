# Calidad: GitHub, librerías y pruebas

**Responsable:** Freddy Adbre Berru Sanchez
**Proyecto:** MecAuto

## 1. Repositorio y buenas prácticas

Repositorio: <https://github.com/MAF-171/MecAuto>

**Estructura:** `backend/` (Spring Boot, Java 21), `frontend/` (React 18 + Vite), `database/` (schema.sql, seeds y consultas), `docs/` (API, arquitectura, patrones, verificación) y `README.md`.

**Buenas prácticas**
- Ramas: `main` (estable) y ramas de trabajo por integrante (`feature/...`) que se unen con Pull Request.
- README con requisitos y pasos de ejecución.
- Código limpio, comentarios y patrones de diseño.
- Secretos fuera del repositorio (`.gitignore` y `.env.local`).
- Commits con mensaje descriptivo por cambio.

## 2. Librerías Java con Maven (`pom.xml`)

| Librería | Versión | Uso en MecAuto |
|---|---|---|
| Google Guava | 33.4.8-jre | `Preconditions` en `InventarioService` (valida cantidad y stock) y `Sets` en `ClienteService` |
| Apache POI (poi-ooxml) | 5.4.0 | `ExcelExporter`: exporta reportes a `.xlsx` |
| Apache Commons Lang 3 | gestionada por Spring Boot | `PlacaUtil` y `TextoUtil`: normalizan placas y textos de búsqueda |
| Logback | gestionada por Spring Boot | Registro en `logs/mecauto.log` (5 MB por archivo, hasta 14 archivos) |
| Spring Security + jjwt | jjwt 0.12.6 | Autenticación JWT, roles y BCrypt |
| Validation, Mail, Actuator | gestionadas por Spring Boot | Validación `@Valid`, código de recuperación por correo, salud del sistema |
| springdoc-openapi, mssql-jdbc | springdoc 2.8.6 | Swagger UI y conexión a SQL Server |
| JUnit 5, Mockito, H2 | gestionadas por Spring Boot | Pruebas unitarias y de integración (H2 solo en pruebas) |

Las cuatro librerías pedidas en el curso son Guava, Apache POI, Apache Commons Lang y Logback.

## 3. Pruebas unitarias y de integración con TDD

**Ciclo TDD:** (1) Rojo: escribir la prueba que falla; (2) Verde: código mínimo para que pase; (3) Refactorizar sin romper la prueba.

**Resultado de `mvn test` (28/10/2026):** `Tests run: 138, Failures: 0, Errors: 0, Skipped: 0` — `BUILD SUCCESS`, en 23 clases de prueba.

**Qué se prueba**
- Reglas de negocio: estados de la orden, IGV, stock y franjas horarias.
- Servicios con Mockito: clientes, vehículos, citas, órdenes, cotizaciones, comprobantes, inventario, usuarios y recuperación de contraseña.
- Seguridad: JWT, bloqueo por intentos fallidos y límite de solicitudes públicas.
- Integración (`SeguridadEndpointsIT`, con H2): protección de rutas por rol; se ejecuta con `mvn verify`.

**Ejemplo real (`EstadoOrdenTest`)**
```java
@Test @DisplayName("No se permiten saltos ni retrocesos")
void sinSaltosNiRetrocesos() {
    assertFalse(EstadoOrden.RECIBIDA.puedePasarA(EstadoOrden.COMPLETADA));
    assertFalse(EstadoOrden.EN_REPARACION.puedePasarA(EstadoOrden.EN_DIAGNOSTICO));
}
```

Cómo ejecutar:
```powershell
cd backend
mvn test
```
