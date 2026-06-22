package com.thalicloud.order.dto.response;

import java.util.List;

public record PagedOrdersResponse(
        List<OrderResponse> orders,
        PaginationInfo pagination
) {

    public record PaginationInfo(int page, int pageSize, long totalCount, int totalPages) {}
}
