package pe.mecauto.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import pe.mecauto.entity.MovimientoInventario;

import java.util.Optional;

public interface MovimientoInventarioRepository
        extends JpaRepository<MovimientoInventario, Long>, JpaSpecificationExecutor<MovimientoInventario> {

    @Override
    @EntityGraph(attributePaths = {"repuesto", "usuario"})
    Page<MovimientoInventario> findAll(Specification<MovimientoInventario> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"repuesto", "usuario"})
    java.util.List<MovimientoInventario> findAll(Specification<MovimientoInventario> spec, org.springframework.data.domain.Sort sort);

    Optional<MovimientoInventario> findByClaveIdempotencia(String clave);

    boolean existsByClaveIdempotencia(String clave);
}
