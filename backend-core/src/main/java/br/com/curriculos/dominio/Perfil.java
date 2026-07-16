package br.com.curriculos.dominio;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;

@Entity
@Table(name = "perfil")
@Getter
@Setter
public class Perfil {

    @Id
    private Short id;

    private String nomeCompleto;
    private String email;
    private String telefone;
    private String localizacao;
    private String tituloProfissional;
    private String objetivo;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> preferencias;

    private OffsetDateTime atualizadoEm;
}
