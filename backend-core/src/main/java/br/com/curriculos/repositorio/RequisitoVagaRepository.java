package br.com.curriculos.repositorio;

import br.com.curriculos.dominio.RequisitoVaga;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RequisitoVagaRepository extends JpaRepository<RequisitoVaga, Long> {
    List<RequisitoVaga> findByVagaIdOrderByIdAsc(Long vagaId);
    void deleteByVagaId(Long vagaId);
}
