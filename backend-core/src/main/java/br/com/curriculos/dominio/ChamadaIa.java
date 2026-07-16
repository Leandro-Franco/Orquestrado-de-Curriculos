package br.com.curriculos.dominio;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "chamada_ia")
@Getter
@Setter
public class ChamadaIa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String operacao;
    private String modelo;
    private Integer tokensEntrada = 0;
    private Integer tokensSaida = 0;
    private Integer tokensCache = 0;
    private Integer trechosRecuperados = 0;
    private Integer duracaoMs = 0;
    private BigDecimal custoEstimado = BigDecimal.ZERO;
    private Integer tentativas = 1;
    private Boolean valida = true;
    private OffsetDateTime criadoEm = OffsetDateTime.now();
}
