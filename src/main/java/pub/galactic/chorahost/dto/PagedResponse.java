package pub.galactic.chorahost.dto;

import java.util.List;
import java.util.function.Function;

import pub.galactic.chorahost.service.PagedResult;

public record PagedResponse<T>(List<T> items, int page, int size, long totalElements, int totalPages) {

    public static <T, R> PagedResponse<R> from(PagedResult<T> result, Function<T, R> mapper) {
        return new PagedResponse<>(
                result.items().stream().map(mapper).toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages());
    }
}
