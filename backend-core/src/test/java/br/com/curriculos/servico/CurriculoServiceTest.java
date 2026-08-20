package br.com.curriculos.servico;

import br.com.curriculos.dominio.Curriculo;
import br.com.curriculos.dominio.SecaoCurriculo;
import br.com.curriculos.repositorio.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/** Geração controlada (seção 8): só fatos aprovados alimentam o currículo. */
@ExtendWith(MockitoExtension.class)
class CurriculoServiceTest {

    @Mock CurriculoRepository curriculos;
    @Mock SecaoCurriculoRepository secoes;
    @Mock VersaoCurriculoRepository versoes;
    @Mock FatoRepository fatos;
    @Mock RequisitoVagaRepository requisitos;
    @Mock VagaRepository vagas;
    @Mock PerfilRepository perfis;
    @Mock EventoAuditoriaRepository auditoria;
    @Mock AiClient aiClient;

    CurriculoService servico;

    @BeforeEach
    void configurar() {
        servico = new CurriculoService(curriculos, secoes, versoes, fatos,
                requisitos, vagas, perfis, auditoria, aiClient);
    }

    @Test
    void semFatosAprovadosNaoHaGeracao() {
        when(fatos.findByStatusOrderByTipoAsc("APROVADO")).thenReturn(List.of());

        assertThatThrownBy(() -> servico.gerar(null, "Currículo", "classico"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("fatos aprovados");
        org.mockito.Mockito.verifyNoInteractions(aiClient);
    }

    @Test
    void secaoDeOutroCurriculoNaoPodeSerEditada() {
        Curriculo curriculo = new Curriculo();
        curriculo.setId(1L);
        when(curriculos.findById(1L)).thenReturn(Optional.of(curriculo));

        SecaoCurriculo alheia = new SecaoCurriculo();
        alheia.setId(50L);
        alheia.setCurriculoId(2L); // pertence a outro currículo
        when(secoes.findById(50L)).thenReturn(Optional.of(alheia));

        assertThatThrownBy(() -> servico.editarSecao(1L, 50L, "t", "c", 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("não pertence");
    }
}
