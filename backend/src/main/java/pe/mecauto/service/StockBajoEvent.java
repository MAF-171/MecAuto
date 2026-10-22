package pe.mecauto.service;

/** Evento (patrón Observer): se publica cuando un repuesto queda en o por debajo de su stock mínimo (RN04). */
public record StockBajoEvent(Long repuestoId, String codigo, String nombre, int stockActual, int stockMinimo) {
}
