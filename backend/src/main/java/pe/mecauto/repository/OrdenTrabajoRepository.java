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
import pe.mecauto.domain.EstadoOrden;
import pe.mecauto.entity.OrdenTrabajo;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrdenTrabajoRepository extends JpaRepository<OrdenTrabajo, Long>, JpaSpecificationExecutor<OrdenTrabajo> {

    @Override
    @EntityGraph(attributePaths = {"cliente", "vehiculo", "mecanico"})
    Page<OrdenTrabajo> findAll(Specification<OrdenTrabajo> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"cliente", "vehiculo", "mecanico"})
    List<OrdenTrabajo> findAll(Specification<OrdenTrabajo> spec, Sort sort);

    /** Bloquea la orden: dos cambios de estado simultáneos se ejecutan uno después del otro. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from OrdenTrabajo o where o.id = :id")
    Optional<OrdenTrabajo> findByIdForUpdate(@Param("id") Long id);

    /** Búsqueda por código para la consulta pública (trae el vehículo para comparar la placa). */
    @EntityGraph(attributePaths = {"vehiculo"})
    Optional<OrdenTrabajo> findByCodigoIgnoreCase(String codigo);

    boolean existsByVehiculoIdAndEstadoIn(Long vehiculoId, Collection<EstadoOrden> estados);

    boolean existsByClienteIdAndEstadoIn(Long clienteId, Collection<EstadoOrden> estados);

    boolean existsByCitaId(Long citaId);

    @EntityGraph(attributePaths = {"cliente", "vehiculo", "mecanico"})
    List<OrdenTrabajo> findByVehiculoIdOrderByFechaIngresoDesc(Long vehiculoId);

    @Query("select o.estado, count(o) from OrdenTrabajo o group by o.estado")
    List<Object[]> contarPorEstado();

    @Query("select o.estado, count(o) from OrdenTrabajo o where o.mecanico.id = :mecanicoId group by o.estado")
    List<Object[]> contarPorEstadoDeMecanico(@Param("mecanicoId") Long mecanicoId);

    @Query("select count(distinct o.vehiculo.id) from OrdenTrabajo o where o.estado in :estados")
    long contarVehiculosEnEstados(@Param("estados") Collection<EstadoOrden> estados);

    @EntityGraph(attributePaths = {"cliente", "vehiculo", "mecanico"})
    List<OrdenTrabajo> findTop8ByOrderByFechaIngresoDesc();

    @EntityGraph(attributePaths = {"cliente", "vehiculo", "mecanico"})
    List<OrdenTrabajo> findTop8ByMecanicoIdOrderByFechaIngresoDesc(Long mecanicoId);
}
