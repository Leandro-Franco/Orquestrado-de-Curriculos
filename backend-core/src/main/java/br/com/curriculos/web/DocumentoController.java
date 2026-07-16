package br.com.curriculos.web;

import br.com.curriculos.dominio.Documento;
import br.com.curriculos.repositorio.DocumentoRepository;
import br.com.curriculos.servico.DocumentoService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documentos")
public class DocumentoController {

    private final DocumentoRepository documentos;
    private final DocumentoService documentoService;

    public DocumentoController(DocumentoRepository documentos, DocumentoService documentoService) {
        this.documentos = documentos;
        this.documentoService = documentoService;
    }

    @GetMapping
    public List<Documento> listar() {
        return documentos.findAll();
    }

    @GetMapping("/{id}")
    public Documento buscar(@PathVariable Long id) {
        return documentos.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Documento não encontrado: " + id));
    }

    @PostMapping("/texto")
    public Map<String, Object> importarTexto(@RequestBody Map<String, String> corpo) {
        return documentoService.importarTexto(
                corpo.getOrDefault("titulo", "Texto importado"),
                corpo.get("conteudo"));
    }

    @PostMapping("/arquivo")
    public Map<String, Object> importarArquivo(@RequestParam("arquivo") MultipartFile arquivo,
                                               @RequestParam(value = "titulo", required = false) String titulo) {
        String nome = titulo != null && !titulo.isBlank() ? titulo
                : arquivo.getOriginalFilename() != null ? arquivo.getOriginalFilename() : "Arquivo importado";
        return documentoService.importarArquivo(nome, arquivo);
    }
}
