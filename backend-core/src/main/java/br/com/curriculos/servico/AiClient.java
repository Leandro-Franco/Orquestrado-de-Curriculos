package br.com.curriculos.servico;

import br.com.curriculos.dominio.ChamadaIa;
import br.com.curriculos.repositorio.ChamadaIaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;

/**
 * Único ponto de saída do Backend Core para o Serviço de IA.
 * O frontend nunca fala com a IA diretamente (Contexto Mestre, seção 9).
 * Toda chamada registra métricas de tokens e custo em {@code chamada_ia}.
 */
@Service
public class AiClient {

    private final RestClient http;
    private final ChamadaIaRepository chamadas;

    public AiClient(@Value("${app.ai-service-url}") String baseUrl, ChamadaIaRepository chamadas) {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory();
        factory.setReadTimeout(Duration.ofMinutes(5)); // geração de seção pode ser demorada
        this.http = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
        this.chamadas = chamadas;
    }

    /** Executa uma operação registrada no harness e retorna o campo {@code result}. */
    @SuppressWarnings("unchecked")
    public Map<String, Object> executar(String operacao, Map<String, Object> entrada) {
        Map<String, Object> resposta = http.post()
                .uri("/v1/operations/{op}", operacao)
                .contentType(MediaType.APPLICATION_JSON)
                .body(entrada)
                .retrieve()
                .body(Map.class);
        if (resposta == null || !resposta.containsKey("result")) {
            throw new IllegalStateException("Resposta inválida do serviço de IA para a operação " + operacao);
        }
        registrar(operacao, (Map<String, Object>) resposta.get("usage"));
        return (Map<String, Object>) resposta.get("result");
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

    private void registrar(String operacao, Map<String, Object> usage) {
        ChamadaIa c = new ChamadaIa();
        c.setOperacao(operacao);
        if (usage != null) {
            c.setModelo((String) usage.getOrDefault("model", "desconhecido"));
            c.setTokensEntrada(inteiro(usage.get("input_tokens")));
            c.setTokensSaida(inteiro(usage.get("output_tokens")));
            c.setTokensCache(inteiro(usage.get("cached_tokens")));
            c.setTrechosRecuperados(inteiro(usage.get("retrieved_chunks")));
            c.setDuracaoMs(inteiro(usage.get("duration_ms")));
            c.setTentativas(inteiro(usage.getOrDefault("attempts", 1)));
            c.setValida(Boolean.TRUE.equals(usage.getOrDefault("valid", true)));
            Object custo = usage.get("estimated_cost");
            if (custo instanceof Number n) {
                c.setCustoEstimado(BigDecimal.valueOf(n.doubleValue()));
            }
        }
        chamadas.save(c);
    }

    private Integer inteiro(Object valor) {
        return valor instanceof Number n ? n.intValue() : 0;
    }
}
