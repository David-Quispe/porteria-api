ALTER TABLE registro_acceso DROP CONSTRAINT ck_registro_resultado;
ALTER TABLE registro_acceso ADD CONSTRAINT ck_registro_resultado CHECK (resultado IN
    ('AUTORIZADO', 'NO_AUTORIZADO', 'INACTIVO', 'VENCIDO', 'FUERA_DE_HORARIO', 'ENTRADA_REPETIDA'));

CREATE INDEX ix_registro_persona_autorizado_fecha
    ON registro_acceso (persona_id, fecha_hora DESC, id DESC)
    WHERE resultado = 'AUTORIZADO';
