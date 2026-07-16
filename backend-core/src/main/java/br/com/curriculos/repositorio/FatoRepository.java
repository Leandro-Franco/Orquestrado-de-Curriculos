package br.com.curriculos.repositorio;

import br.com.curriculos.dominio.Fato;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FatoRepository extends JpaRepository<Fato, Long> {
    List<Fato> findByStatusOrderByTipoAsc(String status);
    List<Fato> findByTipoAndStatus(String tipo, String status);
}
