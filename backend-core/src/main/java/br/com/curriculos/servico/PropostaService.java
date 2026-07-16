package br.com.curriculos.servico;

import br.com.curriculos.dominio.*;
import br.com.curriculos.repositorio.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Aprovação humana das propostas da LLM. Somente aqui uma proposta
 * vira fato oficial — e cada decisão fica auditada (seções 4 e 5.4).
 */
@Service
public class PropostaService {

    private final PropostaRepository propostas;
    private final FatoRepository fatos;
    private final EvidenciaRepository evidencias;
    private final EventoAuditoriaRepository auditoria;
    private final FatoService fatoService;

    public PropostaService(PropostaRepository propostas, FatoRepository fatos,
                           EvidenciaRepository evidencias, EventoAuditoriaRepository auditoria,
                           FatoService fatoService) {
        this.propostas = propostas;
        this.fatos = fatos;
        this.evidencias = evidencias;
        this.auditoria = auditoria;
        this.fatoService = fatoService;
    }

    @Transactional
    public Fato aprovar(Long propostaId) {
        Proposta proposta = pendente(propostaId);
        fatoService.validar(proposta.getTipoFato(), proposta.getPayloadProposto());

        Fato fato;
        if ("ATUALIZAR".equals(proposta.getAcao()) && proposta.getFatoAlvoId() != null) {
            fato = fatos.findById(proposta.getFatoAlvoId())
                    .orElseThrow(() -> new IllegalStateException("Fato alvo não existe mais"));
            fato.setPayload(proposta.getPayloadProposto());
            fato.setAtualizadoEm(OffsetDateTime.now());
        } else {
            fato = new Fato();
            fato.setTipo(proposta.getTipoFato());
            fato.setPayload(proposta.getPayloadProposto());
        }
        fato = fatos.save(fato);

        if (proposta.getDocumentoOrigemId() != null) {
            Evidencia evidencia = new Evidencia();
            evidencia.setFatoId(fato.getId());
            evidencia.setDocumentoId(proposta.getDocumentoOrigemId());
            evidencia.setTrecho(proposta.getTrechoEvidencia());
            evidencias.save(evidencia);
        }

        proposta.setStatus("APROVADA");
        proposta.setDecididoEm(OffsetDateTime.now());
        propostas.save(proposta);

        auditoria.save(EventoAuditoria.de("proposta", proposta.getId(), "APROVADA", Map.of(
                "fato_id", fato.getId(),
                "acao", proposta.getAcao(),
                "tipo", proposta.getTipoFato(),
                "modelo", String.valueOf(proposta.getModelo()),
                "versao_prompt", String.valueOf(proposta.getVersaoPrompt()))));

        // Reindexação incremental: apenas o conteúdo afetado (seção 6, passo 15).
        fatoService.reindexar(fato);
        return fato;
    }

    @Transactional
    public void rejeitar(Long propostaId, String motivo) {
        Proposta proposta = pendente(propostaId);
        proposta.setStatus("REJEITADA");
        proposta.setDecididoEm(OffsetDateTime.now());
        propostas.save(proposta);
        auditoria.save(EventoAuditoria.de("proposta", propostaId, "REJEITADA",
                Map.of("motivo", String.valueOf(motivo))));
    }

    private Proposta pendente(Long id) {
        Proposta proposta = propostas.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Proposta não encontrada: " + id));
        if (!"PENDENTE".equals(proposta.getStatus())) {
            throw new IllegalStateException("Proposta já decidida: " + proposta.getStatus());
        }
        return proposta;
    }
}
