package br.com.curriculos.web;

import br.com.curriculos.dominio.ChamadaIa;
import br.com.curriculos.dominio.EventoAuditoria;
import br.com.curriculos.repositorio.ChamadaIaRepository;
import br.com.curriculos.repositorio.EventoAuditoriaRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class MetricasController {

    private final ChamadaIaRepository chamadas;
    private final EventoAuditoriaRepository auditoria;

    public MetricasController(ChamadaIaRepository chamadas, EventoAuditoriaRepository auditoria) {
        this.chamadas = chamadas;
        this.auditoria = auditoria;
    }

    @GetMapping("/metricas/ia")
    public Map<String, Object> metricas() {
        List<Map<String, Object>> resumo = chamadas.resumoPorOperacao();
        List<ChamadaIa> ultimas = chamadas.findTop50ByOrderByCriadoEmDesc();
        return Map.of("resumo", resumo, "ultimas", ultimas);
    }

    @GetMapping("/auditoria")
    public List<EventoAuditoria> auditoria() {
        return auditoria.findTop100ByOrderByCriadoEmDesc();
    }
}
