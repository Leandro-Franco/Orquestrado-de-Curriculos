# ADR-005 — Avaliação do Spring AI e adoção de contratos tipados no Backend Core

**Status:** Aceita (avaliação solicitada pelo usuário em 20/08/2026)

## Contexto

O Spring oferece o **Spring AI** (1.0 GA em maio/2025), framework de engenharia de IA para
aplicações Java. Suas capacidades principais são:

- `ChatClient` — API fluente sobre ~20 provedores de modelo (Anthropic, OpenAI, Ollama, Azure…);
- **saída estruturada** — mapeamento da resposta do modelo para POJOs, com type safety;
- `VectorStore` — abstração portátil sobre ~20 bancos vetoriais, **incluindo PostgreSQL/pgvector**,
  com linguagem de filtro por metadados;
- *advisors* para RAG, memória de conversa e chamada de ferramentas.

A pergunta avaliada: o Spring AI é viável neste projeto?

## Análise

Confrontando as capacidades do Spring AI com o que já existe:

| Capacidade do Spring AI | Onde já está implementado |
|---|---|
| `ChatClient` multiprovedor | `ai-service/app/providers/` (Anthropic + OpenAI-compatible: OpenAI, Grok, Ollama, personalizado — ADR-004) |
| Saída estruturada | `ai-service/app/schemas.py` (Pydantic) + política de repetição no harness |
| `VectorStore` sobre pgvector | `ai-service/app/rag.py` (ADR-003) |
| Advisors/RAG | `ChatController` + `rag.py` |

O Spring AI resolveria **exatamente os mesmos problemas**, porém em Java, dentro do Backend Core.
Adotá-lo como orquestrador implicaria uma de duas coisas:

1. **Duplicar** o harness em Java — dois códigos fazendo o mesmo, o dobro de manutenção; ou
2. **Substituir** o serviço Python — o que reverte a decisão da seção 17 do Contexto Mestre
   ("a orquestração de IA será isolada em um serviço Python com FastAPI"), descarta código
   testado e em produção local, e apaga a fronteira de segurança que mantém a LLM longe da
   base canônica (seções 9 e 13).

A seção 16, regra 5, do Contexto Mestre também é explícita: *"Não adicione tecnologias apenas
para valorizar o portfólio."* O ganho aqui seria de vitrine, não de arquitetura.

## Decisão

1. **Não adotar o Spring AI como orquestrador.** A orquestração permanece no serviço Python
   (decisão da seção 17 preservada).

2. **Adotar o princípio que o Spring AI defende no lado Java**: respostas de IA como objetos
   tipados, não como `Map<String, Object>`. Este era um problema real de manutenção — havia
   8 pontos com `@SuppressWarnings("unchecked")` e leitura de mapas por string mágica em
   `AiClient`, `DocumentoService`, `VagaService`, `CurriculoService` e `ChatController`.

   Cria-se `ContratosIa` — records Java que espelham `schemas.py`, desserializados por Jackson
   com estratégia *snake_case*. `AiClient.executar(operacao, entrada, Contrato.class)` devolve
   o objeto tipado; o envelope `usage` também vira record.

   O contrato passa a ser validado **nas duas pontas**: Pydantic no serviço de IA (que já
   garante a forma antes de responder) e o record no Backend Core — defesa em profundidade,
   e o `schemas.py` ganha um espelho legível em Java.

## Consequências

- Erros de contrato aparecem em tempo de compilação, não em produção.
- `ContratosIa.java` e `schemas.py` precisam evoluir juntos; a simetria de nomes é proposital
  para tornar a divergência óbvia em revisão.
- Nenhuma dependência nova: apenas Jackson, já presente via Spring Boot.
- Onde o resultado é persistido como JSONB (`vaga.analise`, `curriculo.estrategia`), converte-se
  o record de volta para mapa com `AiClient.comoMapa` — o formato gravado permanece idêntico,
  sem migração de dados.

## Quando revisitar

Se o projeto algum dia precisar **eliminar o serviço Python** (por exemplo: publicação em um
ambiente onde manter dois runtimes seja caro demais), o Spring AI passa a ser a alternativa
natural — ele cobre provedores, saída estruturada e pgvector com um só framework. Nesse
cenário, `ContratosIa` já descreve os contratos a implementar, e a migração seria
substituir a chamada HTTP por um `ChatClient` local, mantendo `harness`/`prompts` como
especificação. Este ADR deve ser revisto (e a seção 17 renegociada) antes de tal mudança.
