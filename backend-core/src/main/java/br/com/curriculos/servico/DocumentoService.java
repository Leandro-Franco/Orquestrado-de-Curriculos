package br.com.curriculos.servico;

import br.com.curriculos.dominio.Documento;
import br.com.curriculos.dominio.EventoAuditoria;
import br.com.curriculos.dominio.Fato;
import br.com.curriculos.dominio.Proposta;
import br.com.curriculos.repositorio.DocumentoRepository;
import br.com.curriculos.repositorio.EventoAuditoriaRepository;
import br.com.curriculos.repositorio.FatoRepository;
import br.com.curriculos.repositorio.PropostaRepository;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;

/**
 * Fluxo de atualização do conhecimento (Contexto Mestre, seção 6):
 * importar -> extrair texto -> normalizar -> LLM extrai -> comparar com a base
 * -> criar propostas pendentes. A LLM nunca escreve diretamente no banco.
 */
@Service
public class DocumentoService {

    private final DocumentoRepository documentos;
    private final PropostaRepository propostas;
    private final FatoRepository fatos;
    private final EventoAuditoriaRepository auditoria;
    private final AiClient aiClient;
    private final Path storageDir;

    public DocumentoService(DocumentoRepository documentos, PropostaRepository propostas,
                            FatoRepository fatos, EventoAuditoriaRepository auditoria,
                            AiClient aiClient, @Value("${app.storage-dir}") String storageDir) {
        this.documentos = documentos;
        this.propostas = propostas;
        this.fatos = fatos;
        this.auditoria = auditoria;
        this.aiClient = aiClient;
        this.storageDir = Path.of(storageDir);
    }

    @Transactional
    public Map<String, Object> importarTexto(String titulo, String conteudo) {
        String texto = normalizar(conteudo);
        if (texto.isBlank()) throw new IllegalArgumentException("Conteúdo vazio");
        Documento doc = novoDocumento(titulo, "TEXTO", null, null, texto);
        return processar(doc);
    }

    @Transactional
    public Map<String, Object> importarArquivo(String titulo, MultipartFile arquivo) {
        try {
            byte[] bytes = arquivo.getBytes();
            String texto = normalizar(extrairTexto(arquivo.getOriginalFilename(), arquivo.getContentType(), bytes));
            if (texto.isBlank()) {
                throw new IllegalArgumentException("Não foi possível extrair texto do arquivo");
            }
            Files.createDirectories(storageDir);
            String nomeInterno = UUID.randomUUID() + "-" + Path.of(Objects.requireNonNullElse(
                    arquivo.getOriginalFilename(), "arquivo")).getFileName();
            Path destino = storageDir.resolve(nomeInterno);
            Files.write(destino, bytes);

            Documento doc = novoDocumento(titulo, "ARQUIVO", arquivo, destino.toString(), texto);
            return processar(doc);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao importar arquivo: " + e.getMessage(), e);
        }
    }

    private Documento novoDocumento(String titulo, String origem, MultipartFile arquivo,
                                    String caminho, String texto) {
        String hash = sha256(texto);
        documentos.findBySha256(hash).ifPresent(existente -> {
            throw new IllegalArgumentException(
                    "Documento com o mesmo conteúdo já importado (id " + existente.getId() + ")");
        });
        Documento doc = new Documento();
        doc.setTitulo(titulo);
        doc.setOrigem(origem);
        doc.setSha256(hash);
        doc.setTextoExtraido(texto);
        doc.setCaminhoArmazenamento(caminho);
        if (arquivo != null) {
            doc.setNomeArquivo(arquivo.getOriginalFilename());
            doc.setMimeType(arquivo.getContentType());
        }
        return documentos.save(doc);
    }

    /** Indexa o documento e gera propostas a partir da extração da LLM. */
    private Map<String, Object> processar(Documento doc) {
        try {
            aiClient.indexar("DOCUMENTO", doc.getId(), doc.getTextoExtraido());
        } catch (Exception e) {
            // indexação é derivada; não bloqueia a importação
        }
        List<Proposta> criadas = extrairPropostas(doc);
        doc.setStatus("PROCESSADO");
        documentos.save(doc);
        auditoria.save(EventoAuditoria.de("documento", doc.getId(), "IMPORTADO",
                Map.of("propostas_geradas", criadas.size())));
        return Map.of("documentoId", doc.getId(), "propostasGeradas", criadas.size());
    }

    @SuppressWarnings("unchecked")
    private List<Proposta> extrairPropostas(Documento doc) {
        List<Fato> existentes = fatos.findByStatusOrderByTipoAsc("APROVADO");
        List<Map<String, Object>> fatosExistentes = existentes.stream()
                .map(f -> Map.<String, Object>of("id", f.getId(), "tipo", f.getTipo(), "payload", f.getPayload()))
                .toList();

        Map<String, Object> resultado = aiClient.executar("extrair-conhecimento", Map.of(
                "documento_id", doc.getId(),
                "texto", doc.getTextoExtraido(),
                "fatos_existentes", fatosExistentes));

        List<Map<String, Object>> extraidas =
                (List<Map<String, Object>>) resultado.getOrDefault("propostas", List.of());

        List<Proposta> criadas = new ArrayList<>();
        for (Map<String, Object> p : extraidas) {
            Map<String, Object> payload = (Map<String, Object>) p.get("payload");
            String tipo = (String) p.get("tipo_fato");
            if (payload == null || tipo == null) continue;
            // Duplicidade exata é descartada deterministicamente, sem consumir IA.
            boolean duplicada = existentes.stream()
                    .anyMatch(f -> f.getTipo().equals(tipo) && f.getPayload().equals(payload));
            if (duplicada) continue;

            Proposta proposta = new Proposta();
            proposta.setAcao((String) p.getOrDefault("acao", "CRIAR"));
            proposta.setTipoFato(tipo);
            proposta.setPayloadProposto(payload);
            proposta.setDocumentoOrigemId(doc.getId());
            proposta.setJustificativa((String) p.get("justificativa"));
            proposta.setTrechoEvidencia((String) p.get("trecho_evidencia"));
            proposta.setModelo((String) p.get("modelo"));
            proposta.setVersaoPrompt((String) p.get("versao_prompt"));
            Object confianca = p.get("confianca");
            if (confianca instanceof Number n) {
                proposta.setConfianca(BigDecimal.valueOf(Math.min(1.0, Math.max(0.0, n.doubleValue()))));
            }
            Object alvo = p.get("fato_alvo_id");
            if (alvo instanceof Number n && "ATUALIZAR".equals(proposta.getAcao())) {
                fatos.findById(n.longValue()).ifPresent(f -> {
                    proposta.setFatoAlvoId(f.getId());
                    proposta.setPayloadAnterior(f.getPayload());
                });
            }
            criadas.add(propostas.save(proposta));
        }
        return criadas;
    }

    private String extrairTexto(String nome, String mime, byte[] bytes) throws Exception {
        boolean pdf = (mime != null && mime.contains("pdf")) || (nome != null && nome.toLowerCase().endsWith(".pdf"));
        if (pdf) {
            try (PDDocument documento = Loader.loadPDF(bytes)) {
                return new PDFTextStripper().getText(documento);
            }
        }
        // Demais formatos tratados como texto puro (.txt, .md). OCR/DOCX ficam fora do MVP (seção 14).
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private String normalizar(String texto) {
        if (texto == null) return "";
        return texto.replace("\r\n", "\n")
                .replaceAll("[ \\t]+\\n", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .strip();
    }

    private String sha256(String texto) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(texto.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
