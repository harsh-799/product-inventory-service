package com.harsh.product.inventory.service;

import com.harsh.product.inventory.dto.request.ProductRequest;
import com.harsh.product.inventory.dto.response.ProductResponse;
import com.harsh.product.inventory.entity.Product;
import com.harsh.product.inventory.entity.User;
import com.harsh.product.inventory.exception.ProductNotFoundException;
import com.harsh.product.inventory.repository.ProductRepository;
import com.harsh.product.inventory.repository.UserRepository;
import com.harsh.product.inventory.security.CustomUserDetails;
import lombok.AllArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    private ProductResponse mapToResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .createdBy(product.getCreatedBy() != null ? product.getCreatedBy().getEmail() : null)
                .createdOn(product.getCreatedOn())
                .modifiedBy(product.getModifiedBy() != null ? product.getModifiedBy().getEmail() : null)
                .modifiedOn(product.getModifiedOn())
                .build();
    }

    private Product getProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() ->
                        new ProductNotFoundException("Product not found with id: " + id));
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    public ProductResponse createProduct(ProductRequest productRequest) {
        User currentUser = getCurrentUser();

        Product product = Product.builder()
                .name(productRequest.getName())
                .createdBy(currentUser)
                .build();

        Product savedProduct = productRepository.save(product);
        return mapToResponse(savedProduct);
    }

    public List<ProductResponse> getAllProducts() {
        List<Product> productList = productRepository.findAll();

        return productList.stream()
                .map(this::mapToResponse)
                .toList();
    }

    public ProductResponse getProductById(Long id) {
        Product product = getProduct(id);
        return mapToResponse(product);
    }

    public ProductResponse updateProduct(Long id, ProductRequest productRequest) {
        Product product = getProduct(id);

        User currentUser = getCurrentUser();

        product.setName(productRequest.getName());
        product.setModifiedBy(currentUser);

        Product updatedProduct = productRepository.save(product);
        return mapToResponse(updatedProduct);
    }

    public void deleteProduct(Long id) {
        Product product = getProduct(id);
        productRepository.delete(product);
    }


}
