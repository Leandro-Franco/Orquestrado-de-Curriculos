package br.com.curriculos.servico;

import br.com.curriculos.dominio.Curriculo;
import br.com.curriculos.dominio.Perfil;
import br.com.curriculos.dominio.SecaoCurriculo;
import br.com.curriculos.repositorio.FatoRepository;
import br.com.curriculos.repositorio.PerfilRepository;
import br.com.curriculos.repositorio.SecaoCurriculoRepository;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.*;

/**
 * Preview e PDF a partir da MESMA origem visual (ADR-001):
 * o HTML gerado pelo Thymeleaf é o artefato derivado do modelo estruturado,
 * e o Chromium (via Playwright) o imprime em A4.
 */
@Service
public class RenderizacaoService {

    private final TemplateEngine templateEngine;
    private final SecaoCurriculoRepository secoes;
    private final PerfilRepository perfis;
    private final FatoRepository fatos;
    private final CurriculoService curriculoService;

    private Playwright playwright;
    private Browser browser;

    public RenderizacaoService(TemplateEngine templateEngine, SecaoCurriculoRepository secoes,
                               PerfilRepository perfis, FatoRepository fatos,
                               CurriculoService curriculoService) {
        this.templateEngine = templateEngine;
        this.secoes = secoes;
        this.perfis = perfis;
        this.fatos = fatos;
        this.curriculoService = curriculoService;
    }

    public String renderizarHtml(Long curriculoId) {
        Curriculo curriculo = curriculoService.buscar(curriculoId);
        Perfil perfil = perfis.findById((short) 1).orElseGet(Perfil::new);

        List<Map<String, Object>> secoesView = new ArrayList<>();
        String tituloProfissional = null;
        for (SecaoCurriculo s : secoes.findByCurriculoIdOrderByOrdemAsc(curriculoId)) {
            if ("TITULO".equals(s.getTipo())) {
                tituloProfissional = s.getConteudo();
                continue; // o título vira parte do cabeçalho, não uma seção
            }
            if (s.getConteudo() == null || s.getConteudo().isBlank()) continue;
            Map<String, Object> view = new LinkedHashMap<>();
            view.put("titulo", s.getTitulo() != null ? s.getTitulo() : s.getTipo());
            view.put("linhas", Arrays.stream(s.getConteudo().split("\n"))
                    .filter(l -> !l.isBlank()).toList());
            secoesView.add(view);
        }

        // Links (LinkedIn, GitHub…) entram no cabeçalho, como no modelo do usuário.
        List<String> links = fatos.findByTipoAndStatus("LINK", "APROVADO").stream()
                .map(f -> String.valueOf(f.getPayload().getOrDefault("url", "")))
                .filter(u -> !u.isBlank())
                .toList();

        Context ctx = new Context(Locale.of("pt", "BR"));
        ctx.setVariable("perfil", perfil);
        ctx.setVariable("tituloProfissional", tituloProfissional != null
                ? tituloProfissional : perfil.getTituloProfissional());
        ctx.setVariable("links", links);
        ctx.setVariable("secoes", secoesView);
        return templateEngine.process("curriculo-a4", ctx);
    }

    public byte[] gerarPdf(Long curriculoId) {
        String html = renderizarHtml(curriculoId);
        synchronized (this) {
            if (browser == null) {
                playwright = Playwright.create();
                browser = playwright.chromium().launch();
            }
        }
        try (Page page = browser.newPage()) {
            page.setContent(html);
            return page.pdf(new Page.PdfOptions()
                    .setFormat("A4")
                    .setPrintBackground(true)
                    .setPreferCSSPageSize(true));
        }
    }

    @PreDestroy
    void fechar() {
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
    }
}
