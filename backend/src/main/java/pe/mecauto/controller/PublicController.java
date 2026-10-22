package pe.mecauto.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pe.mecauto.dto.CatalogoDtos.ServicioPublicoDto;
import pe.mecauto.dto.CitaDtos.CitaPublicaRequest;
import pe.mecauto.dto.CitaDtos.CitaPublicaResponse;
import pe.mecauto.dto.CitaDtos.DisponibilidadDto;
import pe.mecauto.dto.SeguimientoDtos.ConsultaOrdenRequest;
import pe.mecauto.dto.SeguimientoDtos.SeguimientoOrdenDto;
import pe.mecauto.exception.ValidacionException;
import pe.mecauto.service.CitaService;
import pe.mecauto.service.ConfiguracionService;
import pe.mecauto.service.LimitadorSolicitudes;
import pe.mecauto.service.SeguimientoService;
import pe.mecauto.service.ServicioService;
import pe.mecauto.util.Tiempo;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Endpoints SIN autenticación para la página pública. Solo exponen información del catálogo, los datos de
 * contacto configurados y el registro de solicitudes de cita. No devuelven datos de clientes ni de vehículos.
 */
@RestController
@RequestMapping("/api/v1/public")
public class PublicController {

    private final ConfiguracionService configuracion;
    private final ServicioService servicios;
    private final CitaService citas;
    private final LimitadorSolicitudes limitador;
    private final SeguimientoService seguimiento;

    public PublicController(ConfiguracionService configuracion, ServicioService servicios, CitaService citas,
                            LimitadorSolicitudes limitador, SeguimientoService seguimiento) {
        this.configuracion = configuracion;
        this.servicios = servicios;
        this.citas = citas;
        this.limitador = limitador;
        this.seguimiento = seguimiento;
    }

    @GetMapping("/taller")
    public Map<String, String> taller() {
        return configuracion.obtenerPublica();
    }

    @GetMapping("/servicios")
    public List<ServicioPublicoDto> servicios() {
        return servicios.listarPublico();
    }

    @GetMapping("/citas/disponibilidad")
    public DisponibilidadDto disponibilidad(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        if (fecha.isBefore(Tiempo.hoy()) || fecha.isAfter(Tiempo.hoy().plusDays(90))) {
            throw new ValidacionException("fecha", "Elige una fecha dentro de los próximos 90 días");
        }
        return citas.disponibilidad(fecha);
    }

    @PostMapping("/citas")
    @ResponseStatus(HttpStatus.CREATED)
    public CitaPublicaResponse solicitarCita(@Valid @RequestBody CitaPublicaRequest req, HttpServletRequest http) {
        limitador.verificar(http.getRemoteAddr());
        return citas.crearPublica(req);
    }

    /** Estado de una orden: código + últimos 3 caracteres de la placa. Máximo 15 intentos cada 10 minutos por IP. */
    @PostMapping("/ordenes/consulta")
    public SeguimientoOrdenDto consultarOrden(@Valid @RequestBody ConsultaOrdenRequest req, HttpServletRequest http) {
        limitador.verificar("consulta:" + http.getRemoteAddr(), 15, Duration.ofMinutes(10));
        return seguimiento.consultar(req.codigo(), req.placa());
    }
}
