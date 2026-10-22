package pe.mecauto.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Implementación del DAO con JdbcTemplate (SQL parametrizado con "?": nunca se concatenan datos del usuario).
 * Funciona sobre SQL Server; las pruebas la ejecutan sobre H2.
 */
@Repository
public class ClienteDAOImpl implements ClienteDAO {

    private static final String SQL_EXISTE =
            "SELECT COUNT(*) FROM cliente WHERE nro_documento = ?";
    private static final String SQL_EXISTE_OTRO =
            "SELECT COUNT(*) FROM cliente WHERE nro_documento = ? AND id_cliente <> ?";

    private final JdbcTemplate jdbc;

    public ClienteDAOImpl(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean existePorDocumento(String nroDocumento) {
        Integer n = jdbc.queryForObject(SQL_EXISTE, Integer.class, nroDocumento);
        return n != null && n > 0;
    }

    @Override
    public boolean existeOtroConDocumento(String nroDocumento, Long idExcluido) {
        Integer n = jdbc.queryForObject(SQL_EXISTE_OTRO, Integer.class, nroDocumento, idExcluido);
        return n != null && n > 0;
    }
}
