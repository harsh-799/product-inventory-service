package com.harsh.product.inventory.controller;

import com.harsh.product.inventory.dto.request.ItemRequest;
import com.harsh.product.inventory.dto.request.ProductRequest;
import com.harsh.product.inventory.dto.response.ApiResponse;
import com.harsh.product.inventory.dto.response.ItemResponse;
import com.harsh.product.inventory.dto.response.ProductCreateResponse;
import com.harsh.product.inventory.dto.response.ProductPageResponse;
import com.harsh.product.inventory.dto.response.ProductResponse;
import com.harsh.product.inventory.dto.response.ProductUpdateResponse;
import com.harsh.product.inventory.service.ProductService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@AllArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ApiResponse<ProductCreateResponse>> createProduct(@Valid @RequestBody ProductRequest productRequest) {
        ProductCreateResponse product = productService.createProduct(productRequest);

        ApiResponse<ProductCreateResponse> response = ApiResponse.<ProductCreateResponse>builder()
                .success(true)
                .message("Product created successfully")
                .data(product)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<ProductPageResponse>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        ProductPageResponse products = productService.getAllProducts(pageable);

        ApiResponse<ProductPageResponse> response = ApiResponse.<ProductPageResponse>builder()
                .success(true)
                .message("Products fetched successfully")
                .data(products)
                .build();
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(@PathVariable Long id) {
        ProductResponse product = productService.getProductById(id);
        ApiResponse<ProductResponse> response = ApiResponse.<ProductResponse>builder()
                .success(true)
                .message("Product fetched successfully")
                .data(product)
                .build();
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductUpdateResponse>> updateProduct(@PathVariable Long id, @Valid @RequestBody ProductRequest productRequest) {
        ProductUpdateResponse product = productService.updateProduct(id, productRequest);
        ApiResponse<ProductUpdateResponse> response = ApiResponse.<ProductUpdateResponse>builder()
                .success(true)
                .message("Product updated successfully")
                .data(product)
                .build();
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping("/{productId}/items")
    public ResponseEntity<ApiResponse<ItemResponse>> createItem(
            @PathVariable Long productId,
            @Valid @RequestBody ItemRequest itemRequest) {
        ItemResponse item = productService.createItem(productId, itemRequest);
        ApiResponse<ItemResponse> response = ApiResponse.<ItemResponse>builder()
                .success(true)
                .message("Item created successfully")
                .data(item)
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{productId}/items")
    public ResponseEntity<ApiResponse<List<ItemResponse>>> getItemsByProductId(@PathVariable Long productId) {
        List<ItemResponse> items = productService.getItemsByProductId(productId);
        ApiResponse<List<ItemResponse>> response = ApiResponse.<List<ItemResponse>>builder()
                .success(true)
                .message("Items fetched successfully")
                .data(items)
                .build();
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
