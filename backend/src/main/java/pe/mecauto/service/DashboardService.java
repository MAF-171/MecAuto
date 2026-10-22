package pe.mecauto.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.mecauto.domain.EstadoCita;
import pe.mecauto.domain.EstadoComprobante;
import pe.mecauto.domain.EstadoOrden;
import pe.mecauto.domain.Rol;
import pe.mecauto.dto.CatalogoDtos.RepuestoDto;
import pe.mecauto.dto.DashboardDtos.DashboardDto;
import pe.mecauto.dto.DashboardDtos.PuntoIngreso;
import pe.mecauto.dto.OrdenDtos.OrdenResumenDto;
import pe.mecauto.entity.Comprobante;
import pe.mecauto.mapper.CatalogoMapper;
import pe.mecauto.mapper.OperacionMapper;
import pe.mecauto.repository.CitaRepository;
import pe.mecauto.repository.ClienteRepository;
import pe.mecauto.repository.ComprobanteRepository;
import pe.mecauto.repository.OrdenTrabajoRepository;
import pe.mecauto.repository.RepuestoRepository;
import pe.mecauto.repository.VehiculoRepository;
import pe.mecauto.security.UsuarioPrincipal;
import pe.mecauto.util.Tiempo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Indicadores del panel: todos salen de consultas reales a la base de datos. */
@Service
@Transactional(readOnly = true)
public class DashboardService {

    private static final EnumSet<EstadoOrden> ACTIVAS = EnumSet.of(
            EstadoOrden.RECIBIDA, EstadoOrden.EN_DIAGNOSTICO, EstadoOrden.EN_REPARACION, EstadoOrden.COMPLETADA);

    private final ClienteRepository clientes;
    private final VehiculoRepository vehiculos;
    private final CitaRepository citas;
    private final OrdenTrabajoRepository ordenes;
    private final RepuestoRepository repuestos;
    private final ComprobanteRepository comprobantes;

    public DashboardService(ClienteRepository clientes, VehiculoRepository vehiculos, CitaRepository citas,
                            OrdenTrabajoRepository ordenes, RepuestoRepository repuestos,
                            ComprobanteRepository comprobantes) {
        this.clientes = clientes;
        this.vehiculos = vehiculos;
        this.citas = citas;
        this.ordenes = ordenes;
        this.repuestos = repuestos;
        this.comprobantes = comprobantes;
    }

    public DashboardDto obtener(UsuarioPrincipal actor) {
        boolean mecanico = actor.getRol() == Rol.MECANICO;
        boolean admin = actor.getRol() == Rol.ADMINISTRADOR;

        List<Object[]> filas = mecanico ? ordenes.contarPorEstadoDeMecanico(actor.getId()) : ordenes.contarPorEstado();
        Map<EstadoOrden, Long> conteo = new EnumMap<>(EstadoOrden.class);
        for (EstadoOrden e : EstadoOrden.values()) {
            conteo.put(e, 0L);
        }
        for (Object[] f : filas) {
            conteo.put((EstadoOrden) f[0], (Long) f[1]);
        }
        Map<String, Long> porEstado = new LinkedHashMap<>();
        conteo.forEach((k, v) -> porEstado.put(k.name(), v));

        long pendientes = conteo.get(EstadoOrden.RECIBIDA);
        long enProceso = conteo.get(EstadoOrden.EN_DIAGNOSTICO) + conteo.get(EstadoOrden.EN_REPARACION);
        long finalizadas = conteo.get(EstadoOrden.COMPLETADA) + conteo.get(EstadoOrden.ENTREGADA);
        long activas = pendientes + enProceso + conteo.get(EstadoOrden.COMPLETADA);

        List<OrdenResumenDto> recientes = (mecanico
                ? ordenes.findTop8ByMecanicoIdOrderByFechaIngresoDesc(actor.getId())
                : ordenes.findTop8ByOrderByFechaIngresoDesc())
                .stream().map(OperacionMapper::aResumen).toList();

        List<RepuestoDto> alertas = repuestos.listarStockBajo().stream().limit(8).map(CatalogoMapper::aDto).toList();

        LocalDate hoy = Tiempo.hoy();
        Long citasHoy = mecanico ? null : citas.countByFechaHoraGreaterThanEqualAndFechaHoraLessThanAndEstadoNot(
                hoy.atStartOfDay(), hoy.plusDays(1).atStartOfDay(), EstadoCita.CANCELADA);

        BigDecimal ingresosMes = null;
        List<PuntoIngreso> semanas = null;
        if (admin) {
            LocalDate inicio = hoy.withDayOfMonth(1);
            LocalDate fin = inicio.plusMonths(1);
            ingresosMes = comprobantes.sumarTotal(EstadoComprobante.EMITIDO, inicio.atStartOfDay(), fin.atStartOfDay());
            semanas = ingresosPorSemana(inicio, fin);
        }

        return new DashboardDto(
                mecanico ? null : clientes.countByActivoTrue(),
                mecanico ? null : vehiculos.countByActivoTrue(),
                citasHoy, pendientes, enProceso, finalizadas, activas,
                mecanico ? null : ordenes.contarVehiculosEnEstados(ACTIVAS),
                repuestos.contarStockBajo(), ingresosMes, porEstado, alertas, recientes, semanas);
    }

    private List<PuntoIngreso> ingresosPorSemana(LocalDate inicio, LocalDate finExclusivo) {
        int cantidad = (int) Math.ceil(inicio.lengthOfMonth() / 7.0);
        BigDecimal[] totales = new BigDecimal[cantidad];
        for (int i = 0; i < cantidad; i++) {
            totales[i] = BigDecimal.ZERO;
        }
        List<Comprobante> lista = comprobantes
                .findByEstadoAndFechaEmisionGreaterThanEqualAndFechaEmisionLessThanOrderByFechaEmisionAsc(
                        EstadoComprobante.EMITIDO, inicio.atStartOfDay(), finExclusivo.atStartOfDay());
        for (Comprobante c : lista) {
            int idx = Math.min(cantidad - 1, (c.getFechaEmision().getDayOfMonth() - 1) / 7);
            totales[idx] = totales[idx].add(c.getTotal());
        }
        List<PuntoIngreso> r = new ArrayList<>();
        for (int i = 0; i < cantidad; i++) {
            r.add(new PuntoIngreso("Sem. " + (i + 1), totales[i]));
        }
        return r;
    }
}
