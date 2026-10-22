package pe.mecauto.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import pe.mecauto.domain.Rol;
import pe.mecauto.entity.Usuario;

import java.util.Collection;
import java.util.List;

/** Usuario autenticado tal como lo ve Spring Security. Incluye id y rol para usarlos en los servicios. */
public class UsuarioPrincipal implements UserDetails {

    private final Long id;
    private final String username;
    private final String passwordHash;
    private final String nombre;
    private final Rol rol;
    private final boolean activo;

    public UsuarioPrincipal(Usuario u) {
        this.id = u.getId();
        this.username = u.getUsername();
        this.passwordHash = u.getPasswordHash();
        this.nombre = u.getNombre();
        this.rol = u.getRol();
        this.activo = u.isActivo();
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public Rol getRol() {
        return rol;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + rol.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isEnabled() {
        return activo;
    }
}
