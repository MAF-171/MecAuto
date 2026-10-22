/* ============================================================
   MecAuto - Esquema de base de datos (Microsoft SQL Server, T-SQL)
   Ejecutar en SQL Server Management Studio 21 (o en VS Code con la
   extensión "SQL Server (mssql)").

   - NO es destructivo: solo crea lo que no existe. No hace DROP.
   - Se puede ejecutar varias veces sin error.
   - Orden de ejecución:  1) schema.sql   2) seed.sql   (3) seed-demo.sql opcional)
   ============================================================ */

IF DB_ID(N'MECAUTO') IS NULL
BEGIN
    CREATE DATABASE MECAUTO;
END
GO

USE MECAUTO;
GO

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

/* ------------------------------------------------------------
   usuario: acceso al sistema. rol = ADMINISTRADOR | RECEPCION | MECANICO
   La contraseña se guarda como hash BCrypt (nunca en texto plano).
   ------------------------------------------------------------ */
IF OBJECT_ID(N'dbo.usuario', N'U') IS NULL
CREATE TABLE dbo.usuario (
    id_usuario     BIGINT IDENTITY(1,1) CONSTRAINT PK_usuario PRIMARY KEY,
    username       VARCHAR(40)   NOT NULL CONSTRAINT UQ_usuario_username UNIQUE,
    password_hash  VARCHAR(100)  NOT NULL,
    nombre         VARCHAR(100)  NOT NULL,
    rol            VARCHAR(20)   NOT NULL
        CONSTRAINT CK_usuario_rol CHECK (rol IN ('ADMINISTRADOR','RECEPCION','MECANICO')),
    activo         BIT           NOT NULL CONSTRAINT DF_usuario_activo DEFAULT 1,
    creado_en      DATETIME2(0)  NOT NULL CONSTRAINT DF_usuario_creado DEFAULT SYSDATETIME()
);
GO

/* ------------------------------------------------------------
   cliente (persona natural con DNI/CE o empresa con RUC)
   ------------------------------------------------------------ */
IF OBJECT_ID(N'dbo.cliente', N'U') IS NULL
CREATE TABLE dbo.cliente (
    id_cliente       BIGINT IDENTITY(1,1) CONSTRAINT PK_cliente PRIMARY KEY,
    tipo_documento   VARCHAR(3)    NOT NULL
        CONSTRAINT CK_cliente_tipodoc CHECK (tipo_documento IN ('DNI','RUC','CE')),
    nro_documento    VARCHAR(12)   NOT NULL CONSTRAINT UQ_cliente_documento UNIQUE,
    nombre           VARCHAR(120)  NOT NULL,   -- nombres y apellidos o razón social
    telefono         VARCHAR(20)   NULL,
    correo           VARCHAR(100)  NULL,
    direccion        VARCHAR(200)  NULL,
    activo           BIT           NOT NULL CONSTRAINT DF_cliente_activo DEFAULT 1,
    creado_en        DATETIME2(0)  NOT NULL CONSTRAINT DF_cliente_creado DEFAULT SYSDATETIME()
);
GO

/* ------------------------------------------------------------
   vehiculo (RN01: pertenece a un solo cliente; la placa no se repite)
   ------------------------------------------------------------ */
IF OBJECT_ID(N'dbo.vehiculo', N'U') IS NULL
CREATE TABLE dbo.vehiculo (
    id_vehiculo   BIGINT IDENTITY(1,1) CONSTRAINT PK_vehiculo PRIMARY KEY,
    id_cliente    BIGINT       NOT NULL
        CONSTRAINT FK_vehiculo_cliente REFERENCES dbo.cliente (id_cliente),
    placa         VARCHAR(10)  NOT NULL CONSTRAINT UQ_vehiculo_placa UNIQUE,
    marca         VARCHAR(40)  NOT NULL,
    modelo        VARCHAR(40)  NOT NULL,
    anio          INT          NULL CONSTRAINT CK_vehiculo_anio CHECK (anio IS NULL OR anio BETWEEN 1950 AND 2100),
    color         VARCHAR(30)  NULL,
    kilometraje   INT          NOT NULL CONSTRAINT DF_vehiculo_km DEFAULT 0
        CONSTRAINT CK_vehiculo_km CHECK (kilometraje >= 0),
    activo        BIT          NOT NULL CONSTRAINT DF_vehiculo_activo DEFAULT 1,
    creado_en     DATETIME2(0) NOT NULL CONSTRAINT DF_vehiculo_creado DEFAULT SYSDATETIME()
);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_vehiculo_cliente')
    CREATE INDEX IX_vehiculo_cliente ON dbo.vehiculo (id_cliente);
GO

/* ------------------------------------------------------------
   servicio (catálogo). Los precios INCLUYEN IGV (RN06).
   precio_referencial NULL = "según diagnóstico".
   ------------------------------------------------------------ */
IF OBJECT_ID(N'dbo.servicio', N'U') IS NULL
CREATE TABLE dbo.servicio (
    id_servicio         BIGINT IDENTITY(1,1) CONSTRAINT PK_servicio PRIMARY KEY,
    nombre              VARCHAR(100)  NOT NULL CONSTRAINT UQ_servicio_nombre UNIQUE,
    descripcion         VARCHAR(500)  NULL,
    precio_referencial  DECIMAL(12,2) NULL CONSTRAINT CK_servicio_precio CHECK (precio_referencial IS NULL OR precio_referencial >= 0),
    duracion_minutos    INT           NULL CONSTRAINT CK_servicio_duracion CHECK (duracion_minutos IS NULL OR duracion_minutos > 0),
    activo              BIT           NOT NULL CONSTRAINT DF_servicio_activo DEFAULT 1
);
GO

/* ------------------------------------------------------------
   proveedor
   ------------------------------------------------------------ */
IF OBJECT_ID(N'dbo.proveedor', N'U') IS NULL
CREATE TABLE dbo.proveedor (
    id_proveedor  BIGINT IDENTITY(1,1) CONSTRAINT PK_proveedor PRIMARY KEY,
    razon_social  VARCHAR(120) NOT NULL,
    ruc           VARCHAR(11)  NOT NULL CONSTRAINT UQ_proveedor_ruc UNIQUE,
    telefono      VARCHAR(20)  NULL,
    activo        BIT          NOT NULL CONSTRAINT DF_proveedor_activo DEFAULT 1
);
GO

/* ------------------------------------------------------------
   repuesto (RN03: el stock nunca puede ser negativo -> CHECK en la BD)
   version: control de concurrencia optimista de JPA (@Version)
   ------------------------------------------------------------ */
IF OBJECT_ID(N'dbo.repuesto', N'U') IS NULL
CREATE TABLE dbo.repuesto (
    id_repuesto    BIGINT IDENTITY(1,1) CONSTRAINT PK_repuesto PRIMARY KEY,
    id_proveedor   BIGINT NULL
        CONSTRAINT FK_repuesto_proveedor REFERENCES dbo.proveedor (id_proveedor),
    codigo         VARCHAR(20)   NOT NULL CONSTRAINT UQ_repuesto_codigo UNIQUE,
    nombre         VARCHAR(100)  NOT NULL,
    categoria      VARCHAR(40)   NULL,
    precio_compra  DECIMAL(12,2) NOT NULL CONSTRAINT DF_repuesto_pcompra DEFAULT 0
        CONSTRAINT CK_repuesto_pcompra CHECK (precio_compra >= 0),
    precio_venta   DECIMAL(12,2) NOT NULL CONSTRAINT CK_repuesto_pventa CHECK (precio_venta >= 0),
    stock_actual   INT           NOT NULL CONSTRAINT DF_repuesto_stock DEFAULT 0
        CONSTRAINT CK_repuesto_stock CHECK (stock_actual >= 0),
    stock_minimo   INT           NOT NULL CONSTRAINT DF_repuesto_min DEFAULT 0
        CONSTRAINT CK_repuesto_min CHECK (stock_minimo >= 0),
    activo         BIT           NOT NULL CONSTRAINT DF_repuesto_activo DEFAULT 1,
    version        INT           NOT NULL CONSTRAINT DF_repuesto_version DEFAULT 0
);
GO

/* ------------------------------------------------------------
   movimiento_inventario: historial de entradas, salidas y ajustes.
   cantidad es con signo (ENTRADA +, SALIDA -, AJUSTE +/-).
   clave_idempotencia evita registrar dos veces la misma operación.
   ------------------------------------------------------------ */
IF OBJECT_ID(N'dbo.movimiento_inventario', N'U') IS NULL
CREATE TABLE dbo.movimiento_inventario (
    id_movimiento      BIGINT IDENTITY(1,1) CONSTRAINT PK_movimiento PRIMARY KEY,
    id_repuesto        BIGINT NOT NULL
        CONSTRAINT FK_movimiento_repuesto REFERENCES dbo.repuesto (id_repuesto),
    tipo               VARCHAR(10) NOT NULL
        CONSTRAINT CK_movimiento_tipo CHECK (tipo IN ('ENTRADA','SALIDA','AJUSTE')),
    cantidad           INT         NOT NULL CONSTRAINT CK_movimiento_cant CHECK (cantidad <> 0),
    stock_resultante   INT         NOT NULL CONSTRAINT CK_movimiento_resultante CHECK (stock_resultante >= 0),
    motivo             VARCHAR(200) NULL,
    id_orden           BIGINT      NULL,      -- se llena cuando la salida proviene de una orden de trabajo
    id_usuario         BIGINT      NULL
        CONSTRAINT FK_movimiento_usuario REFERENCES dbo.usuario (id_usuario),
    fecha              DATETIME2(0) NOT NULL CONSTRAINT DF_movimiento_fecha DEFAULT SYSDATETIME(),
    clave_idempotencia VARCHAR(80) NULL
);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UX_movimiento_idempotencia')
    CREATE UNIQUE INDEX UX_movimiento_idempotencia
        ON dbo.movimiento_inventario (clave_idempotencia) WHERE clave_idempotencia IS NOT NULL;
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_movimiento_repuesto_fecha')
    CREATE INDEX IX_movimiento_repuesto_fecha ON dbo.movimiento_inventario (id_repuesto, fecha DESC);
GO

/* ------------------------------------------------------------
   cita (la solicitud pública se guarda aquí; cliente y vehículo
   pueden quedar NULL hasta que recepción los vincule)
   ------------------------------------------------------------ */
IF OBJECT_ID(N'dbo.cita', N'U') IS NULL
CREATE TABLE dbo.cita (
    id_cita            BIGINT IDENTITY(1,1) CONSTRAINT PK_cita PRIMARY KEY,
    codigo             VARCHAR(12)  NOT NULL CONSTRAINT UQ_cita_codigo UNIQUE,   -- CITA-0001
    id_cliente         BIGINT NULL CONSTRAINT FK_cita_cliente  REFERENCES dbo.cliente (id_cliente),
    id_vehiculo        BIGINT NULL CONSTRAINT FK_cita_vehiculo REFERENCES dbo.vehiculo (id_vehiculo),
    id_servicio        BIGINT NOT NULL CONSTRAINT FK_cita_servicio REFERENCES dbo.servicio (id_servicio),
    nombre_contacto    VARCHAR(120) NOT NULL,
    telefono_contacto  VARCHAR(20)  NOT NULL,
    correo_contacto    VARCHAR(100) NULL,
    placa              VARCHAR(10)  NOT NULL,
    marca              VARCHAR(40)  NULL,
    modelo             VARCHAR(40)  NULL,
    fecha_hora         DATETIME2(0) NOT NULL,
    motivo             VARCHAR(300) NULL,
    estado             VARCHAR(12)  NOT NULL CONSTRAINT DF_cita_estado DEFAULT 'PENDIENTE'
        CONSTRAINT CK_cita_estado CHECK (estado IN ('PENDIENTE','CONFIRMADA','COMPLETADA','CANCELADA')),
    origen             VARCHAR(10)  NOT NULL CONSTRAINT DF_cita_origen DEFAULT 'INTERNA'
        CONSTRAINT CK_cita_origen CHECK (origen IN ('PUBLICA','INTERNA')),
    creado_en          DATETIME2(0) NOT NULL CONSTRAINT DF_cita_creado DEFAULT SYSDATETIME()
);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_cita_fecha')
    CREATE INDEX IX_cita_fecha ON dbo.cita (fecha_hora, estado);
GO

/* ------------------------------------------------------------
   orden_trabajo (RN02: estados en orden fijo; controlado en el servicio)
   total incluye IGV (RN06)
   ------------------------------------------------------------ */
IF OBJECT_ID(N'dbo.orden_trabajo', N'U') IS NULL
CREATE TABLE dbo.orden_trabajo (
    id_orden              BIGINT IDENTITY(1,1) CONSTRAINT PK_orden PRIMARY KEY,
    codigo                VARCHAR(12)  NOT NULL CONSTRAINT UQ_orden_codigo UNIQUE,   -- OT-0001
    id_cliente            BIGINT NOT NULL CONSTRAINT FK_orden_cliente  REFERENCES dbo.cliente (id_cliente),
    id_vehiculo           BIGINT NOT NULL CONSTRAINT FK_orden_vehiculo REFERENCES dbo.vehiculo (id_vehiculo),
    id_mecanico           BIGINT NULL     CONSTRAINT FK_orden_mecanico REFERENCES dbo.usuario (id_usuario),
    id_cita               BIGINT NULL     CONSTRAINT FK_orden_cita     REFERENCES dbo.cita (id_cita),
    falla                 VARCHAR(500) NOT NULL,
    diagnostico           VARCHAR(1000) NULL,
    trabajos_realizados   VARCHAR(1000) NULL,
    kilometraje_ingreso   INT NULL CONSTRAINT CK_orden_km CHECK (kilometraje_ingreso IS NULL OR kilometraje_ingreso >= 0),
    estado                VARCHAR(15) NOT NULL CONSTRAINT DF_orden_estado DEFAULT 'RECIBIDA'
        CONSTRAINT CK_orden_estado CHECK (estado IN
            ('RECIBIDA','EN_DIAGNOSTICO','EN_REPARACION','COMPLETADA','ENTREGADA','CANCELADA')),
    fecha_ingreso         DATETIME2(0) NOT NULL CONSTRAINT DF_orden_ingreso DEFAULT SYSDATETIME(),
    fecha_entrega_est     DATE NULL,
    fecha_cierre          DATETIME2(0) NULL,
    mano_obra             DECIMAL(12,2) NOT NULL CONSTRAINT DF_orden_mo DEFAULT 0
        CONSTRAINT CK_orden_mo CHECK (mano_obra >= 0),
    descuento             DECIMAL(12,2) NOT NULL CONSTRAINT DF_orden_desc DEFAULT 0
        CONSTRAINT CK_orden_desc CHECK (descuento >= 0),
    total                 DECIMAL(12,2) NOT NULL CONSTRAINT DF_orden_total DEFAULT 0
        CONSTRAINT CK_orden_total CHECK (total >= 0),
    id_creado_por         BIGINT NULL CONSTRAINT FK_orden_creador REFERENCES dbo.usuario (id_usuario),
    version               INT NOT NULL CONSTRAINT DF_orden_version DEFAULT 0
);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_orden_estado')
    CREATE INDEX IX_orden_estado ON dbo.orden_trabajo (estado, fecha_ingreso DESC);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_orden_vehiculo')
    CREATE INDEX IX_orden_vehiculo ON dbo.orden_trabajo (id_vehiculo);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_orden_mecanico')
    CREATE INDEX IX_orden_mecanico ON dbo.orden_trabajo (id_mecanico);
GO

IF OBJECT_ID(N'dbo.detalle_orden_servicio', N'U') IS NULL
CREATE TABLE dbo.detalle_orden_servicio (
    id_detalle       BIGINT IDENTITY(1,1) CONSTRAINT PK_det_ord_serv PRIMARY KEY,
    id_orden         BIGINT NOT NULL CONSTRAINT FK_dos_orden REFERENCES dbo.orden_trabajo (id_orden),
    id_servicio      BIGINT NOT NULL CONSTRAINT FK_dos_servicio REFERENCES dbo.servicio (id_servicio),
    descripcion      VARCHAR(200) NOT NULL,
    cantidad         INT NOT NULL CONSTRAINT CK_dos_cant CHECK (cantidad > 0),
    precio_unitario  DECIMAL(12,2) NOT NULL CONSTRAINT CK_dos_precio CHECK (precio_unitario >= 0)
);
GO

/* descontado = 1 cuando ya se registró la salida de inventario de esa línea
   (evita descontar dos veces). Un repuesto aparece una sola vez por orden. */
IF OBJECT_ID(N'dbo.detalle_orden_repuesto', N'U') IS NULL
CREATE TABLE dbo.detalle_orden_repuesto (
    id_detalle       BIGINT IDENTITY(1,1) CONSTRAINT PK_det_ord_rep PRIMARY KEY,
    id_orden         BIGINT NOT NULL CONSTRAINT FK_dor_orden REFERENCES dbo.orden_trabajo (id_orden),
    id_repuesto      BIGINT NOT NULL CONSTRAINT FK_dor_repuesto REFERENCES dbo.repuesto (id_repuesto),
    cantidad         INT NOT NULL CONSTRAINT CK_dor_cant CHECK (cantidad > 0),
    precio_unitario  DECIMAL(12,2) NOT NULL CONSTRAINT CK_dor_precio CHECK (precio_unitario >= 0),
    descontado       BIT NOT NULL CONSTRAINT DF_dor_descontado DEFAULT 0,
    CONSTRAINT UQ_dor_orden_repuesto UNIQUE (id_orden, id_repuesto)
);
GO

/* Historial de cambios de estado de la orden (trazabilidad) */
IF OBJECT_ID(N'dbo.orden_historial', N'U') IS NULL
CREATE TABLE dbo.orden_historial (
    id_historial     BIGINT IDENTITY(1,1) CONSTRAINT PK_orden_hist PRIMARY KEY,
    id_orden         BIGINT NOT NULL CONSTRAINT FK_oh_orden REFERENCES dbo.orden_trabajo (id_orden),
    estado_anterior  VARCHAR(15) NULL,
    estado_nuevo     VARCHAR(15) NOT NULL,
    id_usuario       BIGINT NULL CONSTRAINT FK_oh_usuario REFERENCES dbo.usuario (id_usuario),
    comentario       VARCHAR(300) NULL,
    fecha            DATETIME2(0) NOT NULL CONSTRAINT DF_oh_fecha DEFAULT SYSDATETIME()
);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_orden_hist_orden')
    CREATE INDEX IX_orden_hist_orden ON dbo.orden_historial (id_orden, fecha);
GO

/* ------------------------------------------------------------
   cotizacion: precios con IGV incluido; se guarda el desglose.
   ------------------------------------------------------------ */
IF OBJECT_ID(N'dbo.cotizacion', N'U') IS NULL
CREATE TABLE dbo.cotizacion (
    id_cotizacion   BIGINT IDENTITY(1,1) CONSTRAINT PK_cotizacion PRIMARY KEY,
    codigo          VARCHAR(12)  NOT NULL CONSTRAINT UQ_cotizacion_codigo UNIQUE,   -- COT-0001
    id_cliente      BIGINT NOT NULL CONSTRAINT FK_cot_cliente  REFERENCES dbo.cliente (id_cliente),
    id_vehiculo     BIGINT NULL     CONSTRAINT FK_cot_vehiculo REFERENCES dbo.vehiculo (id_vehiculo),
    id_orden        BIGINT NULL     CONSTRAINT FK_cot_orden    REFERENCES dbo.orden_trabajo (id_orden),
    fecha_emision   DATE NOT NULL CONSTRAINT DF_cot_fecha DEFAULT CAST(SYSDATETIME() AS DATE),
    vigencia_hasta  DATE NOT NULL,
    estado          VARCHAR(12) NOT NULL CONSTRAINT DF_cot_estado DEFAULT 'PENDIENTE'
        CONSTRAINT CK_cot_estado CHECK (estado IN ('PENDIENTE','APROBADA','RECHAZADA','CONVERTIDA')),
    descuento       DECIMAL(12,2) NOT NULL CONSTRAINT DF_cot_desc DEFAULT 0 CONSTRAINT CK_cot_desc CHECK (descuento >= 0),
    base_imponible  DECIMAL(12,2) NOT NULL,
    igv             DECIMAL(12,2) NOT NULL,
    total           DECIMAL(12,2) NOT NULL CONSTRAINT CK_cot_total CHECK (total >= 0),
    observaciones   VARCHAR(500) NULL,
    id_creado_por   BIGINT NULL CONSTRAINT FK_cot_creador REFERENCES dbo.usuario (id_usuario),
    creado_en       DATETIME2(0) NOT NULL CONSTRAINT DF_cot_creado DEFAULT SYSDATETIME()
);
GO

IF OBJECT_ID(N'dbo.detalle_cotizacion', N'U') IS NULL
CREATE TABLE dbo.detalle_cotizacion (
    id_detalle       BIGINT IDENTITY(1,1) CONSTRAINT PK_det_cot PRIMARY KEY,
    id_cotizacion    BIGINT NOT NULL CONSTRAINT FK_dc_cotizacion REFERENCES dbo.cotizacion (id_cotizacion),
    tipo             VARCHAR(10) NOT NULL CONSTRAINT CK_dc_tipo CHECK (tipo IN ('SERVICIO','REPUESTO','MANO_OBRA')),
    id_servicio      BIGINT NULL CONSTRAINT FK_dc_servicio REFERENCES dbo.servicio (id_servicio),
    id_repuesto      BIGINT NULL CONSTRAINT FK_dc_repuesto REFERENCES dbo.repuesto (id_repuesto),
    descripcion      VARCHAR(200) NOT NULL,
    cantidad         INT NOT NULL CONSTRAINT CK_dc_cant CHECK (cantidad > 0),
    precio_unitario  DECIMAL(12,2) NOT NULL CONSTRAINT CK_dc_precio CHECK (precio_unitario >= 0)
);
GO

/* ------------------------------------------------------------
   comprobante interno (boleta/factura). NO es comprobante electrónico SUNAT.
   RN05: un solo comprobante EMITIDO por orden -> índice único filtrado.
   Los datos del cliente se copian (snapshot) para conservar el documento tal como se emitió.
   ------------------------------------------------------------ */
IF OBJECT_ID(N'dbo.comprobante', N'U') IS NULL
CREATE TABLE dbo.comprobante (
    id_comprobante     BIGINT IDENTITY(1,1) CONSTRAINT PK_comprobante PRIMARY KEY,
    id_orden           BIGINT NOT NULL CONSTRAINT FK_comp_orden REFERENCES dbo.orden_trabajo (id_orden),
    tipo               VARCHAR(7) NOT NULL CONSTRAINT CK_comp_tipo CHECK (tipo IN ('BOLETA','FACTURA')),
    serie              VARCHAR(4) NOT NULL,
    numero             VARCHAR(8) NOT NULL,
    cliente_nombre     VARCHAR(120) NOT NULL,
    cliente_tipo_doc   VARCHAR(3)   NOT NULL,
    cliente_documento  VARCHAR(12)  NOT NULL,
    vehiculo_placa     VARCHAR(10)  NULL,
    base_imponible     DECIMAL(12,2) NOT NULL,
    igv                DECIMAL(12,2) NOT NULL,
    total              DECIMAL(12,2) NOT NULL CONSTRAINT CK_comp_total CHECK (total >= 0),
    metodo_pago        VARCHAR(15) NOT NULL
        CONSTRAINT CK_comp_pago CHECK (metodo_pago IN ('EFECTIVO','TRANSFERENCIA','TARJETA','YAPE_PLIN')),
    estado             VARCHAR(8) NOT NULL CONSTRAINT DF_comp_estado DEFAULT 'EMITIDO'
        CONSTRAINT CK_comp_estado CHECK (estado IN ('EMITIDO','ANULADO')),
    motivo_anulacion   VARCHAR(200) NULL,
    fecha_emision      DATETIME2(0) NOT NULL CONSTRAINT DF_comp_fecha DEFAULT SYSDATETIME(),
    id_emitido_por     BIGINT NULL CONSTRAINT FK_comp_usuario REFERENCES dbo.usuario (id_usuario),
    CONSTRAINT UQ_comp_serie_numero UNIQUE (tipo, serie, numero)
);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UX_comprobante_orden_emitido')
    CREATE UNIQUE INDEX UX_comprobante_orden_emitido
        ON dbo.comprobante (id_orden) WHERE estado = 'EMITIDO';
GO

IF OBJECT_ID(N'dbo.detalle_comprobante', N'U') IS NULL
CREATE TABLE dbo.detalle_comprobante (
    id_detalle       BIGINT IDENTITY(1,1) CONSTRAINT PK_det_comp PRIMARY KEY,
    id_comprobante   BIGINT NOT NULL CONSTRAINT FK_dcomp_comprobante REFERENCES dbo.comprobante (id_comprobante),
    descripcion      VARCHAR(200) NOT NULL,
    cantidad         INT NOT NULL CONSTRAINT CK_dcomp_cant CHECK (cantidad > 0),
    precio_unitario  DECIMAL(12,2) NOT NULL,
    importe          DECIMAL(12,2) NOT NULL
);
GO

/* ------------------------------------------------------------
   secuencia: correlativos (OT, COT, CITA y series de comprobantes).
   Se incrementa con bloqueo de fila para no repetir números.
   ------------------------------------------------------------ */
IF OBJECT_ID(N'dbo.secuencia', N'U') IS NULL
CREATE TABLE dbo.secuencia (
    nombre  VARCHAR(20) NOT NULL CONSTRAINT PK_secuencia PRIMARY KEY,
    ultimo  INT NOT NULL CONSTRAINT DF_secuencia_ultimo DEFAULT 0
);
GO

/* ------------------------------------------------------------
   configuracion_taller: datos configurables (clave/valor)
   ------------------------------------------------------------ */
IF OBJECT_ID(N'dbo.configuracion_taller', N'U') IS NULL
CREATE TABLE dbo.configuracion_taller (
    clave  VARCHAR(40)  NOT NULL CONSTRAINT PK_configuracion PRIMARY KEY,
    valor  VARCHAR(300) NULL
);
GO

PRINT 'Esquema MECAUTO creado/verificado correctamente.';
GO
