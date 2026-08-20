-- Configuração do provedor de IA feita pela interface (ADR-004).
-- Linha única; a chave nunca é exposta de volta ao frontend.
CREATE TABLE configuracao_ia (
    id                   SMALLINT PRIMARY KEY DEFAULT 1 CHECK (id = 1),
    provider             VARCHAR(30) NOT NULL DEFAULT 'fake', -- fake | anthropic | openai | grok | ollama | personalizado
    api_key              TEXT,
    base_url             VARCHAR(300),
    modelo_economico     VARCHAR(80),
    modelo_intermediario VARCHAR(80),
    modelo_avancado      VARCHAR(80),
    atualizado_em        TIMESTAMPTZ NOT NULL DEFAULT now()
);
INSERT INTO configuracao_ia (id) VALUES (1);
