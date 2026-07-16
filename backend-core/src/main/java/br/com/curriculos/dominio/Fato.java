package br.com.curriculos.dominio;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Fato profissional aprovado — a fonte oficial da verdade (ADR-002).
 * Tipos: EXPERIENCIA, FORMACAO, CURSO, CERTIFICACAO, PROJETO, HABILIDADE, IDIOMA, LINK.
 */
@Entity
@Table(name = "fato")
@Getter
@Setter
public class Fato {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String tipo;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> payload;

    private String status = "APROVADO";

    private OffsetDateTime criadoEm = OffsetDateTime.now();
    private OffsetDateTime atualizadoEm = OffsetDateTime.now();
}
