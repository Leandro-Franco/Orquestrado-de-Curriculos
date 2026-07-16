package br.com.curriculos.web;

import br.com.curriculos.dominio.Perfil;
import br.com.curriculos.repositorio.PerfilRepository;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/perfil")
public class PerfilController {

    private final PerfilRepository perfis;

    public PerfilController(PerfilRepository perfis) {
        this.perfis = perfis;
    }

    @GetMapping
    public Perfil buscar() {
        return perfis.findById((short) 1).orElseGet(() -> {
            Perfil p = new Perfil();
            p.setId((short) 1);
            return perfis.save(p);
        });
    }

    @PutMapping
    public Perfil atualizar(@RequestBody Map<String, Object> corpo) {
        Perfil p = buscar();
        if (corpo.containsKey("nomeCompleto")) p.setNomeCompleto((String) corpo.get("nomeCompleto"));
        if (corpo.containsKey("email")) p.setEmail((String) corpo.get("email"));
        if (corpo.containsKey("telefone")) p.setTelefone((String) corpo.get("telefone"));
        if (corpo.containsKey("localizacao")) p.setLocalizacao((String) corpo.get("localizacao"));
        if (corpo.containsKey("tituloProfissional")) p.setTituloProfissional((String) corpo.get("tituloProfissional"));
        if (corpo.containsKey("objetivo")) p.setObjetivo((String) corpo.get("objetivo"));
        p.setAtualizadoEm(OffsetDateTime.now());
        return perfis.save(p);
    }
}
