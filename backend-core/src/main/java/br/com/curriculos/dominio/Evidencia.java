package br.com.curriculos.dominio;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "evidencia")
@Getter
@Setter
public class Evidencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long fatoId;
    private Long documentoId;

    @Column(columnDefinition = "text")
    private String trecho;

    private OffsetDateTime criadoEm = OffsetDateTime.now();
}
