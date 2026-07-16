package br.com.curriculos.web;

import br.com.curriculos.dominio.Curriculo;
import br.com.curriculos.dominio.SecaoCurriculo;
import br.com.curriculos.dominio.VersaoCurriculo;
import br.com.curriculos.repositorio.CurriculoRepository;
import br.com.curriculos.repositorio.SecaoCurriculoRepository;
import br.com.curriculos.repositorio.VersaoCurriculoRepository;
import br.com.curriculos.servico.CurriculoService;
import br.com.curriculos.servico.RenderizacaoService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/curriculos")
public class CurriculoController {

    private final CurriculoRepository curriculos;
    private final SecaoCurriculoRepository secoes;
    private final VersaoCurriculoRepository versoes;
    private final CurriculoService curriculoService;
    private final RenderizacaoService renderizacao;

    public CurriculoController(CurriculoRepository curriculos, SecaoCurriculoRepository secoes,
                               VersaoCurriculoRepository versoes, CurriculoService curriculoService,
                               RenderizacaoService renderizacao) {
        this.curriculos = curriculos;
        this.secoes = secoes;
        this.versoes = versoes;
        this.curriculoService = curriculoService;
        this.renderizacao = renderizacao;
    }

    @GetMapping
    public List<Curriculo> listar() {
        return curriculos.findAllByOrderByCriadoEmDesc();
    }

    @PostMapping
    public Curriculo gerar(@RequestBody Map<String, Object> corpo) {
        Object vagaId = corpo.get("vagaId");
        return curriculoService.gerar(
                vagaId instanceof Number n ? n.longValue() : null,
                (String) corpo.get("titulo"),
                (String) corpo.get("template"));
    }

    @GetMapping("/{id}")
    public Map<String, Object> buscar(@PathVariable Long id) {
        Curriculo curriculo = curriculoService.buscar(id);
        return Map.of("curriculo", curriculo,
                "secoes", secoes.findByCurriculoIdOrderByOrdemAsc(id));
    }

    @PutMapping("/{id}/secoes/{secaoId}")
    public SecaoCurriculo editarSecao(@PathVariable Long id, @PathVariable Long secaoId,
                                      @RequestBody Map<String, Object> corpo) {
        Object ordem = corpo.get("ordem");
        return curriculoService.editarSecao(id, secaoId,
                (String) corpo.get("titulo"),
                (String) corpo.get("conteudo"),
                ordem instanceof Number n ? n.intValue() : null);
    }

    @PostMapping("/{id}/secoes/{secaoId}/regerar")
    public SecaoCurriculo regerarSecao(@PathVariable Long id, @PathVariable Long secaoId) {
        return curriculoService.regerarSecao(id, secaoId);
    }

    @GetMapping("/{id}/versoes")
    public List<VersaoCurriculo> versoes(@PathVariable Long id) {
        return versoes.findByCurriculoIdOrderByNumeroDesc(id);
    }

    @GetMapping(value = "/{id}/preview", produces = MediaType.TEXT_HTML_VALUE)
    public String preview(@PathVariable Long id) {
        return renderizacao.renderizarHtml(id);
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id) {
        byte[] pdf = renderizacao.gerarPdf(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=curriculo-" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
