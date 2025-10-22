package org.podhub.podhub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Wrapper genérico para respuestas paginadas con cursor
 *
 * @param <T> Tipo de datos en la lista
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaginatedResponse<T> {

    /**
     * Lista de elementos de la página actual
     */
    private List<T> data;

    /**
     * Cursor para la siguiente página (null si no hay más)
     * Formato: ISO-8601 timestamp del último elemento
     */
    private String nextCursor;

    /**
     * Indica si hay más elementos después de esta página
     */
    private boolean hasMore;

    /**
     * Número de elementos en esta página
     */
    private int count;
}
