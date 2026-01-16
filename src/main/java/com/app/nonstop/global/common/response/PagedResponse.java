package com.app.nonstop.global.common.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PagedResponse<T> {
    private List<T> items;
    private long totalCount;
    private boolean hasMore;
    private Integer limit;
    private Integer offset;

    public static <T> PagedResponse<T> of(List<T> items, long totalCount, Integer limit, Integer offset) {
        boolean hasMore = false;
        if (limit != null && offset != null) {
            hasMore = (offset + items.size()) < totalCount;
        }

        return PagedResponse.<T>builder()
                .items(items)
                .totalCount(totalCount)
                .hasMore(hasMore)
                .limit(limit)
                .offset(offset)
                .build();
    }
}
