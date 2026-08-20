package br.com.curriculos.servico;

import br.com.curriculos.dominio.Documento;
import br.com.curriculos.dominio.Fato;
import br.com.curriculos.dominio.Proposta;
import br.com.curriculos.repositorio.*;
import br.com.curriculos.servico.ia.ContratosIa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Fluxo de atualização do conhecimento (seção 6): a extração da LLM vira
 * proposta pendente; duplicidade exata é descartada sem consumir IA.
 */
@ExtendWith(MockitoExtension.class)
class DocumentoServiceTest {

    @Mock DocumentoRepository documentos;
    @Mock PropostaRepository propostas;
    @Mock FatoRepository fatos;
    @Mock EventoAuditoriaRepository auditoria;
    @Mock AiClient aiClient;
    @TempDir Path armazenamento;

    DocumentoService servico;

    @BeforeEach
    void configurar() {
        servico = new DocumentoService(documentos, propostas, fatos, auditoria,
                aiClient, armazenamento.toString());
        lenient().when(documentos.save(any())).thenAnswer(inv -> {
            Documento doc = inv.getArgument(0);
            if (doc.getId() == null) doc.setId(1L);
            return doc;
        });
        lenient().when(propostas.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    /** Campos não relevantes ao teste vão como null — o record normaliza. */
    private ContratosIa.Extracao extracaoCom(String tipo, Map<String, Object> payload) {
        return new ContratosIa.Extracao(List.of(new ContratosIa.PropostaExtraida(
                "CRIAR", tipo, payload, null, "mencionada no texto", "…trecho…",
                0.8, "fake:modelo", "v1")));
    }

    @Test
    void importarTextoGeraPropostasPendentes() {
        when(documentos.findBySha256(any())).thenReturn(Optional.empty());
        when(fatos.findByStatusOrderByTipoAsc("APROVADO")).thenReturn(List.of());
        when(aiClient.executar(eq("extrair-conhecimento"), any(), eq(ContratosIa.Extracao.class)))
                .thenReturn(extracaoCom("HABILIDADE", Map.of("nome", "Java")));

        Map<String, Object> resultado = servico.importarTexto("Currículo", "Trabalho com Java.");

        assertThat(resultado.get("propostasGeradas")).isEqualTo(1);
        ArgumentCaptor<Proposta> criada = ArgumentCaptor.forClass(Proposta.class);
        verify(propostas).save(criada.capture());
        assertThat(criada.getValue().getStatus()).isEqualTo("PENDENTE");
        assertThat(criada.getValue().getDocumentoOrigemId()).isEqualTo(1L);
        // A LLM nunca escreve na base oficial: nenhum fato é criado aqui.
        verify(fatos, never()).save(any());
    }

    @Test
    void duplicidadeExataEDescartadaSemVirarProposta() {
        Fato existente = new Fato();
        existente.setId(4L);
        existente.setTipo("HABILIDADE");
        existente.setPayload(Map.of("nome", "Java"));

        when(documentos.findBySha256(any())).thenReturn(Optional.empty());
        when(fatos.findByStatusOrderByTipoAsc("APROVADO")).thenReturn(List.of(existente));
        when(aiClient.executar(eq("extrair-conhecimento"), any(), eq(ContratosIa.Extracao.class)))
                .thenReturn(extracaoCom("HABILIDADE", Map.of("nome", "Java")));

        Map<String, Object> resultado = servico.importarTexto("Currículo", "Java de novo.");

        assertThat(resultado.get("propostasGeradas")).isEqualTo(0);
        verify(propostas, never()).save(any());
    }

    @Test
    void conteudoRepetidoEBarradoPeloHash() {
        Documento anterior = new Documento();
        anterior.setId(8L);
        when(documentos.findBySha256(any())).thenReturn(Optional.of(anterior));

        assertThatThrownBy(() -> servico.importarTexto("Cópia", "mesmo conteúdo"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("já importado");
        verify(aiClient, never()).executar(any(), any(), any());
    }

    @Test
    void conteudoVazioERejeitadoSemConsumirIa() {
        assertThatThrownBy(() -> servico.importarTexto("Vazio", "   \n  "))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(aiClient);
    }
}
