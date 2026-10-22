package pe.mecauto.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.mecauto.domain.EstadoOrden;
import pe.mecauto.entity.DetalleOrdenRepuesto;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface DetalleOrdenRepuestoRepository extends JpaRepository<DetalleOrdenRepuesto, Long> {

    /** Repuestos usados en órdenes de un periodo: (código, nombre, cantidad total). */
    @Query("select r.codigo, r.nombre, sum(d.cantidad) from DetalleOrdenRepuesto d "
            + "join d.orden o join d.repuesto r "
            + "where o.estado in :estados and o.fechaIngreso >= :desde and o.fechaIngreso < :hasta "
            + "group by r.codigo, r.nombre order by sum(d.cantidad) desc")
    List<Object[]> repuestosMasUsados(@Param("estados") Collection<EstadoOrden> estados,
                                      @Param("desde") LocalDateTime desde,
                                      @Param("hasta") LocalDateTime hasta);
}
