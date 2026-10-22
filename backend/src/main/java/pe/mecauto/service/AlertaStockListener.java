package pe.mecauto.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Observador del evento de stock bajo: deja constancia en el log una vez confirmada la transacción. */
@Component
public class AlertaStockListener {

    private static final Logger log = LoggerFactory.getLogger(AlertaStockListener.class);

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alAlertar(StockBajoEvent e) {
        log.warn("ALERTA DE STOCK: {} ({}) quedó en {} unidades (mínimo {})", e.nombre(), e.codigo(), e.stockActual(), e.stockMinimo());
    }
}
