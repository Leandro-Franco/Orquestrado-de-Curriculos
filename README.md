# Orquestrado-de-Curriculos

Sistema de gestão de conhecimento profissional que gera currículos direcionados a vagas, **sem inventar fatos**: a LLM apenas propõe; você aprova; só fatos aprovados entram no currículo.

Documentos de referência:

- [CONTEXTO-MESTRE.md](CONTEXTO-MESTRE.md) — escopo, arquitetura e decisões (fonte oficial).
- [docs/ETAPAS.md](docs/ETAPAS.md) — como a implementação foi organizada, etapa por etapa.
- [docs/adr/](docs/adr/) — registros de decisão arquitetural.

## Arquitetura

```
React + TS (5173)  →  Backend Core Spring Boot (8080)  →  Serviço de IA FastAPI (8000)  →  Provedor LLM
                              │                                   │
                              └────────── PostgreSQL + pgvector (5432) ──────────┘
                              (fonte oficial dos fatos)     (somente memória vetorial)
```

- O frontend só fala com o Backend Core.
- O Serviço de IA expõe apenas operações registradas (harness fechado) e só escreve na tabela vetorial.
- Preview e PDF nascem do mesmo HTML (Thymeleaf) impresso por Chromium via Playwright.

## Como executar

### Opção 1 — Docker Compose (recomendada)

```bash
cp .env.example .env      # ajuste se quiser usar o provedor real
docker compose up --build # use "sudo docker compose ..." se seu usuário não estiver no grupo docker
```

> Nesta máquina o usuário atual não está no grupo `docker`; rode com `sudo` ou execute uma vez `sudo usermod -aG docker $USER` e reinicie a sessão.

Acesse: frontend em `http://localhost:5173`, API em `http://localhost:8080`, serviço de IA em `http://localhost:8000/docs`.

Por padrão `AI_PROVIDER=fake`: todo o fluxo funciona **sem chave de API e sem custo**, com heurísticas determinísticas. Para usar a Anthropic de verdade, defina no `.env`:

```
AI_PROVIDER=anthropic
ANTHROPIC_API_KEY=sk-ant-...
```

### Opção 2 — desenvolvimento local (sem contêiner para os serviços)

```bash
# 1. Banco
docker compose up -d postgres

# 2. Serviço de IA
cd ai-service && python3 -m venv .venv && .venv/bin/pip install -r requirements.txt
.venv/bin/uvicorn app.main:app --port 8000

# 3. Backend Core (na primeira vez, instale o Chromium do Playwright)
cd backend-core
mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install chromium"
mvn spring-boot:run

# 4. Frontend
cd frontend && npm install && npm run dev
```

## Fluxo de uso

1. **Perfil** — preencha seus dados de contato e objetivo.
2. **Documentos** — importe um currículo antigo, certificado ou anotação (texto, .pdf, .txt, .md). A LLM extrai fatos e gera **propostas**.
3. **Propostas** — revise o diff (atual × proposto), aprove ou rejeite. Só a aprovação grava na base.
4. **Base de conhecimento** — consulte/adicione fatos manualmente e veja as evidências de cada um.
5. **Vagas** — cole a descrição de uma vaga e clique em "Analisar": requisitos extraídos e compatibilidade (ALTA/MÉDIA/PARCIAL/AUSENTE/INCONCLUSIVA) requisito a requisito.
6. **Currículos** — gere um currículo direcionado (estratégia → seções → validação factual), edite/regenere seção a seção, veja o preview e exporte o **PDF A4**.
7. **Métricas de IA** — tokens, custo estimado e duração de cada chamada.

## Garantias de confiabilidade implementadas

| Regra (Contexto Mestre) | Onde está no código |
|---|---|
| LLM nunca escreve na base oficial | `PropostaService` é o único caminho de proposta → fato |
| Toda extração vira proposta pendente | `DocumentoService.extrairPropostas` |
| Vaga é conteúdo não confiável | `prompts.py` (`<conteudo_nao_confiavel>`) |
| Só operações registradas | `harness.py` (`OPERACOES`) + rota 404 |
| Saída estruturada validada | schemas Pydantic + `messages.parse` |
| IDs de fatos citados são verificados | `VagaService.relacionar`, `CurriculoService.redigirSecao` |
| Validação factual pós-redação | `CurriculoService.validar` → alertas na UI |
| Métricas por chamada | envelope `usage` → tabela `chamada_ia` |
| Preview = PDF | `RenderizacaoService` + `curriculo-a4.html` |

## Limitações conhecidas (documentadas como evolução)

- Embedding determinístico local (hashing, 256 dim) — qualidade semântica limitada; troca isolada em `ai-service/app/rag.py` (ADR-003).
- Provedor fake gera propostas simplificadas (marca campos "(revisar)") — serve para exercitar o fluxo, não para qualidade de extração.
- Fontes do PDF usam pilha de sistema com fallback; para fontes 100% idênticas entre ambientes, adicione arquivos de fonte em `backend-core/src/main/resources/fonts/` e declare `@font-face` no template.
- OCR de documentos escaneados e DOCX ficam fora do MVP (seção 14).

