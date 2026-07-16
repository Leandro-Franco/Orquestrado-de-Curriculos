package br.com.curriculos.repositorio;

import br.com.curriculos.dominio.Evidencia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EvidenciaRepository extends JpaRepository<Evidencia, Long> {
    List<Evidencia> findByFatoId(Long fatoId);
}
