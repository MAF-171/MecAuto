package pe.mecauto.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import pe.mecauto.domain.Rol;
import pe.mecauto.entity.Usuario;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long>, JpaSpecificationExecutor<Usuario> {

    Optional<Usuario> findByUsername(String username);

    boolean existsByUsername(String username);

    Optional<Usuario> findByCorreo(String correo);

    Optional<Usuario> findByTelefono(String telefono);

    List<Usuario> findByRolAndActivoTrueOrderByNombreAsc(Rol rol);

    long countByRolAndActivoTrue(Rol rol);
}
