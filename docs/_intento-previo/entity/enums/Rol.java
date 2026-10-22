package pe.mecauto.entity.enums;

/** Roles del sistema (RF15). Los nombres coinciden con el documento académico. */
public enum Rol {
    ADMINISTRADOR,
    RECEPCION,
    MECANICO;

    /** Nombre de la autoridad que usa Spring Security (prefijo ROLE_). */
    public String autoridad() {
        return "ROLE_" + name();
    }
}
