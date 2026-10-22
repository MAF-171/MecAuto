package pe.mecauto.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.mecauto.entity.OrdenHistorial;

import java.util.List;

public interface OrdenHistorialRepository extends JpaRepository<OrdenHistorial, Long> {

    @EntityGraph(attributePaths = {"usuario"})
    List<OrdenHistorial> findByOrdenIdOrderByFechaAscIdAsc(Long ordenId);
}
