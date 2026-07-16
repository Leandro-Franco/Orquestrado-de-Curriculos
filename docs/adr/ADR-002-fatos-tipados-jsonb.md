# ADR-002 — Memória estruturada como fatos tipados com payload JSONB

**Status:** Aceita

## Contexto
A memória estruturada (seção 5.1) contém oito tipos de informação (experiências, formações, cursos, certificações, projetos, habilidades, idiomas, links). Modelar cada tipo como tabela própria multiplicaria entidades, repositórios, controladores e telas, sem ganho funcional no MVP de usuário único.

## Decisão
Uma única tabela `fato` com:
- `tipo` — enumeração fechada dos oito tipos;
- `payload` JSONB — campos específicos do tipo, validados no Backend Core antes da persistência;
- `status` — apenas fatos `APROVADO` alimentam análise de vagas e geração de currículos.

O fluxo de propostas, o diff visual, as evidências e a reindexação vetorial operam de forma uniforme sobre qualquer tipo de fato.

## Consequências
- Menos código e um fluxo de aprovação único para todos os tipos.
- Validação por tipo fica no serviço Java (`FatoService`), não no banco.
- Evolução futura: se um tipo ganhar regras relacionais fortes (ex.: experiência ligada a empresa normalizada), pode ser promovido a tabela própria com migração dirigida.
