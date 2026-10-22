package pe.mecauto.mapper;

import pe.mecauto.dto.PersonaDtos.ClienteDto;
import pe.mecauto.dto.PersonaDtos.VehiculoDto;
import pe.mecauto.dto.UsuarioDtos.UsuarioDto;
import pe.mecauto.entity.Cliente;
import pe.mecauto.entity.Usuario;
import pe.mecauto.entity.Vehiculo;

/** Mapper: convierte entidades en DTO para no exponer las entidades (ni el hash de contraseña) por la API. */
public final class PersonaMapper {

    private PersonaMapper() {
    }

    public static UsuarioDto aDto(Usuario u) {
        return new UsuarioDto(u.getId(), u.getUsername(), u.getNombre(), u.getRol(), u.isActivo(), u.getCreadoEn());
    }

    public static ClienteDto aDto(Cliente c, Long cantidadVehiculos) {
        return new ClienteDto(c.getId(), c.getTipoDocumento(), c.getNroDocumento(), c.getNombre(), c.getTelefono(),
                c.getCorreo(), c.getDireccion(), c.isActivo(), c.getCreadoEn(), cantidadVehiculos);
    }

    public static ClienteDto aDto(Cliente c) {
        return aDto(c, null);
    }

    public static VehiculoDto aDto(Vehiculo v) {
        return new VehiculoDto(v.getId(), v.getCliente().getId(), v.getCliente().getNombre(), v.getPlaca(),
                v.getMarca(), v.getModelo(), v.getAnio(), v.getColor(), v.getKilometraje(), v.isActivo());
    }
}
