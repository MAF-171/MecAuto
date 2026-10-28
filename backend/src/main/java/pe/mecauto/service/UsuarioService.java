package pe.mecauto.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.mecauto.domain.Rol;
import pe.mecauto.dto.PageDto;
import pe.mecauto.dto.UsuarioDtos.*;
import pe.mecauto.entity.Usuario;
import pe.mecauto.exception.ConflictoException;
import pe.mecauto.exception.NegocioException;
import pe.mecauto.exception.RecursoNoEncontradoException;
import pe.mecauto.mapper.PersonaMapper;
import pe.mecauto.repository.UsuarioRepository;
import pe.mecauto.security.UsuarioPrincipal;
import pe.mecauto.exception.ValidacionException;
import pe.mecauto.util.TelefonoUtil;
import pe.mecauto.util.TextoUtil;

import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class UsuarioService {

    private final UsuarioRepository repo;
    private final PasswordEncoder encoder;

    public UsuarioService(UsuarioRepository repo, PasswordEncoder encoder) {
        this.repo = repo;
        this.encoder = encoder;
    }

    public PageDto<UsuarioDto> listar(String q, int pagina, int tamano) {
        String texto = "%" + TextoUtil.limpiarBusqueda(q).toLowerCase(Locale.ROOT) + "%";
        Specification<Usuario> spec = (r, cq, cb) -> cb.or(
                cb.like(cb.lower(r.get("nombre")), texto),
                cb.like(cb.lower(r.get("username")), texto));
        Page<Usuario> page = repo.findAll(spec, PageRequest.of(pagina, Math.min(tamano, 100), Sort.by("nombre")));
        return PageDto.de(page, PersonaMapper::aDto);
    }

    public List<UsuarioDto> mecanicosActivos() {
        return repo.findByRolAndActivoTrueOrderByNombreAsc(Rol.MECANICO).stream().map(PersonaMapper::aDto).toList();
    }

    public UsuarioDto obtener(Long id) {
        return PersonaMapper.aDto(buscar(id));
    }

    @Transactional
    public UsuarioDto crear(UsuarioCrearRequest req) {
        String username = req.username().trim().toLowerCase(Locale.ROOT);
        if (repo.existsByUsername(username)) {
            throw new ConflictoException("Ya existe un usuario con ese nombre de usuario");
        }
        Usuario u = new Usuario();
        u.setUsername(username);
        u.setNombre(req.nombre().trim());
        u.setRol(req.rol());
        u.setPasswordHash(encoder.encode(req.password()));
        u.setActivo(true);
        asignarContacto(u, req.correo(), req.telefono());
        return PersonaMapper.aDto(repo.save(u));
    }

    @Transactional
    public UsuarioDto actualizar(Long id, UsuarioActualizarRequest req, UsuarioPrincipal actor) {
        Usuario u = buscar(id);
        if (u.getRol() == Rol.ADMINISTRADOR && req.rol() != Rol.ADMINISTRADOR) {
            exigirOtroAdministradorActivo(u);
        }
        u.setNombre(req.nombre().trim());
        u.setRol(req.rol());
        asignarContacto(u, req.correo(), req.telefono());
        return PersonaMapper.aDto(u);
    }

    @Transactional
    public UsuarioDto cambiarEstado(Long id, boolean activo, UsuarioPrincipal actor) {
        Usuario u = buscar(id);
        if (!activo) {
            if (u.getId().equals(actor.getId())) {
                throw new NegocioException("No puedes desactivar tu propia cuenta");
            }
            if (u.getRol() == Rol.ADMINISTRADOR) {
                exigirOtroAdministradorActivo(u);
            }
        }
        u.setActivo(activo);
        return PersonaMapper.aDto(u);
    }

    @Transactional
    public void cambiarPassword(Long id, String nuevaPassword) {
        buscar(id).setPasswordHash(encoder.encode(nuevaPassword));
    }

    /** Correo y celular son opcionales, pero no pueden repetirse: con ellos se recupera la contraseña. */
    private void asignarContacto(Usuario u, String correoTexto, String telefonoTexto) {
        String correo = TextoUtil.aNullSiVacio(correoTexto);
        if (correo != null) {
            correo = correo.toLowerCase(Locale.ROOT);
            if (repo.findByCorreo(correo).filter(otro -> !otro.getId().equals(u.getId())).isPresent()) {
                throw new ConflictoException("Ese correo ya está registrado en otro usuario");
            }
        }
        String telefono = null;
        if (TextoUtil.aNullSiVacio(telefonoTexto) != null) {
            telefono = TelefonoUtil.normalizar(telefonoTexto);
            if (telefono == null) {
                throw new ValidacionException("telefono", "Escribe un número de celular válido");
            }
            if (repo.findByTelefono(telefono).filter(otro -> !otro.getId().equals(u.getId())).isPresent()) {
                throw new ConflictoException("Ese celular ya está registrado en otro usuario");
            }
        }
        u.setCorreo(correo);
        u.setTelefono(telefono);
    }

    private void exigirOtroAdministradorActivo(Usuario u) {
        if (u.isActivo() && repo.countByRolAndActivoTrue(Rol.ADMINISTRADOR) <= 1) {
            throw new NegocioException("Debe quedar al menos un administrador activo");
        }
    }

    private Usuario buscar(Long id) {
        return repo.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Usuario", id));
    }
}
