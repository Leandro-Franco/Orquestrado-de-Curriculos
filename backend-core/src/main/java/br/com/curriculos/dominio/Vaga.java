package br.com.curriculos.dominio;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;

@Entity
@Table(name = "vaga")
@Getter
@Setter
public class Vaga {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String titulo;
    private String empresa;

    @Column(columnDefinition = "text")
    private String descricaoBruta;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> analise;

    private String status = "CADASTRADA"; // CADASTRADA | ANALISADA
    private OffsetDateTime criadoEm = OffsetDateTime.now();
}
