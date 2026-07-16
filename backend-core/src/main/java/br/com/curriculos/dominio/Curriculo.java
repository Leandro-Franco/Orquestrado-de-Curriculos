package br.com.curriculos.dominio;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;

@Entity
@Table(name = "curriculo")
@Getter
@Setter
public class Curriculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long vagaId;
    private String titulo;
    private String template = "classico";

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> estrategia;

    private String status = "RASCUNHO";
    private OffsetDateTime criadoEm = OffsetDateTime.now();
    private OffsetDateTime atualizadoEm = OffsetDateTime.now();
}
