package br.com.curriculos.repositorio;

import br.com.curriculos.dominio.Curriculo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CurriculoRepository extends JpaRepository<Curriculo, Long> {
    List<Curriculo> findAllByOrderByCriadoEmDesc();
}
