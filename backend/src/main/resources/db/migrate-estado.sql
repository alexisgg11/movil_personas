-- Convierte el indicador antiguo sin perder los registros ya inactivos.
-- WHERE estado IS NULL evita reactivar registros al repetir el arranque.
ALTER TABLE personas.persona ADD COLUMN IF NOT EXISTS activo BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE personas.persona ADD COLUMN IF NOT EXISTS estado VARCHAR(8);
UPDATE personas.persona SET estado = CASE WHEN activo THEN 'ACTIVO' ELSE 'INACTIVO' END WHERE estado IS NULL;
ALTER TABLE personas.persona ALTER COLUMN estado SET DEFAULT 'ACTIVO';
ALTER TABLE personas.persona ALTER COLUMN estado SET NOT NULL;
ALTER TABLE personas.persona DROP COLUMN activo;
ALTER TABLE personas.persona DROP CONSTRAINT IF EXISTS ck_persona_estado;
ALTER TABLE personas.persona ADD CONSTRAINT ck_persona_estado CHECK (estado IN ('ACTIVO', 'INACTIVO'));
