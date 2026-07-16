package br.com.curriculos.repositorio;

import br.com.curriculos.dominio.VersaoCurriculo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VersaoCurriculoRepository extends JpaRepository<VersaoCurriculo, Long> {
    List<VersaoCurriculo> findByCurriculoIdOrderByNumeroDesc(Long curriculoId);
}
