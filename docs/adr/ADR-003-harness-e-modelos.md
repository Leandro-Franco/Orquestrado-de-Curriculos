# ADR-003 — Harness de IA com operações fechadas e níveis de modelo

**Status:** Aceita (detalha as seções 9 e 10 do Contexto Mestre)

## Contexto
O frontend não chama a API de IA; toda interação passa por Backend Core → Serviço de IA (FastAPI) → provedor. O provedor deve ser substituível e os custos controlados.

## Decisão
1. **Registro fechado de operações.** O serviço de IA expõe `POST /v1/operations/{nome}` apenas para operações registradas (extrair-conhecimento, analisar-vaga, relacionar-requisitos, gerar-estrategia, gerar-secao, validar-afirmacoes, resumir-documento). Nome fora do registro → 404. Não existe agente genérico.
2. **Níveis de modelo** configuráveis por variável de ambiente:
   - `ECONOMICO` (`claude-haiku-4-5`): relacionar requisitos, resumir documento;
   - `INTERMEDIARIO` (`claude-sonnet-5`): extrair conhecimento, analisar vaga, validar afirmações;
   - `AVANCADO` (`claude-opus-4-8`): gerar estratégia e redigir seções.
3. **Provedor plugável**: `anthropic` (SDK oficial, saídas estruturadas via JSON Schema) e `fake` (determinístico, sem rede, para desenvolvimento e testes locais sem custo).
4. **Conteúdo não confiável delimitado**: descrições de vaga e textos de documentos entram no prompt dentro de `<conteudo_nao_confiavel>`, com instrução explícita de tratá-los como dado.
5. **Saída estruturada obrigatória**: cada operação tem um schema Pydantic; resposta inválida gera uma nova tentativa (máx. 2) e depois erro.
6. **Métricas**: o serviço de IA devolve `usage` (modelo, tokens, cache, duração, custo estimado, tentativas, validade) em todo envelope; o Backend Core persiste em `chamada_ia`.
7. **Embeddings**: no MVP, embedding determinístico local (hashing de tokens, 256 dimensões) — suficiente para busca semântica aproximada sem dependência externa. Troca por provedor real é uma mudança isolada em `rag.py`.

## Consequências
- O sistema funciona de ponta a ponta sem chave de API (provedor fake).
- Adicionar operação = registrar prompt + schema + nível; nada de ferramentas livres.
- O embedding hash tem qualidade semântica limitada; documentado como evolução.
