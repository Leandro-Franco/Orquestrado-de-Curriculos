package br.com.curriculos.servico;

import br.com.curriculos.dominio.*;
import br.com.curriculos.repositorio.*;
import br.com.curriculos.servico.ia.ContratosIa;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;

/**
 * Geração controlada em etapas (Contexto Mestre, seção 8):
 * recuperação -> estratégia -> redação por seção -> validação factual -> revisão humana.
 * Somente fatos APROVADOS alimentam a geração.
 */
@Service
public class CurriculoService {

    private static final List<String> SECOES_PADRAO = List.of(
            "TITULO", "RESUMO", "EXPERIENCIAS", "PROJETOS",
            "COMPETENCIAS", "FORMACAO", "CURSOS", "IDIOMAS");

    private final CurriculoRepository curriculos;
    private final SecaoCurriculoRepository secoes;
    private final VersaoCurriculoRepository versoes;
    private final FatoRepository fatos;
    private final RequisitoVagaRepository requisitos;
    private final VagaRepository vagas;
    private final PerfilRepository perfis;
    private final EventoAuditoriaRepository auditoria;
    private final AiClient aiClient;

    public CurriculoService(CurriculoRepository curriculos, SecaoCurriculoRepository secoes,
                            VersaoCurriculoRepository versoes, FatoRepository fatos,
                            RequisitoVagaRepository requisitos, VagaRepository vagas,
                            PerfilRepository perfis, EventoAuditoriaRepository auditoria,
                            AiClient aiClient) {
        this.curriculos = curriculos;
        this.secoes = secoes;
        this.versoes = versoes;
        this.fatos = fatos;
        this.requisitos = requisitos;
        this.vagas = vagas;
        this.perfis = perfis;
        this.auditoria = auditoria;
        this.aiClient = aiClient;
    }

    @Transactional
    public Curriculo gerar(Long vagaId, String titulo, String template) {
        List<Fato> aprovados = fatos.findByStatusOrderByTipoAsc("APROVADO");
        if (aprovados.isEmpty()) {
            throw new IllegalStateException("Não há fatos aprovados na base de conhecimento");
        }
        Map<String, Object> analiseVaga = Map.of();
        List<Map<String, Object>> compatibilidade = List.of();
        if (vagaId != null) {
            Vaga vaga = vagas.findById(vagaId)
                    .orElseThrow(() -> new IllegalArgumentException("Vaga não encontrada: " + vagaId));
            if (vaga.getAnalise() != null) analiseVaga = vaga.getAnalise();
            compatibilidade = requisitos.findByVagaIdOrderByIdAsc(vagaId).stream()
                    .map(r -> Map.<String, Object>of(
                            "descricao", r.getDescricao(),
                            "tipo", r.getTipo(),
                            "compatibilidade", String.valueOf(r.getCompatibilidade()),
                            "fatos", r.getFatosRelacionados() == null ? List.of() : r.getFatosRelacionados()))
                    .toList();
        }

        // Etapa 2 — estratégia (modelo avançado). Guardada como JSONB e reenviada
        // à IA na redação; o Backend Core não lê seus campos.
        ContratosIa.Estrategia estrategia = aiClient.executar("gerar-estrategia", Map.of(
                "analise_vaga", analiseVaga,
                "compatibilidade", compatibilidade,
                "fatos", fatosComoPayload(aprovados),
                "perfil", perfilComoPayload()), ContratosIa.Estrategia.class);
        Map<String, Object> estrategiaMapa = aiClient.comoMapa(estrategia);

        Curriculo curriculo = new Curriculo();
        curriculo.setVagaId(vagaId);
        curriculo.setTitulo(titulo != null && !titulo.isBlank() ? titulo : "Currículo");
        curriculo.setTemplate(template != null ? template : "classico");
        curriculo.setEstrategia(estrategiaMapa);
        curriculo = curriculos.save(curriculo);

        // Etapa 3 e 4 — redação e validação factual, seção a seção.
        int ordem = 0;
        for (String tipo : SECOES_PADRAO) {
            SecaoCurriculo secao = new SecaoCurriculo();
            secao.setCurriculoId(curriculo.getId());
            secao.setTipo(tipo);
            secao.setOrdem(ordem++);
            redigirSecao(secao, estrategiaMapa, analiseVaga, aprovados);
            secoes.save(secao);
        }

        curriculo.setStatus("GERADO");
        curriculo = curriculos.save(curriculo);
        salvarVersao(curriculo, "Geração inicial");
        auditoria.save(EventoAuditoria.de("curriculo", curriculo.getId(), "GERADO",
                Map.of("vaga_id", String.valueOf(vagaId))));
        return curriculo;
    }

    /** Regeneração por seção — não reprocessa o currículo inteiro (seção 10). */
    @Transactional
    public SecaoCurriculo regerarSecao(Long curriculoId, Long secaoId) {
        Curriculo curriculo = buscar(curriculoId);
        SecaoCurriculo secao = secaoDe(curriculoId, secaoId);
        Map<String, Object> analiseVaga = Map.of();
        if (curriculo.getVagaId() != null) {
            Vaga vaga = vagas.findById(curriculo.getVagaId()).orElse(null);
            if (vaga != null && vaga.getAnalise() != null) analiseVaga = vaga.getAnalise();
        }
        List<Fato> aprovados = fatos.findByStatusOrderByTipoAsc("APROVADO");
        redigirSecao(secao, curriculo.getEstrategia() == null ? Map.of() : curriculo.getEstrategia(),
                analiseVaga, aprovados);
        secao = secoes.save(secao);
        tocar(curriculo);
        salvarVersao(curriculo, "Seção regenerada: " + secao.getTipo());
        return secao;
    }

    @Transactional
    public SecaoCurriculo editarSecao(Long curriculoId, Long secaoId, String titulo,
                                      String conteudo, Integer ordem) {
        Curriculo curriculo = buscar(curriculoId);
        SecaoCurriculo secao = secaoDe(curriculoId, secaoId);
        if (titulo != null) secao.setTitulo(titulo);
        if (conteudo != null) secao.setConteudo(conteudo);
        if (ordem != null) secao.setOrdem(ordem);
        secao.setAtualizadoEm(OffsetDateTime.now());
        secao = secoes.save(secao);
        tocar(curriculo);
        salvarVersao(curriculo, "Edição manual: " + secao.getTipo());
        return secao;
    }

    private void redigirSecao(SecaoCurriculo secao, Map<String, Object> estrategia,
                              Map<String, Object> analiseVaga, List<Fato> aprovados) {
        ContratosIa.Secao resultado = aiClient.executar("gerar-secao", Map.of(
                "tipo_secao", secao.getTipo(),
                "estrategia", estrategia,
                "analise_vaga", analiseVaga,
                "fatos", fatosComoPayload(aprovados),
                "perfil", perfilComoPayload()), ContratosIa.Secao.class);

        secao.setTitulo(resultado.titulo() != null ? resultado.titulo() : secao.getTipo());
        secao.setConteudo(resultado.conteudo() != null ? resultado.conteudo() : "");

        // A LLM só pode citar fatos que realmente existem (validação de saída).
        Set<Long> validos = new HashSet<>();
        aprovados.forEach(f -> validos.add(f.getId()));
        secao.setFatosUtilizados(resultado.fatosUtilizados().stream()
                .filter(id -> id != null && validos.contains(id))
                .toList());
        secao.setAtualizadoEm(OffsetDateTime.now());

        // Etapa 4 — validação factual contra a base canônica.
        secao.setAlertasValidacao(validar(secao.getConteudo(), aprovados));
    }

    private List<String> validar(String conteudo, List<Fato> aprovados) {
        if (conteudo == null || conteudo.isBlank()) return List.of();
        ContratosIa.Validacao resultado = aiClient.executar("validar-afirmacoes",
                Map.of("conteudo", conteudo, "fatos", fatosComoPayload(aprovados)),
                ContratosIa.Validacao.class);
        return resultado.afirmacoes().stream()
                .filter(ContratosIa.Afirmacao::naoSustentada)
                .map(a -> "Sem suporte na base: \"" + a.texto() + "\""
                        + (a.nota() != null && !a.nota().isBlank() ? " — " + a.nota() : ""))
                .toList();
    }

    @Transactional
    public VersaoCurriculo salvarVersao(Curriculo curriculo, String nota) {
        List<Map<String, Object>> snapshotSecoes = secoes
                .findByCurriculoIdOrderByOrdemAsc(curriculo.getId()).stream()
                .map(s -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("tipo", s.getTipo());
                    m.put("ordem", s.getOrdem());
                    m.put("titulo", s.getTitulo());
                    m.put("conteudo", s.getConteudo());
                    return m;
                })
                .toList();
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("titulo", curriculo.getTitulo());
        snapshot.put("template", curriculo.getTemplate());
        snapshot.put("secoes", snapshotSecoes);

        int proximo = versoes.findByCurriculoIdOrderByNumeroDesc(curriculo.getId()).stream()
                .findFirst().map(v -> v.getNumero() + 1).orElse(1);
        VersaoCurriculo versao = new VersaoCurriculo();
        versao.setCurriculoId(curriculo.getId());
        versao.setNumero(proximo);
        versao.setSnapshot(snapshot);
        versao.setNota(nota);
        return versoes.save(versao);
    }

    public Curriculo buscar(Long id) {
        return curriculos.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Currículo não encontrado: " + id));
    }

    private SecaoCurriculo secaoDe(Long curriculoId, Long secaoId) {
        SecaoCurriculo secao = secoes.findById(secaoId)
                .orElseThrow(() -> new IllegalArgumentException("Seção não encontrada: " + secaoId));
        if (!secao.getCurriculoId().equals(curriculoId)) {
            throw new IllegalArgumentException("Seção não pertence ao currículo informado");
        }
        return secao;
    }

    private void tocar(Curriculo curriculo) {
        curriculo.setAtualizadoEm(OffsetDateTime.now());
        curriculos.save(curriculo);
    }

    private List<Map<String, Object>> fatosComoPayload(List<Fato> lista) {
        return lista.stream()
                .map(f -> Map.<String, Object>of("id", f.getId(), "tipo", f.getTipo(), "payload", f.getPayload()))
                .toList();
    }

    private Map<String, Object> perfilComoPayload() {
        return perfis.findById((short) 1).map(p -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("nome", p.getNomeCompleto());
            m.put("email", p.getEmail());
            m.put("telefone", p.getTelefone());
            m.put("localizacao", p.getLocalizacao());
            m.put("titulo_profissional", p.getTituloProfissional());
            m.put("objetivo", p.getObjetivo());
            return m;
        }).orElse(Map.of());
    }
}
