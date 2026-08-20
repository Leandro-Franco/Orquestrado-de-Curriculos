package br.com.curriculos.servico;

import br.com.curriculos.dominio.Fato;
import br.com.curriculos.repositorio.EventoAuditoriaRepository;
import br.com.curriculos.repositorio.FatoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Validação determinística por tipo (sem IA) — Contexto Mestre, seção 10. */
@ExtendWith(MockitoExtension.class)
class FatoServiceTest {

    @Mock FatoRepository fatos;
    @Mock EventoAuditoriaRepository auditoria;
    @Mock AiClient aiClient;

    FatoService servico;

    @BeforeEach
    void configurar() {
        servico = new FatoService(fatos, auditoria, aiClient);
    }

    @Test
    void aceitaPayloadCompletoPorTipo() {
        assertThatCode(() -> servico.validar("EXPERIENCIA",
                Map.of("cargo", "Dev", "empresa", "X", "inicio", "2024")))
                .doesNotThrowAnyException();
        assertThatCode(() -> servico.validar("HABILIDADE", Map.of("nome", "Java")))
                .doesNotThrowAnyException();
        assertThatCode(() -> servico.validar("LINK",
                Map.of("rotulo", "GitHub", "url", "github.com/x")))
                .doesNotThrowAnyException();
    }

    @Test
    void rejeitaCampoObrigatorioAusenteOuVazio() {
        assertThatThrownBy(() -> servico.validar("EXPERIENCIA", Map.of("cargo", "Dev")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("empresa");
        assertThatThrownBy(() -> servico.validar("IDIOMA", Map.of("idioma", "Inglês", "nivel", " ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nivel");
    }

    @Test
    void rejeitaTipoDesconhecido() {
        assertThatThrownBy(() -> servico.validar("SUPERPODER", Map.of("nome", "voar")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tipo de fato desconhecido");
    }

    @Test
    void criarSalvaAuditaEReindexa() {
        when(fatos.save(any())).thenAnswer(inv -> {
            Fato fato = inv.getArgument(0);
            fato.setId(7L);
            return fato;
        });

        Fato criado = servico.criar("HABILIDADE", Map.of("nome", "Docker"));

        assertThat(criado.getId()).isEqualTo(7L);
        assertThat(criado.getStatus()).isEqualTo("APROVADO");
        verify(auditoria).save(any());
        verify(aiClient).indexar(eq("FATO"), eq(7L), contains("Docker"));
    }

    @Test
    void falhaDeReindexacaoNaoImpedeAOperacao() {
        when(fatos.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("IA fora do ar"))
                .when(aiClient).indexar(any(), any(), any());

        assertThatCode(() -> servico.criar("HABILIDADE", Map.of("nome", "Git")))
                .doesNotThrowAnyException();
    }
}
