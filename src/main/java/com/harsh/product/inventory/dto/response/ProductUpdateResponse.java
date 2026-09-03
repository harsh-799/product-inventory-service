package com.harsh.product.inventory.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductUpdateResponse {

    private Long id;
    private String name;
    private String modifiedBy;
    private LocalDateTime modifiedOn;
}
