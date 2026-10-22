package pe.mecauto.service;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.mecauto.dto.CatalogoDtos.ProveedorDto;
import pe.mecauto.dto.CatalogoDtos.ProveedorRequest;
import pe.mecauto.entity.Proveedor;
import pe.mecauto.exception.ConflictoException;
import pe.mecauto.exception.RecursoNoEncontradoException;
import pe.mecauto.mapper.CatalogoMapper;
import pe.mecauto.repository.ProveedorRepository;
import pe.mecauto.util.TextoUtil;

import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class ProveedorService {

    private final ProveedorRepository repo;

    public ProveedorService(ProveedorRepository repo) {
        this.repo = repo;
    }

    public List<ProveedorDto> listar(String q, boolean soloActivos) {
        String t = "%" + TextoUtil.limpiarBusqueda(q).toLowerCase(Locale.ROOT) + "%";
        Specification<Proveedor> spec = (r, cq, cb) -> cb.or(
                cb.like(cb.lower(r.get("razonSocial")), t), cb.like(r.get("ruc"), t));
        if (soloActivos) {
            spec = spec.and((r, cq, cb) -> cb.isTrue(r.get("activo")));
        }
        return repo.findAll(spec, Sort.by("razonSocial")).stream().map(CatalogoMapper::aDto).toList();
    }

    @Transactional
    public ProveedorDto crear(ProveedorRequest req) {
        if (repo.existsByRuc(req.ruc())) {
            throw new ConflictoException("Ya existe un proveedor con ese RUC");
        }
        Proveedor p = new Proveedor();
        aplicar(p, req);
        return CatalogoMapper.aDto(repo.save(p));
    }

    @Transactional
    public ProveedorDto actualizar(Long id, ProveedorRequest req) {
        Proveedor p = buscar(id);
        if (!p.getRuc().equals(req.ruc()) && repo.existsByRuc(req.ruc())) {
            throw new ConflictoException("Ya existe un proveedor con ese RUC");
        }
        aplicar(p, req);
        return CatalogoMapper.aDto(p);
    }

    @Transactional
    public ProveedorDto cambiarEstado(Long id, boolean activo) {
        Proveedor p = buscar(id);
        p.setActivo(activo);
        return CatalogoMapper.aDto(p);
    }

    private void aplicar(Proveedor p, ProveedorRequest req) {
        p.setRazonSocial(req.razonSocial().trim());
        p.setRuc(req.ruc());
        p.setTelefono(TextoUtil.aNullSiVacio(req.telefono()));
    }

    private Proveedor buscar(Long id) {
        return repo.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Proveedor", id));
    }
}
