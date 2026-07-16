package br.com.curriculos.dominio;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;

@Entity
@Table(name = "versao_curriculo")
@Getter
@Setter
public class VersaoCurriculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long curriculoId;
    private Integer numero;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> snapshot;

    private String nota;
    private OffsetDateTime criadoEm = OffsetDateTime.now();
}
