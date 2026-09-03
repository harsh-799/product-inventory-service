package com.harsh.product.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.harsh.product.inventory.dto.request.ProductRequest;
import com.harsh.product.inventory.dto.response.ProductCreateResponse;
import com.harsh.product.inventory.dto.response.ProductListResponse;
import com.harsh.product.inventory.dto.response.ProductPageResponse;
import com.harsh.product.inventory.dto.response.ProductResponse;
import com.harsh.product.inventory.dto.response.ProductUpdateResponse;
import com.harsh.product.inventory.exception.GlobalExceptionHandler;
import com.harsh.product.inventory.exception.ProductNotFoundException;
import com.harsh.product.inventory.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductController productController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(productController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/products - Returns 200 OK with paginated products")
    void getAllProducts_Success() throws Exception {
        ProductListResponse item = ProductListResponse.builder()
                .id(1L)
                .name("Test Product")
                .createdBy("Admin")
                .createdOn(LocalDateTime.now())
                .build();

        ProductPageResponse pageResponse = ProductPageResponse.builder()
                .products(List.of(item))
                .page(0)
                .isLast(true)
                .totalElements(1L)
                .build();

        when(productService.getAllProducts(any(Pageable.class))).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/products")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Products fetched successfully"))
                .andExpect(jsonPath("$.data.products[0].name").value("Test Product"));
    }

    @Test
    @DisplayName("GET /api/v1/products/{id} - Success returns 200 OK")
    void getProductById_Success() throws Exception {
        ProductResponse response = ProductResponse.builder()
                .id(1L)
                .name("Keyboard")
                .createdBy("Admin")
                .createdOn(LocalDateTime.now())
                .build();

        when(productService.getProductById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.name").value("Keyboard"));
    }

    @Test
    @DisplayName("GET /api/v1/products/{id} - Not found returns 404")
    void getProductById_NotFound() throws Exception {
        when(productService.getProductById(999L))
                .thenThrow(new ProductNotFoundException("Product not found with id: 999"));

        mockMvc.perform(get("/api/v1/products/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Product not found with id: 999"));
    }

    @Test
    @DisplayName("POST /api/v1/products - Success returns 201 Created")
    void createProduct_Success() throws Exception {
        ProductRequest request = new ProductRequest("Mouse");
        ProductCreateResponse response = ProductCreateResponse.builder().id(2L).name("Mouse").build();

        when(productService.createProduct(any(ProductRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(2L))
                .andExpect(jsonPath("$.data.name").value("Mouse"));
    }

    @Test
    @DisplayName("PUT /api/v1/products/{id} - Success returns 200 OK")
    void updateProduct_Success() throws Exception {
        ProductRequest request = new ProductRequest("Updated Mouse");
        ProductUpdateResponse response = ProductUpdateResponse.builder()
                .id(2L)
                .name("Updated Mouse")
                .modifiedBy("admin@test.com")
                .modifiedOn(LocalDateTime.now())
                .build();

        when(productService.updateProduct(eq(2L), any(ProductRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/products/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Updated Mouse"));
    }

    @Test
    @DisplayName("DELETE /api/v1/products/{id} - Success returns 204 No Content")
    void deleteProduct_Success() throws Exception {
        doNothing().when(productService).deleteProduct(1L);

        mockMvc.perform(delete("/api/v1/products/1"))
                .andExpect(status().isNoContent());
    }
}
