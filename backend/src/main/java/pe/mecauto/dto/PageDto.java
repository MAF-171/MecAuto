package pe.mecauto.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/** Respuesta paginada estándar de los listados. La página empieza en 0. */
public record PageDto<T>(List<T> contenido, int pagina, int tamano, long totalElementos, int totalPaginas) {

    public static <E, T> PageDto<T> de(Page<E> page, Function<E, T> convertidor) {
        return new PageDto<>(
                page.getContent().stream().map(convertidor).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
