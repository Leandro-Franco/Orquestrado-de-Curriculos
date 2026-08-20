package br.com.curriculos.servico.ia;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guarda a costura entre o serviço de IA (Python, snake_case) e o Backend Core
 * (Java, camelCase) — ADR-005. Os JSONs abaixo são o formato real produzido
 * pelos schemas Pydantic de {@code ai-service/app/schemas.py}.
 */
class ContratosIaTest {

    private final ObjectMapper json = new ObjectMapper();

    @Test
    void extracaoMapeiaSnakeCaseEPayloadDinamico() throws Exception {
        String resposta = """
                {"propostas": [{
                  "acao": "ATUALIZAR",
                  "tipo_fato": "HABILIDADE",
                  "payload": {"nome": "Java", "nivel": "avançado"},
                  "fato_alvo_id": 7,
                  "justificativa": "nível citado no documento",
                  "trecho_evidencia": "…Java avançado…",
                  "confianca": 0.9,
                  "modelo": "claude-sonnet-5",
                  "versao_prompt": "v1"
                }]}
                """;

        ContratosIa.Extracao extracao = json.readValue(resposta, ContratosIa.Extracao.class);

        assertThat(extracao.propostas()).hasSize(1);
        ContratosIa.PropostaExtraida proposta = extracao.propostas().getFirst();
        assertThat(proposta.acao()).isEqualTo("ATUALIZAR");
        assertThat(proposta.tipoFato()).isEqualTo("HABILIDADE");
        assertThat(proposta.fatoAlvoId()).isEqualTo(7L);
        assertThat(proposta.trechoEvidencia()).isEqualTo("…Java avançado…");
        assertThat(proposta.versaoPrompt()).isEqualTo("v1");
        assertThat(proposta.confianca()).isEqualTo(0.9);
        // O payload varia por tipo de fato (ADR-002), então segue como mapa.
        assertThat(proposta.payload()).containsEntry("nivel", "avançado");
    }

    @Test
    void analiseDeVagaMapeiaListasEmSnakeCase() throws Exception {
        String resposta = """
                {"titulo": "Dev Backend", "empresa": "ACME", "senioridade": "Pleno",
                 "modalidade": "Remoto", "localizacao": "",
                 "responsabilidades": ["manter APIs"],
                 "requisitos_obrigatorios": [{"descricao": "Java", "categoria": "tecnologia"}],
                 "requisitos_desejaveis": [{"descricao": "Inglês", "categoria": "idioma"}],
                 "tecnologias": ["Java", "Docker"], "idiomas": ["Inglês"],
                 "palavras_chave": ["backend"], "competencias_comportamentais": [],
                 "criterios_eliminatorios": []}
                """;

        ContratosIa.AnaliseVaga analise = json.readValue(resposta, ContratosIa.AnaliseVaga.class);

        assertThat(analise.titulo()).isEqualTo("Dev Backend");
        assertThat(analise.requisitosObrigatorios()).singleElement()
                .extracting(ContratosIa.RequisitoExtraido::descricao).isEqualTo("Java");
        assertThat(analise.requisitosDesejaveis()).singleElement()
                .extracting(ContratosIa.RequisitoExtraido::categoria).isEqualTo("idioma");
        assertThat(analise.palavrasChave()).containsExactly("backend");
        assertThat(analise.tecnologias()).containsExactly("Java", "Docker");
    }

    @Test
    void relacionamentoMapeiaIdsDeFatos() throws Exception {
        String resposta = """
                {"relacoes": [{"requisito_id": 42, "compatibilidade": "ALTA",
                               "fatos": [1, 2, 3], "justificativa": "evidência direta"}]}
                """;

        ContratosIa.Relacionamento relacionamento =
                json.readValue(resposta, ContratosIa.Relacionamento.class);

        ContratosIa.Relacao relacao = relacionamento.relacoes().getFirst();
        assertThat(relacao.requisitoId()).isEqualTo(42L);
        assertThat(relacao.compatibilidade()).isEqualTo("ALTA");
        assertThat(relacao.fatos()).containsExactly(1L, 2L, 3L);
    }

    @Test
    void envelopeSeparaResultadoDeMetricas() throws Exception {
        String resposta = """
                {"result": {"resposta": "olá"},
                 "usage": {"model": "claude-haiku-4-5", "input_tokens": 120,
                           "output_tokens": 45, "cached_tokens": 0, "retrieved_chunks": 3,
                           "duration_ms": 850, "estimated_cost": 0.000345,
                           "attempts": 1, "valid": true}}
                """;

        ContratosIa.Envelope envelope = json.readValue(resposta, ContratosIa.Envelope.class);

        assertThat(envelope.result()).containsEntry("resposta", "olá");
        assertThat(envelope.usage().model()).isEqualTo("claude-haiku-4-5");
        assertThat(envelope.usage().inputTokens()).isEqualTo(120);
        assertThat(envelope.usage().retrievedChunks()).isEqualTo(3);
        assertThat(envelope.usage().durationMs()).isEqualTo(850);
        assertThat(envelope.usage().estimatedCost()).isEqualTo(0.000345);
        assertThat(envelope.usage().valid()).isTrue();
    }

    @Test
    void listasAusentesViramVaziasEmVezDeNulo() throws Exception {
        ContratosIa.Validacao validacao = json.readValue("{}", ContratosIa.Validacao.class);
        ContratosIa.Secao secao = json.readValue("{\"titulo\":\"Resumo\"}", ContratosIa.Secao.class);
        ContratosIa.Estrategia estrategia = json.readValue("{}", ContratosIa.Estrategia.class);

        assertThat(validacao.afirmacoes()).isEmpty();
        assertThat(secao.fatosUtilizados()).isEmpty();
        assertThat(estrategia.lacunas()).isEmpty();
        assertThat(estrategia.competenciasPrioritarias()).isEmpty();
    }

    @Test
    void campoNovoNoServicoDeIaNaoQuebraOBackend() throws Exception {
        // Evolução do schemas.py não pode derrubar o Backend Core.
        ContratosIa.Conversa conversa = json.readValue(
                "{\"resposta\": \"oi\", \"campo_futuro\": 123}", ContratosIa.Conversa.class);

        assertThat(conversa.resposta()).isEqualTo("oi");
    }

    @Test
    void afirmacaoSemSuporteEIdentificada() throws Exception {
        String resposta = """
                {"afirmacoes": [
                  {"texto": "Liderei 20 pessoas", "sustentada": false, "fatos": [], "nota": "sem evidência"},
                  {"texto": "Trabalhei com Java", "sustentada": true, "fatos": [5], "nota": ""}]}
                """;

        ContratosIa.Validacao validacao = json.readValue(resposta, ContratosIa.Validacao.class);

        assertThat(validacao.afirmacoes()).filteredOn(ContratosIa.Afirmacao::naoSustentada)
                .singleElement()
                .extracting(ContratosIa.Afirmacao::texto).isEqualTo("Liderei 20 pessoas");
    }
}
