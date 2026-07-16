package br.com.curriculos.repositorio;

import br.com.curriculos.dominio.Vaga;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VagaRepository extends JpaRepository<Vaga, Long> {
    List<Vaga> findAllByOrderByCriadoEmDesc();
}
