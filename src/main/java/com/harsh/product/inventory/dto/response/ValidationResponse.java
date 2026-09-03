package com.harsh.product.inventory.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ValidationResponse {
    private String field;
    private String message;
}
