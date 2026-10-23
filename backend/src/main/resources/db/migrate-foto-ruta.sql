ALTER TABLE personas.persona DROP CONSTRAINT IF EXISTS persona_foto_ruta_check;
ALTER TABLE personas.persona DROP CONSTRAINT IF EXISTS ck_persona_foto_ruta;
ALTER TABLE personas.persona ADD CONSTRAINT ck_persona_foto_ruta
    CHECK (foto_ruta ~ '^([0-9]{8}/)?[0-9a-f-]{36}[.]jpg$'
        AND (foto_ruta NOT LIKE '%/%' OR split_part(foto_ruta, '/', 1) = dni));
