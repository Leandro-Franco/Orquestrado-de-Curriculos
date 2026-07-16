package br.com.curriculos.web;

import br.com.curriculos.dominio.RequisitoVaga;
import br.com.curriculos.dominio.Vaga;
import br.com.curriculos.repositorio.RequisitoVagaRepository;
import br.com.curriculos.repositorio.VagaRepository;
import br.com.curriculos.servico.VagaService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vagas")
public class VagaController {

    private final VagaRepository vagas;
    private final RequisitoVagaRepository requisitos;
    private final VagaService vagaService;

    public VagaController(VagaRepository vagas, RequisitoVagaRepository requisitos, VagaService vagaService) {
        this.vagas = vagas;
        this.requisitos = requisitos;
        this.vagaService = vagaService;
    }

    @GetMapping
    public List<Vaga> listar() {
        return vagas.findAllByOrderByCriadoEmDesc();
    }

    @GetMapping("/{id}")
    public Vaga buscar(@PathVariable Long id) {
        return vagaService.buscar(id);
    }

    @PostMapping
    public Vaga criar(@RequestBody Map<String, String> corpo) {
        return vagaService.criar(corpo.get("titulo"), corpo.get("empresa"), corpo.get("descricao"));
    }

    @PostMapping("/{id}/analisar")
    public Vaga analisar(@PathVariable Long id) {
        return vagaService.analisar(id);
    }

    @GetMapping("/{id}/requisitos")
    public List<RequisitoVaga> requisitos(@PathVariable Long id) {
        return requisitos.findByVagaIdOrderByIdAsc(id);
    }
}
