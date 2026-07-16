package br.com.curriculos.dominio;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Proposta de atualização da base de conhecimento gerada pela LLM.
 * Só vira fato após aprovação humana (Contexto Mestre, seção 4).
 */
@Entity
@Table(name = "proposta")
@Getter
@Setter
public class Proposta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String acao; // CRIAR | ATUALIZAR
    private String tipoFato;
    private Long fatoAlvoId;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> payloadAnterior;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> payloadProposto;

    private Long documentoOrigemId;

    @Column(columnDefinition = "text")
    private String justificativa;

    @Column(columnDefinition = "text")
    private String trechoEvidencia;

    private BigDecimal confianca;
    private String status = "PENDENTE"; // PENDENTE | APROVADA | REJEITADA
    private String modelo;
    private String versaoPrompt;
    private OffsetDateTime decididoEm;
    private OffsetDateTime criadoEm = OffsetDateTime.now();
}
