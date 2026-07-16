package br.com.curriculos.dominio;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "documento")
@Getter
@Setter
public class Documento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String titulo;
    private String nomeArquivo;
    private String mimeType;
    private String caminhoArmazenamento;
    private String sha256;
    private String origem; // TEXTO | ARQUIVO

    @Column(columnDefinition = "text")
    private String textoExtraido;

    private String status = "IMPORTADO";
    private OffsetDateTime criadoEm = OffsetDateTime.now();
}
