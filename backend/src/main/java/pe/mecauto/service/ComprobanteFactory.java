package pe.mecauto.service;

import org.springframework.stereotype.Component;
import pe.mecauto.domain.CalculadoraIgv;
import pe.mecauto.domain.EstadoComprobante;
import pe.mecauto.domain.MetodoPago;
import pe.mecauto.domain.TipoComprobante;
import pe.mecauto.domain.TipoDocumento;
import pe.mecauto.entity.Comprobante;
import pe.mecauto.entity.DetalleComprobante;
import pe.mecauto.entity.DetalleOrdenRepuesto;
import pe.mecauto.entity.DetalleOrdenServicio;
import pe.mecauto.entity.OrdenTrabajo;
import pe.mecauto.entity.Usuario;
import pe.mecauto.util.Tiempo;

import java.math.BigDecimal;

/**
 * Patrón Factory: centraliza cómo se construye un comprobante a partir de una orden.
 * - Cliente con RUC -> FACTURA; cliente con DNI o carné de extranjería -> BOLETA (reglas de emisión del documento).
 * - Los precios ya incluyen IGV: se desglosa base e IGV del total (no se suma otra vez).
 */
@Component
public class ComprobanteFactory {

    public TipoComprobante tipoPara(OrdenTrabajo orden) {
        return orden.getCliente().getTipoDocumento() == TipoDocumento.RUC ? TipoComprobante.FACTURA : TipoComprobante.BOLETA;
    }

    public Comprobante crear(OrdenTrabajo orden, MetodoPago pago, Usuario emisor, String serie, String numero) {
        Comprobante c = new Comprobante();
        c.setOrden(orden);
        c.setTipo(tipoPara(orden));
        c.setSerie(serie);
        c.setNumero(numero);
        c.setClienteNombre(orden.getCliente().getNombre());
        c.setClienteTipoDoc(orden.getCliente().getTipoDocumento());
        c.setClienteDocumento(orden.getCliente().getNroDocumento());
        c.setVehiculoPlaca(orden.getVehiculo().getPlaca());
        c.setMetodoPago(pago);
        c.setEstado(EstadoComprobante.EMITIDO);
        c.setEmitidoPor(emisor);
        c.setFechaEmision(Tiempo.ahora());

        for (DetalleOrdenServicio s : orden.getServicios()) {
            agregar(c, s.getDescripcion(), s.getCantidad(), s.getPrecioUnitario());
        }
        for (DetalleOrdenRepuesto r : orden.getRepuestos()) {
            agregar(c, r.getRepuesto().getNombre() + " (" + r.getRepuesto().getCodigo() + ")", r.getCantidad(), r.getPrecioUnitario());
        }
        if (orden.getManoObra().signum() > 0) {
            agregar(c, "Mano de obra", 1, orden.getManoObra());
        }
        if (orden.getDescuento().signum() > 0) {
            agregar(c, "Descuento", 1, orden.getDescuento().negate());
        }

        CalculadoraIgv.Desglose d = CalculadoraIgv.desglosar(orden.getTotal());
        c.setBaseImponible(d.baseImponible());
        c.setIgv(d.igv());
        c.setTotal(d.total());
        return c;
    }

    private void agregar(Comprobante c, String descripcion, int cantidad, BigDecimal precio) {
        DetalleComprobante d = new DetalleComprobante();
        d.setComprobante(c);
        d.setDescripcion(descripcion.length() > 200 ? descripcion.substring(0, 200) : descripcion);
        d.setCantidad(cantidad);
        d.setPrecioUnitario(precio);
        d.setImporte(CalculadoraIgv.importe(cantidad, precio));
        c.getDetalles().add(d);
    }
}
