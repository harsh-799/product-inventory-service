package com.harsh.product.inventory.service;

import com.harsh.product.inventory.dto.request.ItemRequest;
import com.harsh.product.inventory.dto.request.ProductRequest;
import com.harsh.product.inventory.dto.response.ItemResponse;
import com.harsh.product.inventory.dto.response.ProductCreateResponse;
import com.harsh.product.inventory.dto.response.ProductPageResponse;
import com.harsh.product.inventory.dto.response.ProductResponse;
import com.harsh.product.inventory.dto.response.ProductUpdateResponse;
import com.harsh.product.inventory.entity.Item;
import com.harsh.product.inventory.entity.Product;
import com.harsh.product.inventory.entity.User;
import com.harsh.product.inventory.enums.Role;
import com.harsh.product.inventory.exception.ProductNotFoundException;
import com.harsh.product.inventory.repository.ItemRepository;
import com.harsh.product.inventory.repository.ProductRepository;
import com.harsh.product.inventory.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ItemRepository itemRepository;

    @InjectMocks
    private ProductService productService;

    private User testUser;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("user@example.com")
                .fullname("Test User")
                .role(Role.USER)
                .build();

        testProduct = Product.builder()
                .id(1L)
                .name("Test Product")
                .createdBy(testUser)
                .createdOn(LocalDateTime.now())
                .build();

        Authentication auth = new UsernamePasswordAuthenticationToken("user@example.com", null);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should return paginated products successfully")
    void getAllProducts_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> page = new PageImpl<>(List.of(testProduct), pageable, 1);

        when(productRepository.findAll(pageable)).thenReturn(page);

        ProductPageResponse response = productService.getAllProducts(pageable);

        assertNotNull(response);
        assertEquals(1, response.getProducts().size());
        assertEquals(0, response.getPage());
        assertEquals(1, response.getTotalElements());
        assertTrue(response.getIsLast());
        assertEquals("Test Product", response.getProducts().get(0).getName());
        verify(productRepository).findAll(pageable);
    }

    @Test
    @DisplayName("Should return product by ID when product exists")
    void getProductById_Success() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        ProductResponse response = productService.getProductById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Test Product", response.getName());
        assertEquals("Test User", response.getCreatedBy());
        verify(productRepository).findById(1L);
    }

    @Test
    @DisplayName("Should throw ProductNotFoundException when product ID does not exist")
    void getProductById_NotFound() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> productService.getProductById(999L));
        verify(productRepository).findById(999L);
    }

    @Test
    @DisplayName("Should create and persist product with current user as createdBy")
    void createProduct_Success() {
        ProductRequest request = new ProductRequest("New Product");

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
        when(productRepository.save(any(Product.class))).thenReturn(
                Product.builder().id(2L).name("New Product").createdBy(testUser).build()
        );

        ProductCreateResponse response = productService.createProduct(request);

        assertNotNull(response);
        assertEquals(2L, response.getId());
        assertEquals("New Product", response.getName());
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("Should update product and set modifiedBy to current user")
    void updateProduct_Success() {
        ProductRequest request = new ProductRequest("Updated Product");

        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        ProductUpdateResponse response = productService.updateProduct(1L, request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Updated Product", response.getName());
        verify(productRepository).save(testProduct);
    }

    @Test
    @DisplayName("Should delete product when product exists")
    void deleteProduct_Success() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        productService.deleteProduct(1L);

        verify(productRepository).delete(testProduct);
    }

    @Test
    @DisplayName("Should throw ProductNotFoundException when deleting non-existent product")
    void deleteProduct_NotFound() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> productService.deleteProduct(999L));
        verify(productRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Should return items associated with existing product")
    void getItemsByProductId_Success() {
        Item item1 = Item.builder().id(101L).product(testProduct).quantity(5).build();
        Item item2 = Item.builder().id(102L).product(testProduct).quantity(10).build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(itemRepository.findByProductId(1L)).thenReturn(List.of(item1, item2));

        List<ItemResponse> items = productService.getItemsByProductId(1L);

        assertNotNull(items);
        assertEquals(2, items.size());
        assertEquals(101L, items.get(0).getId());
        assertEquals(5, items.get(0).getQuantity());
        assertEquals(102L, items.get(1).getId());
        assertEquals(10, items.get(1).getQuantity());
    }

    @Test
    @DisplayName("Should throw ProductNotFoundException when fetching items for non-existent product")
    void getItemsByProductId_ProductNotFound() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> productService.getItemsByProductId(999L));
        verify(itemRepository, never()).findByProductId(any());
    }

    @Test
    @DisplayName("Should create item for existing product")
    void createItem_Success() {
        ItemRequest request = new ItemRequest(20);
        Item savedItem = Item.builder().id(50L).product(testProduct).quantity(20).build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(itemRepository.save(any(Item.class))).thenReturn(savedItem);

        ItemResponse response = productService.createItem(1L, request);

        assertNotNull(response);
        assertEquals(50L, response.getId());
        assertEquals(20, response.getQuantity());
        verify(itemRepository).save(any(Item.class));
    }

    @Test
    @DisplayName("Should throw ProductNotFoundException when creating item for non-existent product")
    void createItem_ProductNotFound() {
        ItemRequest request = new ItemRequest(20);

        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> productService.createItem(999L, request));
        verify(itemRepository, never()).save(any());
    }
}
