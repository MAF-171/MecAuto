package pe.mecauto.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.mecauto.entity.Secuencia;

import java.util.Optional;

public interface SecuenciaRepository extends JpaRepository<Secuencia, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Secuencia s where s.nombre = :nombre")
    Optional<Secuencia> findByNombreForUpdate(@Param("nombre") String nombre);
}
