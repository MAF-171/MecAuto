package pe.mecauto.mapper;

import pe.mecauto.domain.CalculadoraIgv;
import pe.mecauto.domain.EstadoCotizacion;
import pe.mecauto.dto.CitaDtos.CitaDto;
import pe.mecauto.dto.ComprobanteDtos.ComprobanteDto;
import pe.mecauto.dto.ComprobanteDtos.LineaComprobanteDto;
import pe.mecauto.dto.CotizacionDtos.CotizacionDto;
import pe.mecauto.dto.CotizacionDtos.LineaCotizacionDto;
import pe.mecauto.dto.OrdenDtos.LineaRepuestoDto;
import pe.mecauto.dto.OrdenDtos.LineaServicioDto;
import pe.mecauto.dto.OrdenDtos.OrdenResumenDto;
import pe.mecauto.entity.Cita;
import pe.mecauto.entity.Comprobante;
import pe.mecauto.entity.Cotizacion;
import pe.mecauto.entity.DetalleCotizacion;
import pe.mecauto.entity.DetalleOrdenRepuesto;
import pe.mecauto.entity.DetalleOrdenServicio;
import pe.mecauto.entity.OrdenTrabajo;
import pe.mecauto.util.Tiempo;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public final class OperacionMapper {

    private OperacionMapper() {
    }

    public static CitaDto aDto(Cita c) {
        return new CitaDto(c.getId(), c.getCodigo(),
                c.getCliente() == null ? null : c.getCliente().getId(),
                c.getCliente() == null ? null : c.getCliente().getNombre(),
                c.getVehiculo() == null ? null : c.getVehiculo().getId(),
                c.getServicio().getId(), c.getServicio().getNombre(),
                c.getNombreContacto(), c.getTelefonoContacto(), c.getCorreoContacto(), c.getPlaca(),
                c.getMarca(), c.getModelo(), c.getFechaHora(), c.getMotivo(), c.getEstado(), c.getOrigen(),
                c.getCreadoEn());
    }

    public static OrdenResumenDto aResumen(OrdenTrabajo o) {
        return new OrdenResumenDto(o.getId(), o.getCodigo(), o.getCliente().getId(), o.getCliente().getNombre(),
                o.getVehiculo().getId(), o.getVehiculo().getPlaca(),
                o.getVehiculo().getMarca() + " " + o.getVehiculo().getModelo(),
                o.getMecanico() == null ? null : o.getMecanico().getId(),
                o.getMecanico() == null ? null : o.getMecanico().getNombre(),
                o.getFalla(), o.getEstado(), o.getFechaIngreso(), o.getFechaEntregaEstimada(), o.getTotal());
    }

    public static LineaServicioDto aDto(DetalleOrdenServicio d) {
        return new LineaServicioDto(d.getId(), d.getServicio().getId(), d.getDescripcion(), d.getCantidad(),
                d.getPrecioUnitario(), CalculadoraIgv.importe(d.getCantidad(), d.getPrecioUnitario()));
    }

    public static LineaRepuestoDto aDto(DetalleOrdenRepuesto d) {
        return new LineaRepuestoDto(d.getId(), d.getRepuesto().getId(), d.getRepuesto().getCodigo(),
                d.getRepuesto().getNombre(), d.getCantidad(), d.getPrecioUnitario(),
                CalculadoraIgv.importe(d.getCantidad(), d.getPrecioUnitario()), d.isDescontado(),
                d.getRepuesto().getStockActual());
    }

    public static LineaCotizacionDto aDto(DetalleCotizacion d) {
        return new LineaCotizacionDto(d.getId(), d.getTipo(),
                d.getServicio() == null ? null : d.getServicio().getId(),
                d.getRepuesto() == null ? null : d.getRepuesto().getId(),
                d.getDescripcion(), d.getCantidad(), d.getPrecioUnitario(),
                CalculadoraIgv.importe(d.getCantidad(), d.getPrecioUnitario()));
    }

    /** conDetalle=false para listados (no recorre las líneas). */
    public static CotizacionDto aDto(Cotizacion c, boolean conDetalle) {
        BigDecimal subtotal = c.getTotal().add(c.getDescuento());
        boolean vencida = c.getEstado() == EstadoCotizacion.PENDIENTE && c.getVigenciaHasta().isBefore(Tiempo.hoy());
        List<LineaCotizacionDto> lineas = conDetalle ? c.getDetalles().stream().map(OperacionMapper::aDto).toList() : null;
        return new CotizacionDto(c.getId(), c.getCodigo(), c.getCliente().getId(), c.getCliente().getNombre(),
                c.getVehiculo() == null ? null : c.getVehiculo().getId(),
                c.getVehiculo() == null ? null : c.getVehiculo().getPlaca(),
                c.getOrdenId(), c.getFechaEmision(), c.getVigenciaHasta(), c.getEstado(), vencida, subtotal,
                c.getDescuento(), c.getBaseImponible(), c.getIgv(), c.getTotal(), c.getObservaciones(), lineas);
    }

    public static ComprobanteDto aDto(Comprobante c, Map<String, String> taller) {
        List<LineaComprobanteDto> lineas = c.getDetalles().stream()
                .map(d -> new LineaComprobanteDto(d.getDescripcion(), d.getCantidad(), d.getPrecioUnitario(), d.getImporte()))
                .toList();
        return new ComprobanteDto(c.getId(), c.getOrden().getId(), c.getOrden().getCodigo(), c.getTipo(), c.getSerie(),
                c.getNumero(), c.numeroCompleto(), c.getClienteNombre(), c.getClienteTipoDoc(), c.getClienteDocumento(),
                c.getVehiculoPlaca(), c.getBaseImponible(), c.getIgv(), c.getTotal(), c.getMetodoPago(), c.getEstado(),
                c.getMotivoAnulacion(), c.getFechaEmision(),
                c.getEmitidoPor() == null ? null : c.getEmitidoPor().getNombre(), lineas, taller);
    }
}
