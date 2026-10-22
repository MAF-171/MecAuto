package pe.mecauto.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import pe.mecauto.entity.Vehiculo;

import java.util.List;
import java.util.Optional;

public interface VehiculoRepository extends JpaRepository<Vehiculo, Long>, JpaSpecificationExecutor<Vehiculo> {

    @Override
    @EntityGraph(attributePaths = {"cliente"})
    Page<Vehiculo> findAll(Specification<Vehiculo> spec, Pageable pageable);

    Optional<Vehiculo> findByPlaca(String placa);

    boolean existsByPlaca(String placa);

    long countByActivoTrue();

    long countByClienteIdAndActivoTrue(Long clienteId);

    @EntityGraph(attributePaths = {"cliente"})
    List<Vehiculo> findByClienteIdOrderByPlacaAsc(Long clienteId);
}
