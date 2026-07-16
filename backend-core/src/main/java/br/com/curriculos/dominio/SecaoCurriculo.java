package br.com.curriculos.dominio;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.List;

@Entity
@Table(name = "secao_curriculo")
@Getter
@Setter
public class SecaoCurriculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long curriculoId;
    private String tipo; // TITULO | RESUMO | EXPERIENCIAS | PROJETOS | COMPETENCIAS | FORMACAO | CURSOS | IDIOMAS
    private Integer ordem = 0;
    private String titulo;

    @Column(columnDefinition = "text")
    private String conteudo;

    @JdbcTypeCode(SqlTypes.JSON)
    private List<Long> fatosUtilizados;

    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> alertasValidacao;

    private OffsetDateTime atualizadoEm = OffsetDateTime.now();
}
