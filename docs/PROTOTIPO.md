# Prototipo UX/UI, sistema en funcionamiento y conclusiones

**Responsable:** Renzo Gerardo Butron Nina
**Proyecto:** MecAuto

## 1. Mockup de las ventanas

Se diseñó un mockup de las ventanas que atienden los procesos del sistema, con flechas que muestran cómo se conectan:

- **Sistema Ventas:** menú principal con *Mantenimiento* (Clientes, Productos, Configuración) y *Ventas* (Boleta).
- **Clientes / Productos:** ventana de búsqueda con tabla y botones Agregar, Editar, Borrar y Cerrar. Cada acción abre su ventana (*Nuevo Cliente*, *Nuevo Producto*) o un diálogo de confirmación (*¿Desea borrar el registro?*).
- **Boleta:** comprobante generado desde el sistema.
- **Configuración:** conexión a la base de datos (SQL Server, puerto 1433).

## 2. Pantallas finales del sistema

Del mockup se pasó a la aplicación real (React 18 + Vite) con identidad roja y negra y la llanta de la marca en el logo.

1. **Inicio de sesión**, con el enlace «¿Olvidaste tu contraseña?».
2. **Recuperar contraseña**: código de 6 dígitos enviado al correo o celular, que vence en 15 minutos.
3. **Panel principal** con indicadores.
4. **Clientes y vehículos.**
5. **Órdenes de trabajo**, en lista y en tablero por estados.
6. **Inventario de repuestos** con alerta de stock mínimo.
7. **Reportes de gestión** (exportables a Excel).
8. **Gestión de usuarios** (correo y celular para recuperar contraseña).
9. **Sitio web público:** inicio, solicitud de cita en línea y seguimiento de la orden.
10. **Versión para celular.**

## 3. Pantalla conectada a la base de datos

La pantalla de *Órdenes de trabajo* lee y escribe en SQL Server a través de la API:

| Dato en pantalla | Origen |
|---|---|
| N.º de orden | `orden_trabajo.codigo` — `GET /api/v1/ordenes` |
| Placa | `vehiculo.placa` — `GET /api/v1/ordenes` |
| Estado | `orden_trabajo.estado` — `POST /api/v1/ordenes/{id}/estado` |
| Nueva orden | `INSERT` en `orden_trabajo` — `POST /api/v1/ordenes` |

Todas las rutas usan el prefijo `/api/v1` y exigen el token JWT según el rol.

## 4. Comprobantes

- **Boleta:** para personas naturales (DNI o carné de extranjería).
- **Factura:** para empresas con RUC; incluye razón social y valor de venta e IGV.
- **Reglas:** IGV 18 %; un solo comprobante por orden y solo en estado Completada; numeración `B001-00000001` / `F001-00000001`.
- **Nota:** son comprobantes internos de demostración; no tienen validez tributaria ante SUNAT.

## 5. Conclusiones

- **Problema:** gestión manual del taller, sin historial centralizado, sin control de órdenes ni de inventario.
- **Solución:** sistema web integral en funcionamiento (clientes, vehículos, órdenes, inventario, comprobantes, reportes, roles y sitio público).
- **Logros del avance 3:** arquitectura por capas, patrones MVC/DAO/SOLID, seguridad con JWT, repositorio en GitHub y 138 pruebas automáticas sin fallos.
- **Siguiente paso:** pruebas manuales por rol, despliegue, manuales y entrega final el 30/11/2026.
