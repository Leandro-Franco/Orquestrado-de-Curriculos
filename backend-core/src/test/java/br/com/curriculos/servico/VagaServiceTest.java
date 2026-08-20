package br.com.curriculos.servico;

import br.com.curriculos.dominio.Fato;
import br.com.curriculos.dominio.RequisitoVaga;
import br.com.curriculos.dominio.Vaga;
import br.com.curriculos.repositorio.*;
import br.com.curriculos.servico.ia.ContratosIa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Análise de vagas (seção 7): a saída da LLM é validada — só entram níveis
 * de compatibilidade conhecidos e IDs de fatos que existem de verdade.
 */
@ExtendWith(MockitoExtension.class)
class VagaServiceTest {

    @Mock VagaRepository vagas;
    @Mock RequisitoVagaRepository requisitos;
    @Mock FatoRepository fatos;
    @Mock EventoAuditoriaRepository auditoria;
    @Mock AiClient aiClient;

    VagaService servico;

    @BeforeEach
    void configurar() {
        servico = new VagaService(vagas, requisitos, fatos, auditoria, aiClient);
    }

    /** Só os campos usados no teste; o restante o record normaliza para vazio. */
    private ContratosIa.AnaliseVaga analiseCom(String titulo,
                                               List<ContratosIa.RequisitoExtraido> obrigatorios) {
        return new ContratosIa.AnaliseVaga(titulo, null, null, null, null,
                null, obrigatorios, null, null, null, null, null, null);
    }

    @Test
    void analisarSaneiaNivelInvalidoEIdDeFatoInexistente() {
        Vaga vaga = new Vaga();
        vaga.setId(1L);
        vaga.setDescricaoBruta("Vaga de dev Java com Docker");
        when(vagas.findById(1L)).thenReturn(Optional.of(vaga));
        when(vagas.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AtomicLong sequencia = new AtomicLong(100);
        when(requisitos.saveAll(any())).thenAnswer(inv -> {
            List<RequisitoVaga> lista = inv.getArgument(0);
            lista.forEach(r -> {
                if (r.getId() == null) r.setId(sequencia.incrementAndGet());
            });
            return lista;
        });

        Fato fatoJava = new Fato();
        fatoJava.setId(13L);
        fatoJava.setTipo("HABILIDADE");
        fatoJava.setPayload(Map.of("nome", "Java"));
        when(fatos.findByStatusOrderByTipoAsc("APROVADO")).thenReturn(List.of(fatoJava));

        when(aiClient.executar(eq("analisar-vaga"), any(), eq(ContratosIa.AnaliseVaga.class)))
                .thenReturn(analiseCom("Dev Backend", List.of(
                        new ContratosIa.RequisitoExtraido("Experiência com Java", "tecnologia"),
                        new ContratosIa.RequisitoExtraido("Experiência com Docker", "tecnologia"))));

        // A LLM responde com um nível inválido e cita um fato inexistente (999):
        // ambos devem ser saneados pelo backend.
        when(aiClient.executar(eq("relacionar-requisitos"), any(), eq(ContratosIa.Relacionamento.class)))
                .thenReturn(new ContratosIa.Relacionamento(List.of(
                        new ContratosIa.Relacao(101L, "ALTA", List.of(13L, 999L), "Java comprovado"),
                        new ContratosIa.Relacao(102L, "NIVEL_INVENTADO", List.of(), null))));

        Vaga analisada = servico.analisar(1L);

        assertThat(analisada.getStatus()).isEqualTo("ANALISADA");
        assertThat(analisada.getTitulo()).isEqualTo("Dev Backend");

        ArgumentCaptor<List<RequisitoVaga>> salvos = ArgumentCaptor.captor();
        verify(requisitos, atLeastOnce()).saveAll(salvos.capture());
        List<RequisitoVaga> finais = salvos.getAllValues().getLast();

        RequisitoVaga java = finais.stream().filter(r -> r.getId() == 101L).findFirst().orElseThrow();
        assertThat(java.getCompatibilidade()).isEqualTo("ALTA");
        assertThat(java.getFatosRelacionados()).containsExactly(13L); // 999 descartado

        RequisitoVaga docker = finais.stream().filter(r -> r.getId() == 102L).findFirst().orElseThrow();
        assertThat(docker.getCompatibilidade()).isEqualTo("INCONCLUSIVA"); // nível inválido
    }

    @Test
    void requisitoSemDescricaoEDescartado() {
        Vaga vaga = new Vaga();
        vaga.setId(3L);
        vaga.setDescricaoBruta("Vaga qualquer");
        when(vagas.findById(3L)).thenReturn(Optional.of(vaga));
        when(vagas.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(requisitos.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(fatos.findByStatusOrderByTipoAsc("APROVADO")).thenReturn(List.of());
        when(aiClient.executar(eq("analisar-vaga"), any(), eq(ContratosIa.AnaliseVaga.class)))
                .thenReturn(analiseCom(null, List.of(
                        new ContratosIa.RequisitoExtraido("  ", "tecnologia"),
                        new ContratosIa.RequisitoExtraido(null, "tecnologia"))));

        servico.analisar(3L);

        ArgumentCaptor<List<RequisitoVaga>> salvos = ArgumentCaptor.captor();
        verify(requisitos, atLeastOnce()).saveAll(salvos.capture());
        assertThat(salvos.getAllValues().getFirst()).isEmpty();
    }

    @Test
    void semFatosAprovadosNaoHaComoRelacionar() {
        Vaga vaga = new Vaga();
        vaga.setId(2L);
        vaga.setDescricaoBruta("Vaga qualquer");
        when(vagas.findById(2L)).thenReturn(Optional.of(vaga));
        when(vagas.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(requisitos.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(fatos.findByStatusOrderByTipoAsc("APROVADO")).thenReturn(List.of());
        when(aiClient.executar(eq("analisar-vaga"), any(), eq(ContratosIa.AnaliseVaga.class)))
                .thenReturn(analiseCom(null, List.of(
                        new ContratosIa.RequisitoExtraido("Java", "tecnologia"))));

        servico.analisar(2L);

        // nenhum fato aprovado -> não há como relacionar; nada de chute
        verify(aiClient, never()).executar(eq("relacionar-requisitos"), any(), any());
    }
}
