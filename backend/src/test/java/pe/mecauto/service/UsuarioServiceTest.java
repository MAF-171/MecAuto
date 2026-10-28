package pe.mecauto.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import pe.mecauto.domain.Rol;
import pe.mecauto.dto.UsuarioDtos.UsuarioActualizarRequest;
import pe.mecauto.dto.UsuarioDtos.UsuarioCrearRequest;
import pe.mecauto.dto.UsuarioDtos.UsuarioDto;
import pe.mecauto.entity.Usuario;
import pe.mecauto.exception.ConflictoException;
import pe.mecauto.exception.NegocioException;
import pe.mecauto.exception.ValidacionException;
import pe.mecauto.repository.UsuarioRepository;
import pe.mecauto.security.UsuarioPrincipal;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Reglas de usuarios del personal con repositorio simulado (no requiere SQL Server). */
@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock UsuarioRepository repo;
    @Mock PasswordEncoder encoder;

    UsuarioService service;
    Usuario admin;
    UsuarioPrincipal actorAdmin;

    @BeforeEach
    void preparar() {
        service = new UsuarioService(repo, encoder);
        admin = usuario(1L, "admin", Rol.ADMINISTRADOR);
        actorAdmin = new UsuarioPrincipal(admin);
    }

    private static Usuario usuario(long id, String username, Rol rol) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setUsername(username);
        u.setNombre(username);
        u.setRol(rol);
        u.setPasswordHash("hash");
        return u;
    }

    @Test
    @DisplayName("Crear guarda el usuario en minúsculas, con hash y contacto normalizado")
    void crear() {
        when(repo.existsByUsername("jperez")).thenReturn(false);
        when(repo.findByCorreo("juan@taller.pe")).thenReturn(Optional.empty());
        when(repo.findByTelefono("+51987654321")).thenReturn(Optional.empty());
        when(encoder.encode("Clave123456")).thenReturn("hash-bcrypt");
        when(repo.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        UsuarioDto u = service.crear(new UsuarioCrearRequest(" JPerez ", " Juan Pérez ", Rol.MECANICO, "Clave123456",
                "Juan@Taller.pe", "987 654 321"));

        assertEquals("jperez", u.username());
        assertEquals("Juan Pérez", u.nombre());
        assertEquals("juan@taller.pe", u.correo());
        assertEquals("+51987654321", u.telefono());
        assertTrue(u.activo());
    }

    @Test
    @DisplayName("No se repite el nombre de usuario")
    void usernameRepetido() {
        when(repo.existsByUsername("admin")).thenReturn(true);
        assertThrows(ConflictoException.class, () -> service.crear(
                new UsuarioCrearRequest("admin", "Otro", Rol.RECEPCION, "Clave123456", null, null)));
    }

    @Test
    @DisplayName("Correo o celular de otro usuario dan conflicto; un celular inválido se rechaza")
    void contactoRepetidoOInvalido() {
        Usuario otro = usuario(2L, "otro", Rol.RECEPCION);
        when(repo.findById(1L)).thenReturn(Optional.of(admin));
        when(repo.findByCorreo("otro@taller.pe")).thenReturn(Optional.of(otro));
        when(repo.findByTelefono("+51911111111")).thenReturn(Optional.of(otro));

        assertThrows(ConflictoException.class, () -> service.actualizar(1L,
                new UsuarioActualizarRequest("Admin", Rol.ADMINISTRADOR, "otro@taller.pe", null), actorAdmin));
        assertThrows(ConflictoException.class, () -> service.actualizar(1L,
                new UsuarioActualizarRequest("Admin", Rol.ADMINISTRADOR, null, "911111111"), actorAdmin));
        assertThrows(ValidacionException.class, () -> service.actualizar(1L,
                new UsuarioActualizarRequest("Admin", Rol.ADMINISTRADOR, null, "12"), actorAdmin));
    }

    @Test
    @DisplayName("Conservar su propio correo al editar no es conflicto")
    void mismoCorreoPropio() {
        admin.setCorreo("admin@taller.pe");
        when(repo.findById(1L)).thenReturn(Optional.of(admin));
        when(repo.findByCorreo("admin@taller.pe")).thenReturn(Optional.of(admin));

        UsuarioDto u = service.actualizar(1L,
                new UsuarioActualizarRequest("Admin", Rol.ADMINISTRADOR, "admin@taller.pe", ""), actorAdmin);

        assertEquals("admin@taller.pe", u.correo());
        assertNull(u.telefono());
    }

    @Test
    @DisplayName("Debe quedar al menos un administrador activo")
    void ultimoAdministrador() {
        when(repo.findById(1L)).thenReturn(Optional.of(admin));
        when(repo.countByRolAndActivoTrue(Rol.ADMINISTRADOR)).thenReturn(1L);

        assertThrows(NegocioException.class, () -> service.actualizar(1L,
                new UsuarioActualizarRequest("Admin", Rol.RECEPCION, null, null), actorAdmin));
        assertEquals(Rol.ADMINISTRADOR, admin.getRol());
    }

    @Test
    @DisplayName("Nadie puede desactivar su propia cuenta")
    void noSeDesactivaASiMismo() {
        when(repo.findById(1L)).thenReturn(Optional.of(admin));
        assertThrows(NegocioException.class, () -> service.cambiarEstado(1L, false, actorAdmin));
    }

    @Test
    @DisplayName("Se desactiva a otro administrador si queda al menos uno")
    void desactivaOtroAdministrador() {
        Usuario segundo = usuario(2L, "admin2", Rol.ADMINISTRADOR);
        when(repo.findById(2L)).thenReturn(Optional.of(segundo));
        when(repo.countByRolAndActivoTrue(Rol.ADMINISTRADOR)).thenReturn(2L);

        assertFalse(service.cambiarEstado(2L, false, actorAdmin).activo());
    }

    @Test
    @DisplayName("Cambiar contraseña guarda solo el hash")
    void cambiarPassword() {
        when(repo.findById(1L)).thenReturn(Optional.of(admin));
        when(encoder.encode("NuevaClave2026")).thenReturn("hash-nuevo");

        service.cambiarPassword(1L, "NuevaClave2026");

        assertEquals("hash-nuevo", admin.getPasswordHash());
    }
}
