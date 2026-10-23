ALTER TABLE personas.persona ADD COLUMN IF NOT EXISTS direccion_id BIGINT;
DO '
DECLARE
    fila RECORD;
    candidato VARCHAR(6);
    coincidencias INTEGER;
    sufijo TEXT;
    normalizado TEXT;
    via TEXT;
    nueva_id BIGINT;
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=''personas'' AND table_name=''persona'' AND column_name=''direccion'') THEN
        FOR fila IN SELECT dni, direccion FROM personas.persona WHERE direccion_id IS NULL LOOP
            sufijo := trim(regexp_replace(fila.direccion, ''^.*,'', ''''));
            normalizado := translate(upper(sufijo), ''ÁÉÍÓÚÜ'', ''AEIOUU'');
            IF normalizado = ''26 DE OCTUBRE'' THEN normalizado := ''VEINTISEIS DE OCTUBRE''; END IF;
            SELECT count(*), min(id) INTO coincidencias, candidato FROM personas.distrito
                WHERE translate(upper(nombre), ''ÁÉÍÓÚÜ'', ''AEIOUU'') = normalizado;
            IF coincidencias <> 1 THEN
                RAISE EXCEPTION ''La direccion del DNI % requiere identificar su distrito antes de migrar. Se conserva el texto original.'', fila.dni;
            END IF;
            via := CASE WHEN position('','' IN fila.direccion) > 0 THEN trim(regexp_replace(fila.direccion, '',[^,]*$'', '''')) ELSE trim(fila.direccion) END;
            IF length(via) = 0 THEN via := trim(fila.direccion); END IF;
            INSERT INTO personas.direccion(detalle, distrito_id) VALUES (via, candidato) RETURNING id INTO nueva_id;
            UPDATE personas.persona SET direccion_id = nueva_id WHERE dni = fila.dni;
        END LOOP;
        ALTER TABLE personas.persona DROP COLUMN direccion;
    END IF;
    ALTER TABLE personas.persona ALTER COLUMN direccion_id SET NOT NULL;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid=''personas.persona''::regclass AND conname=''fk_persona_direccion'') THEN
        ALTER TABLE personas.persona ADD CONSTRAINT fk_persona_direccion FOREIGN KEY (direccion_id) REFERENCES personas.direccion(id) ON DELETE RESTRICT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid=''personas.persona''::regclass AND conname=''uq_persona_direccion'') THEN
        ALTER TABLE personas.persona ADD CONSTRAINT uq_persona_direccion UNIQUE (direccion_id);
    END IF;
END ';
