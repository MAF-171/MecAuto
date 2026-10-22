package pe.mecauto.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pe.mecauto.entity.Secuencia;
import pe.mecauto.repository.SecuenciaRepository;

/** Genera correlativos (OT-0001, COT-0001, series de comprobantes) sin repetir números: bloquea la fila mientras dura la transacción. */
@Service
public class SecuenciaService {

    private final SecuenciaRepository repo;

    public SecuenciaService(SecuenciaRepository repo) {
        this.repo = repo;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public int siguiente(String nombre) {
        Secuencia s = repo.findByNombreForUpdate(nombre)
                .orElseGet(() -> repo.saveAndFlush(new Secuencia(nombre, 0)));
        s.setUltimo(s.getUltimo() + 1);
        return s.getUltimo();
    }

    /** Ej.: siguienteCodigo("OT") -> "OT-0001". */
    @Transactional(propagation = Propagation.MANDATORY)
    public String siguienteCodigo(String prefijo) {
        return String.format("%s-%04d", prefijo, siguiente(prefijo));
    }

    @Transactional
    public void asegurar(String nombre) {
        if (!repo.existsById(nombre)) {
            repo.save(new Secuencia(nombre, 0));
        }
    }
}
