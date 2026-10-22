/* ============================================================
   MecAuto - DATOS DE DEMOSTRACIÓN COMPLETOS (clientes, vehículos, mecánicos,
   órdenes de trabajo, inventario, boletas y factura).
   Todos los datos son FICTICIOS (menos los datos del taller). Sirven para la demostración.
   Ejecutar DESPUÉS de schema.sql y seed.sql (el administrador se crea al iniciar el backend).
   Usuarios de demostración (contraseña: Demo2026!): recepcion, jperez, dhans, mfeels.
   Se puede ejecutar una sola vez: si ya existe el cliente demo, no hace nada.
   ============================================================ */
USE MECAUTO;
GO
SET NOCOUNT ON;
SET XACT_ABORT ON;
IF EXISTS (SELECT 1 FROM dbo.cliente WHERE nro_documento = '45871236')
BEGIN
    PRINT 'Los datos de demostración ya estaban cargados. No se hizo ningún cambio.';
    SET NOEXEC ON;
END
GO
BEGIN TRAN;
DECLARE @hoy DATETIME2(0) = CAST(CAST(SYSDATETIME() AS DATE) AS DATETIME2(0));

/* Datos reales del taller (solo completa los que estén vacíos) */
UPDATE dbo.configuracion_taller SET valor = N'20061305069' WHERE clave = 'ruc' AND (valor IS NULL OR valor = '');
UPDATE dbo.configuracion_taller SET valor = N'Ctra. Central 1676, Chaclacayo 15472' WHERE clave = 'direccion' AND (valor IS NULL OR valor = '');
UPDATE dbo.configuracion_taller SET valor = N'(51) 944058789' WHERE clave = 'telefono' AND (valor IS NULL OR valor = '');
UPDATE dbo.configuracion_taller SET valor = N'administracion_mec@mecauto.com' WHERE clave = 'correo' AND (valor IS NULL OR valor = '');

/* Usuarios: recepción y mecánicos */
IF NOT EXISTS (SELECT 1 FROM dbo.usuario WHERE username = N'recepcion') INSERT INTO dbo.usuario (username, password_hash, nombre, rol, activo) VALUES (N'recepcion', '$2b$12$MgcA3BHkDdrc5O4wTDgAzuoHqZPKYvw0wgmSMwpfsqOLIru7jyW5K', N'Ángel Flores', 'RECEPCION', 1);
IF NOT EXISTS (SELECT 1 FROM dbo.usuario WHERE username = N'jperez') INSERT INTO dbo.usuario (username, password_hash, nombre, rol, activo) VALUES (N'jperez', '$2b$12$MgcA3BHkDdrc5O4wTDgAzuoHqZPKYvw0wgmSMwpfsqOLIru7jyW5K', N'Juan Pérez', 'MECANICO', 1);
IF NOT EXISTS (SELECT 1 FROM dbo.usuario WHERE username = N'dhans') INSERT INTO dbo.usuario (username, password_hash, nombre, rol, activo) VALUES (N'dhans', '$2b$12$MgcA3BHkDdrc5O4wTDgAzuoHqZPKYvw0wgmSMwpfsqOLIru7jyW5K', N'David Hans', 'MECANICO', 1);
IF NOT EXISTS (SELECT 1 FROM dbo.usuario WHERE username = N'mfeels') INSERT INTO dbo.usuario (username, password_hash, nombre, rol, activo) VALUES (N'mfeels', '$2b$12$MgcA3BHkDdrc5O4wTDgAzuoHqZPKYvw0wgmSMwpfsqOLIru7jyW5K', N'Mauro Feels', 'MECANICO', 1);

/* Proveedores y repuestos */
IF NOT EXISTS (SELECT 1 FROM dbo.proveedor WHERE ruc = '20601234567') INSERT INTO dbo.proveedor (razon_social, ruc, telefono) VALUES (N'Autopartes Lima S.A.C.', '20601234567', '014455660');
IF NOT EXISTS (SELECT 1 FROM dbo.proveedor WHERE ruc = '20602345678') INSERT INTO dbo.proveedor (razon_social, ruc, telefono) VALUES (N'Lubricantes del Sur S.A.C.', '20602345678', '014455661');
IF NOT EXISTS (SELECT 1 FROM dbo.repuesto WHERE codigo = 'FIL-001') INSERT INTO dbo.repuesto (id_proveedor, codigo, nombre, categoria, precio_compra, precio_venta, stock_actual, stock_minimo) VALUES ((SELECT id_proveedor FROM dbo.proveedor WHERE ruc = '20601234567'), 'FIL-001', N'Filtro de aceite', N'Filtros', 18, 35, 41, 3);
IF NOT EXISTS (SELECT 1 FROM dbo.repuesto WHERE codigo = 'ACE-5W30') INSERT INTO dbo.repuesto (id_proveedor, codigo, nombre, categoria, precio_compra, precio_venta, stock_actual, stock_minimo) VALUES ((SELECT id_proveedor FROM dbo.proveedor WHERE ruc = '20602345678'), 'ACE-5W30', N'Aceite sintético 5W-30 (1 L)', N'Lubricantes', 32, 52, 12, 3);
IF NOT EXISTS (SELECT 1 FROM dbo.repuesto WHERE codigo = 'PAS-DEL') INSERT INTO dbo.repuesto (id_proveedor, codigo, nombre, categoria, precio_compra, precio_venta, stock_actual, stock_minimo) VALUES ((SELECT id_proveedor FROM dbo.proveedor WHERE ruc = '20601234567'), 'PAS-DEL', N'Pastillas de freno delanteras', N'Frenos', 85, 160, 4, 5);
IF NOT EXISTS (SELECT 1 FROM dbo.repuesto WHERE codigo = 'BAT-12V') INSERT INTO dbo.repuesto (id_proveedor, codigo, nombre, categoria, precio_compra, precio_venta, stock_actual, stock_minimo) VALUES ((SELECT id_proveedor FROM dbo.proveedor WHERE ruc = '20602345678'), 'BAT-12V', N'Batería 12 V 45 Ah', N'Eléctrico', 280, 420, 1, 2);
IF NOT EXISTS (SELECT 1 FROM dbo.repuesto WHERE codigo = 'BUJ-NGK') INSERT INTO dbo.repuesto (id_proveedor, codigo, nombre, categoria, precio_compra, precio_venta, stock_actual, stock_minimo) VALUES ((SELECT id_proveedor FROM dbo.proveedor WHERE ruc = '20601234567'), 'BUJ-NGK', N'Bujías NGK (juego x4)', N'Encendido', 44, 85, 13, 4);
IF NOT EXISTS (SELECT 1 FROM dbo.repuesto WHERE codigo = 'FIL-AIRE') INSERT INTO dbo.repuesto (id_proveedor, codigo, nombre, categoria, precio_compra, precio_venta, stock_actual, stock_minimo) VALUES ((SELECT id_proveedor FROM dbo.proveedor WHERE ruc = '20601234567'), 'FIL-AIRE', N'Filtro de aire', N'Filtros', 22, 45, 17, 3);
IF NOT EXISTS (SELECT 1 FROM dbo.repuesto WHERE codigo = 'LIQ-FR') INSERT INTO dbo.repuesto (id_proveedor, codigo, nombre, categoria, precio_compra, precio_venta, stock_actual, stock_minimo) VALUES ((SELECT id_proveedor FROM dbo.proveedor WHERE ruc = '20602345678'), 'LIQ-FR', N'Líquido de frenos DOT 4', N'Frenos', 15, 30, 3, 4);

/* Clientes y vehículos */
INSERT INTO dbo.cliente (tipo_documento, nro_documento, nombre, telefono, correo, direccion) SELECT 'DNI','45871236',N'Carlos Mendoza Rojas','987654321',N'carlos.mendoza@correo.example',N'Av. Arequipa 1450, Lima' WHERE NOT EXISTS (SELECT 1 FROM dbo.cliente WHERE nro_documento = '45871236');
INSERT INTO dbo.cliente (tipo_documento, nro_documento, nombre, telefono, correo, direccion) SELECT 'DNI','70123458',N'María Torres Vega','955123456',N'maria.torres@correo.example',N'Jr. Cusco 320, Miraflores' WHERE NOT EXISTS (SELECT 1 FROM dbo.cliente WHERE nro_documento = '70123458');
INSERT INTO dbo.cliente (tipo_documento, nro_documento, nombre, telefono, correo, direccion) SELECT 'RUC','20556789011',N'Transportes Andinos S.A.C.','014456789',N'contacto@andinos.example',N'Av. Argentina 2200, Callao' WHERE NOT EXISTS (SELECT 1 FROM dbo.cliente WHERE nro_documento = '20556789011');
INSERT INTO dbo.cliente (tipo_documento, nro_documento, nombre, telefono, correo, direccion) SELECT 'DNI','41236587',N'Luis Paredes Quispe','944321987',N'luis.paredes@correo.example',N'Calle Los Pinos 118, San Borja' WHERE NOT EXISTS (SELECT 1 FROM dbo.cliente WHERE nro_documento = '41236587');
INSERT INTO dbo.cliente (tipo_documento, nro_documento, nombre, telefono, correo, direccion) SELECT 'CE','002458713',N'Ana Gómez Silva','933214765',N'ana.gomez@correo.example',N'Av. Brasil 980, Jesús María' WHERE NOT EXISTS (SELECT 1 FROM dbo.cliente WHERE nro_documento = '002458713');
INSERT INTO dbo.cliente (tipo_documento, nro_documento, nombre, telefono, correo, direccion) SELECT 'DNI','46325871',N'Jorge Salazar León','922876543',N'jorge.salazar@correo.example',N'Av. Universitaria 1700, Pueblo Libre' WHERE NOT EXISTS (SELECT 1 FROM dbo.cliente WHERE nro_documento = '46325871');
INSERT INTO dbo.vehiculo (id_cliente, placa, marca, modelo, anio, color, kilometraje) SELECT (SELECT id_cliente FROM dbo.cliente WHERE nro_documento = '45871236'), 'ABC-123', N'Toyota', N'Yaris', 2018, N'Plata', 64200 WHERE NOT EXISTS (SELECT 1 FROM dbo.vehiculo WHERE placa = 'ABC-123');
INSERT INTO dbo.vehiculo (id_cliente, placa, marca, modelo, anio, color, kilometraje) SELECT (SELECT id_cliente FROM dbo.cliente WHERE nro_documento = '70123458'), 'XYZ-789', N'Hyundai', N'Accent', 2020, N'Blanco', 38900 WHERE NOT EXISTS (SELECT 1 FROM dbo.vehiculo WHERE placa = 'XYZ-789');
INSERT INTO dbo.vehiculo (id_cliente, placa, marca, modelo, anio, color, kilometraje) SELECT (SELECT id_cliente FROM dbo.cliente WHERE nro_documento = '20556789011'), 'F5K-029', N'Kia', N'Rio', 2019, N'Rojo', 51300 WHERE NOT EXISTS (SELECT 1 FROM dbo.vehiculo WHERE placa = 'F5K-029');
INSERT INTO dbo.vehiculo (id_cliente, placa, marca, modelo, anio, color, kilometraje) SELECT (SELECT id_cliente FROM dbo.cliente WHERE nro_documento = '20556789011'), 'B7P-402', N'Nissan', N'Sentra', 2017, N'Negro', 87450 WHERE NOT EXISTS (SELECT 1 FROM dbo.vehiculo WHERE placa = 'B7P-402');
INSERT INTO dbo.vehiculo (id_cliente, placa, marca, modelo, anio, color, kilometraje) SELECT (SELECT id_cliente FROM dbo.cliente WHERE nro_documento = '41236587'), 'C2M-611', N'Chevrolet', N'Sail', 2016, N'Azul', 99120 WHERE NOT EXISTS (SELECT 1 FROM dbo.vehiculo WHERE placa = 'C2M-611');
INSERT INTO dbo.vehiculo (id_cliente, placa, marca, modelo, anio, color, kilometraje) SELECT (SELECT id_cliente FROM dbo.cliente WHERE nro_documento = '002458713'), 'D9R-370', N'Suzuki', N'Swift', 2021, N'Gris', 22800 WHERE NOT EXISTS (SELECT 1 FROM dbo.vehiculo WHERE placa = 'D9R-370');
INSERT INTO dbo.vehiculo (id_cliente, placa, marca, modelo, anio, color, kilometraje) SELECT (SELECT id_cliente FROM dbo.cliente WHERE nro_documento = '46325871'), 'H4T-815', N'Toyota', N'Hilux', 2019, N'Blanco', 76500 WHERE NOT EXISTS (SELECT 1 FROM dbo.vehiculo WHERE placa = 'H4T-815');

/* Órdenes de trabajo (los códigos continúan la numeración existente) */
DECLARE @n INT = (SELECT ultimo FROM dbo.secuencia WHERE nombre = 'OT');
DECLARE @oid BIGINT, @cid BIGINT, @vid BIGINT, @mid BIGINT, @rid BIGINT, @cod VARCHAR(12), @uid BIGINT, @cmp BIGINT, @sec INT, @stk INT;
DECLARE @recep BIGINT = (SELECT id_usuario FROM dbo.usuario WHERE username = 'recepcion');

-- Orden 1: Luz de check engine encendida
SET @n += 1; SET @cod = 'OT-' + RIGHT('0000' + CAST(@n AS VARCHAR(10)), 4);
SELECT @vid = id_vehiculo, @cid = id_cliente FROM dbo.vehiculo WHERE placa = 'ABC-123';
SET @mid = (SELECT id_usuario FROM dbo.usuario WHERE username = 'jperez');
INSERT INTO dbo.orden_trabajo (codigo, id_cliente, id_vehiculo, id_mecanico, falla, diagnostico, trabajos_realizados, kilometraje_ingreso, estado, fecha_ingreso, fecha_entrega_est, fecha_cierre, mano_obra, descuento, total, id_creado_por) SELECT @cod, @cid, @vid, @mid, N'Luz de check engine encendida', NULL, NULL, kilometraje, 'RECIBIDA', DATEADD(MINUTE,555,DATEADD(DAY,-0,@hoy)), DATEADD(DAY,1,CAST(@hoy AS DATE)), NULL, 40, 0, 120, @recep FROM dbo.vehiculo WHERE id_vehiculo = @vid;
SET @oid = SCOPE_IDENTITY();
INSERT INTO dbo.detalle_orden_servicio (id_orden, id_servicio, descripcion, cantidad, precio_unitario) SELECT @oid, id_servicio, nombre, 1, 80 FROM dbo.servicio WHERE nombre = N'Diagnóstico computarizado';
INSERT INTO dbo.orden_historial (id_orden, estado_anterior, estado_nuevo, id_usuario, comentario, fecha) VALUES (@oid, NULL, 'RECIBIDA', @recep, 'Orden registrada', DATEADD(MINUTE,555,DATEADD(DAY,-0,@hoy)));

-- Orden 2: Cambio de aceite y filtros
SET @n += 1; SET @cod = 'OT-' + RIGHT('0000' + CAST(@n AS VARCHAR(10)), 4);
SELECT @vid = id_vehiculo, @cid = id_cliente FROM dbo.vehiculo WHERE placa = 'XYZ-789';
SET @mid = (SELECT id_usuario FROM dbo.usuario WHERE username = 'dhans');
INSERT INTO dbo.orden_trabajo (codigo, id_cliente, id_vehiculo, id_mecanico, falla, diagnostico, trabajos_realizados, kilometraje_ingreso, estado, fecha_ingreso, fecha_entrega_est, fecha_cierre, mano_obra, descuento, total, id_creado_por) SELECT @cod, @cid, @vid, @mid, N'Cambio de aceite y filtros', N'Se revisan niveles y estado del filtro; se programa el cambio.', NULL, kilometraje, 'EN_DIAGNOSTICO', DATEADD(MINUTE,615,DATEADD(DAY,-1,@hoy)), DATEADD(DAY,2,CAST(@hoy AS DATE)), NULL, 30.5, 0, 185.5, @recep FROM dbo.vehiculo WHERE id_vehiculo = @vid;
SET @oid = SCOPE_IDENTITY();
INSERT INTO dbo.detalle_orden_servicio (id_orden, id_servicio, descripcion, cantidad, precio_unitario) SELECT @oid, id_servicio, nombre, 1, 120 FROM dbo.servicio WHERE nombre = N'Cambio de aceite y filtros';
INSERT INTO dbo.detalle_orden_repuesto (id_orden, id_repuesto, cantidad, precio_unitario, descontado) SELECT @oid, id_repuesto, 1, precio_venta, 0 FROM dbo.repuesto WHERE codigo = 'FIL-001';
INSERT INTO dbo.orden_historial (id_orden, estado_anterior, estado_nuevo, id_usuario, comentario, fecha) VALUES (@oid, NULL, 'RECIBIDA', @recep, 'Orden registrada', DATEADD(MINUTE,615,DATEADD(DAY,-1,@hoy)));
INSERT INTO dbo.orden_historial (id_orden, estado_anterior, estado_nuevo, id_usuario, comentario, fecha) VALUES (@oid, 'RECIBIDA', 'EN_DIAGNOSTICO', ISNULL(@mid, @recep), NULL, DATEADD(MINUTE,735,DATEADD(DAY,-1,@hoy)));

-- Orden 3: Ruido al frenar
SET @n += 1; SET @cod = 'OT-' + RIGHT('0000' + CAST(@n AS VARCHAR(10)), 4);
SELECT @vid = id_vehiculo, @cid = id_cliente FROM dbo.vehiculo WHERE placa = 'F5K-029';
SET @mid = (SELECT id_usuario FROM dbo.usuario WHERE username = 'jperez');
INSERT INTO dbo.orden_trabajo (codigo, id_cliente, id_vehiculo, id_mecanico, falla, diagnostico, trabajos_realizados, kilometraje_ingreso, estado, fecha_ingreso, fecha_entrega_est, fecha_cierre, mano_obra, descuento, total, id_creado_por) SELECT @cod, @cid, @vid, @mid, N'Ruido al frenar', N'Pastillas delanteras desgastadas; discos dentro de tolerancia.', NULL, kilometraje, 'EN_REPARACION', DATEADD(MINUTE,675,DATEADD(DAY,-2,@hoy)), DATEADD(DAY,3,CAST(@hoy AS DATE)), NULL, 80, 0, 420, @recep FROM dbo.vehiculo WHERE id_vehiculo = @vid;
SET @oid = SCOPE_IDENTITY();
INSERT INTO dbo.detalle_orden_servicio (id_orden, id_servicio, descripcion, cantidad, precio_unitario) SELECT @oid, id_servicio, nombre, 1, 150 FROM dbo.servicio WHERE nombre = N'Sistema de frenos';
INSERT INTO dbo.detalle_orden_repuesto (id_orden, id_repuesto, cantidad, precio_unitario, descontado) SELECT @oid, id_repuesto, 1, precio_venta, 0 FROM dbo.repuesto WHERE codigo = 'PAS-DEL';
INSERT INTO dbo.detalle_orden_repuesto (id_orden, id_repuesto, cantidad, precio_unitario, descontado) SELECT @oid, id_repuesto, 1, precio_venta, 0 FROM dbo.repuesto WHERE codigo = 'LIQ-FR';
INSERT INTO dbo.orden_historial (id_orden, estado_anterior, estado_nuevo, id_usuario, comentario, fecha) VALUES (@oid, NULL, 'RECIBIDA', @recep, 'Orden registrada', DATEADD(MINUTE,675,DATEADD(DAY,-2,@hoy)));
INSERT INTO dbo.orden_historial (id_orden, estado_anterior, estado_nuevo, id_usuario, comentario, fecha) VALUES (@oid, 'RECIBIDA', 'EN_DIAGNOSTICO', ISNULL(@mid, @recep), NULL, DATEADD(MINUTE,795,DATEADD(DAY,-2,@hoy)));
INSERT INTO dbo.orden_historial (id_orden, estado_anterior, estado_nuevo, id_usuario, comentario, fecha) VALUES (@oid, 'EN_DIAGNOSTICO', 'EN_REPARACION', ISNULL(@mid, @recep), NULL, DATEADD(MINUTE,915,DATEADD(DAY,-2,@hoy)));

-- Orden 4: Alineación y balanceo
SET @n += 1; SET @cod = 'OT-' + RIGHT('0000' + CAST(@n AS VARCHAR(10)), 4);
SELECT @vid = id_vehiculo, @cid = id_cliente FROM dbo.vehiculo WHERE placa = 'B7P-402';
SET @mid = NULL;
INSERT INTO dbo.orden_trabajo (codigo, id_cliente, id_vehiculo, id_mecanico, falla, diagnostico, trabajos_realizados, kilometraje_ingreso, estado, fecha_ingreso, fecha_entrega_est, fecha_cierre, mano_obra, descuento, total, id_creado_por) SELECT @cod, @cid, @vid, @mid, N'Alineación y balanceo', N'Desalineación leve en el eje delantero.', N'Alineación y balanceo de las cuatro ruedas; cambio de filtro de aire.', kilometraje, 'COMPLETADA', DATEADD(MINUTE,735,DATEADD(DAY,-3,@hoy)), DATEADD(DAY,1,CAST(@hoy AS DATE)), DATEADD(MINUTE,1095,DATEADD(DAY,-3,@hoy)), 25, 0, 160, @recep FROM dbo.vehiculo WHERE id_vehiculo = @vid;
SET @oid = SCOPE_IDENTITY();
INSERT INTO dbo.detalle_orden_servicio (id_orden, id_servicio, descripcion, cantidad, precio_unitario) SELECT @oid, id_servicio, nombre, 1, 90 FROM dbo.servicio WHERE nombre = N'Alineación y balanceo';
INSERT INTO dbo.detalle_orden_repuesto (id_orden, id_repuesto, cantidad, precio_unitario, descontado) SELECT @oid, id_repuesto, 1, precio_venta, 1 FROM dbo.repuesto WHERE codigo = 'FIL-AIRE';
SELECT @rid = id_repuesto FROM dbo.repuesto WHERE codigo = 'FIL-AIRE';
INSERT INTO dbo.movimiento_inventario (id_repuesto, tipo, cantidad, stock_resultante, motivo, id_orden, id_usuario, fecha, clave_idempotencia) VALUES (@rid, 'SALIDA', -1, 16, 'Salida por orden de trabajo ' + @cod, @oid, @mid, DATEADD(MINUTE,1095,DATEADD(DAY,-3,@hoy)), 'demo-' + @cod + '-FIL-AIRE');
INSERT INTO dbo.orden_historial (id_orden, estado_anterior, estado_nuevo, id_usuario, comentario, fecha) VALUES (@oid, NULL, 'RECIBIDA', @recep, 'Orden registrada', DATEADD(MINUTE,735,DATEADD(DAY,-3,@hoy)));
INSERT INTO dbo.orden_historial (id_orden, estado_anterior, estado_nuevo, id_usuario, comentario, fecha) VALUES (@oid, 'RECIBIDA', 'EN_DIAGNOSTICO', ISNULL(@mid, @recep), NULL, DATEADD(MINUTE,855,DATEADD(DAY,-3,@hoy)));
INSERT INTO dbo.orden_historial (id_orden, estado_anterior, estado_nuevo, id_usuario, comentario, fecha) VALUES (@oid, 'EN_DIAGNOSTICO', 'EN_REPARACION', ISNULL(@mid, @recep), NULL, DATEADD(MINUTE,975,DATEADD(DAY,-3,@hoy)));
INSERT INTO dbo.orden_historial (id_orden, estado_anterior, estado_nuevo, id_usuario, comentario, fecha) VALUES (@oid, 'EN_REPARACION', 'COMPLETADA', @recep, NULL, DATEADD(MINUTE,1095,DATEADD(DAY,-3,@hoy)));
SET @sec = (SELECT ultimo FROM dbo.secuencia WHERE nombre = 'F001') + 1; UPDATE dbo.secuencia SET ultimo = @sec WHERE nombre = 'F001';
INSERT INTO dbo.comprobante (id_orden, tipo, serie, numero, cliente_nombre, cliente_tipo_doc, cliente_documento, vehiculo_placa, base_imponible, igv, total, metodo_pago, estado, fecha_emision, id_emitido_por) VALUES (@oid, 'FACTURA', 'F001', RIGHT('00000000' + CAST(@sec AS VARCHAR(10)), 8), N'Transportes Andinos S.A.C.', 'RUC', '20556789011', 'B7P-402', 135.59, 24.41, 160, 'TRANSFERENCIA', 'EMITIDO', DATEADD(MINUTE,1095,DATEADD(DAY,-3,@hoy)), @recep);
SET @cmp = SCOPE_IDENTITY();
INSERT INTO dbo.detalle_comprobante (id_comprobante, descripcion, cantidad, precio_unitario, importe) VALUES (@cmp, N'Alineación y balanceo', 1, 90, 90);
INSERT INTO dbo.detalle_comprobante (id_comprobante, descripcion, cantidad, precio_unitario, importe) VALUES (@cmp, N'Filtro de aire (FIL-AIRE)', 1, 45, 45);
INSERT INTO dbo.detalle_comprobante (id_comprobante, descripcion, cantidad, precio_unitario, importe) VALUES (@cmp, N'Mano de obra', 1, 25, 25);

-- Orden 5: Mantenimiento 60 000 km
SET @n += 1; SET @cod = 'OT-' + RIGHT('0000' + CAST(@n AS VARCHAR(10)), 4);
SELECT @vid = id_vehiculo, @cid = id_cliente FROM dbo.vehiculo WHERE placa = 'C2M-611';
SET @mid = (SELECT id_usuario FROM dbo.usuario WHERE username = 'mfeels');
INSERT INTO dbo.orden_trabajo (codigo, id_cliente, id_vehiculo, id_mecanico, falla, diagnostico, trabajos_realizados, kilometraje_ingreso, estado, fecha_ingreso, fecha_entrega_est, fecha_cierre, mano_obra, descuento, total, id_creado_por) SELECT @cod, @cid, @vid, @mid, N'Mantenimiento 60 000 km', N'Mantenimiento programado según kilometraje.', N'Cambio de aceite y filtros, bujías y revisión general.', kilometraje, 'ENTREGADA', DATEADD(MINUTE,555,DATEADD(DAY,-4,@hoy)), DATEADD(DAY,1,CAST(@hoy AS DATE)), DATEADD(MINUTE,915,DATEADD(DAY,-4,@hoy)), 27, 0, 650, @recep FROM dbo.vehiculo WHERE id_vehiculo = @vid;
SET @oid = SCOPE_IDENTITY();
INSERT INTO dbo.detalle_orden_servicio (id_orden, id_servicio, descripcion, cantidad, precio_unitario) SELECT @oid, id_servicio, nombre, 1, 250 FROM dbo.servicio WHERE nombre = N'Mantenimiento preventivo';
INSERT INTO dbo.detalle_orden_repuesto (id_orden, id_repuesto, cantidad, precio_unitario, descontado) SELECT @oid, id_repuesto, 1, precio_venta, 1 FROM dbo.repuesto WHERE codigo = 'FIL-001';
SELECT @rid = id_repuesto FROM dbo.repuesto WHERE codigo = 'FIL-001';
INSERT INTO dbo.movimiento_inventario (id_repuesto, tipo, cantidad, stock_resultante, motivo, id_orden, id_usuario, fecha, clave_idempotencia) VALUES (@rid, 'SALIDA', -1, 40, 'Salida por orden de trabajo ' + @cod, @oid, @mid, DATEADD(MINUTE,915,DATEADD(DAY,-4,@hoy)), 'demo-' + @cod + '-FIL-001');
INSERT INTO dbo.detalle_orden_repuesto (id_orden, id_repuesto, cantidad, precio_unitario, descontado) SELECT @oid, id_repuesto, 4, precio_venta, 1 FROM dbo.repuesto WHERE codigo = 'ACE-5W30';
SELECT @rid = id_repuesto FROM dbo.repuesto WHERE codigo = 'ACE-5W30';
INSERT INTO dbo.movimiento_inventario (id_repuesto, tipo, cantidad, stock_resultante, motivo, id_orden, id_usuario, fecha, clave_idempotencia) VALUES (@rid, 'SALIDA', -4, 8, 'Salida por orden de trabajo ' + @cod, @oid, @mid, DATEADD(MINUTE,915,DATEADD(DAY,-4,@hoy)), 'demo-' + @cod + '-ACE-5W30');
INSERT INTO dbo.detalle_orden_repuesto (id_orden, id_repuesto, cantidad, precio_unitario, descontado) SELECT @oid, id_repuesto, 1, precio_venta, 1 FROM dbo.repuesto WHERE codigo = 'FIL-AIRE';
SELECT @rid = id_repuesto FROM dbo.repuesto WHERE codigo = 'FIL-AIRE';
INSERT INTO dbo.movimiento_inventario (id_repuesto, tipo, cantidad, stock_resultante, motivo, id_orden, id_usuario, fecha, clave_idempotencia) VALUES (@rid, 'SALIDA', -1, 15, 'Salida por orden de trabajo ' + @cod, @oid, @mid, DATEADD(MINUTE,915,DATEADD(DAY,-4,@hoy)), 'demo-' + @cod + '-FIL-AIRE');
INSERT INTO dbo.detalle_orden_repuesto (id_orden, id_repuesto, cantidad, precio_unitario, descontado) SELECT @oid, id_repuesto, 1, precio_venta, 1 FROM dbo.repuesto WHERE codigo = 'BUJ-NGK';
SELECT @rid = id_repuesto FROM dbo.repuesto WHERE codigo = 'BUJ-NGK';
INSERT INTO dbo.movimiento_inventario (id_repuesto, tipo, cantidad, stock_resultante, motivo, id_orden, id_usuario, fecha, clave_idempotencia) VALUES (@rid, 'SALIDA', -1, 12, 'Salida por orden de trabajo ' + @cod, @oid, @mid, DATEADD(MINUTE,915,DATEADD(DAY,-4,@hoy)), 'demo-' + @cod + '-BUJ-NGK');
INSERT INTO dbo.orden_historial (id_orden, estado_anterior, estado_nuevo, id_usuario, comentario, fecha) VALUES (@oid, NULL, 'RECIBIDA', @recep, 'Orden registrada', DATEADD(MINUTE,555,DATEADD(DAY,-4,@hoy)));
INSERT INTO dbo.orden_historial (id_orden, estado_anterior, estado_nuevo, id_usuario, comentario, fecha) VALUES (@oid, 'RECIBIDA', 'EN_DIAGNOSTICO', ISNULL(@mid, @recep), NULL, DATEADD(MINUTE,675,DATEADD(DAY,-4,@hoy)));
INSERT INTO dbo.orden_historial (id_orden, estado_anterior, estado_nuevo, id_usuario, comentario, fecha) VALUES (@oid, 'EN_DIAGNOSTICO', 'EN_REPARACION', ISNULL(@mid, @recep), NULL, DATEADD(MINUTE,795,DATEADD(DAY,-4,@hoy)));
INSERT INTO dbo.orden_historial (id_orden, estado_anterior, estado_nuevo, id_usuario, comentario, fecha) VALUES (@oid, 'EN_REPARACION', 'COMPLETADA', @recep, NULL, DATEADD(MINUTE,915,DATEADD(DAY,-4,@hoy)));
INSERT INTO dbo.orden_historial (id_orden, estado_anterior, estado_nuevo, id_usuario, comentario, fecha) VALUES (@oid, 'COMPLETADA', 'ENTREGADA', @recep, NULL, DATEADD(MINUTE,1035,DATEADD(DAY,-4,@hoy)));
SET @sec = (SELECT ultimo FROM dbo.secuencia WHERE nombre = 'B001') + 1; UPDATE dbo.secuencia SET ultimo = @sec WHERE nombre = 'B001';
INSERT INTO dbo.comprobante (id_orden, tipo, serie, numero, cliente_nombre, cliente_tipo_doc, cliente_documento, vehiculo_placa, base_imponible, igv, total, metodo_pago, estado, fecha_emision, id_emitido_por) VALUES (@oid, 'BOLETA', 'B001', RIGHT('00000000' + CAST(@sec AS VARCHAR(10)), 8), N'Luis Paredes Quispe', 'DNI', '41236587', 'C2M-611', 550.85, 99.15, 650, 'YAPE_PLIN', 'EMITIDO', DATEADD(MINUTE,915,DATEADD(DAY,-4,@hoy)), @recep);
SET @cmp = SCOPE_IDENTITY();
INSERT INTO dbo.detalle_comprobante (id_comprobante, descripcion, cantidad, precio_unitario, importe) VALUES (@cmp, N'Mantenimiento preventivo', 1, 250, 250);
INSERT INTO dbo.detalle_comprobante (id_comprobante, descripcion, cantidad, precio_unitario, importe) VALUES (@cmp, N'Filtro de aceite (FIL-001)', 1, 35, 35);
INSERT INTO dbo.detalle_comprobante (id_comprobante, descripcion, cantidad, precio_unitario, importe) VALUES (@cmp, N'Aceite sintético 5W-30 (1 L) (ACE-5W30)', 4, 52, 208);
INSERT INTO dbo.detalle_comprobante (id_comprobante, descripcion, cantidad, precio_unitario, importe) VALUES (@cmp, N'Filtro de aire (FIL-AIRE)', 1, 45, 45);
INSERT INTO dbo.detalle_comprobante (id_comprobante, descripcion, cantidad, precio_unitario, importe) VALUES (@cmp, N'Bujías NGK (juego x4) (BUJ-NGK)', 1, 85, 85);
INSERT INTO dbo.detalle_comprobante (id_comprobante, descripcion, cantidad, precio_unitario, importe) VALUES (@cmp, N'Mano de obra', 1, 27, 27);

-- Orden 6: Fuga de refrigerante
SET @n += 1; SET @cod = 'OT-' + RIGHT('0000' + CAST(@n AS VARCHAR(10)), 4);
SELECT @vid = id_vehiculo, @cid = id_cliente FROM dbo.vehiculo WHERE placa = 'D9R-370';
SET @mid = (SELECT id_usuario FROM dbo.usuario WHERE username = 'dhans');
INSERT INTO dbo.orden_trabajo (codigo, id_cliente, id_vehiculo, id_mecanico, falla, diagnostico, trabajos_realizados, kilometraje_ingreso, estado, fecha_ingreso, fecha_entrega_est, fecha_cierre, mano_obra, descuento, total, id_creado_por) SELECT @cod, @cid, @vid, @mid, N'Fuga de refrigerante', N'Fuga en manguera del radiador.', NULL, kilometraje, 'EN_REPARACION', DATEADD(MINUTE,615,DATEADD(DAY,-5,@hoy)), DATEADD(DAY,3,CAST(@hoy AS DATE)), NULL, 300, 0, 890, @recep FROM dbo.vehiculo WHERE id_vehiculo = @vid;
SET @oid = SCOPE_IDENTITY();
INSERT INTO dbo.detalle_orden_servicio (id_orden, id_servicio, descripcion, cantidad, precio_unitario) SELECT @oid, id_servicio, nombre, 1, 450 FROM dbo.servicio WHERE nombre = N'Sistema de refrigeración';
INSERT INTO dbo.detalle_orden_servicio (id_orden, id_servicio, descripcion, cantidad, precio_unitario) SELECT @oid, id_servicio, nombre, 1, 80 FROM dbo.servicio WHERE nombre = N'Diagnóstico computarizado';
INSERT INTO dbo.detalle_orden_repuesto (id_orden, id_repuesto, cantidad, precio_unitario, descontado) SELECT @oid, id_repuesto, 2, precio_venta, 0 FROM dbo.repuesto WHERE codigo = 'LIQ-FR';
INSERT INTO dbo.orden_historial (id_orden, estado_anterior, estado_nuevo, id_usuario, comentario, fecha) VALUES (@oid, NULL, 'RECIBIDA', @recep, 'Orden registrada', DATEADD(MINUTE,615,DATEADD(DAY,-5,@hoy)));
INSERT INTO dbo.orden_historial (id_orden, estado_anterior, estado_nuevo, id_usuario, comentario, fecha) VALUES (@oid, 'RECIBIDA', 'EN_DIAGNOSTICO', ISNULL(@mid, @recep), NULL, DATEADD(MINUTE,735,DATEADD(DAY,-5,@hoy)));
INSERT INTO dbo.orden_historial (id_orden, estado_anterior, estado_nuevo, id_usuario, comentario, fecha) VALUES (@oid, 'EN_DIAGNOSTICO', 'EN_REPARACION', ISNULL(@mid, @recep), NULL, DATEADD(MINUTE,855,DATEADD(DAY,-5,@hoy)));

-- Orden 7: Vibración en el volante
SET @n += 1; SET @cod = 'OT-' + RIGHT('0000' + CAST(@n AS VARCHAR(10)), 4);
SELECT @vid = id_vehiculo, @cid = id_cliente FROM dbo.vehiculo WHERE placa = 'H4T-815';
SET @mid = (SELECT id_usuario FROM dbo.usuario WHERE username = 'jperez');
INSERT INTO dbo.orden_trabajo (codigo, id_cliente, id_vehiculo, id_mecanico, falla, diagnostico, trabajos_realizados, kilometraje_ingreso, estado, fecha_ingreso, fecha_entrega_est, fecha_cierre, mano_obra, descuento, total, id_creado_por) SELECT @cod, @cid, @vid, @mid, N'Vibración en el volante', NULL, NULL, kilometraje, 'RECIBIDA', DATEADD(MINUTE,675,DATEADD(DAY,-6,@hoy)), DATEADD(DAY,3,CAST(@hoy AS DATE)), NULL, 0, 0, 0, @recep FROM dbo.vehiculo WHERE id_vehiculo = @vid;
SET @oid = SCOPE_IDENTITY();
INSERT INTO dbo.orden_historial (id_orden, estado_anterior, estado_nuevo, id_usuario, comentario, fecha) VALUES (@oid, NULL, 'RECIBIDA', @recep, 'Orden registrada', DATEADD(MINUTE,675,DATEADD(DAY,-6,@hoy)));

-- Orden 8: Batería descargada
SET @n += 1; SET @cod = 'OT-' + RIGHT('0000' + CAST(@n AS VARCHAR(10)), 4);
SELECT @vid = id_vehiculo, @cid = id_cliente FROM dbo.vehiculo WHERE placa = 'ABC-123';
SET @mid = NULL;
INSERT INTO dbo.orden_trabajo (codigo, id_cliente, id_vehiculo, id_mecanico, falla, diagnostico, trabajos_realizados, kilometraje_ingreso, estado, fecha_ingreso, fecha_entrega_est, fecha_cierre, mano_obra, descuento, total, id_creado_por) SELECT @cod, @cid, @vid, @mid, N'Batería descargada', N'Batería sin carga; alternador en buen estado.', N'Prueba de carga y limpieza de bornes.', kilometraje, 'ENTREGADA', DATEADD(MINUTE,735,DATEADD(DAY,-7,@hoy)), DATEADD(DAY,1,CAST(@hoy AS DATE)), DATEADD(MINUTE,1095,DATEADD(DAY,-7,@hoy)), 35, 0, 230, @recep FROM dbo.vehiculo WHERE id_vehiculo = @vid;
SET @oid = SCOPE_IDENTITY();
INSERT INTO dbo.detalle_orden_servicio (id_orden, id_servicio, descripcion, cantidad, precio_unitario) SELECT @oid, id_servicio, nombre, 1, 45 FROM dbo.servicio WHERE nombre = N'Batería';
INSERT INTO dbo.detalle_orden_servicio (id_orden, id_servicio, descripcion, cantidad, precio_unitario) SELECT @oid, id_servicio, nombre, 1, 150 FROM dbo.servicio WHERE nombre = N'Sistema eléctrico';
INSERT INTO dbo.orden_historial (id_orden, estado_anterior, estado_nuevo, id_usuario, comentario, fecha) VALUES (@oid, NULL, 'RECIBIDA', @recep, 'Orden registrada', DATEADD(MINUTE,735,DATEADD(DAY,-7,@hoy)));
INSERT INTO dbo.orden_historial (id_orden, estado_anterior, estado_nuevo, id_usuario, comentario, fecha) VALUES (@oid, 'RECIBIDA', 'EN_DIAGNOSTICO', ISNULL(@mid, @recep), NULL, DATEADD(MINUTE,855,DATEADD(DAY,-7,@hoy)));
INSERT INTO dbo.orden_historial (id_orden, estado_anterior, estado_nuevo, id_usuario, comentario, fecha) VALUES (@oid, 'EN_DIAGNOSTICO', 'EN_REPARACION', ISNULL(@mid, @recep), NULL, DATEADD(MINUTE,975,DATEADD(DAY,-7,@hoy)));
INSERT INTO dbo.orden_historial (id_orden, estado_anterior, estado_nuevo, id_usuario, comentario, fecha) VALUES (@oid, 'EN_REPARACION', 'COMPLETADA', @recep, NULL, DATEADD(MINUTE,1095,DATEADD(DAY,-7,@hoy)));
INSERT INTO dbo.orden_historial (id_orden, estado_anterior, estado_nuevo, id_usuario, comentario, fecha) VALUES (@oid, 'COMPLETADA', 'ENTREGADA', @recep, NULL, DATEADD(MINUTE,1215,DATEADD(DAY,-7,@hoy)));
SET @sec = (SELECT ultimo FROM dbo.secuencia WHERE nombre = 'B001') + 1; UPDATE dbo.secuencia SET ultimo = @sec WHERE nombre = 'B001';
INSERT INTO dbo.comprobante (id_orden, tipo, serie, numero, cliente_nombre, cliente_tipo_doc, cliente_documento, vehiculo_placa, base_imponible, igv, total, metodo_pago, estado, fecha_emision, id_emitido_por) VALUES (@oid, 'BOLETA', 'B001', RIGHT('00000000' + CAST(@sec AS VARCHAR(10)), 8), N'Carlos Mendoza Rojas', 'DNI', '45871236', 'ABC-123', 194.92, 35.08, 230, 'EFECTIVO', 'EMITIDO', DATEADD(MINUTE,1095,DATEADD(DAY,-7,@hoy)), @recep);
SET @cmp = SCOPE_IDENTITY();
INSERT INTO dbo.detalle_comprobante (id_comprobante, descripcion, cantidad, precio_unitario, importe) VALUES (@cmp, N'Batería', 1, 45, 45);
INSERT INTO dbo.detalle_comprobante (id_comprobante, descripcion, cantidad, precio_unitario, importe) VALUES (@cmp, N'Sistema eléctrico', 1, 150, 150);
INSERT INTO dbo.detalle_comprobante (id_comprobante, descripcion, cantidad, precio_unitario, importe) VALUES (@cmp, N'Mano de obra', 1, 35, 35);

-- Orden 9: Revisión de suspensión
SET @n += 1; SET @cod = 'OT-' + RIGHT('0000' + CAST(@n AS VARCHAR(10)), 4);
SELECT @vid = id_vehiculo, @cid = id_cliente FROM dbo.vehiculo WHERE placa = 'XYZ-789';
SET @mid = (SELECT id_usuario FROM dbo.usuario WHERE username = 'mfeels');
INSERT INTO dbo.orden_trabajo (codigo, id_cliente, id_vehiculo, id_mecanico, falla, diagnostico, trabajos_realizados, kilometraje_ingreso, estado, fecha_ingreso, fecha_entrega_est, fecha_cierre, mano_obra, descuento, total, id_creado_por) SELECT @cod, @cid, @vid, @mid, N'Revisión de suspensión', N'Rótulas en buen estado; ajuste de componentes.', N'Inspección de suspensión y dirección; cambio de líquido de frenos.', kilometraje, 'COMPLETADA', DATEADD(MINUTE,555,DATEADD(DAY,-8,@hoy)), DATEADD(DAY,1,CAST(@hoy AS DATE)), DATEADD(MINUTE,915,DATEADD(DAY,-8,@hoy)), 130, 0, 310, @recep FROM dbo.vehiculo WHERE id_vehiculo = @vid;
SET @oid = SCOPE_IDENTITY();
INSERT INTO dbo.detalle_orden_servicio (id_orden, id_servicio, descripcion, cantidad, precio_unitario) SELECT @oid, id_servicio, nombre, 1, 150 FROM dbo.servicio WHERE nombre = N'Suspensión y dirección';
INSERT INTO dbo.detalle_orden_repuesto (id_orden, id_repuesto, cantidad, precio_unitario, descontado) SELECT @oid, id_repuesto, 1, precio_venta, 1 FROM dbo.repuesto WHERE codigo = 'LIQ-FR';
SELECT @rid = id_repuesto FROM dbo.repuesto WHERE codigo = 'LIQ-FR';
INSERT INTO dbo.movimiento_inventario (id_repuesto, tipo, cantidad, stock_resultante, motivo, id_orden, id_usuario, fecha, clave_idempotencia) VALUES (@rid, 'SALIDA', -1, 2, 'Salida por orden de trabajo ' + @cod, @oid, @mid, DATEADD(MINUTE,915,DATEADD(DAY,-8,@hoy)), 'demo-' + @cod + '-LIQ-FR');
INSERT INTO dbo.orden_historial (id_orden, estado_anterior, estado_nuevo, id_usuario, comentario, fecha) VALUES (@oid, NULL, 'RECIBIDA', @recep, 'Orden registrada', DATEADD(MINUTE,555,DATEADD(DAY,-8,@hoy)));
INSERT INTO dbo.orden_historial (id_orden, estado_anterior, estado_nuevo, id_usuario, comentario, fecha) VALUES (@oid, 'RECIBIDA', 'EN_DIAGNOSTICO', ISNULL(@mid, @recep), NULL, DATEADD(MINUTE,675,DATEADD(DAY,-8,@hoy)));
INSERT INTO dbo.orden_historial (id_orden, estado_anterior, estado_nuevo, id_usuario, comentario, fecha) VALUES (@oid, 'EN_DIAGNOSTICO', 'EN_REPARACION', ISNULL(@mid, @recep), NULL, DATEADD(MINUTE,795,DATEADD(DAY,-8,@hoy)));
INSERT INTO dbo.orden_historial (id_orden, estado_anterior, estado_nuevo, id_usuario, comentario, fecha) VALUES (@oid, 'EN_REPARACION', 'COMPLETADA', @recep, NULL, DATEADD(MINUTE,915,DATEADD(DAY,-8,@hoy)));
SET @sec = (SELECT ultimo FROM dbo.secuencia WHERE nombre = 'B001') + 1; UPDATE dbo.secuencia SET ultimo = @sec WHERE nombre = 'B001';
INSERT INTO dbo.comprobante (id_orden, tipo, serie, numero, cliente_nombre, cliente_tipo_doc, cliente_documento, vehiculo_placa, base_imponible, igv, total, metodo_pago, estado, fecha_emision, id_emitido_por) VALUES (@oid, 'BOLETA', 'B001', RIGHT('00000000' + CAST(@sec AS VARCHAR(10)), 8), N'María Torres Vega', 'DNI', '70123458', 'XYZ-789', 262.71, 47.29, 310, 'TARJETA', 'EMITIDO', DATEADD(MINUTE,915,DATEADD(DAY,-8,@hoy)), @recep);
SET @cmp = SCOPE_IDENTITY();
INSERT INTO dbo.detalle_comprobante (id_comprobante, descripcion, cantidad, precio_unitario, importe) VALUES (@cmp, N'Suspensión y dirección', 1, 150, 150);
INSERT INTO dbo.detalle_comprobante (id_comprobante, descripcion, cantidad, precio_unitario, importe) VALUES (@cmp, N'Líquido de frenos DOT 4 (LIQ-FR)', 1, 30, 30);
INSERT INTO dbo.detalle_comprobante (id_comprobante, descripcion, cantidad, precio_unitario, importe) VALUES (@cmp, N'Mano de obra', 1, 130, 130);

-- Orden 10: Aire acondicionado no enfría
SET @n += 1; SET @cod = 'OT-' + RIGHT('0000' + CAST(@n AS VARCHAR(10)), 4);
SELECT @vid = id_vehiculo, @cid = id_cliente FROM dbo.vehiculo WHERE placa = 'F5K-029';
SET @mid = (SELECT id_usuario FROM dbo.usuario WHERE username = 'dhans');
INSERT INTO dbo.orden_trabajo (codigo, id_cliente, id_vehiculo, id_mecanico, falla, diagnostico, trabajos_realizados, kilometraje_ingreso, estado, fecha_ingreso, fecha_entrega_est, fecha_cierre, mano_obra, descuento, total, id_creado_por) SELECT @cod, @cid, @vid, @mid, N'Aire acondicionado no enfría', N'Se inicia la revisión de carga de gas.', NULL, kilometraje, 'EN_DIAGNOSTICO', DATEADD(MINUTE,615,DATEADD(DAY,-9,@hoy)), DATEADD(DAY,3,CAST(@hoy AS DATE)), NULL, 0, 0, 0, @recep FROM dbo.vehiculo WHERE id_vehiculo = @vid;
SET @oid = SCOPE_IDENTITY();
INSERT INTO dbo.orden_historial (id_orden, estado_anterior, estado_nuevo, id_usuario, comentario, fecha) VALUES (@oid, NULL, 'RECIBIDA', @recep, 'Orden registrada', DATEADD(MINUTE,615,DATEADD(DAY,-9,@hoy)));
INSERT INTO dbo.orden_historial (id_orden, estado_anterior, estado_nuevo, id_usuario, comentario, fecha) VALUES (@oid, 'RECIBIDA', 'EN_DIAGNOSTICO', ISNULL(@mid, @recep), NULL, DATEADD(MINUTE,735,DATEADD(DAY,-9,@hoy)));

UPDATE dbo.secuencia SET ultimo = @n WHERE nombre = 'OT';

/* Stock actual de los repuestos y movimiento de stock inicial */
UPDATE dbo.repuesto SET stock_actual = 40 WHERE codigo = 'FIL-001';
INSERT INTO dbo.movimiento_inventario (id_repuesto, tipo, cantidad, stock_resultante, motivo, id_usuario, fecha, clave_idempotencia) SELECT id_repuesto, 'ENTRADA', 41, 41, 'Stock inicial (demostración)', NULL, DATEADD(DAY,-30,@hoy), 'demo-inicial-FIL-001' FROM dbo.repuesto WHERE codigo = 'FIL-001';
UPDATE dbo.repuesto SET stock_actual = 8 WHERE codigo = 'ACE-5W30';
INSERT INTO dbo.movimiento_inventario (id_repuesto, tipo, cantidad, stock_resultante, motivo, id_usuario, fecha, clave_idempotencia) SELECT id_repuesto, 'ENTRADA', 12, 12, 'Stock inicial (demostración)', NULL, DATEADD(DAY,-30,@hoy), 'demo-inicial-ACE-5W30' FROM dbo.repuesto WHERE codigo = 'ACE-5W30';
UPDATE dbo.repuesto SET stock_actual = 4 WHERE codigo = 'PAS-DEL';
INSERT INTO dbo.movimiento_inventario (id_repuesto, tipo, cantidad, stock_resultante, motivo, id_usuario, fecha, clave_idempotencia) SELECT id_repuesto, 'ENTRADA', 4, 4, 'Stock inicial (demostración)', NULL, DATEADD(DAY,-30,@hoy), 'demo-inicial-PAS-DEL' FROM dbo.repuesto WHERE codigo = 'PAS-DEL';
UPDATE dbo.repuesto SET stock_actual = 1 WHERE codigo = 'BAT-12V';
INSERT INTO dbo.movimiento_inventario (id_repuesto, tipo, cantidad, stock_resultante, motivo, id_usuario, fecha, clave_idempotencia) SELECT id_repuesto, 'ENTRADA', 1, 1, 'Stock inicial (demostración)', NULL, DATEADD(DAY,-30,@hoy), 'demo-inicial-BAT-12V' FROM dbo.repuesto WHERE codigo = 'BAT-12V';
UPDATE dbo.repuesto SET stock_actual = 12 WHERE codigo = 'BUJ-NGK';
INSERT INTO dbo.movimiento_inventario (id_repuesto, tipo, cantidad, stock_resultante, motivo, id_usuario, fecha, clave_idempotencia) SELECT id_repuesto, 'ENTRADA', 13, 13, 'Stock inicial (demostración)', NULL, DATEADD(DAY,-30,@hoy), 'demo-inicial-BUJ-NGK' FROM dbo.repuesto WHERE codigo = 'BUJ-NGK';
UPDATE dbo.repuesto SET stock_actual = 15 WHERE codigo = 'FIL-AIRE';
INSERT INTO dbo.movimiento_inventario (id_repuesto, tipo, cantidad, stock_resultante, motivo, id_usuario, fecha, clave_idempotencia) SELECT id_repuesto, 'ENTRADA', 17, 17, 'Stock inicial (demostración)', NULL, DATEADD(DAY,-30,@hoy), 'demo-inicial-FIL-AIRE' FROM dbo.repuesto WHERE codigo = 'FIL-AIRE';
UPDATE dbo.repuesto SET stock_actual = 2 WHERE codigo = 'LIQ-FR';
INSERT INTO dbo.movimiento_inventario (id_repuesto, tipo, cantidad, stock_resultante, motivo, id_usuario, fecha, clave_idempotencia) SELECT id_repuesto, 'ENTRADA', 3, 3, 'Stock inicial (demostración)', NULL, DATEADD(DAY,-30,@hoy), 'demo-inicial-LIQ-FR' FROM dbo.repuesto WHERE codigo = 'LIQ-FR';

COMMIT;
PRINT 'Datos de demostración completos cargados.';
GO
SET NOEXEC OFF;
GO