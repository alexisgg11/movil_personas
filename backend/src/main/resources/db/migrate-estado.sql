-- Conserva los indicadores existentes y permite repetir el arranque sin reactivaciones.
DO '
DECLARE
    tipo TEXT;
    invalido BOOLEAN;
BEGIN
    LOCK TABLE personas.persona IN ACCESS EXCLUSIVE MODE;
    SELECT data_type INTO tipo FROM information_schema.columns
      WHERE table_schema = ''personas'' AND table_name = ''persona'' AND column_name = ''estado'';
    IF tipo IS NULL THEN
        IF EXISTS (SELECT 1 FROM information_schema.columns
          WHERE table_schema = ''personas'' AND table_name = ''persona'' AND column_name = ''activo'') THEN
            ALTER TABLE personas.persona RENAME COLUMN activo TO estado;
        ELSE
            ALTER TABLE personas.persona ADD COLUMN estado BOOLEAN NOT NULL DEFAULT TRUE;
        END IF;
    ELSIF tipo IN (''character varying'', ''text'') THEN
        SELECT EXISTS (SELECT 1 FROM personas.persona
          WHERE estado IS NULL OR estado NOT IN (''ACTIVO'', ''INACTIVO'')) INTO invalido;
        IF invalido THEN
            RAISE EXCEPTION ''Hay estados desconocidos; la conversion requiere corregirlos antes de continuar.'';
        END IF;
        ALTER TABLE personas.persona DROP CONSTRAINT IF EXISTS ck_persona_estado;
        ALTER TABLE personas.persona ALTER COLUMN estado DROP DEFAULT;
        ALTER TABLE personas.persona ALTER COLUMN estado TYPE BOOLEAN
          USING CASE estado WHEN ''ACTIVO'' THEN TRUE WHEN ''INACTIVO'' THEN FALSE END;
    ELSIF tipo <> ''boolean'' THEN
        RAISE EXCEPTION ''El tipo de estado no admite una conversion segura: %'', tipo;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns
      WHERE table_schema = ''personas'' AND table_name = ''persona'' AND column_name = ''activo'') THEN
        IF EXISTS (SELECT 1 FROM personas.persona WHERE estado IS DISTINCT FROM activo) THEN
            RAISE EXCEPTION ''Los indicadores estado y activo no coinciden; no se modificaron los registros.'';
        END IF;
        ALTER TABLE personas.persona DROP COLUMN activo;
    END IF;
    ALTER TABLE personas.persona DROP CONSTRAINT IF EXISTS ck_persona_estado;
    ALTER TABLE personas.persona ALTER COLUMN estado SET NOT NULL;
    ALTER TABLE personas.persona ALTER COLUMN estado SET DEFAULT TRUE;
END';
