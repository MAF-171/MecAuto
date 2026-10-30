# Alternativas de solución y requerimientos (SRS)

**Responsable:** Jairo Ernesto Ramos Hidalgo
**Proyecto:** MecAuto

## 1. Alternativas de solución evaluadas

| # | Alternativa | Ventajas | Desventajas |
|---|---|---|---|
| 1 | Panel administrativo clásico (menú lateral, tablas y formularios) | Curva de aprendizaje baja, desarrollo rápido | Seguimiento de órdenes poco visual |
| 2 | Tablero Kanban de órdenes (navegación superior y tarjetas por estado) | El estado se aprecia de un vistazo | Menor densidad de datos |
| 3 | Web móvil-primero (elementos grandes y barra inferior) | Uso cómodo desde tablet o celular | Menos eficiente para trabajo administrativo |

Las tres alternativas comparten las mismas pantallas base: login, panel, clientes y vehículos, órdenes e inventario.

## 2. Solución final

Combina el panel lateral (1), el tablero por estados dentro de órdenes (2) y el diseño responsivo para tablet y celular (3). Las tres se implementan con **Java 21 + Spring Boot 3.4 + SQL Server** en el backend y **React 18 + Vite** en el frontend.

## 3. Requerimientos funcionales (17 RF)

| Código | Requerimiento |
|---|---|
| RF15 | Seguridad: inicio de sesión y roles |
| RF01–RF04 | Clientes, vehículos e historial |
| RF05–RF07 | Órdenes: asignar, cambiar estado, consultar |
| RF08–RF10 | Inventario: entradas, salidas y stock |
| RF11–RF12 | Cotización y comprobantes |
| RF13–RF14 | Reportes e indicadores |
| RF16–RF17 | Sitio público: citas y seguimiento de la orden |

## 4. Requerimientos no funcionales (8 RNF)

| Código | Requerimiento |
|---|---|
| RNF01 | Interfaz web clara |
| RNF02 | Autenticación y protección |
| RNF03 | Integridad de datos |
| RNF04 | Consulta rápida |
| RNF05 | Navegadores actuales |
| RNF06 | Interfaz adaptable |
| RNF07 | Mantenibilidad |
| RNF08 | Información disponible |

> Nota: el detalle completo (descripción, prioridad y criterios de aceptación de cada requerimiento) está en el capítulo de requerimientos del documento Proyecto Final.
