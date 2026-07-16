package br.com.curriculos.repositorio;

import br.com.curriculos.dominio.Documento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DocumentoRepository extends JpaRepository<Documento, Long> {
    Optional<Documento> findBySha256(String sha256);
}
