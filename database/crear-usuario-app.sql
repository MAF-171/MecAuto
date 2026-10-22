/* =====================================================================
   MecAuto - Usuario de SQL Server para la aplicación (opcional, recomendado)
   Archivo: database/crear-usuario-app.sql

   Crea un inicio de sesión (LOGIN) y un usuario con permisos SOLO sobre
   MECAUTO, para que el backend NO use la cuenta "sa".

   ANTES DE EJECUTAR: cambie la contraseña de la línea marcada. El script se
   detiene si no la cambia. NO suba su contraseña real a Git.

   Requisito: el servidor debe permitir "Autenticación de SQL Server y de
   Windows" (SSMS > clic derecho en el servidor > Propiedades > Seguridad).
   ===================================================================== */

USE master;
GO

DECLARE @Clave NVARCHAR(128) = N'MECAUTOFLORES61305069';   -- <== escriba aquí una contraseña fuerte (mín. 12 caracteres)

IF @Clave = N'CAMBIAR_ESTA_CLAVE'
    THROW 50000, N'Edite la contraseña (@Clave) antes de ejecutar este script.', 1;

IF NOT EXISTS (SELECT 1 FROM sys.server_principals WHERE name = N'mecauto_app')
BEGIN
    DECLARE @sql NVARCHAR(MAX) =
        N'CREATE LOGIN mecauto_app WITH PASSWORD = N''' + REPLACE(@Clave, N'''', N'''''') + N''', CHECK_POLICY = ON;';
    EXEC (@sql);
END
GO

USE MECAUTO;
GO

IF NOT EXISTS (SELECT 1 FROM sys.database_principals WHERE name = N'mecauto_app')
    CREATE USER mecauto_app FOR LOGIN mecauto_app;
GO

-- Permisos de lectura y escritura sobre los datos (no puede crear ni borrar tablas).
ALTER ROLE db_datareader ADD MEMBER mecauto_app;
ALTER ROLE db_datawriter ADD MEMBER mecauto_app;
GO

PRINT N'Usuario mecauto_app listo. Úselo en MECAUTO_DB_USER y MECAUTO_DB_PASSWORD del backend.';
GO
