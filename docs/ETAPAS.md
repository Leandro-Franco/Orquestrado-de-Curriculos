# Etapas da implementação

Registro de como o MVP foi construído, para estudo e revisão. Cada etapa é
coesa e pode ser lida de forma independente.

## Etapa 1 — Fundações e banco de dados

- `docker-compose.yml` — quatro serviços: `postgres` (pgvector/pg16), `ai-service`, `backend-core`, `frontend`.
- `backend-core/src/main/resources/db/migration/V1__schema.sql` — esquema completo (Flyway): perfil, documento, fato, evidência, proposta, vaga, requisito_vaga, curriculo, secao_curriculo, versao_curriculo, chamada_ia, evento_auditoria, trecho_vetorial.
- ADRs 001 (PDF), 002 (fatos tipados com JSONB), 003 (harness e níveis de modelo).

## Etapa 2 — Backend Core (Spring Boot, Java 21)

Pacotes em `backend-core/src/main/java/br/com/curriculos/`:

- `dominio/` — 12 entidades JPA espelhando o esquema (payloads JSONB via `@JdbcTypeCode(SqlTypes.JSON)`).
- `repositorio/` — Spring Data JPA; consulta nativa de agregação de métricas em `ChamadaIaRepository`.
- `servico/` — o coração das regras:
  - `AiClient` — único canal com o serviço de IA; grava métricas de cada chamada.
  - `FatoService` — validação determinística por tipo (sem IA) e reindexação incremental.
  - `DocumentoService` — importação (texto, PDF via PDFBox, txt/md), normalização, dedupe por SHA-256, extração via IA → propostas pendentes.
  - `PropostaService` — único caminho proposta → fato; cria evidência e audita.
  - `VagaService` — análise da vaga + compatibilidade requisito a requisito (IDs de fatos citados pela LLM são validados contra a base).
  - `CurriculoService` — estratégia → redação por seção → validação factual → versão; regeneração por seção.
  - `RenderizacaoService` — Thymeleaf → HTML único para preview e PDF (Playwright/Chromium, A4).
- `web/` — controladores REST (`/api/...`) e tratador global de erros.

## Etapa 3 — Serviço de IA (FastAPI, Python 3.12)

Em `ai-service/app/`:

- `harness.py` — registro fechado de 7 operações com nível de modelo, schema de saída e política de repetição; calcula custo estimado.
- `prompts.py` — prompts compactos; conteúdo externo delimitado em `<conteudo_nao_confiavel>`.
- `schemas.py` — contratos Pydantic de todas as operações (saída estruturada obrigatória).
- `providers/` — `fake` (determinístico, sem custo, para desenvolvimento) e `anthropic` (SDK oficial, `messages.parse` com `output_format`).
- `rag.py` — memória vetorial no pgvector; embedding hash local de 256 dim; upsert/busca/remoção incremental.
- `routers/` — `POST /v1/operations/{nome}` (404 fora do registro) e `/v1/index/*`.

## Etapa 4 — Frontend (React + TypeScript + Vite)

Em `frontend/src/`:

- `api.ts` — cliente HTTP do Backend Core (o frontend nunca fala com a IA).
- `pages/` — Perfil, Conhecimento (fatos + evidências), Documentos (importação), Propostas (diff visual + aprovar/rejeitar), Vagas e VagaDetalhe (compatibilidade), Currículos e CurriculoEditor (edição/regeneração por seção, alertas de validação, preview em iframe, exportação PDF, versões), Métricas.
- Servido por nginx no compose, com proxy `/api` → backend.

## Etapa 5 — Verificação

- Backend: `mvn compile` — **OK** (Java 25 local, target 21).
- Serviço de IA: sintaxe validada com `py_compile` — **OK**. Execução de runtime não testada nesta máquina (sem `pip`/`python3-venv`; Docker exige sudo/grupo docker).
- Frontend: `npm install` + `tsc --noEmit` — **OK**.
- Teste de ponta a ponta: requer `docker compose up --build` (ver README).

## Etapa 6 — Multiprovedor, chat contextual e testes

- **ADR-004**: provedores configuráveis pela interface (Anthropic, OpenAI, Grok,
  Ollama/Llama e APIs OpenAI-compatible) com chave apenas no backend (tabela
  `configuracao_ia`, migração V2, cabeçalhos internos `X-AI-*`); chat fixo
  somente-leitura com contexto da tela (operação `conversar` no harness).
- **Testes**: unitários do backend (18, JUnit+Mockito), pytest do serviço de IA
  (harness/API/RAG com provedor fake) e E2E autolimpante (`scripts/teste_e2e.py`)
  que cobre a PoC de PDF da seção 12. Migração V3 ajustou FKs para remoção limpa
  e os controladores ganharam DELETE de documento, vaga e currículo.
- Rodar: `make test` (ver README).

## Etapa 7 — Avaliação do Spring AI e contratos tipados

- **ADR-005**: o Spring AI foi avaliado e **não** adotado como orquestrador (duplicaria o
  harness Python ou reverteria a seção 17). Adotou-se o princípio que ele defende no lado
  Java: respostas de IA tipadas.
- `servico/ia/ContratosIa.java` — records que espelham `schemas.py`, com Jackson em
  snake_case e listas nunca nulas. `AiClient.executar(op, entrada, Contrato.class)` devolve
  o objeto tipado; o envelope `usage` também virou record.
- Removidos todos os `@SuppressWarnings("unchecked")` do caminho de IA
  (`AiClient`, `DocumentoService`, `VagaService`, `CurriculoService`, `ChatController`).
- `scripts/verificar_contratos.py` — guardião que falha se `schemas.py` e `ContratosIa.java`
  divergirem; entra em `make test`.

## Sugestões de estudo (ordem de leitura)

1. `V1__schema.sql` — o modelo de dados conta a história do sistema.
2. `DocumentoService` → `PropostaService` — o ciclo proposta/aprovação.
3. `harness.py` + `prompts.py` — como a IA é contida.
4. `VagaService` → `CurriculoService` — análise e geração em etapas.
5. `RenderizacaoService` + `curriculo-a4.html` — preview = PDF.
6. Páginas React na mesma ordem do fluxo de uso.
