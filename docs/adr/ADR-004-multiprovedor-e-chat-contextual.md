# ADR-004 — Provedores de IA configuráveis pela interface e chat contextual

**Status:** Aceita (solicitada pelo usuário em 15/07/2026)

## Contexto
O MVP nasceu com provedor definido por variável de ambiente (`fake`/`anthropic`).
O usuário precisa (1) trocar de provedor — Anthropic, OpenAI, Grok, Llama/Ollama e
outras APIs populares — pela própria interface, e (2) um chat fixo em todas as
telas que reconheça o conteúdo exibido e converse sobre ele.

## Decisões

### 1. Configuração de provedor pela interface, chave só no backend
- Nova tabela `configuracao_ia` (linha única): provedor, chave, base URL e os três
  modelos por nível (econômico/intermediário/avançado).
- O frontend envia a chave **uma única vez** (PUT); o GET devolve apenas a versão
  mascarada (`•••abcd`). A chave nunca volta ao navegador — preserva a decisão
  "chaves de API ficam somente no backend" (Contexto Mestre, seção 17).
- O Backend Core injeta a configuração em cabeçalhos internos (`X-AI-*`) de cada
  chamada ao serviço de IA, que permanece **sem estado**. Sem configuração → cai
  nos padrões de ambiente (provedor `fake`).
- No PUT, `apiKey` ausente/nula = manter a chave atual; string vazia = limpar.

### 2. Cobertura de provedores via duas famílias de cliente
- `anthropic` — SDK oficial, saídas estruturadas nativas (`messages.parse`).
- `openai-compat` — SDK da OpenAI apontando para qualquer `base_url` compatível:
  OpenAI (padrão), **Grok** (`https://api.x.ai/v1`), **Ollama/Llama local**
  (`http://localhost:11434/v1`), Groq, DeepSeek etc. JSON garantido por
  `response_format` quando suportado + validação Pydantic com repetição.
- Custo estimado só é calculado para modelos com preço conhecido (tabela local);
  demais registram 0 e ficam claros na tela de métricas.

### 3. Chat contextual restrito (não é agente)
- Nova operação registrada `conversar` no harness — mesmas regras das demais:
  schema de saída, política de repetição, métricas.
- O frontend captura o **texto visível da tela atual** e o envia como
  `contexto_tela`, delimitado como conteúdo não confiável; o backend agrega
  trechos do RAG relevantes à pergunta.
- O chat é **somente leitura**: não altera fatos, propostas nem currículos; o
  prompt o instrui a orientar o usuário para os fluxos oficiais (aprovação
  humana). Sem ferramentas, sem autonomia — mantém a decisão "não existe agente
  genérico" (seção 9).
- O histórico do chat vive apenas na sessão do navegador — coerente com "o
  histórico de conversas não é memória oficial" (seção 17).

## Consequências
- Chave em texto plano no banco local: aceitável no MVP de usuário único e
  execução local; criptografia em repouso fica como evolução para publicação.
- O serviço de IA ganha dependência do pacote `openai`.
- Trocar de provedor não exige reiniciar contêineres.
