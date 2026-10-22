package pe.mecauto.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public final class DashboardDtos {

    private DashboardDtos() {
    }

    public record PuntoIngreso(String etiqueta, BigDecimal total) {
    }

    /**
     * Indicadores calculados con datos reales de SQL Server.
     * Para el rol MECANICO las cifras de órdenes se limitan a sus órdenes asignadas
     * y los campos financieros (ingresos) vienen vacíos.
     */
    public record DashboardDto(
            Long totalClientes, Long totalVehiculos, Long citasHoy,
            long ordenesPendientes, long ordenesEnProceso, long ordenesFinalizadas, long ordenesActivas,
            Long vehiculosEnTaller, Long repuestosStockBajo, BigDecimal ingresosMes,
            Map<String, Long> ordenesPorEstado,
            List<CatalogoDtos.RepuestoDto> alertasStock,
            List<OrdenDtos.OrdenResumenDto> ordenesRecientes,
            List<PuntoIngreso> ingresosPorSemana) {
    }
}
