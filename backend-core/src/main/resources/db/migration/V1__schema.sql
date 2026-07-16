-- Esquema inicial do Gerador Inteligente de Currículos.
-- O banco relacional é a fonte oficial dos fatos (Contexto Mestre, seção 5.1).

CREATE EXTENSION IF NOT EXISTS vector;

-- Perfil canônico: linha única (MVP de usuário único).
CREATE TABLE perfil (
    id                  SMALLINT PRIMARY KEY DEFAULT 1 CHECK (id = 1),
    nome_completo       VARCHAR(200),
    email               VARCHAR(200),
    telefone            VARCHAR(50),
    localizacao         VARCHAR(200),
    titulo_profissional VARCHAR(200),
    objetivo            TEXT,
    preferencias        JSONB,
    atualizado_em       TIMESTAMPTZ NOT NULL DEFAULT now()
);
INSERT INTO perfil (id) VALUES (1);

-- Memória documental: originais preservados como evidência (seção 5.2).
CREATE TABLE documento (
    id                    BIGSERIAL PRIMARY KEY,
    titulo                VARCHAR(300) NOT NULL,
    nome_arquivo          VARCHAR(300),
    mime_type             VARCHAR(100),
    caminho_armazenamento VARCHAR(500),
    sha256                CHAR(64),
    origem                VARCHAR(20)  NOT NULL, -- TEXTO | ARQUIVO
    texto_extraido        TEXT,
    status                VARCHAR(20)  NOT NULL DEFAULT 'IMPORTADO',
    criado_em             TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Memória estruturada: fatos aprovados, tipados, com payload validado por tipo (ADR-002).
CREATE TABLE fato (
    id            BIGSERIAL PRIMARY KEY,
    tipo          VARCHAR(30) NOT NULL, -- EXPERIENCIA | FORMACAO | CURSO | CERTIFICACAO | PROJETO | HABILIDADE | IDIOMA | LINK
    payload       JSONB       NOT NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'APROVADO',
    criado_em     TIMESTAMPTZ NOT NULL DEFAULT now(),
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_fato_tipo ON fato (tipo);

-- Associação fato <-> documento de origem (evidência).
CREATE TABLE evidencia (
    id           BIGSERIAL PRIMARY KEY,
    fato_id      BIGINT NOT NULL REFERENCES fato (id) ON DELETE CASCADE,
    documento_id BIGINT NOT NULL REFERENCES documento (id) ON DELETE CASCADE,
    trecho       TEXT,
    criado_em    TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Toda extração da LLM vira proposta pendente; a LLM nunca escreve na base oficial (seção 4).
CREATE TABLE proposta (
    id                   BIGSERIAL PRIMARY KEY,
    acao                 VARCHAR(20) NOT NULL, -- CRIAR | ATUALIZAR
    tipo_fato            VARCHAR(30) NOT NULL,
    fato_alvo_id         BIGINT REFERENCES fato (id),
    payload_anterior     JSONB,
    payload_proposto     JSONB       NOT NULL,
    documento_origem_id  BIGINT REFERENCES documento (id),
    justificativa        TEXT,
    trecho_evidencia     TEXT,
    confianca            NUMERIC(3, 2),
    status               VARCHAR(20) NOT NULL DEFAULT 'PENDENTE', -- PENDENTE | APROVADA | REJEITADA
    modelo               VARCHAR(60),
    versao_prompt        VARCHAR(20),
    decidido_em          TIMESTAMPTZ,
    criado_em            TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_proposta_status ON proposta (status);

-- Vagas: a descrição bruta é conteúdo externo não confiável (seção 7).
CREATE TABLE vaga (
    id              BIGSERIAL PRIMARY KEY,
    titulo          VARCHAR(300),
    empresa         VARCHAR(200),
    descricao_bruta TEXT        NOT NULL,
    analise         JSONB,
    status          VARCHAR(20) NOT NULL DEFAULT 'CADASTRADA', -- CADASTRADA | ANALISADA
    criado_em       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE requisito_vaga (
    id                 BIGSERIAL PRIMARY KEY,
    vaga_id            BIGINT      NOT NULL REFERENCES vaga (id) ON DELETE CASCADE,
    descricao          TEXT        NOT NULL,
    tipo               VARCHAR(20) NOT NULL, -- OBRIGATORIO | DESEJAVEL
    categoria          VARCHAR(50),
    compatibilidade    VARCHAR(20),          -- ALTA | MEDIA | PARCIAL | AUSENTE | INCONCLUSIVA
    fatos_relacionados JSONB,
    justificativa      TEXT
);

-- Currículo: modelo estruturado é a fonte oficial; o HTML é artefato derivado (seção 12).
CREATE TABLE curriculo (
    id            BIGSERIAL PRIMARY KEY,
    vaga_id       BIGINT REFERENCES vaga (id),
    titulo        VARCHAR(300) NOT NULL,
    template      VARCHAR(50)  NOT NULL DEFAULT 'classico',
    estrategia    JSONB,
    status        VARCHAR(20)  NOT NULL DEFAULT 'RASCUNHO',
    criado_em     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    atualizado_em TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE secao_curriculo (
    id                BIGSERIAL PRIMARY KEY,
    curriculo_id      BIGINT      NOT NULL REFERENCES curriculo (id) ON DELETE CASCADE,
    tipo              VARCHAR(30) NOT NULL, -- TITULO | RESUMO | EXPERIENCIAS | PROJETOS | COMPETENCIAS | FORMACAO | CURSOS | IDIOMAS
    ordem             INT         NOT NULL DEFAULT 0,
    titulo            VARCHAR(200),
    conteudo          TEXT,
    fatos_utilizados  JSONB,
    alertas_validacao JSONB,
    atualizado_em     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE versao_curriculo (
    id           BIGSERIAL PRIMARY KEY,
    curriculo_id BIGINT      NOT NULL REFERENCES curriculo (id) ON DELETE CASCADE,
    numero       INT         NOT NULL,
    snapshot     JSONB       NOT NULL,
    nota         VARCHAR(300),
    criado_em    TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Métricas de cada chamada à LLM (seção 10).
CREATE TABLE chamada_ia (
    id                  BIGSERIAL PRIMARY KEY,
    operacao            VARCHAR(60) NOT NULL,
    modelo              VARCHAR(60),
    tokens_entrada      INT              DEFAULT 0,
    tokens_saida        INT              DEFAULT 0,
    tokens_cache        INT              DEFAULT 0,
    trechos_recuperados INT              DEFAULT 0,
    duracao_ms          INT              DEFAULT 0,
    custo_estimado      NUMERIC(12, 6)   DEFAULT 0,
    tentativas          INT              DEFAULT 1,
    valida              BOOLEAN          DEFAULT TRUE,
    criado_em           TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Trilha de auditoria de decisões e alterações (seção 5.4).
CREATE TABLE evento_auditoria (
    id          BIGSERIAL PRIMARY KEY,
    entidade    VARCHAR(50) NOT NULL,
    entidade_id BIGINT,
    acao        VARCHAR(50) NOT NULL,
    detalhe     JSONB,
    criado_em   TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Memória vetorial (seção 5.3): escrita e lida exclusivamente pelo serviço de IA.
-- O RAG recupera evidências; nunca é fonte da verdade.
CREATE TABLE trecho_vetorial (
    id        BIGSERIAL PRIMARY KEY,
    origem    VARCHAR(20) NOT NULL, -- DOCUMENTO | FATO
    origem_id BIGINT      NOT NULL,
    conteudo  TEXT        NOT NULL,
    embedding vector(256) NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_trecho_origem ON trecho_vetorial (origem, origem_id);
