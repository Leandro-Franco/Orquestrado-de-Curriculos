package br.com.curriculos.servico;

import br.com.curriculos.dominio.*;
import br.com.curriculos.repositorio.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Análise de vagas (Contexto Mestre, seção 7). A descrição da vaga é
 * conteúdo externo não confiável: vai à LLM como dado delimitado, nunca
 * como instrução. A compatibilidade relaciona requisitos a fatos aprovados.
 */
@Service
public class VagaService {

    private final VagaRepository vagas;
    private final RequisitoVagaRepository requisitos;
    private final FatoRepository fatos;
    private final EventoAuditoriaRepository auditoria;
    private final AiClient aiClient;

    public VagaService(VagaRepository vagas, RequisitoVagaRepository requisitos,
                       FatoRepository fatos, EventoAuditoriaRepository auditoria, AiClient aiClient) {
        this.vagas = vagas;
        this.requisitos = requisitos;
        this.fatos = fatos;
        this.auditoria = auditoria;
        this.aiClient = aiClient;
    }

    @Transactional
    public Vaga criar(String titulo, String empresa, String descricao) {
        if (descricao == null || descricao.isBlank()) {
            throw new IllegalArgumentException("Descrição da vaga é obrigatória");
        }
        Vaga vaga = new Vaga();
        vaga.setTitulo(titulo);
        vaga.setEmpresa(empresa);
        vaga.setDescricaoBruta(descricao.strip());
        return vagas.save(vaga);
    }

    @SuppressWarnings("unchecked")
    @Transactional
    public Vaga analisar(Long vagaId) {
        Vaga vaga = buscar(vagaId);

        // Etapa 1 — extração estruturada da descrição (modelo intermediário).
        Map<String, Object> analise = aiClient.executar("analisar-vaga",
                Map.of("descricao", vaga.getDescricaoBruta()));
        vaga.setAnalise(analise);
        if (vaga.getTitulo() == null || vaga.getTitulo().isBlank()) {
            vaga.setTitulo((String) analise.get("titulo"));
        }
        if (vaga.getEmpresa() == null || vaga.getEmpresa().isBlank()) {
            vaga.setEmpresa((String) analise.get("empresa"));
        }

        requisitos.deleteByVagaId(vagaId);
        List<RequisitoVaga> novos = new ArrayList<>();
        novos.addAll(criarRequisitos(vagaId, (List<Map<String, Object>>) analise.get("requisitos_obrigatorios"), "OBRIGATORIO"));
        novos.addAll(criarRequisitos(vagaId, (List<Map<String, Object>>) analise.get("requisitos_desejaveis"), "DESEJAVEL"));
        novos = requisitos.saveAll(novos);

        // Etapa 2 — relacionar cada requisito às evidências aprovadas (modelo econômico).
        List<Fato> aprovados = fatos.findByStatusOrderByTipoAsc("APROVADO");
        if (!novos.isEmpty() && !aprovados.isEmpty()) {
            relacionar(novos, aprovados);
        } else {
            novos.forEach(r -> r.setCompatibilidade("INCONCLUSIVA"));
        }
        requisitos.saveAll(novos);

        vaga.setStatus("ANALISADA");
        vaga = vagas.save(vaga);
        auditoria.save(EventoAuditoria.de("vaga", vagaId, "ANALISADA",
                Map.of("requisitos", novos.size())));
        return vaga;
    }

    private List<RequisitoVaga> criarRequisitos(Long vagaId, List<Map<String, Object>> itens, String tipo) {
        if (itens == null) return List.of();
        List<RequisitoVaga> lista = new ArrayList<>();
        for (Map<String, Object> item : itens) {
            String descricao = (String) item.get("descricao");
            if (descricao == null || descricao.isBlank()) continue;
            RequisitoVaga r = new RequisitoVaga();
            r.setVagaId(vagaId);
            r.setDescricao(descricao);
            r.setTipo(tipo);
            r.setCategoria((String) item.get("categoria"));
            lista.add(r);
        }
        return lista;
    }

    @SuppressWarnings("unchecked")
    private void relacionar(List<RequisitoVaga> novos, List<Fato> aprovados) {
        List<Map<String, Object>> reqPayload = novos.stream()
                .map(r -> Map.<String, Object>of("id", r.getId(), "descricao", r.getDescricao(), "tipo", r.getTipo()))
                .toList();
        List<Map<String, Object>> fatosPayload = aprovados.stream()
                .map(f -> Map.<String, Object>of("id", f.getId(), "tipo", f.getTipo(), "payload", f.getPayload()))
                .toList();

        Map<String, Object> resultado = aiClient.executar("relacionar-requisitos",
                Map.of("requisitos", reqPayload, "fatos", fatosPayload));
        List<Map<String, Object>> relacoes =
                (List<Map<String, Object>>) resultado.getOrDefault("relacoes", List.of());

        Map<Long, Map<String, Object>> porRequisito = new HashMap<>();
        for (Map<String, Object> rel : relacoes) {
            Object id = rel.get("requisito_id");
            if (id instanceof Number n) porRequisito.put(n.longValue(), rel);
        }
        Set<String> niveisValidos = Set.of("ALTA", "MEDIA", "PARCIAL", "AUSENTE", "INCONCLUSIVA");
        Set<Long> idsValidos = new HashSet<>();
        aprovados.forEach(f -> idsValidos.add(f.getId()));

        for (RequisitoVaga r : novos) {
            Map<String, Object> rel = porRequisito.get(r.getId());
            if (rel == null) {
                r.setCompatibilidade("INCONCLUSIVA");
                continue;
            }
            String nivel = String.valueOf(rel.get("compatibilidade")).toUpperCase();
            r.setCompatibilidade(niveisValidos.contains(nivel) ? nivel : "INCONCLUSIVA");
            r.setJustificativa((String) rel.get("justificativa"));
            List<Long> relacionados = new ArrayList<>();
            Object fatosRel = rel.get("fatos");
            if (fatosRel instanceof List<?> lista) {
                for (Object o : lista) {
                    // A LLM só pode citar fatos que realmente existem (validação de saída).
                    if (o instanceof Number n && idsValidos.contains(n.longValue())) {
                        relacionados.add(n.longValue());
                    }
                }
            }
            r.setFatosRelacionados(relacionados);
            if (relacionados.isEmpty() && !"AUSENTE".equals(r.getCompatibilidade())) {
                r.setCompatibilidade("INCONCLUSIVA");
            }
        }
    }

    public Vaga buscar(Long id) {
        return vagas.findById(id).orElseThrow(() -> new IllegalArgumentException("Vaga não encontrada: " + id));
    }
}
