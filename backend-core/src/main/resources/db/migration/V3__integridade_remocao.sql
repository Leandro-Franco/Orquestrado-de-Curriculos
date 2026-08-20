-- Permite remover documentos, fatos e vagas sem violar FKs de referência
-- histórica: a proposta/currículo guardam o vínculo enquanto ele existir e
-- passam a NULL quando a origem é removida (a auditoria preserva a história).

ALTER TABLE proposta DROP CONSTRAINT proposta_documento_origem_id_fkey;
ALTER TABLE proposta ADD CONSTRAINT proposta_documento_origem_id_fkey
    FOREIGN KEY (documento_origem_id) REFERENCES documento (id) ON DELETE SET NULL;

ALTER TABLE proposta DROP CONSTRAINT proposta_fato_alvo_id_fkey;
ALTER TABLE proposta ADD CONSTRAINT proposta_fato_alvo_id_fkey
    FOREIGN KEY (fato_alvo_id) REFERENCES fato (id) ON DELETE SET NULL;

ALTER TABLE curriculo DROP CONSTRAINT curriculo_vaga_id_fkey;
ALTER TABLE curriculo ADD CONSTRAINT curriculo_vaga_id_fkey
    FOREIGN KEY (vaga_id) REFERENCES vaga (id) ON DELETE SET NULL;
