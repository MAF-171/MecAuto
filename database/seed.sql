/* ============================================================
   MecAuto - Datos iniciales NECESARIOS (no son datos de prueba)
   Ejecutar DESPUÉS de schema.sql. Se puede repetir sin duplicar.

   - NO crea usuarios ni contraseñas. El primer administrador se crea
     al iniciar el backend con las variables MECAUTO_ADMIN_USER y
     MECAUTO_ADMIN_PASSWORD (ver README).
   - Los datos de contacto del taller quedan VACÍOS: complétalos desde
     el panel (Configuración). No se inventan RUC, teléfonos ni direcciones.
   ============================================================ */
USE MECAUTO;
GO

/* Correlativos */
MERGE dbo.secuencia AS t
USING (VALUES ('OT'),('COT'),('CITA'),('B001'),('F001')) AS s(nombre)
ON t.nombre = s.nombre
WHEN NOT MATCHED THEN INSERT (nombre, ultimo) VALUES (s.nombre, 0);
GO

/* Configuración del taller */
MERGE dbo.configuracion_taller AS t
USING (VALUES
    ('nombre_taller',      'Taller Mecánico MecAuto'),
    ('ruc',                ''),
    ('direccion',          ''),
    ('telefono',           ''),
    ('whatsapp',           ''),
    ('correo',             ''),
    ('horario_texto',      'Lunes a sábado, 8:00 a 18:00'),
    ('hora_apertura',      '08:00'),
    ('hora_cierre',        '18:00'),
    ('duracion_franja_min','60'),
    ('citas_por_franja',   '2'),
    ('dias_laborables',    '1,2,3,4,5,6'),   -- 1 = lunes ... 7 = domingo
    ('serie_boleta',       'B001'),
    ('serie_factura',      'F001')
) AS s(clave, valor)
ON t.clave = s.clave
WHEN NOT MATCHED THEN INSERT (clave, valor) VALUES (s.clave, s.valor);
GO

/* Catálogo de servicios (precios en soles, IGV incluido; NULL = según diagnóstico) */
MERGE dbo.servicio AS t
USING (VALUES
 ('Mantenimiento preventivo',    'Revisión programada de los puntos clave del vehículo según su kilometraje.', NULL,   120),
 ('Cambio de aceite y filtros',  'Cambio de aceite de motor y filtro de aceite, con revisión de niveles.',      120.00,  45),
 ('Diagnóstico computarizado',   'Lectura de fallas con escáner y explicación clara de los resultados.',         80.00,  30),
 ('Sistema de frenos',           'Revisión de pastillas, discos y sistema de frenado completo.',                 NULL,    90),
 ('Suspensión y dirección',      'Inspección de amortiguadores, rótulas y componentes de dirección.',            NULL,   120),
 ('Sistema eléctrico',           'Diagnóstico de fallas eléctricas, luces, fusibles y cableado.',                NULL,    60),
 ('Batería',                     'Prueba de carga, limpieza de bornes y cambio de batería si corresponde.',      45.00,  30),
 ('Sistema de refrigeración',    'Revisión de radiador, mangueras, termostato y nivel de refrigerante.',         NULL,    60),
 ('Aire acondicionado',          'Revisión de carga de gas, filtro de habitáculo y funcionamiento general.',     NULL,    60),
 ('Neumáticos',                  'Revisión de desgaste, presión y rotación de neumáticos.',                      NULL,    30),
 ('Alineación y balanceo',       'Alineación de dirección y balanceo de ruedas para un manejo estable.',         90.00,   60),
 ('Transmisión',                 'Revisión de caja de cambios, aceite de transmisión y embrague.',               NULL,   120),
 ('Revisión pre-viaje',          'Chequeo general antes de un viaje largo: niveles, frenos, luces y neumáticos.',60.00,   45),
 ('Inspección general',          'Revisión integral del vehículo con reporte detallado por escrito.',            70.00,   60)
) AS s(nombre, descripcion, precio, duracion)
ON t.nombre = s.nombre
WHEN NOT MATCHED THEN INSERT (nombre, descripcion, precio_referencial, duracion_minutos, activo)
     VALUES (s.nombre, s.descripcion, s.precio, s.duracion, 1);
GO

PRINT 'Datos iniciales cargados.';
GO
