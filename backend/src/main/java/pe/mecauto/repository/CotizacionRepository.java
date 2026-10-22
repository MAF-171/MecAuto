package pe.mecauto.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.mecauto.entity.Cotizacion;

import java.util.List;
import java.util.Optional;

public interface CotizacionRepository extends JpaRepository<Cotizacion, Long>, JpaSpecificationExecutor<Cotizacion> {

    @Override
    @EntityGraph(attributePaths = {"cliente", "vehiculo"})
    Page<Cotizacion> findAll(Specification<Cotizacion> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"cliente", "vehiculo"})
    List<Cotizacion> findAll(Specification<Cotizacion> spec, Sort sort);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Cotizacion c where c.id = :id")
    Optional<Cotizacion> findByIdForUpdate(@Param("id") Long id);
}
