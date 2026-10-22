package pe.mecauto.controller;

/** Expresiones de permisos reutilizadas en @PreAuthorize (se verifican siempre en el servidor). */
final class Roles {

    static final String ADMIN = "hasRole('ADMINISTRADOR')";
    static final String ADMIN_RECEPCION = "hasAnyRole('ADMINISTRADOR','RECEPCION')";
    static final String TODOS = "hasAnyRole('ADMINISTRADOR','RECEPCION','MECANICO')";

    private Roles() {
    }
}
