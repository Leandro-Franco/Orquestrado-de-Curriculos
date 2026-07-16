package br.com.curriculos.repositorio;

import br.com.curriculos.dominio.EventoAuditoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventoAuditoriaRepository extends JpaRepository<EventoAuditoria, Long> {
    List<EventoAuditoria> findTop100ByOrderByCriadoEmDesc();
}
