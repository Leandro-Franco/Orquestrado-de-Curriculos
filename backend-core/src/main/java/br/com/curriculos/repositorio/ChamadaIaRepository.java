package br.com.curriculos.repositorio;

import br.com.curriculos.dominio.ChamadaIa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Map;

public interface ChamadaIaRepository extends JpaRepository<ChamadaIa, Long> {

    List<ChamadaIa> findTop50ByOrderByCriadoEmDesc();

    @Query(value = """
            SELECT operacao,
                   modelo,
                   COUNT(*)                          AS chamadas,
                   COALESCE(SUM(tokens_entrada), 0)  AS tokens_entrada,
                   COALESCE(SUM(tokens_saida), 0)    AS tokens_saida,
                   COALESCE(SUM(tokens_cache), 0)    AS tokens_cache,
                   COALESCE(SUM(custo_estimado), 0)  AS custo_estimado,
                   COALESCE(AVG(duracao_ms), 0)      AS duracao_media_ms
            FROM chamada_ia
            GROUP BY operacao, modelo
            ORDER BY custo_estimado DESC
            """, nativeQuery = true)
    List<Map<String, Object>> resumoPorOperacao();
}
