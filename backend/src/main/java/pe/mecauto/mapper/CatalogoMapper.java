package pe.mecauto.mapper;

import pe.mecauto.dto.CatalogoDtos.MovimientoDto;
import pe.mecauto.dto.CatalogoDtos.ProveedorDto;
import pe.mecauto.dto.CatalogoDtos.RepuestoDto;
import pe.mecauto.dto.CatalogoDtos.ServicioDto;
import pe.mecauto.dto.CatalogoDtos.ServicioPublicoDto;
import pe.mecauto.entity.MovimientoInventario;
import pe.mecauto.entity.Proveedor;
import pe.mecauto.entity.Repuesto;
import pe.mecauto.entity.Servicio;

public final class CatalogoMapper {

    private CatalogoMapper() {
    }

    public static ServicioDto aDto(Servicio s) {
        return new ServicioDto(s.getId(), s.getNombre(), s.getDescripcion(), s.getPrecioReferencial(),
                s.getDuracionMinutos(), s.isActivo());
    }

    public static ServicioPublicoDto aPublicoDto(Servicio s) {
        return new ServicioPublicoDto(s.getId(), s.getNombre(), s.getDescripcion(), s.getPrecioReferencial(),
                s.getDuracionMinutos());
    }

    public static ProveedorDto aDto(Proveedor p) {
        return new ProveedorDto(p.getId(), p.getRazonSocial(), p.getRuc(), p.getTelefono(), p.isActivo());
    }

    public static RepuestoDto aDto(Repuesto r) {
        Proveedor p = r.getProveedor();
        return new RepuestoDto(r.getId(), p == null ? null : p.getId(), p == null ? null : p.getRazonSocial(),
                r.getCodigo(), r.getNombre(), r.getCategoria(), r.getPrecioCompra(), r.getPrecioVenta(),
                r.getStockActual(), r.getStockMinimo(), r.estaBajoStock(), r.isActivo());
    }

    public static MovimientoDto aDto(MovimientoInventario m) {
        return new MovimientoDto(m.getId(), m.getRepuesto().getId(), m.getRepuesto().getCodigo(),
                m.getRepuesto().getNombre(), m.getTipo(), m.getCantidad(), m.getStockResultante(), m.getMotivo(),
                m.getOrdenId(), m.getUsuario() == null ? null : m.getUsuario().getNombre(), m.getFecha());
    }
}
