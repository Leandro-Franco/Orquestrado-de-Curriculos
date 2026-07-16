package br.com.curriculos.dominio;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Entity
@Table(name = "requisito_vaga")
@Getter
@Setter
public class RequisitoVaga {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long vagaId;

    @Column(columnDefinition = "text")
    private String descricao;

    private String tipo; // OBRIGATORIO | DESEJAVEL
    private String categoria;
    private String compatibilidade; // ALTA | MEDIA | PARCIAL | AUSENTE | INCONCLUSIVA

    @JdbcTypeCode(SqlTypes.JSON)
    private List<Long> fatosRelacionados;

    @Column(columnDefinition = "text")
    private String justificativa;
}
