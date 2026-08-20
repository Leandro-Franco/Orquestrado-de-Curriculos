package br.com.curriculos.dominio;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "configuracao_ia")
@Getter
@Setter
public class ConfiguracaoIa {

    @Id
    private Short id;

    private String provider = "fake"; // fake | anthropic | openai | grok | ollama | personalizado

    @Column(columnDefinition = "text")
    private String apiKey;

    private String baseUrl;
    private String modeloEconomico;
    private String modeloIntermediario;
    private String modeloAvancado;
    private OffsetDateTime atualizadoEm = OffsetDateTime.now();
}
