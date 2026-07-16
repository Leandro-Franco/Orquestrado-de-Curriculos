# Gerador Inteligente de Currículos

Antes de responder a qualquer solicitação deste projeto, leia [CONTEXTO-MESTRE.md](CONTEXTO-MESTRE.md). Ele é a fonte oficial de escopo, arquitetura, decisões estabelecidas e regras de atuação da IA.

Regras inegociáveis (resumo — o documento mestre prevalece):

- **Fase atual: implementação (iniciada em 14/07/2026).** O MVP está implementado; ver [docs/ETAPAS.md](docs/ETAPAS.md) para o mapa do código e [docs/adr/](docs/adr/) para as decisões.
- Preservar as decisões já estabelecidas (seção 17 do documento mestre); apontar contradições em vez de sobrescrevê-las silenciosamente.
- Não inventar requisitos nem expandir o escopo do MVP; declarar suposições explicitamente.
- Priorizar integridade, rastreabilidade, segurança factual e controle de custos. A LLM nunca escreve na base oficial; tudo passa por proposta e aprovação humana.
- Mudança de arquitetura relevante → criar/atualizar ADR e manter o documento mestre coerente.
