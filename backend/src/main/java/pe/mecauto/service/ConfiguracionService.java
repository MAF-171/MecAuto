package pe.mecauto.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.mecauto.domain.FranjasHorarias;
import pe.mecauto.entity.ConfiguracionTaller;
import pe.mecauto.exception.ValidacionException;
import pe.mecauto.repository.ConfiguracionTallerRepository;
import pe.mecauto.util.TextoUtil;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Ajustes del taller (tabla configuracion_taller). Solo se aceptan las claves conocidas. */
@Service
@Transactional(readOnly = true)
public class ConfiguracionService {

    /** Clave -> valor por defecto. Los datos de contacto quedan vacíos hasta que el administrador los complete. */
    private static final Map<String, String> DEFECTOS = new LinkedHashMap<>();

    static {
        DEFECTOS.put("nombre_taller", "Taller Mecánico MecAuto");
        DEFECTOS.put("ruc", "");
        DEFECTOS.put("direccion", "");
        DEFECTOS.put("telefono", "");
        DEFECTOS.put("whatsapp", "");
        DEFECTOS.put("correo", "");
        DEFECTOS.put("horario_texto", "Lunes a sábado, 8:00 a 18:00");
        DEFECTOS.put("hora_apertura", "08:00");
        DEFECTOS.put("hora_cierre", "18:00");
        DEFECTOS.put("duracion_franja_min", "60");
        DEFECTOS.put("citas_por_franja", "2");
        DEFECTOS.put("dias_laborables", "1,2,3,4,5,6");
        DEFECTOS.put("serie_boleta", "B001");
        DEFECTOS.put("serie_factura", "F001");
    }

    private static final List<String> CLAVES_PUBLICAS =
            List.of("nombre_taller", "direccion", "telefono", "whatsapp", "correo", "horario_texto");

    private final ConfiguracionTallerRepository repo;

    public ConfiguracionService(ConfiguracionTallerRepository repo) {
        this.repo = repo;
    }

    public Map<String, String> obtenerTodo() {
        Map<String, String> r = new LinkedHashMap<>(DEFECTOS);
        repo.findAll().forEach(c -> {
            if (DEFECTOS.containsKey(c.getClave())) {
                r.put(c.getClave(), c.getValor() == null ? "" : c.getValor());
            }
        });
        return r;
    }

    public String obtener(String clave) {
        return obtenerTodo().get(clave);
    }

    public Map<String, String> obtenerPublica() {
        Map<String, String> todo = obtenerTodo();
        Map<String, String> r = new LinkedHashMap<>();
        CLAVES_PUBLICAS.forEach(k -> r.put(k, todo.get(k)));
        return r;
    }

    /** Datos del emisor para imprimir comprobantes y cotizaciones. */
    public Map<String, String> datosEmisor() {
        Map<String, String> todo = obtenerTodo();
        Map<String, String> r = new LinkedHashMap<>();
        for (String k : List.of("nombre_taller", "ruc", "direccion", "telefono", "correo")) {
            r.put(k, todo.get(k));
        }
        return r;
    }

    @Transactional
    public Map<String, String> actualizar(Map<String, String> cambios) {
        for (Map.Entry<String, String> e : cambios.entrySet()) {
            if (!DEFECTOS.containsKey(e.getKey())) {
                throw new ValidacionException(e.getKey(), "Ajuste desconocido");
            }
            String valor = e.getValue() == null ? "" : e.getValue().trim();
            validar(e.getKey(), valor);
            ConfiguracionTaller c = repo.findById(e.getKey()).orElseGet(() -> new ConfiguracionTaller(e.getKey(), ""));
            c.setValor(valor);
            repo.save(c);
        }
        repo.flush();
        Map<String, String> todo = obtenerTodo();
        if (!LocalTime.parse(todo.get("hora_apertura")).isBefore(LocalTime.parse(todo.get("hora_cierre")))) {
            // la excepción deshace toda la transacción: no queda nada guardado a medias
            throw new ValidacionException("hora_cierre", "El cierre debe ser posterior a la apertura");
        }
        return todo;
    }

    @Transactional
    public void asegurarValoresPorDefecto() {
        DEFECTOS.forEach((k, v) -> {
            if (!repo.existsById(k)) {
                repo.save(new ConfiguracionTaller(k, v));
            }
        });
    }

    private void validar(String clave, String valor) {
        if (valor.length() > 300) {
            throw new ValidacionException(clave, "Máximo 300 caracteres");
        }
        try {
            switch (clave) {
                case "hora_apertura", "hora_cierre" -> LocalTime.parse(valor);
                case "duracion_franja_min" -> {
                    int n = Integer.parseInt(valor);
                    if (n < 15 || n > 480) {
                        throw new ValidacionException(clave, "Debe estar entre 15 y 480 minutos");
                    }
                }
                case "citas_por_franja" -> {
                    int n = Integer.parseInt(valor);
                    if (n < 1 || n > 50) {
                        throw new ValidacionException(clave, "Debe estar entre 1 y 50");
                    }
                }
                case "dias_laborables" -> {
                    if (!valor.matches("^[1-7](,[1-7])*$")) {
                        throw new ValidacionException(clave, "Usa números del 1 (lunes) al 7 (domingo) separados por coma");
                    }
                }
                case "serie_boleta" -> {
                    if (!valor.matches("^B[A-Z0-9]{3}$")) {
                        throw new ValidacionException(clave, "La serie de boleta tiene 4 caracteres y empieza con B");
                    }
                }
                case "serie_factura" -> {
                    if (!valor.matches("^F[A-Z0-9]{3}$")) {
                        throw new ValidacionException(clave, "La serie de factura tiene 4 caracteres y empieza con F");
                    }
                }
                case "ruc" -> {
                    if (!valor.isEmpty() && !valor.matches("^\\d{11}$")) {
                        throw new ValidacionException(clave, "El RUC debe tener 11 dígitos");
                    }
                }
                default -> {
                }
            }
        } catch (DateTimeParseException | NumberFormatException ex) {
            throw new ValidacionException(clave, "Valor inválido");
        }
    }

    /** Construye las reglas de horario a partir de la configuración. */
    public FranjasHorarias franjas() {
        Map<String, String> c = obtenerTodo();
        Set<DayOfWeek> dias = EnumSet.noneOf(DayOfWeek.class);
        for (String d : c.get("dias_laborables").split(",")) {
            if (!TextoUtil.limpiarBusqueda(d).isEmpty()) {
                dias.add(DayOfWeek.of(Integer.parseInt(d.trim())));
            }
        }
        return new FranjasHorarias(LocalTime.parse(c.get("hora_apertura")), LocalTime.parse(c.get("hora_cierre")),
                Integer.parseInt(c.get("duracion_franja_min")), dias);
    }

    public int citasPorFranja() {
        return Integer.parseInt(obtener("citas_por_franja"));
    }
}
