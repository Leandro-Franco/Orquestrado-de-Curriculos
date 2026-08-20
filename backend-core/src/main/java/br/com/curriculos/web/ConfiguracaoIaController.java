package br.com.curriculos.web;

import br.com.curriculos.dominio.ConfiguracaoIa;
import br.com.curriculos.dominio.EventoAuditoria;
import br.com.curriculos.repositorio.ConfiguracaoIaRepository;
import br.com.curriculos.repositorio.EventoAuditoriaRepository;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Configuração do provedor de IA pela interface (ADR-004).
 * A chave é gravada uma vez e NUNCA devolvida ao frontend — o GET
 * retorna apenas a forma mascarada.
 */
@RestController
@RequestMapping("/api/configuracao-ia")
public class ConfiguracaoIaController {

    private static final Set<String> PROVIDERS_VALIDOS =
            Set.of("fake", "anthropic", "openai", "grok", "ollama", "personalizado");

    private final ConfiguracaoIaRepository configuracoes;
    private final EventoAuditoriaRepository auditoria;

    public ConfiguracaoIaController(ConfiguracaoIaRepository configuracoes,
                                    EventoAuditoriaRepository auditoria) {
        this.configuracoes = configuracoes;
        this.auditoria = auditoria;
    }

    @GetMapping
    public Map<String, Object> buscar() {
        return comoResposta(atual());
    }

    @PutMapping
    public Map<String, Object> atualizar(@RequestBody Map<String, String> corpo) {
        ConfiguracaoIa cfg = atual();

        String provider = corpo.get("provider");
        if (provider != null) {
            if (!PROVIDERS_VALIDOS.contains(provider)) {
                throw new IllegalArgumentException("Provedor inválido: " + provider);
            }
            cfg.setProvider(provider);
        }
        // Convenção: apiKey ausente = manter a atual; string vazia = limpar.
        if (corpo.containsKey("apiKey") && corpo.get("apiKey") != null) {
            cfg.setApiKey(corpo.get("apiKey").isBlank() ? null : corpo.get("apiKey").strip());
        }
        if (corpo.containsKey("baseUrl")) cfg.setBaseUrl(vazioComoNulo(corpo.get("baseUrl")));
        if (corpo.containsKey("modeloEconomico")) cfg.setModeloEconomico(vazioComoNulo(corpo.get("modeloEconomico")));
        if (corpo.containsKey("modeloIntermediario")) cfg.setModeloIntermediario(vazioComoNulo(corpo.get("modeloIntermediario")));
        if (corpo.containsKey("modeloAvancado")) cfg.setModeloAvancado(vazioComoNulo(corpo.get("modeloAvancado")));
        cfg.setAtualizadoEm(OffsetDateTime.now());
        cfg = configuracoes.save(cfg);

        // Auditoria sem nunca registrar a chave.
        auditoria.save(EventoAuditoria.de("configuracao_ia", 1L, "ATUALIZADA",
                Map.of("provider", String.valueOf(cfg.getProvider()),
                        "base_url", String.valueOf(cfg.getBaseUrl()))));
        return comoResposta(cfg);
    }

    private ConfiguracaoIa atual() {
        return configuracoes.findById((short) 1).orElseGet(() -> {
            ConfiguracaoIa nova = new ConfiguracaoIa();
            nova.setId((short) 1);
            return configuracoes.save(nova);
        });
    }

    private Map<String, Object> comoResposta(ConfiguracaoIa cfg) {
        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("provider", cfg.getProvider());
        resposta.put("apiKeyMascarada", mascarar(cfg.getApiKey()));
        resposta.put("baseUrl", cfg.getBaseUrl());
        resposta.put("modeloEconomico", cfg.getModeloEconomico());
        resposta.put("modeloIntermediario", cfg.getModeloIntermediario());
        resposta.put("modeloAvancado", cfg.getModeloAvancado());
        return resposta;
    }

    private String mascarar(String chave) {
        if (chave == null || chave.isBlank()) return null;
        String fim = chave.length() > 4 ? chave.substring(chave.length() - 4) : "";
        return "•••" + fim;
    }

    private String vazioComoNulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.strip();
    }
}
