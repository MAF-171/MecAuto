package pe.mecauto.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import pe.mecauto.domain.Rol;
import pe.mecauto.dto.UsuarioDtos;
import pe.mecauto.entity.Usuario;
import pe.mecauto.repository.UsuarioRepository;
import pe.mecauto.service.ConfiguracionService;
import pe.mecauto.service.SecuenciaService;

import java.util.List;
import java.util.Locale;

/**
 * Se ejecuta al iniciar el backend:
 *  1) asegura los correlativos y ajustes por defecto (si seed.sql no se ejecutó, igual funciona);
 *  2) si NO existe ningún usuario, crea el administrador inicial con las variables de entorno
 *     MECAUTO_ADMIN_USER y MECAUTO_ADMIN_PASSWORD. No hay una cuenta ni contraseña por defecto.
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final AppProperties props;
    private final SecuenciaService secuencias;
    private final ConfiguracionService configuracion;

    public DataInitializer(UsuarioRepository usuarios, PasswordEncoder encoder, AppProperties props,
                           SecuenciaService secuencias, ConfiguracionService configuracion) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.props = props;
        this.secuencias = secuencias;
        this.configuracion = configuracion;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (String s : List.of("OT", "COT", "CITA", "B001", "F001")) {
            secuencias.asegurar(s);
        }
        configuracion.asegurarValoresPorDefecto();

        if (usuarios.count() > 0) {
            return;
        }
        AppProperties.AdminInicial a = props.adminInicial();
        String user = a.username() == null ? "" : a.username().trim().toLowerCase(Locale.ROOT);
        String pass = a.password() == null ? "" : a.password();
        if (user.isEmpty() || pass.isEmpty()) {
            log.warn("No hay usuarios en la base de datos. Define MECAUTO_ADMIN_USER y MECAUTO_ADMIN_PASSWORD y reinicia para crear el administrador inicial.");
            return;
        }
        if (!pass.matches(UsuarioDtos.REGEX_PASSWORD)) {
            log.error("MECAUTO_ADMIN_PASSWORD no cumple la política ({}). No se creó el administrador.", UsuarioDtos.MSG_PASSWORD);
            return;
        }
        Usuario u = new Usuario();
        u.setUsername(user);
        u.setNombre(a.nombre() == null || a.nombre().isBlank() ? "Administrador" : a.nombre());
        u.setRol(Rol.ADMINISTRADOR);
        u.setPasswordHash(encoder.encode(pass));
        u.setActivo(true);
        usuarios.save(u);
        log.info("Administrador inicial '{}' creado. Puedes quitar MECAUTO_ADMIN_PASSWORD del entorno.", user);
    }
}
