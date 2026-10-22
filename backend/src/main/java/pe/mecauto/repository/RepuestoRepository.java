package pe.mecauto.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.mecauto.entity.Repuesto;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RepuestoRepository extends JpaRepository<Repuesto, Long>, JpaSpecificationExecutor<Repuesto> {

    @Override
    @EntityGraph(attributePaths = {"proveedor"})
    Page<Repuesto> findAll(Specification<Repuesto> spec, Pageable pageable);

    boolean existsByCodigo(String codigo);

    /** Bloquea la fila mientras dura la transacción: dos salidas simultáneas no pueden dejar el stock negativo. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Repuesto r where r.id = :id")
    Optional<Repuesto> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Repuesto r where r.id in :ids order by r.id")
    List<Repuesto> findAllByIdForUpdate(@Param("ids") Collection<Long> ids);

    @Query("select count(r) from Repuesto r where r.activo = true and r.stockActual <= r.stockMinimo")
    long contarStockBajo();

    @Query("select r from Repuesto r where r.activo = true and r.stockActual <= r.stockMinimo order by r.stockActual asc, r.nombre asc")
    List<Repuesto> listarStockBajo();
}
