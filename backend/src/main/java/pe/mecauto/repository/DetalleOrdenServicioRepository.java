package pe.mecauto.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.mecauto.domain.EstadoOrden;
import pe.mecauto.entity.DetalleOrdenServicio;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DetalleOrdenServicioRepository extends JpaRepository<DetalleOrdenServicio, Long> {

    /** Primer servicio registrado en la orden (para mostrar en la consulta pública). */
    Optional<DetalleOrdenServicio> findFirstByOrdenIdOrderByIdAsc(Long ordenId);

    /** Servicios más solicitados en un periodo: pares (nombre del servicio, cantidad total). */
    @Query("select s.nombre, sum(d.cantidad) from DetalleOrdenServicio d "
            + "join d.orden o join d.servicio s "
            + "where o.estado in :estados and o.fechaIngreso >= :desde and o.fechaIngreso < :hasta "
            + "group by s.nombre order by sum(d.cantidad) desc")
    List<Object[]> serviciosMasSolicitados(@Param("estados") Collection<EstadoOrden> estados,
                                           @Param("desde") LocalDateTime desde,
                                           @Param("hasta") LocalDateTime hasta);
}
