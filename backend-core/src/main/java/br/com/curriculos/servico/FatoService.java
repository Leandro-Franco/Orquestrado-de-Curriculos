package br.com.curriculos.servico;

import br.com.curriculos.dominio.EventoAuditoria;
import br.com.curriculos.dominio.Fato;
import br.com.curriculos.repositorio.EventoAuditoriaRepository;
import br.com.curriculos.repositorio.FatoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * CRUD da memória estruturada, com validação determinística por tipo
 * (validação de campos não consome IA — Contexto Mestre, seção 10).
 */
@Service
public class FatoService {

    /** Campos obrigatórios do payload por tipo de fato (ADR-002). */
    private static final Map<String, List<String>> CAMPOS_OBRIGATORIOS = Map.of(
            "EXPERIENCIA", List.of("cargo", "empresa", "inicio"),
            "FORMACAO", List.of("curso", "instituicao"),
            "CURSO", List.of("nome"),
            "CERTIFICACAO", List.of("nome"),
            "PROJETO", List.of("nome", "descricao"),
            "HABILIDADE", List.of("nome"),
            "IDIOMA", List.of("idioma", "nivel"),
            "LINK", List.of("rotulo", "url")
    );

    private final FatoRepository fatos;
    private final EventoAuditoriaRepository auditoria;
    private final AiClient aiClient;

    public FatoService(FatoRepository fatos, EventoAuditoriaRepository auditoria, AiClient aiClient) {
        this.fatos = fatos;
        this.auditoria = auditoria;
        this.aiClient = aiClient;
    }

    public void validar(String tipo, Map<String, Object> payload) {
        List<String> obrigatorios = CAMPOS_OBRIGATORIOS.get(tipo);
        if (obrigatorios == null) {
            throw new IllegalArgumentException("Tipo de fato desconhecido: " + tipo);
        }
        for (String campo : obrigatorios) {
            Object valor = payload == null ? null : payload.get(campo);
            if (valor == null || valor.toString().isBlank()) {
                throw new IllegalArgumentException("Campo obrigatório ausente para " + tipo + ": " + campo);
            }
        }
    }

    @Transactional
    public Fato criar(String tipo, Map<String, Object> payload) {
        validar(tipo, payload);
        Fato fato = new Fato();
        fato.setTipo(tipo);
        fato.setPayload(payload);
        fato = fatos.save(fato);
        auditoria.save(EventoAuditoria.de("fato", fato.getId(), "CRIADO_MANUALMENTE", Map.of("tipo", tipo)));
        reindexar(fato);
        return fato;
    }

    @Transactional
    public Fato atualizar(Long id, Map<String, Object> payload) {
        Fato fato = fatos.findById(id).orElseThrow(() -> new IllegalArgumentException("Fato não encontrado: " + id));
        validar(fato.getTipo(), payload);
        Map<String, Object> anterior = fato.getPayload();
        fato.setPayload(payload);
        fato.setAtualizadoEm(OffsetDateTime.now());
        fato = fatos.save(fato);
        auditoria.save(EventoAuditoria.de("fato", id, "ATUALIZADO_MANUALMENTE", Map.of("payload_anterior", anterior)));
        reindexar(fato);
        return fato;
    }

    @Transactional
    public void remover(Long id) {
        Fato fato = fatos.findById(id).orElseThrow(() -> new IllegalArgumentException("Fato não encontrado: " + id));
        fatos.delete(fato);
        auditoria.save(EventoAuditoria.de("fato", id, "REMOVIDO", Map.of("tipo", fato.getTipo())));
        try {
            aiClient.removerIndice("FATO", id);
        } catch (Exception e) {
            // índice vetorial é derivado; falha aqui não bloqueia a remoção do fato
        }
    }

    /** Serializa o fato como texto simples para indexação vetorial. */
    public String comoTexto(Fato fato) {
        StringBuilder sb = new StringBuilder(fato.getTipo()).append(": ");
        fato.getPayload().forEach((k, v) -> sb.append(k).append("=").append(v).append("; "));
        return sb.toString();
    }

    void reindexar(Fato fato) {
        try {
            aiClient.indexar("FATO", fato.getId(), comoTexto(fato));
        } catch (Exception e) {
            // reindexação incremental é melhor esforço; o banco relacional segue como fonte oficial
        }
    }
}
