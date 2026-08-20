package br.com.curriculos.servico;

import br.com.curriculos.dominio.ChamadaIa;
import br.com.curriculos.dominio.ConfiguracaoIa;
import br.com.curriculos.repositorio.ChamadaIaRepository;
import br.com.curriculos.repositorio.ConfiguracaoIaRepository;
import br.com.curriculos.servico.ia.ContratosIa;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Único ponto de saída do Backend Core para o Serviço de IA.
 * O frontend nunca fala com a IA diretamente (Contexto Mestre, seção 9).
 * Toda chamada registra métricas de tokens e custo em {@code chamada_ia}.
 * A configuração de provedor (ADR-004) viaja em cabeçalhos internos X-AI-*;
 * o serviço de IA permanece sem estado e a chave nunca chega ao navegador.
 *
 * <p>As respostas chegam tipadas em {@link ContratosIa} (ADR-005): erro de
 * contrato falha na compilação, não em produção.
 */
@Service
public class AiClient {

    private final RestClient http;
    private final ChamadaIaRepository chamadas;
    private final ConfiguracaoIaRepository configuracoes;
    private final ObjectMapper json;

    public AiClient(@Value("${app.ai-service-url}") String baseUrl,
                    ChamadaIaRepository chamadas,
                    ConfiguracaoIaRepository configuracoes,
                    ObjectMapper json) {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory();
        factory.setReadTimeout(Duration.ofMinutes(5)); // geração de seção pode ser demorada
        this.http = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
        this.chamadas = chamadas;
        this.configuracoes = configuracoes;
        this.json = json;
    }

    /**
     * Executa uma operação registrada no harness e devolve o resultado já
     * convertido para o contrato da operação.
     */
    public <T> T executar(String operacao, Map<String, Object> entrada, Class<T> contrato) {
        return json.convertValue(resultadoBruto(operacao, entrada), contrato);
    }

    /**
     * Variante sem contrato, para quando o resultado é apenas repassado ou
     * persistido como JSONB sem que o Backend Core leia seus campos.
     */
    public Map<String, Object> executar(String operacao, Map<String, Object> entrada) {
        return resultadoBruto(operacao, entrada);
    }

    private Map<String, Object> resultadoBruto(String operacao, Map<String, Object> entrada) {
        ContratosIa.Envelope envelope = http.post()
                .uri("/v1/operations/{op}", operacao)
                .contentType(MediaType.APPLICATION_JSON)
                .headers(this::aplicarConfiguracao)
                .body(entrada)
                .retrieve()
                .body(ContratosIa.Envelope.class);
        if (envelope == null || envelope.result() == null) {
            throw new IllegalStateException(
                    "Resposta inválida do serviço de IA para a operação " + operacao);
        }
        registrar(operacao, envelope.usage());
        return envelope.result();
    }

    /** Converte um contrato de volta para mapa, no formato gravado em colunas JSONB. */
    public Map<String, Object> comoMapa(Object contrato) {
        if (contrato == null) return Map.of();
        return json.convertValue(contrato, new TypeReference<Map<String, Object>>() {
        });
    }

    /** Indexa (ou reindexa) um conteúdo na memória vetorial. */
    public void indexar(String origem, Long origemId, String conteudo) {
        http.post().uri("/v1/index/upsert")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("origem", origem, "origem_id", origemId, "conteudo", conteudo))
                .retrieve().toBodilessEntity();
    }

    public void removerIndice(String origem, Long origemId) {
        http.delete().uri("/v1/index/{origem}/{id}", origem, origemId)
                .retrieve().toBodilessEntity();
    }

    /** Busca semântica na memória vetorial (usada pelo chat contextual). */
    public List<ContratosIa.TrechoRag> buscar(String consulta, int topK) {
        ContratosIa.BuscaRag resposta = http.post().uri("/v1/index/search")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("consulta", consulta, "top_k", topK))
                .retrieve()
                .body(ContratosIa.BuscaRag.class);
        return resposta == null ? List.of() : resposta.trechos();
    }

    private void aplicarConfiguracao(HttpHeaders headers) {
        ConfiguracaoIa cfg = configuracoes.findById((short) 1).orElse(null);
        if (cfg == null || cfg.getProvider() == null || "fake".equals(cfg.getProvider())) {
            return; // sem configuração explícita: o serviço usa os padrões de ambiente
        }
        headers.set("X-AI-Provider", cfg.getProvider());
        setSePresente(headers, "X-AI-Api-Key", cfg.getApiKey());
        setSePresente(headers, "X-AI-Base-Url", cfg.getBaseUrl());
        setSePresente(headers, "X-AI-Modelo-Economico", cfg.getModeloEconomico());
        setSePresente(headers, "X-AI-Modelo-Intermediario", cfg.getModeloIntermediario());
        setSePresente(headers, "X-AI-Modelo-Avancado", cfg.getModeloAvancado());
    }

    private void setSePresente(HttpHeaders headers, String nome, String valor) {
        if (valor != null && !valor.isBlank()) headers.set(nome, valor);
    }

    private void registrar(String operacao, ContratosIa.Uso uso) {
        ChamadaIa c = new ChamadaIa();
        c.setOperacao(operacao);
        if (uso != null) {
            c.setModelo(uso.model() != null ? uso.model() : "desconhecido");
            c.setTokensEntrada(ouZero(uso.inputTokens()));
            c.setTokensSaida(ouZero(uso.outputTokens()));
            c.setTokensCache(ouZero(uso.cachedTokens()));
            c.setTrechosRecuperados(ouZero(uso.retrievedChunks()));
            c.setDuracaoMs(ouZero(uso.durationMs()));
            c.setTentativas(uso.attempts() != null ? uso.attempts() : 1);
            c.setValida(!Boolean.FALSE.equals(uso.valid()));
            if (uso.estimatedCost() != null) {
                c.setCustoEstimado(BigDecimal.valueOf(uso.estimatedCost()));
            }
        }
        chamadas.save(c);
    }

    private Integer ouZero(Integer valor) {
        return valor != null ? valor : 0;
    }
}
