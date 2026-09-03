package com.harsh.product.inventory.service;

import com.harsh.product.inventory.dto.request.ProductRequest;
import com.harsh.product.inventory.dto.response.ItemResponse;
import com.harsh.product.inventory.dto.response.ProductCreateResponse;
import com.harsh.product.inventory.dto.response.ProductListResponse;
import com.harsh.product.inventory.dto.response.ProductPageResponse;
import com.harsh.product.inventory.dto.response.ProductResponse;
import com.harsh.product.inventory.dto.response.ProductUpdateResponse;
import com.harsh.product.inventory.entity.Item;
import com.harsh.product.inventory.entity.Product;
import com.harsh.product.inventory.entity.User;
import com.harsh.product.inventory.exception.ProductNotFoundException;
import com.harsh.product.inventory.repository.ItemRepository;
import com.harsh.product.inventory.repository.ProductRepository;
import com.harsh.product.inventory.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;

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

    private ItemResponse mapToItemResponse(Item item) {
        return ItemResponse.builder()
                .id(item.getId())
                .quantity(item.getQuantity())
                .build();
    }

    public ProductCreateResponse createProduct(ProductRequest productRequest) {
        User currentUser = getCurrentUser();

        Product product = Product.builder()
                .name(productRequest.getName())
                .createdBy(currentUser)
                .build();

        Product savedProduct = productRepository.save(product);

        return ProductCreateResponse.builder()
                .id(savedProduct.getId())
                .name(savedProduct.getName())
                .build();
    }

    public ProductPageResponse getAllProducts(Pageable pageable) {
        Page<Product> productPage = productRepository.findAll(pageable);

        List<ProductListResponse> productResponsesList = new ArrayList<>();

        for (Product product : productPage.getContent()) {
            productResponsesList.add(ProductListResponse.builder()
                    .id(product.getId())
                    .name(product.getName())
                    .createdBy(product.getCreatedBy().getFullname())
                    .createdOn(product.getCreatedOn())
                    .modifiedBy(product.getModifiedBy() != null ? product.getModifiedBy().getEmail() : null)
                    .modifiedOn(product.getModifiedOn())
                    .build()
            );
        }

        return ProductPageResponse.builder()
                .products(productResponsesList)
                .page(productPage.getNumber())
                .isLast(productPage.isLast())
                .totalElements(productPage.getTotalElements())
                .build();
    }

    public ProductResponse getProductById(Long id) {
        Product product = getProduct(id);

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .createdOn(product.getCreatedOn())
                .createdBy(product.getCreatedBy().getFullname())
                .modifiedBy(product.getModifiedBy() != null ? product.getModifiedBy().getEmail() : null)
                .modifiedOn(product.getModifiedOn())
                .build();

    }

    public ProductUpdateResponse updateProduct(Long id, ProductRequest productRequest) {
        Product product = getProduct(id);

        User currentUser = getCurrentUser();

        product.setName(productRequest.getName());
        product.setModifiedBy(currentUser);

        Product updatedProduct = productRepository.save(product);

        return ProductUpdateResponse.builder()
                .id(updatedProduct.getId())
                .name(updatedProduct.getName())
                .modifiedBy(updatedProduct.getModifiedBy().getFullname())
                .modifiedOn(updatedProduct.getModifiedOn())
                .build();

    }

    private ProductListResponse mapToListResponse(Product product) {
        return ProductListResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .build();
    }

    public void deleteProduct(Long id) {
        Product product = getProduct(id);
        productRepository.delete(product);
    }

    public List<ItemResponse> getItemsByProductId(Long productId) {
        getProduct(productId);

        List<Item> items = itemRepository.findByProductId(productId);

        List<ItemResponse> itemResponses = new ArrayList<>();

        for (Item item : items) {
            itemResponses.add(mapToItemResponse(item));
        }

        return itemResponses;
    }

}
