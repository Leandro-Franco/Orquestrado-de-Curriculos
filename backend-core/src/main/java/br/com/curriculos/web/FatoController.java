package br.com.curriculos.web;

import br.com.curriculos.dominio.Evidencia;
import br.com.curriculos.dominio.Fato;
import br.com.curriculos.repositorio.EvidenciaRepository;
import br.com.curriculos.repositorio.FatoRepository;
import br.com.curriculos.servico.FatoService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/fatos")
public class FatoController {

    private final FatoRepository fatos;
    private final EvidenciaRepository evidencias;
    private final FatoService fatoService;

    public FatoController(FatoRepository fatos, EvidenciaRepository evidencias, FatoService fatoService) {
        this.fatos = fatos;
        this.evidencias = evidencias;
        this.fatoService = fatoService;
    }

    @GetMapping
    public List<Fato> listar(@RequestParam(required = false) String tipo) {
        return tipo == null
                ? fatos.findByStatusOrderByTipoAsc("APROVADO")
                : fatos.findByTipoAndStatus(tipo, "APROVADO");
    }

    /** Corpo de criação/edição de fato: o payload varia por tipo (ADR-002). */
    public record FatoRequisicao(String tipo, Map<String, Object> payload) {
    }

    @PostMapping
    public Fato criar(@RequestBody FatoRequisicao corpo) {
        return fatoService.criar(corpo.tipo(), corpo.payload());
    }

    @PutMapping("/{id}")
    public Fato atualizar(@PathVariable Long id, @RequestBody FatoRequisicao corpo) {
        return fatoService.atualizar(id, corpo.payload());
    }

    @DeleteMapping("/{id}")
    public void remover(@PathVariable Long id) {
        fatoService.remover(id);
    }

    @GetMapping("/{id}/evidencias")
    public List<Evidencia> evidencias(@PathVariable Long id) {
        return evidencias.findByFatoId(id);
    }
}
