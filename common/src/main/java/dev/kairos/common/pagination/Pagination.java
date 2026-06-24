package dev.kairos.common.pagination;

/**
 * Pagination window for listing. Normalizes its inputs so the use case and
 * repository always receive a sane limit/offset:
 * <ul>
 *   <li>limit &le; 0  -> {@link #DEFAULT_LIMIT}</li>
 *   <li>limit &gt; {@link #MAX_LIMIT} -> {@link #MAX_LIMIT}</li>
 *   <li>offset &lt; 0 -> 0</li>
 * </ul>
 */
public record Pagination(int limit, int offset) {

    public static final int DEFAULT_LIMIT = 20;
    public static final int MAX_LIMIT = 100;


    public Pagination {
        if (limit <= 0) {
            limit = DEFAULT_LIMIT;
        } else if (limit > MAX_LIMIT) {
            limit = MAX_LIMIT;
        }
        if (offset < 0) {
            offset = 0;
        }
    }

    /** Builds a window from optional query params, applying defaults for nulls. */
    public static Pagination of(Integer limit, Integer offset) {
        return new Pagination(
                limit == null ? DEFAULT_LIMIT : limit,
                offset == null ? 0 : offset);
    }
}
