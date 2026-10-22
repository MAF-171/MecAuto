package pe.mecauto.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.mecauto.domain.EstadoCita;
import pe.mecauto.domain.FranjasHorarias;
import pe.mecauto.domain.OrigenCita;
import pe.mecauto.dto.CitaDtos.*;
import pe.mecauto.dto.PageDto;
import pe.mecauto.entity.Cita;
import pe.mecauto.entity.Cliente;
import pe.mecauto.entity.Servicio;
import pe.mecauto.entity.Vehiculo;
import pe.mecauto.exception.ConflictoException;
import pe.mecauto.exception.NegocioException;
import pe.mecauto.exception.RecursoNoEncontradoException;
import pe.mecauto.exception.ValidacionException;
import pe.mecauto.mapper.OperacionMapper;
import pe.mecauto.repository.CitaRepository;
import pe.mecauto.repository.ClienteRepository;
import pe.mecauto.repository.ServicioRepository;
import pe.mecauto.repository.VehiculoRepository;
import pe.mecauto.util.PlacaUtil;
import pe.mecauto.util.Tiempo;
import pe.mecauto.util.TextoUtil;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Citas: registro interno y público, confirmación, reprogramación, cancelación y control de horarios. */
@Service
@Transactional(readOnly = true)
public class CitaService {

    private static final Logger log = LoggerFactory.getLogger(CitaService.class);
    private static final EnumSet<EstadoCita> ABIERTAS = EnumSet.of(EstadoCita.PENDIENTE, EstadoCita.CONFIRMADA);
    private static final int DIAS_MAX_ANTICIPACION = 90;

    private final CitaRepository citas;
    private final ClienteRepository clientes;
    private final VehiculoRepository vehiculos;
    private final ServicioRepository servicios;
    private final ConfiguracionService config;
    private final SecuenciaService secuencias;

    public CitaService(CitaRepository citas, ClienteRepository clientes, VehiculoRepository vehiculos,
                       ServicioRepository servicios, ConfiguracionService config, SecuenciaService secuencias) {
        this.citas = citas;
        this.clientes = clientes;
        this.vehiculos = vehiculos;
        this.servicios = servicios;
        this.config = config;
        this.secuencias = secuencias;
    }

    // ------------------------------------------------------------------ Consultas

    public PageDto<CitaDto> listar(String q, EstadoCita estado, LocalDate desde, LocalDate hasta, int pagina, int tamano) {
        Specification<Cita> spec = (r, cq, cb) -> cb.conjunction();
        String texto = TextoUtil.limpiarBusqueda(q).toLowerCase(Locale.ROOT);
        if (!texto.isEmpty()) {
            String t = "%" + texto + "%";
            spec = spec.and((r, cq, cb) -> cb.or(
                    cb.like(cb.lower(r.get("codigo")), t),
                    cb.like(cb.lower(r.get("nombreContacto")), t),
                    cb.like(cb.lower(r.get("placa")), t),
                    cb.like(r.get("telefonoContacto"), t)));
        }
        if (estado != null) {
            spec = spec.and((r, cq, cb) -> cb.equal(r.get("estado"), estado));
        }
        if (desde != null) {
            spec = spec.and((r, cq, cb) -> cb.greaterThanOrEqualTo(r.<LocalDateTime>get("fechaHora"), desde.atStartOfDay()));
        }
        if (hasta != null) {
            spec = spec.and((r, cq, cb) -> cb.lessThan(r.<LocalDateTime>get("fechaHora"), hasta.plusDays(1).atStartOfDay()));
        }
        Page<Cita> page = citas.findAll(spec, PageRequest.of(pagina, Math.min(tamano, 100), Sort.by("fechaHora").descending()));
        return PageDto.de(page, OperacionMapper::aDto);
    }

    public CitaDto obtener(Long id) {
        return OperacionMapper.aDto(buscar(id));
    }

    /** Franjas libres de un día. Sirve a la página pública y al formulario interno. */
    public DisponibilidadDto disponibilidad(LocalDate fecha) {
        FranjasHorarias horario = config.franjas();
        int capacidad = config.citasPorFranja();
        if (!horario.esDiaLaborable(fecha)) {
            return new DisponibilidadDto(fecha, false, List.of());
        }
        Map<LocalDateTime, Long> ocupadas = new HashMap<>();
        for (Object[] fila : citas.ocupacionPorFranja(fecha.atStartOfDay(), fecha.plusDays(1).atStartOfDay(), ABIERTAS)) {
            ocupadas.put((LocalDateTime) fila[0], (Long) fila[1]);
        }
        LocalDateTime ahora = Tiempo.ahora();
        List<FranjaDto> franjas = new ArrayList<>();
        for (LocalTime inicio : horario.franjasDelDia(fecha)) {
            LocalDateTime dt = LocalDateTime.of(fecha, inicio);
            int libres = dt.isAfter(ahora) ? (int) Math.max(0, capacidad - ocupadas.getOrDefault(dt, 0L)) : 0;
            franjas.add(new FranjaDto(inicio.toString(), libres));
        }
        return new DisponibilidadDto(fecha, true, franjas);
    }

    // ------------------------------------------------------------------ Altas

    @Transactional
    public CitaDto crear(CitaRequest req) {
        Cita c = new Cita();
        c.setOrigen(OrigenCita.INTERNA);
        c.setCodigo(secuencias.siguienteCodigo("CITA"));
        aplicar(c, req);
        return OperacionMapper.aDto(citas.save(c));
    }

    /** Solicitud de la página pública. El campo trampa "sitioWeb" lleno indica un robot: se ignora sin guardar. */
    @Transactional
    public CitaPublicaResponse crearPublica(CitaPublicaRequest req) {
        if (req.sitioWeb() != null && !req.sitioWeb().isBlank()) {
            log.warn("Solicitud pública de cita descartada por el campo trampa");
            return new CitaPublicaResponse("CITA-0000", req.fechaHora(), "Solicitud recibida");
        }
        CitaRequest interno = new CitaRequest(null, null, req.servicioId(), req.nombre(), req.telefono(), req.correo(),
                req.placa(), req.marca(), req.modelo(), req.fechaHora(), req.motivo());
        Cita c = new Cita();
        c.setOrigen(OrigenCita.PUBLICA);
        c.setCodigo(secuencias.siguienteCodigo("CITA"));
        aplicar(c, interno);
        // Si la placa ya está registrada, se vincula con su cliente y vehículo para ahorrar trabajo a recepción.
        vehiculos.findByPlaca(c.getPlaca()).filter(Vehiculo::isActivo).ifPresent(v -> {
            c.setVehiculo(v);
            c.setCliente(v.getCliente());
        });
        Cita guardada = citas.save(c);
        return new CitaPublicaResponse(guardada.getCodigo(), guardada.getFechaHora(),
                "Tu solicitud fue registrada. Te contactaremos para confirmar la cita.");
    }

    // ------------------------------------------------------------- Modificaciones

    @Transactional
    public CitaDto actualizar(Long id, CitaRequest req) {
        Cita c = buscar(id);
        exigirAbierta(c);
        aplicar(c, req);
        return OperacionMapper.aDto(c);
    }

    @Transactional
    public CitaDto confirmar(Long id) {
        Cita c = buscar(id);
        if (c.getEstado() != EstadoCita.PENDIENTE) {
            throw new NegocioException("Solo se puede confirmar una cita pendiente");
        }
        c.setEstado(EstadoCita.CONFIRMADA);
        return OperacionMapper.aDto(c);
    }

    @Transactional
    public CitaDto cancelar(Long id) {
        Cita c = buscar(id);
        exigirAbierta(c);
        c.setEstado(EstadoCita.CANCELADA);
        return OperacionMapper.aDto(c);
    }

    @Transactional
    public CitaDto reprogramar(Long id, ReprogramarRequest req) {
        Cita c = buscar(id);
        exigirAbierta(c);
        LocalDateTime nuevo = validarFranja(req.fechaHora(), c.getPlaca(), c.getId(), c.getFechaHora());
        c.setFechaHora(nuevo);
        return OperacionMapper.aDto(c);
    }

    // ------------------------------------------------------------------ Internos

    private void aplicar(Cita c, CitaRequest req) {
        Servicio servicio = servicios.findById(req.servicioId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Servicio", req.servicioId()));
        if (!servicio.isActivo()) {
            throw new NegocioException("El servicio seleccionado no está disponible");
        }
        String placa = PlacaUtil.normalizar(req.placa());
        if (!PlacaUtil.esValida(placa)) {
            throw new ValidacionException("placa", "Placa inválida (ej.: ABC-123)");
        }
        Cliente cliente = null;
        Vehiculo vehiculo = null;
        if (req.clienteId() != null) {
            cliente = clientes.findById(req.clienteId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Cliente", req.clienteId()));
        }
        if (req.vehiculoId() != null) {
            vehiculo = vehiculos.findById(req.vehiculoId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Vehículo", req.vehiculoId()));
            if (cliente == null) {
                cliente = vehiculo.getCliente();
            } else if (!vehiculo.getCliente().getId().equals(cliente.getId())) {
                throw new ValidacionException("vehiculoId", "El vehículo no pertenece al cliente seleccionado");
            }
        }
        LocalDateTime fecha = validarFranja(req.fechaHora(), placa, c.getId(), c.getFechaHora());

        c.setServicio(servicio);
        c.setCliente(cliente);
        c.setVehiculo(vehiculo);
        c.setNombreContacto(req.nombreContacto().trim());
        c.setTelefonoContacto(req.telefonoContacto().trim());
        c.setCorreoContacto(TextoUtil.aNullSiVacio(req.correoContacto()));
        c.setPlaca(placa);
        c.setMarca(TextoUtil.aNullSiVacio(req.marca()));
        c.setModelo(TextoUtil.aNullSiVacio(req.modelo()));
        c.setMotivo(TextoUtil.aNullSiVacio(req.motivo()));
        c.setFechaHora(fecha);
    }

    /**
     * Valida que la fecha sea futura, esté dentro del horario, haya cupo y la placa no tenga otra cita abierta ese día.
     * Devuelve el inicio de la franja (las citas se guardan al inicio de su franja).
     *
     * @param idActual   id de la cita que se está editando (null si es nueva)
     * @param fechaAntes fecha que ya tenía la cita (null si es nueva); si no cambia de franja no se vuelve a validar
     */
    private LocalDateTime validarFranja(LocalDateTime solicitada, String placa, Long idActual, LocalDateTime fechaAntes) {
        FranjasHorarias horario = config.franjas();
        LocalDateTime inicio = horario.inicioDeFranja(solicitada);
        if (inicio == null) {
            throw new ValidacionException("fechaHora", "La fecha y hora están fuera del horario de atención");
        }
        if (inicio.equals(fechaAntes)) {
            return inicio;
        }
        if (!inicio.isAfter(Tiempo.ahora())) {
            throw new ValidacionException("fechaHora", "Elige una fecha y hora futuras");
        }
        if (inicio.toLocalDate().isAfter(Tiempo.hoy().plusDays(DIAS_MAX_ANTICIPACION))) {
            throw new ValidacionException("fechaHora", "Solo se agenda hasta " + DIAS_MAX_ANTICIPACION + " días adelante");
        }
        LocalDateTime fin = inicio.plusMinutes(horario.minutosPorFranja());
        long ocupadas = citas.countByFechaHoraGreaterThanEqualAndFechaHoraLessThanAndEstadoIn(inicio, fin, ABIERTAS);
        if (ocupadas >= config.citasPorFranja()) {
            throw new ConflictoException("Ese horario ya no tiene cupos. Elige otra hora.");
        }
        LocalDate dia = inicio.toLocalDate();
        boolean placaRepetida = citas.existsByPlacaAndFechaHoraGreaterThanEqualAndFechaHoraLessThanAndEstadoIn(
                placa, dia.atStartOfDay(), dia.plusDays(1).atStartOfDay(), ABIERTAS);
        if (placaRepetida && (idActual == null || fechaAntes == null || !fechaAntes.toLocalDate().equals(dia))) {
            throw new ConflictoException("Ya existe una cita abierta para la placa " + placa + " ese día");
        }
        return inicio;
    }

    private void exigirAbierta(Cita c) {
        if (!c.getEstado().estaAbierta()) {
            throw new NegocioException("La cita ya está " + c.getEstado().name().toLowerCase(Locale.ROOT));
        }
    }

    private Cita buscar(Long id) {
        return citas.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Cita", id));
    }
}
