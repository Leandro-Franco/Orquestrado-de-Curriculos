package br.com.curriculos.web;

import br.com.curriculos.servico.AiClient;
import br.com.curriculos.servico.ia.ContratosIa;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Chat contextual (ADR-004): responde sobre o que está na tela e sobre a
 * base de conhecimento. Somente leitura — não altera fato, proposta ou
 * currículo; escrita continua exigindo os fluxos com aprovação humana.
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final int MAX_CONTEXTO = 6000;
    private static final int MAX_MENSAGEM = 2000;
    private static final int MAX_HISTORICO = 6;

    private final AiClient aiClient;

    public ChatController(AiClient aiClient) {
        this.aiClient = aiClient;
    }

    @PostMapping
    public Map<String, Object> conversar(@RequestBody Map<String, Object> corpo) {
        String mensagem = texto(corpo.get("mensagem"), MAX_MENSAGEM);
        if (mensagem.isBlank()) {
            throw new IllegalArgumentException("Mensagem vazia");
        }
        String contexto = texto(corpo.get("contexto"), MAX_CONTEXTO);

        List<?> historicoBruto = corpo.get("historico") instanceof List<?> lista ? lista : List.of();
        List<?> historico = historicoBruto.size() > MAX_HISTORICO
                ? historicoBruto.subList(historicoBruto.size() - MAX_HISTORICO, historicoBruto.size())
                : historicoBruto;

        // Recuperação limitada: só os trechos mais próximos da pergunta (seção 10).
        List<ContratosIa.TrechoRag> trechos;
        try {
            trechos = aiClient.buscar(mensagem, 3);
        } catch (Exception e) {
            trechos = List.of(); // RAG indisponível não impede o chat
        }

        ContratosIa.Conversa resultado = aiClient.executar("conversar", Map.of(
                "mensagem", mensagem,
                "contexto_tela", contexto,
                "historico", historico,
                "trechos", trechos), ContratosIa.Conversa.class);
        return Map.of("resposta", resultado.resposta() != null ? resultado.resposta() : "");
    }

    private String texto(Object valor, int limite) {
        String s = valor == null ? "" : String.valueOf(valor);
        return s.length() > limite ? s.substring(0, limite) : s;
    }
}
