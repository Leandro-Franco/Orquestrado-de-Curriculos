package br.com.curriculos.repositorio;

import br.com.curriculos.dominio.Proposta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PropostaRepository extends JpaRepository<Proposta, Long> {
    List<Proposta> findByStatusOrderByCriadoEmDesc(String status);
    List<Proposta> findAllByOrderByCriadoEmDesc();
}
