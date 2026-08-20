package br.com.curriculos.servico.ia;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;
import java.util.Map;

/**
 * Contratos tipados das operações do harness — espelho Java de
 * {@code ai-service/app/schemas.py} (ADR-005).
 *
 * <p>O serviço de IA já valida a resposta do modelo contra os schemas Pydantic
 * antes de responder; estes records validam de novo na chegada ao Backend Core.
 * A simetria de nomes com o arquivo Python é proposital: se um lado mudar sem o
 * outro, a divergência fica visível em revisão.
 *
 * <p>Os nomes viajam em {@code snake_case} no JSON e em {@code camelCase} no Java.
 * Listas nulas são normalizadas para vazias, de modo que o consumidor nunca
 * precise checar {@code null}.
 */
public final class ContratosIa {

    private ContratosIa() {
    }

    /** Métricas de uma chamada, presentes em todo envelope (seção 10). */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Uso(
            String model,
            Integer inputTokens,
            Integer outputTokens,
            Integer cachedTokens,
            Integer retrievedChunks,
            Integer durationMs,
            Double estimatedCost,
            Integer attempts,
            Boolean valid) {
    }

    /** Envelope devolvido por {@code POST /v1/operations/{nome}}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Envelope(Map<String, Object> result, Uso usage) {
    }

    // ---------- extrair-conhecimento ----------

    /**
     * Proposta extraída de um documento. O {@code payload} segue como mapa
     * porque seus campos variam conforme o tipo de fato — ver ADR-002.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record PropostaExtraida(
            String acao,
            String tipoFato,
            Map<String, Object> payload,
            Long fatoAlvoId,
            String justificativa,
            String trechoEvidencia,
            Double confianca,
            String modelo,
            String versaoPrompt) {

        public PropostaExtraida {
            if (acao == null || acao.isBlank()) acao = "CRIAR";
            if (payload == null) payload = Map.of();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Extracao(List<PropostaExtraida> propostas) {
        public Extracao {
            if (propostas == null) propostas = List.of();
        }
    }

    // ---------- analisar-vaga ----------

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RequisitoExtraido(String descricao, String categoria) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record AnaliseVaga(
            String titulo,
            String empresa,
            String senioridade,
            String modalidade,
            String localizacao,
            List<String> responsabilidades,
            List<RequisitoExtraido> requisitosObrigatorios,
            List<RequisitoExtraido> requisitosDesejaveis,
            List<String> tecnologias,
            List<String> idiomas,
            List<String> palavrasChave,
            List<String> competenciasComportamentais,
            List<String> criteriosEliminatorios) {

        public AnaliseVaga {
            if (responsabilidades == null) responsabilidades = List.of();
            if (requisitosObrigatorios == null) requisitosObrigatorios = List.of();
            if (requisitosDesejaveis == null) requisitosDesejaveis = List.of();
            if (tecnologias == null) tecnologias = List.of();
            if (idiomas == null) idiomas = List.of();
            if (palavrasChave == null) palavrasChave = List.of();
            if (competenciasComportamentais == null) competenciasComportamentais = List.of();
            if (criteriosEliminatorios == null) criteriosEliminatorios = List.of();
        }
    }

    // ---------- relacionar-requisitos ----------

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Relacao(
            Long requisitoId,
            String compatibilidade,
            List<Long> fatos,
            String justificativa) {

        public Relacao {
            if (fatos == null) fatos = List.of();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Relacionamento(List<Relacao> relacoes) {
        public Relacionamento {
            if (relacoes == null) relacoes = List.of();
        }
    }

    // ---------- gerar-estrategia ----------

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Estrategia(
            String posicionamento,
            List<String> competenciasPrioritarias,
            List<Long> experienciasRelevantes,
            List<Long> projetosDestaque,
            List<String> informacoesSecundarias,
            List<String> lacunas,
            String tom) {

        public Estrategia {
            if (competenciasPrioritarias == null) competenciasPrioritarias = List.of();
            if (experienciasRelevantes == null) experienciasRelevantes = List.of();
            if (projetosDestaque == null) projetosDestaque = List.of();
            if (informacoesSecundarias == null) informacoesSecundarias = List.of();
            if (lacunas == null) lacunas = List.of();
        }
    }

    // ---------- gerar-secao ----------

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Secao(String titulo, String conteudo, List<Long> fatosUtilizados) {
        public Secao {
            if (fatosUtilizados == null) fatosUtilizados = List.of();
        }
    }

    // ---------- validar-afirmacoes ----------

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Afirmacao(String texto, Boolean sustentada, List<Long> fatos, String nota) {
        public Afirmacao {
            if (fatos == null) fatos = List.of();
        }

        /** Afirmação sem suporte na base canônica (seção 8, etapa 4). */
        public boolean naoSustentada() {
            return !Boolean.TRUE.equals(sustentada);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Validacao(List<Afirmacao> afirmacoes) {
        public Validacao {
            if (afirmacoes == null) afirmacoes = List.of();
        }
    }

    // ---------- resumir-documento / conversar ----------

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Resumo(String resumo) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Conversa(String resposta) {
    }

    // ---------- memória vetorial ----------

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record TrechoRag(String origem, Long origemId, String conteudo, Double similaridade) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BuscaRag(List<TrechoRag> trechos) {
        public BuscaRag {
            if (trechos == null) trechos = List.of();
        }
    }
}
