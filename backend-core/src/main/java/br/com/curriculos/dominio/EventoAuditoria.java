package br.com.curriculos.dominio;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;

@Entity
@Table(name = "evento_auditoria")
@Getter
@Setter
public class EventoAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String entidade;
    private Long entidadeId;
    private String acao;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> detalhe;

    private OffsetDateTime criadoEm = OffsetDateTime.now();

    public static EventoAuditoria de(String entidade, Long entidadeId, String acao, Map<String, Object> detalhe) {
        EventoAuditoria e = new EventoAuditoria();
        e.entidade = entidade;
        e.entidadeId = entidadeId;
        e.acao = acao;
        e.detalhe = detalhe;
        return e;
    }
}
