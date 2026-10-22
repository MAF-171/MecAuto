package pe.mecauto.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import pe.mecauto.entity.Cliente;

import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long>, JpaSpecificationExecutor<Cliente> {

    Optional<Cliente> findByNroDocumento(String nroDocumento);

    boolean existsByNroDocumento(String nroDocumento);

    long countByActivoTrue();
}
