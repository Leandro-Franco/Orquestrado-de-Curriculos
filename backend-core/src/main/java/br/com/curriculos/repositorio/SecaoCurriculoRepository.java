package br.com.curriculos.repositorio;

import br.com.curriculos.dominio.SecaoCurriculo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SecaoCurriculoRepository extends JpaRepository<SecaoCurriculo, Long> {
    List<SecaoCurriculo> findByCurriculoIdOrderByOrdemAsc(Long curriculoId);
}
