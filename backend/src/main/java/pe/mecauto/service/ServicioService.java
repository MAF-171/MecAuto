package pe.mecauto.service;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.mecauto.dto.CatalogoDtos.ServicioDto;
import pe.mecauto.dto.CatalogoDtos.ServicioPublicoDto;
import pe.mecauto.dto.CatalogoDtos.ServicioRequest;
import pe.mecauto.entity.Servicio;
import pe.mecauto.exception.ConflictoException;
import pe.mecauto.exception.RecursoNoEncontradoException;
import pe.mecauto.mapper.CatalogoMapper;
import pe.mecauto.repository.ServicioRepository;
import pe.mecauto.util.TextoUtil;

import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class ServicioService {

    private final ServicioRepository repo;

    public ServicioService(ServicioRepository repo) {
        this.repo = repo;
    }

    public List<ServicioDto> listar(String q, boolean soloActivos) {
        String t = "%" + TextoUtil.limpiarBusqueda(q).toLowerCase(Locale.ROOT) + "%";
        Specification<Servicio> spec = (r, cq, cb) -> cb.or(
                cb.like(cb.lower(r.get("nombre")), t),
                cb.like(cb.lower(cb.coalesce(r.<String>get("descripcion"), "")), t));
        if (soloActivos) {
            spec = spec.and((r, cq, cb) -> cb.isTrue(r.get("activo")));
        }
        return repo.findAll(spec, Sort.by("nombre")).stream().map(CatalogoMapper::aDto).toList();
    }

    public List<ServicioPublicoDto> listarPublico() {
        return repo.findByActivoTrueOrderByNombreAsc().stream().map(CatalogoMapper::aPublicoDto).toList();
    }

    public ServicioDto obtener(Long id) {
        return CatalogoMapper.aDto(buscar(id));
    }

    @Transactional
    public ServicioDto crear(ServicioRequest req) {
        if (repo.existsByNombreIgnoreCase(req.nombre().trim())) {
            throw new ConflictoException("Ya existe un servicio con ese nombre");
        }
        Servicio s = new Servicio();
        aplicar(s, req);
        return CatalogoMapper.aDto(repo.save(s));
    }

    @Transactional
    public ServicioDto actualizar(Long id, ServicioRequest req) {
        Servicio s = buscar(id);
        if (repo.existsByNombreIgnoreCaseAndIdNot(req.nombre().trim(), id)) {
            throw new ConflictoException("Ya existe otro servicio con ese nombre");
        }
        aplicar(s, req);
        return CatalogoMapper.aDto(s);
    }

    @Transactional
    public ServicioDto cambiarEstado(Long id, boolean activo) {
        Servicio s = buscar(id);
        s.setActivo(activo);
        return CatalogoMapper.aDto(s);
    }

    private void aplicar(Servicio s, ServicioRequest req) {
        s.setNombre(req.nombre().trim());
        s.setDescripcion(TextoUtil.aNullSiVacio(req.descripcion()));
        s.setPrecioReferencial(req.precioReferencial());
        s.setDuracionMinutos(req.duracionMinutos());
    }

    private Servicio buscar(Long id) {
        return repo.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Servicio", id));
    }
}
