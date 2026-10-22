/* ============================================================
   MecAuto - Consultas de VERIFICACIÓN (solo lectura)
   Úsalas en SSMS para comprobar que lo que haces en la aplicación
   queda guardado en SQL Server. No modifican datos.
   ============================================================ */
USE MECAUTO;
GO

/* 1. Conteo rápido de tablas principales */
SELECT 'usuario' AS tabla, COUNT(*) AS filas FROM dbo.usuario
UNION ALL SELECT 'cliente', COUNT(*) FROM dbo.cliente
UNION ALL SELECT 'vehiculo', COUNT(*) FROM dbo.vehiculo
UNION ALL SELECT 'servicio', COUNT(*) FROM dbo.servicio
UNION ALL SELECT 'repuesto', COUNT(*) FROM dbo.repuesto
UNION ALL SELECT 'cita', COUNT(*) FROM dbo.cita
UNION ALL SELECT 'orden_trabajo', COUNT(*) FROM dbo.orden_trabajo
UNION ALL SELECT 'cotizacion', COUNT(*) FROM dbo.cotizacion
UNION ALL SELECT 'comprobante', COUNT(*) FROM dbo.comprobante;

/* 2. Usuarios (nunca se muestra el hash) */
SELECT id_usuario, username, nombre, rol, activo, creado_en FROM dbo.usuario ORDER BY id_usuario;

/* 3. Últimas citas (las de la web llevan origen = PUBLICA) */
SELECT TOP 20 c.codigo, c.nombre_contacto, c.telefono_contacto, c.placa, s.nombre AS servicio,
       c.fecha_hora, c.estado, c.origen, c.creado_en
FROM dbo.cita c JOIN dbo.servicio s ON s.id_servicio = c.id_servicio
ORDER BY c.id_cita DESC;

/* 4. Órdenes con cliente, vehículo y mecánico */
SELECT TOP 20 o.codigo, o.estado, cl.nombre AS cliente, v.placa, u.nombre AS mecanico,
       o.fecha_ingreso, o.fecha_cierre, o.total
FROM dbo.orden_trabajo o
JOIN dbo.cliente cl ON cl.id_cliente = o.id_cliente
JOIN dbo.vehiculo v ON v.id_vehiculo = o.id_vehiculo
LEFT JOIN dbo.usuario u ON u.id_usuario = o.id_mecanico
ORDER BY o.id_orden DESC;

/* 5. Historial de estados de una orden (cambia el código) */
SELECT o.codigo, h.estado_anterior, h.estado_nuevo, u.nombre AS usuario, h.comentario, h.fecha
FROM dbo.orden_historial h
JOIN dbo.orden_trabajo o ON o.id_orden = h.id_orden
LEFT JOIN dbo.usuario u ON u.id_usuario = h.id_usuario
WHERE o.codigo = 'OT-0001'
ORDER BY h.id_historial;

/* 6. Inventario: repuestos con stock bajo (stock <= mínimo) */
SELECT codigo, nombre, stock_actual, stock_minimo
FROM dbo.repuesto WHERE activo = 1 AND stock_actual <= stock_minimo ORDER BY stock_actual;

/* 7. Movimientos de inventario recientes (cantidad con signo) */
SELECT TOP 30 m.fecha, r.codigo, m.tipo, m.cantidad, m.stock_resultante, m.motivo, m.id_orden
FROM dbo.movimiento_inventario m JOIN dbo.repuesto r ON r.id_repuesto = m.id_repuesto
ORDER BY m.id_movimiento DESC;

/* 8. COMPROBACIÓN DE CONSISTENCIA: el stock de cada repuesto debe ser igual a la suma de sus movimientos
      (solo válido si todo el stock inicial se registró como movimiento). Filas devueltas = diferencias. */
SELECT r.codigo, r.stock_actual, SUM(m.cantidad) AS suma_movimientos
FROM dbo.repuesto r JOIN dbo.movimiento_inventario m ON m.id_repuesto = r.id_repuesto
GROUP BY r.codigo, r.stock_actual
HAVING r.stock_actual <> SUM(m.cantidad);

/* 9. COMPROBACIÓN: no debe haber stock negativo (el CHECK lo impide; esto debe devolver 0 filas) */
SELECT codigo, stock_actual FROM dbo.repuesto WHERE stock_actual < 0;

/* 10. COMPROBACIÓN: no debe haber dos descuentos con la misma clave (idempotencia). 0 filas = correcto */
SELECT clave_idempotencia, COUNT(*) AS veces
FROM dbo.movimiento_inventario WHERE clave_idempotencia IS NOT NULL
GROUP BY clave_idempotencia HAVING COUNT(*) > 1;

/* 11. Cotizaciones y su desglose de IGV (base + IGV debe ser igual al total) */
SELECT codigo, estado, base_imponible, igv, total,
       CASE WHEN base_imponible + igv = total THEN 'OK' ELSE 'REVISAR' END AS cuadra
FROM dbo.cotizacion ORDER BY id_cotizacion DESC;

/* 12. Comprobantes internos (numeración sin duplicados por tipo+serie+número) */
SELECT tipo, serie, numero, estado, cliente_nombre, base_imponible, igv, total, fecha_emision
FROM dbo.comprobante ORDER BY id_comprobante DESC;

/* 13. COMPROBACIÓN: una orden no debe tener dos comprobantes EMITIDOS. 0 filas = correcto */
SELECT id_orden, COUNT(*) AS emitidos FROM dbo.comprobante WHERE estado = 'EMITIDO'
GROUP BY id_orden HAVING COUNT(*) > 1;

/* 14. Ingresos por mes (solo comprobantes emitidos, total con IGV incluido) */
SELECT FORMAT(fecha_emision, 'yyyy-MM') AS mes, COUNT(*) AS comprobantes, SUM(total) AS ingresos
FROM dbo.comprobante WHERE estado = 'EMITIDO'
GROUP BY FORMAT(fecha_emision, 'yyyy-MM') ORDER BY mes DESC;

/* 15. Correlativos actuales */
SELECT nombre, ultimo FROM dbo.secuencia ORDER BY nombre;

/* 16. Configuración del taller */
SELECT clave, valor FROM dbo.configuracion_taller ORDER BY clave;
GO
