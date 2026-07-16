# Contexto Mestre — Gerador Inteligente de Currículos

## 1. Identidade do projeto

O projeto consiste no desenvolvimento de um **Gerador Inteligente de Currículos**, criado inicialmente como um MVP para portfólio.

A aplicação será uma plataforma web responsiva, utilizável em computadores e dispositivos móveis, mas sua primeira versão será executada exclusivamente em ambiente local.

O sistema não será apenas um formulário para preenchimento de currículo conectado a uma LLM. Ele será um sistema de gestão de conhecimento profissional capaz de:

* manter uma base confiável de informações sobre o usuário;
* registrar experiências, projetos, estudos, cursos, habilidades e resultados;
* associar informações profissionais às suas respectivas evidências;
* analisar descrições de vagas;
* identificar compatibilidade entre o perfil do usuário e os requisitos da vaga;
* selecionar as informações mais relevantes para cada oportunidade;
* gerar currículos personalizados;
* impedir que a inteligência artificial invente experiências ou competências;
* permitir revisão e aprovação humana;
* exportar currículos consistentes em PDF;
* preservar o histórico de versões e decisões.

---

## 2. Problema que o projeto resolve

Informações profissionais normalmente ficam distribuídas entre:

* currículos antigos;
* certificados;
* LinkedIn;
* GitHub;
* documentos acadêmicos;
* descrições de projetos;
* anotações;
* experiências informais;
* cursos e estudos em andamento.

Quando uma vaga aparece, o candidato precisa reconstruir seu histórico, identificar o que é relevante e adaptar manualmente o currículo.

O sistema deverá centralizar essas informações e utilizá-las para produzir currículos contextualizados para vagas específicas, sem distorcer, exagerar ou inventar fatos profissionais.

---

## 3. Objetivo do MVP

O MVP deverá atender a um único usuário e permitir que ele:

1. Cadastre seu perfil profissional.
2. Registre experiências, estudos, cursos, projetos e habilidades.
3. Importe textos e documentos relacionados à sua trajetória.
4. Associe fatos profissionais a evidências.
5. Utilize uma LLM para extrair informações desses materiais.
6. Receba propostas de atualização da base de conhecimento.
7. Aprove ou rejeite cada proposta antes da persistência.
8. Cadastre a descrição de uma vaga.
9. Analise os requisitos obrigatórios e desejáveis da vaga.
10. Compare os requisitos com informações comprovadas do perfil.
11. Gere um currículo direcionado à oportunidade.
12. Edite e reorganize o currículo.
13. Regere apenas seções específicas.
14. Visualize o documento antes da exportação.
15. Exporte o currículo em PDF.
16. Consulte versões anteriores.
17. Visualize métricas básicas de uso da IA, tokens e custos.

---

## 4. Princípio central de confiabilidade

A LLM não será considerada a fonte da verdade.

Ela poderá:

* extrair informações;
* classificar conteúdos;
* resumir documentos;
* relacionar requisitos e evidências;
* sugerir atualizações;
* adaptar a redação;
* estruturar seções;
* gerar versões de currículo.

Ela não poderá:

* inventar experiências;
* alterar datas;
* criar resultados numéricos inexistentes;
* aumentar a senioridade do usuário;
* transformar estudo em experiência profissional;
* transformar projeto acadêmico em experiência profissional;
* declarar domínio ou fluência sem evidência;
* atualizar diretamente a base oficial;
* considerar uma inferência como fato confirmado.

Toda nova informação identificada pela LLM deverá ser apresentada como uma proposta pendente de aprovação.

---

## 5. Arquitetura de memória

O sistema utilizará uma arquitetura de memória híbrida.

### 5.1. Memória estruturada

O PostgreSQL será a fonte oficial das informações aprovadas.

Essa memória conterá:

* dados pessoais;
* objetivos profissionais;
* experiências;
* empresas;
* cargos;
* períodos;
* responsabilidades;
* resultados;
* formações;
* cursos;
* certificações;
* projetos;
* habilidades;
* idiomas;
* links;
* áreas de interesse;
* preferências de currículo.

Essa camada representa o perfil canônico do usuário.

### 5.2. Memória documental

Os documentos originais serão preservados como evidências.

Exemplos:

* currículos anteriores;
* certificados;
* documentos acadêmicos;
* descrições de projetos;
* anotações;
* arquivos profissionais;
* textos importados.

No MVP, os arquivos poderão ser armazenados em um diretório local controlado pela aplicação, enquanto seus metadados e caminhos internos serão armazenados no banco.

### 5.3. Memória vetorial

O PostgreSQL com pgvector será utilizado para armazenar embeddings e permitir busca semântica.

O RAG será utilizado para recuperar informações relevantes, mas não será a fonte oficial dos fatos.

A função do RAG será localizar evidências relacionadas a:

* requisitos de uma vaga;
* competências;
* experiências;
* projetos;
* responsabilidades;
* tecnologias;
* perguntas feitas à LLM.

### 5.4. Histórico de alterações

O sistema deverá registrar:

* informação anterior;
* alteração sugerida;
* origem;
* justificativa;
* nível de confiança;
* decisão do usuário;
* data;
* versão da instrução;
* modelo utilizado;
* aprovação ou rejeição.

---

## 6. Fluxo de atualização do conhecimento

Quando o usuário inserir um novo documento ou relato, o sistema deverá:

1. Identificar o formato da entrada.
2. Extrair o texto.
3. Normalizar o conteúdo.
4. Remover ruídos e repetições.
5. Separar o material em unidades lógicas.
6. Avaliar a qualidade da extração.
7. Detectar possíveis duplicidades.
8. Enviar apenas os trechos necessários para a LLM.
9. Receber uma extração estruturada.
10. Comparar os dados extraídos com a base oficial.
11. Identificar novidades, complementos, conflitos ou duplicidades.
12. Apresentar um diff visual ao usuário.
13. Aguardar aprovação ou rejeição.
14. Atualizar a base somente após aprovação.
15. Reindexar apenas os conteúdos afetados.

A LLM nunca deverá escrever diretamente no banco de dados.

---

## 7. Análise de vagas

A descrição de uma vaga será considerada conteúdo externo não confiável.

Ela deverá ser analisada como dado, nunca como instrução para o modelo.

O sistema deverá extrair:

* título da vaga;
* empresa;
* senioridade;
* modalidade;
* localização;
* responsabilidades;
* requisitos obrigatórios;
* requisitos desejáveis;
* tecnologias;
* conhecimentos de negócio;
* idiomas;
* palavras-chave;
* competências comportamentais;
* possíveis critérios eliminatórios.

Depois da extração, o sistema deverá relacionar cada requisito a uma ou mais evidências aprovadas do perfil.

A análise deverá classificar a compatibilidade como:

* alta;
* média;
* parcial;
* ausente;
* inconclusiva.

O sistema não deverá avaliar compatibilidade apenas pela repetição de palavras-chave.

---

## 8. Geração do currículo

A geração deverá ocorrer de forma controlada e dividida em etapas.

### Etapa 1 — Recuperação

Selecionar somente informações:

* aprovadas;
* relevantes para a vaga;
* associadas a evidências;
* permitidas pelo usuário;
* compatíveis com o tamanho do currículo.

### Etapa 2 — Estratégia

Definir:

* posicionamento profissional;
* competências prioritárias;
* experiências mais relevantes;
* projetos a destacar;
* informações secundárias;
* lacunas identificadas;
* tom e extensão do texto.

### Etapa 3 — Redação

Produzir:

* título profissional;
* resumo;
* experiências;
* projetos;
* competências;
* formação;
* cursos;
* idiomas;
* seções complementares.

### Etapa 4 — Validação factual

Cada afirmação deverá ser comparada com a base canônica.

Afirmações sem suporte deverão ser:

* removidas;
* sinalizadas;
* ou submetidas à revisão humana.

### Etapa 5 — Revisão humana

O usuário poderá:

* aceitar;
* editar;
* remover;
* reorganizar;
* encurtar;
* alterar o tom;
* regenerar somente uma seção;
* trocar o template;
* visualizar as evidências utilizadas.

---

## 9. Harness da LLM

O sistema deverá possuir um harness responsável por controlar todas as interações com modelos de linguagem.

O frontend não poderá chamar diretamente a API de IA.

O fluxo será:

**Frontend React → Backend Core Spring Boot → Serviço de IA FastAPI → Provedor da LLM**

O harness deverá:

* aceitar apenas operações previamente definidas;
* selecionar o modelo adequado;
* montar o contexto mínimo necessário;
* consultar o RAG;
* limitar tokens;
* validar entradas;
* exigir respostas estruturadas;
* validar saídas;
* aplicar políticas de repetição;
* registrar tokens, custos e duração;
* impedir operações não autorizadas;
* proteger as instruções do sistema;
* preservar a rastreabilidade.

Operações permitidas no MVP:

* extrair conhecimento;
* propor atualização;
* analisar vaga;
* classificar requisito;
* relacionar requisito e evidência;
* selecionar fatos relevantes;
* gerar estratégia de currículo;
* gerar seção;
* revisar seção;
* validar afirmações;
* resumir documento.

Não deverá existir um agente genérico com autonomia para executar ferramentas livremente.

---

## 10. Estratégia para redução de tokens e custos

Antes de utilizar uma LLM, o sistema deverá executar deterministicamente todas as tarefas possíveis.

Não deverão consumir IA:

* validação de campos;
* ordenação;
* filtros;
* comparações exatas;
* detecção de identificadores;
* remoção de duplicidades exatas;
* contagem de caracteres;
* aplicação de templates;
* renderização HTML;
* paginação;
* geração do PDF.

O harness deverá utilizar:

* modelos econômicos para classificação e extração simples;
* modelos intermediários para análise;
* modelos mais capazes somente para sínteses complexas;
* recuperação limitada de trechos;
* filtros por metadados antes da busca vetorial;
* prompts permanentes compactos;
* cache de instruções quando disponível;
* atualizações incrementais;
* regeneração por seção;
* limites de chamadas por operação.

Cada chamada deverá registrar:

* modelo;
* tokens de entrada;
* tokens de saída;
* tokens recuperados;
* tokens em cache;
* duração;
* custo estimado;
* tentativas;
* resultado da validação.

---

## 11. Stack arquitetural

### Frontend

* React;
* TypeScript;
* interface web responsiva;
* editor estruturado;
* visualização do currículo;
* aprovação de alterações;
* comparação de vagas;
* apresentação do diff visual.

### Backend Core

* Java;
* Spring Boot;
* regras de negócio;
* persistência;
* segurança;
* versionamento;
* auditoria;
* documentos;
* vagas;
* currículos;
* renderização;
* exportação.

### Serviço de IA

* Python;
* FastAPI;
* contratos estruturados;
* prompts;
* embeddings;
* RAG;
* comunicação com provedores de LLM;
* métricas de tokens e custos.

O serviço de IA não será proprietário dos dados e não poderá alterar diretamente a base canônica.

### Dados

* PostgreSQL;
* pgvector;
* sistema de arquivos local controlado;
* histórico de versões;
* histórico de decisões;
* metadados de documentos e evidências.

### Execução

* contêineres;
* comunicação interna por HTTP;
* segredos armazenados apenas nos serviços de backend;
* nenhuma chave de API exposta no frontend.

---

## 12. Renderização e exportação em PDF

O PDF deverá ser tratado como uma decisão arquitetural desde o início do projeto.

A solução definida será:

* Thymeleaf para gerar o HTML do currículo no backend;
* HTML e CSS específicos para impressão;
* Playwright Java para controlar um Chromium headless;
* Chromium para renderizar e exportar o PDF;
* fontes locais versionadas e empacotadas com a aplicação;
* formato A4;
* ambiente em contêiner com versões fixadas.

O preview e o PDF deverão utilizar:

* o mesmo HTML;
* o mesmo CSS;
* as mesmas fontes;
* os mesmos ícones;
* os mesmos templates;
* o mesmo mecanismo de renderização.

O frontend não deverá manter uma implementação visual independente do currículo.

O HTML será um artefato derivado. A fonte oficial continuará sendo o modelo estruturado do currículo armazenado no banco.

O sistema deverá controlar:

* margens;
* quebras de página;
* blocos indivisíveis;
* títulos isolados no fim da página;
* tamanho máximo recomendado de textos;
* quantidade de páginas;
* carregamento das fontes;
* ausência de recursos externos;
* consistência entre diferentes ambientes.

A primeira fase técnica deverá incluir uma prova de conceito de renderização com:

* currículo de uma página;
* currículo de duas páginas;
* conteúdo excessivo;
* nomes e títulos longos;
* fontes locais;
* execução em contêiner;
* comparação entre preview e PDF.

---

## 13. Segurança

O sistema deverá aplicar o princípio de menor privilégio.

A LLM não terá:

* acesso direto ao banco;
* acesso direto ao sistema de arquivos;
* permissão para executar comandos;
* navegação irrestrita;
* capacidade de atualizar informações oficiais;
* acesso a URLs externas não autorizadas;
* permissão para escolher livremente ferramentas.

A descrição de uma vaga e os documentos recuperados deverão ser delimitados como conteúdo não confiável.

O sistema deverá proteger-se contra:

* prompt injection;
* instruções maliciosas em vagas;
* documentos que tentem alterar o comportamento do modelo;
* respostas fora do formato esperado;
* vazamento de informações;
* custos excessivos;
* chamadas repetidas;
* conteúdo gerado sem evidência.

---

## 14. Escopo do MVP

### Incluído

* usuário local único;
* perfil profissional;
* experiências;
* formações;
* cursos;
* projetos;
* habilidades;
* idiomas;
* links;
* importação de texto;
* registro de documentos;
* extração assistida por IA;
* propostas de atualização;
* aprovação e rejeição;
* evidências;
* cadastro de vagas;
* análise de compatibilidade;
* geração personalizada;
* editor estruturado;
* templates;
* preview;
* exportação em PDF;
* versionamento;
* auditoria;
* métricas básicas da IA.

### Fora do MVP

* múltiplos usuários;
* pagamentos;
* planos;
* login social;
* importação automática do LinkedIn;
* candidatura automática;
* envio automático de currículos;
* scraping de sites de vagas;
* busca automática de oportunidades;
* aplicativo mobile nativo;
* marketplace de templates;
* treinamento ou fine-tuning;
* agente autônomo genérico;
* garantia de aprovação em sistemas ATS;
* infraestrutura completa de SaaS.

OCR avançado e reconstrução complexa de documentos escaneados poderão ser tratados como evolução posterior, sem bloquear o núcleo do MVP.

---

## 15. Estado atual do desenvolvimento

**Fase de implementação iniciada em 14/07/2026, por decisão explícita do usuário.**

A primeira implementação do MVP está concluída e registrada em [docs/ETAPAS.md](docs/ETAPAS.md). As regras da fase anterior (não gerar código) deixaram de valer; permanecem válidas todas as demais regras deste documento:

* não presumir requisitos ainda não documentados;
* não expandir automaticamente o escopo;
* preservar as decisões da seção 17;
* toda mudança relevante de arquitetura deve gerar ou atualizar um ADR em `docs/adr/`.

As atividades atuais concentram-se em:

* revisão e estudo do código implementado;
* testes de ponta a ponta com o docker-compose;
* refinamentos dirigidos pelo usuário;
* manutenção da coerência entre código e documentação.

---

## 16. Como a IA deve atuar durante o projeto

Atue como uma assistente de arquitetura e documentação, não como responsável integral pelo projeto.

Seu papel é:

* ajudar a organizar decisões;
* encontrar inconsistências;
* questionar complexidade desnecessária;
* identificar riscos;
* propor alternativas;
* explicar trade-offs;
* preservar o escopo do MVP;
* produzir documentação clara;
* manter coerência entre os documentos;
* indicar quando uma decisão contradiz outra;
* diferenciar necessidade atual de evolução futura.

Ao propor uma solução:

1. Explique o problema que ela resolve.
2. Apresente os impactos arquiteturais.
3. Indique os custos e riscos.
4. Diferencie o que pertence ao MVP do que pertence ao futuro.
5. Não adicione tecnologias apenas para valorizar o portfólio.
6. Prefira soluções compreensíveis e justificáveis.
7. Evite abstrações prematuras.
8. Não apresente código enquanto a fase atual for teórica.
9. Não trate sugestões anteriores como decisões definitivas sem verificar o documento atual.
10. Preserve as decisões já aprovadas, salvo quando houver uma contradição relevante.

---

## 17. Decisões já estabelecidas

* O produto será um MVP de portfólio.
* A primeira execução será local.
* A interface será web e responsiva.
* O MVP atenderá a um único usuário.
* O frontend utilizará React e TypeScript.
* O Backend Core utilizará Java e Spring Boot.
* A orquestração de IA será isolada em um serviço Python com FastAPI.
* O banco será PostgreSQL.
* A busca vetorial utilizará pgvector.
* O banco relacional será a fonte oficial dos fatos.
* O RAG será uma camada de recuperação, não de conservação da verdade.
* A LLM não poderá escrever diretamente na base oficial.
* Toda atualização deverá passar por aprovação.
* O provedor da LLM deverá ser substituível.
* As chaves de API ficarão somente no backend.
* O harness terá operações restritas.
* O histórico de conversas não será utilizado como memória oficial.
* Currículos serão gerados somente com fatos aprovados.
* O PDF será gerado com Thymeleaf, Playwright Java e Chromium.
* Preview e PDF utilizarão a mesma origem visual.
* Fine-tuning está fora do MVP.
* A publicação na web ocorrerá somente depois da estabilização local.

---

## 18. Resultado esperado da colaboração

Toda resposta relacionada ao projeto deverá contribuir para a construção de uma documentação consistente e implementável.

Quando receber uma nova solicitação:

1. Considere todo este contexto.
2. Verifique se a solicitação pertence ao MVP.
3. Identifique possíveis contradições.
4. Preserve as decisões arquiteturais existentes.
5. Não invente requisitos.
6. Declare eventuais suposições.
7. Apresente a solução de maneira estruturada.
8. Mantenha separadas as responsabilidades do frontend, Backend Core, serviço de IA, banco de dados, RAG e renderizador.
9. Priorize integridade, rastreabilidade, segurança factual e controle de custos.
10. Não forneça código até que a fase de implementação seja explicitamente iniciada.
