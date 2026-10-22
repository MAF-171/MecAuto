package pe.mecauto.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.mecauto.domain.EstadoComprobante;
import pe.mecauto.entity.Comprobante;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ComprobanteRepository extends JpaRepository<Comprobante, Long>, JpaSpecificationExecutor<Comprobante> {

    @Override
    @EntityGraph(attributePaths = {"orden"})
    Page<Comprobante> findAll(Specification<Comprobante> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"orden"})
    List<Comprobante> findAll(Specification<Comprobante> spec, Sort sort);

    Optional<Comprobante> findFirstByOrdenIdAndEstado(Long ordenId, EstadoComprobante estado);

    boolean existsByOrdenIdAndEstado(Long ordenId, EstadoComprobante estado);

    @Query("select coalesce(sum(c.total), 0) from Comprobante c "
            + "where c.estado = :estado and c.fechaEmision >= :desde and c.fechaEmision < :hasta")
    BigDecimal sumarTotal(@Param("estado") EstadoComprobante estado,
                          @Param("desde") LocalDateTime desde,
                          @Param("hasta") LocalDateTime hasta);

    List<Comprobante> findByEstadoAndFechaEmisionGreaterThanEqualAndFechaEmisionLessThanOrderByFechaEmisionAsc(
            EstadoComprobante estado, LocalDateTime desde, LocalDateTime hasta);
}
