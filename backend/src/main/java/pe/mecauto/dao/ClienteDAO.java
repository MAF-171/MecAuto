package pe.mecauto.dao;

/**
 * DAO (Data Access Object) de clientes: encapsula las consultas SQL directas sobre la tabla cliente.
 * El servicio depende de esta interfaz y no de JDBC, así que en las pruebas puede sustituirse por una
 * implementación en memoria. El resto del CRUD usa los repositorios de Spring Data JPA.
 */
public interface ClienteDAO {

    /** Indica si ya existe un cliente con ese número de documento (RN: el documento no se repite). */
    boolean existePorDocumento(String nroDocumento);

    /** Indica si existe un cliente distinto de {@code idExcluido} con ese documento (usado al actualizar). */
    boolean existeOtroConDocumento(String nroDocumento, Long idExcluido);
}
