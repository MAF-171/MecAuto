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
import pe.mecauto.domain.EstadoCita;
import pe.mecauto.entity.Cita;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface CitaRepository extends JpaRepository<Cita, Long>, JpaSpecificationExecutor<Cita> {

    @Override
    @EntityGraph(attributePaths = {"servicio", "cliente", "vehiculo"})
    Page<Cita> findAll(Specification<Cita> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"servicio", "cliente", "vehiculo"})
    List<Cita> findAll(Specification<Cita> spec, Sort sort);

    /** Cantidad de citas abiertas dentro de [desde, hasta). */
    long countByFechaHoraGreaterThanEqualAndFechaHoraLessThanAndEstadoIn(
            LocalDateTime desde, LocalDateTime hasta, Collection<EstadoCita> estados);

    boolean existsByPlacaAndFechaHoraGreaterThanEqualAndFechaHoraLessThanAndEstadoIn(
            String placa, LocalDateTime desde, LocalDateTime hasta, Collection<EstadoCita> estados);

    /** Ocupación por franja de un día: pares (inicio de franja, cantidad de citas abiertas). */
    @Query("select c.fechaHora, count(c) from Cita c "
            + "where c.fechaHora >= :desde and c.fechaHora < :hasta and c.estado in :estados "
            + "group by c.fechaHora")
    List<Object[]> ocupacionPorFranja(@Param("desde") LocalDateTime desde,
                                      @Param("hasta") LocalDateTime hasta,
                                      @Param("estados") Collection<EstadoCita> estados);

    long countByFechaHoraGreaterThanEqualAndFechaHoraLessThanAndEstadoNot(
            LocalDateTime desde, LocalDateTime hasta, EstadoCita estado);
}
