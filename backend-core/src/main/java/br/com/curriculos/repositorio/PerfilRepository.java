package br.com.curriculos.repositorio;

import br.com.curriculos.dominio.Perfil;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PerfilRepository extends JpaRepository<Perfil, Short> {
}
