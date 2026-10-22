/* ============================================================
   MecAuto - DATOS DE DEMOSTRACIÓN (OPCIONAL, solo para ensayar)
   Todos los datos son FICTICIOS. No ejecutar en un taller real.
   Ejecutar después de schema.sql y seed.sql. No crea usuarios.
   Se puede repetir: no duplica registros existentes.
   ============================================================ */
USE MECAUTO;
GO

MERGE dbo.proveedor AS t
USING (VALUES
  ('Distribuidora Andina de Repuestos S.A.C. (demo)', '20100000001', '014440001'),
  ('Lubricantes del Pacífico E.I.R.L. (demo)',        '20100000002', '014440002')
) AS s(razon, ruc, tel) ON t.ruc = s.ruc
WHEN NOT MATCHED THEN INSERT (razon_social, ruc, telefono) VALUES (s.razon, s.ruc, s.tel);
GO

MERGE dbo.repuesto AS t
USING (VALUES
  ('FA-102',  'Filtro de aceite',            'Filtros',      12.00,  28.00, 24, 10),
  ('PF-220',  'Pastillas de freno delanteras','Frenos',       55.00,  95.00,  6,  8),
  ('AC-1040', 'Aceite 10W-40 (1 L)',          'Lubricantes',  21.00,  32.00, 38, 20),
  ('BT-4512', 'Batería 12V 45Ah',             'Eléctrico',   210.00, 300.00,  3,  4),
  ('BJ-045',  'Bujía (unidad)',               'Encendido',     8.00,  14.00, 52, 24),
  ('FAI-310', 'Filtro de aire',               'Filtros',      18.00,  36.00, 15,  6)
) AS s(cod, nom, cat, pc, pv, stock, minimo) ON t.codigo = s.cod
WHEN NOT MATCHED THEN INSERT (codigo, nombre, categoria, precio_compra, precio_venta, stock_actual, stock_minimo)
     VALUES (s.cod, s.nom, s.cat, s.pc, s.pv, s.stock, s.minimo);
GO

MERGE dbo.cliente AS t
USING (VALUES
  ('DNI', '10000001', 'Cliente Demo Uno',            '900000001', 'demo1@ejemplo.invalid'),
  ('DNI', '10000002', 'Cliente Demo Dos',            '900000002', 'demo2@ejemplo.invalid'),
  ('RUC', '20999999991', 'Empresa Demo Transportes S.A.C.', '014440099', 'flota@ejemplo.invalid')
) AS s(tipo, doc, nom, tel, mail) ON t.nro_documento = s.doc
WHEN NOT MATCHED THEN INSERT (tipo_documento, nro_documento, nombre, telefono, correo)
     VALUES (s.tipo, s.doc, s.nom, s.tel, s.mail);
GO

INSERT INTO dbo.vehiculo (id_cliente, placa, marca, modelo, anio, kilometraje)
SELECT c.id_cliente, v.placa, v.marca, v.modelo, v.anio, v.km
FROM (VALUES
  ('10000001',    'DEM-001', 'Toyota',  'Yaris',    2018, 62000),
  ('10000002',    'DEM-002', 'Kia',     'Rio',      2020, 41000),
  ('20999999991', 'DEM-003', 'Hyundai', 'H100',     2019, 98000)
) AS v(doc, placa, marca, modelo, anio, km)
JOIN dbo.cliente c ON c.nro_documento = v.doc
WHERE NOT EXISTS (SELECT 1 FROM dbo.vehiculo x WHERE x.placa = v.placa);
GO

PRINT 'Datos de demostración cargados.';
GO
