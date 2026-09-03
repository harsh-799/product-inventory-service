package com.harsh.product.inventory.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class ProductPageResponse {
    private Integer page;
    private Integer size;
    private Long totalElements;
    private Boolean isLast;
    private List<ProductResponse> products;
}
