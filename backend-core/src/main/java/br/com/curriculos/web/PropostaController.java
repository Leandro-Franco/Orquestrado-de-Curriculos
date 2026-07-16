package br.com.curriculos.web;

import br.com.curriculos.dominio.Fato;
import br.com.curriculos.dominio.Proposta;
import br.com.curriculos.repositorio.PropostaRepository;
import br.com.curriculos.servico.PropostaService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/propostas")
public class PropostaController {

    private final PropostaRepository propostas;
    private final PropostaService propostaService;

    public PropostaController(PropostaRepository propostas, PropostaService propostaService) {
        this.propostas = propostas;
        this.propostaService = propostaService;
    }

    @GetMapping
    public List<Proposta> listar(@RequestParam(required = false) String status) {
        return status == null
                ? propostas.findAllByOrderByCriadoEmDesc()
                : propostas.findByStatusOrderByCriadoEmDesc(status);
    }

    @PostMapping("/{id}/aprovar")
    public Fato aprovar(@PathVariable Long id) {
        return propostaService.aprovar(id);
    }

    @PostMapping("/{id}/rejeitar")
    public void rejeitar(@PathVariable Long id, @RequestBody(required = false) Map<String, String> corpo) {
        propostaService.rejeitar(id, corpo != null ? corpo.get("motivo") : null);
    }
}
