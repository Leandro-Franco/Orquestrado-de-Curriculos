package br.com.curriculos.servico;

import br.com.curriculos.dominio.Evidencia;
import br.com.curriculos.dominio.Fato;
import br.com.curriculos.dominio.Proposta;
import br.com.curriculos.repositorio.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * O coração do princípio de confiabilidade (seção 4): somente a aprovação
 * humana transforma proposta em fato oficial, com evidência e auditoria.
 */
@ExtendWith(MockitoExtension.class)
class PropostaServiceTest {

    @Mock PropostaRepository propostas;
    @Mock FatoRepository fatos;
    @Mock EvidenciaRepository evidencias;
    @Mock EventoAuditoriaRepository auditoria;
    @Mock FatoService fatoService;

    PropostaService servico;

    @BeforeEach
    void configurar() {
        servico = new PropostaService(propostas, fatos, evidencias, auditoria, fatoService);
    }

    private Proposta pendente(String acao) {
        Proposta proposta = new Proposta();
        proposta.setId(10L);
        proposta.setAcao(acao);
        proposta.setTipoFato("HABILIDADE");
        proposta.setPayloadProposto(Map.of("nome", "Kafka"));
        proposta.setDocumentoOrigemId(5L);
        proposta.setTrechoEvidencia("experiência com Kafka");
        proposta.setStatus("PENDENTE");
        return proposta;
    }

    @Test
    void aprovarCriacaoGeraFatoEvidenciaEAuditoria() {
        Proposta proposta = pendente("CRIAR");
        when(propostas.findById(10L)).thenReturn(Optional.of(proposta));
        when(fatos.save(any())).thenAnswer(inv -> {
            Fato fato = inv.getArgument(0);
            fato.setId(99L);
            return fato;
        });

        Fato fato = servico.aprovar(10L);

        assertThat(fato.getTipo()).isEqualTo("HABILIDADE");
        assertThat(proposta.getStatus()).isEqualTo("APROVADA");
        assertThat(proposta.getDecididoEm()).isNotNull();

        ArgumentCaptor<Evidencia> capturada = ArgumentCaptor.forClass(Evidencia.class);
        verify(evidencias).save(capturada.capture());
        assertThat(capturada.getValue().getFatoId()).isEqualTo(99L);
        assertThat(capturada.getValue().getDocumentoId()).isEqualTo(5L);
        assertThat(capturada.getValue().getTrecho()).isEqualTo("experiência com Kafka");

        verify(fatoService).validar("HABILIDADE", Map.of("nome", "Kafka"));
        verify(fatoService).reindexar(any());
        verify(auditoria).save(any());
    }

    @Test
    void aprovarAtualizacaoAlteraOFatoAlvo() {
        Proposta proposta = pendente("ATUALIZAR");
        proposta.setFatoAlvoId(3L);
        Fato existente = new Fato();
        existente.setId(3L);
        existente.setTipo("HABILIDADE");
        existente.setPayload(Map.of("nome", "Kafka", "nivel", "básico"));

        when(propostas.findById(10L)).thenReturn(Optional.of(proposta));
        when(fatos.findById(3L)).thenReturn(Optional.of(existente));
        when(fatos.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Fato fato = servico.aprovar(10L);

        assertThat(fato.getId()).isEqualTo(3L);
        assertThat(fato.getPayload()).isEqualTo(Map.of("nome", "Kafka"));
    }

    @Test
    void propostaJaDecididaNaoPodeSerReaprovada() {
        Proposta proposta = pendente("CRIAR");
        proposta.setStatus("REJEITADA");
        when(propostas.findById(10L)).thenReturn(Optional.of(proposta));

        assertThatThrownBy(() -> servico.aprovar(10L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("já decidida");
        verifyNoInteractions(fatos, evidencias);
    }

    @Test
    void payloadInvalidoBloqueiaAprovacao() {
        Proposta proposta = pendente("CRIAR");
        when(propostas.findById(10L)).thenReturn(Optional.of(proposta));
        doThrow(new IllegalArgumentException("Campo obrigatório ausente"))
                .when(fatoService).validar(any(), any());

        assertThatThrownBy(() -> servico.aprovar(10L))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(fatos);
    }

    @Test
    void rejeitarRegistraDecisaoSemTocarNaBase() {
        Proposta proposta = pendente("CRIAR");
        when(propostas.findById(10L)).thenReturn(Optional.of(proposta));

        servico.rejeitar(10L, "informação incorreta");

        assertThat(proposta.getStatus()).isEqualTo("REJEITADA");
        verify(auditoria).save(any());
        verifyNoInteractions(fatos, evidencias);
    }
}
