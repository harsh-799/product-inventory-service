package com.harsh.product.inventory.integration;

import com.harsh.product.inventory.dto.request.ItemRequest;
import com.harsh.product.inventory.dto.request.ProductRequest;
import com.harsh.product.inventory.dto.request.RegisterRequest;
import com.harsh.product.inventory.dto.response.ItemResponse;
import com.harsh.product.inventory.dto.response.ProductCreateResponse;
import com.harsh.product.inventory.dto.response.ProductPageResponse;
import com.harsh.product.inventory.dto.response.RegisterResponse;
import com.harsh.product.inventory.entity.Item;
import com.harsh.product.inventory.entity.Product;
import com.harsh.product.inventory.entity.User;
import com.harsh.product.inventory.enums.Role;
import com.harsh.product.inventory.repository.ItemRepository;
import com.harsh.product.inventory.repository.ProductRepository;
import com.harsh.product.inventory.repository.UserRepository;
import com.harsh.product.inventory.service.AuthService;
import com.harsh.product.inventory.service.ProductService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class InventoryIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private ProductService productService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = userRepository.findByEmail("admin@test.com").orElseGet(() -> {
            User user = User.builder()
                    .email("admin@test.com")
                    .fullname("Admin User")
                    .password(passwordEncoder.encode("AdminPass123!"))
                    .role(Role.ADMIN)
                    .build();
            return userRepository.save(user);
        });

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(testUser.getEmail(), null)
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Integration: Register a new user and verify H2 persistence, password encoding, and default role USER")
    void testUserRegistration() {
        RegisterRequest request = new RegisterRequest("Jane Doe", "janedoe@test.com", "SecurePassword123!");

        RegisterResponse response = authService.register(request);

        assertNotNull(response);
        assertTrue(response.getSuccess());

        Optional<User> persistedUser = userRepository.findByEmail("janedoe@test.com");
        assertTrue(persistedUser.isPresent());
        User user = persistedUser.get();
        assertEquals("Jane Doe", user.getFullname());
        assertEquals(Role.USER, user.getRole());
        assertNotEquals("SecurePassword123!", user.getPassword());
        assertTrue(passwordEncoder.matches("SecurePassword123!", user.getPassword()));
    }

    @Test
    @DisplayName("Integration: Create a product and verify it can be retrieved from the H2 database")
    void testProductPersistence() {
        ProductRequest request = new ProductRequest("Gaming Monitor");

        ProductCreateResponse createResponse = productService.createProduct(request);
        assertNotNull(createResponse);
        assertNotNull(createResponse.getId());

        Optional<Product> foundProduct = productRepository.findById(createResponse.getId());
        assertTrue(foundProduct.isPresent());
        assertEquals("Gaming Monitor", foundProduct.get().getName());
        assertEquals("admin@test.com", foundProduct.get().getCreatedBy().getEmail());
    }

    @Test
    @DisplayName("Integration: Insert multiple products and verify pagination and total count in H2")
    void testProductPagination() {
        productService.createProduct(new ProductRequest("Product Alpha"));
        productService.createProduct(new ProductRequest("Product Beta"));
        productService.createProduct(new ProductRequest("Product Gamma"));

        ProductPageResponse pageResponse = productService.getAllProducts(PageRequest.of(0, 2));

        assertNotNull(pageResponse);
        assertEquals(0, pageResponse.getPage());
        assertEquals(2, pageResponse.getProducts().size());
        assertTrue(pageResponse.getTotalElements() >= 3);
    }

    @Test
    @DisplayName("Integration: Create product and associated item, verify item persistence and retrieval from H2")
    void testItemPersistence() {
        ProductCreateResponse productResponse = productService.createProduct(new ProductRequest("Mechanical Keyboard"));
        Long productId = productResponse.getId();

        ItemResponse itemResponse = productService.createItem(productId, new ItemRequest(25));
        assertNotNull(itemResponse);
        assertNotNull(itemResponse.getId());
        assertEquals(25, itemResponse.getQuantity());

        List<ItemResponse> items = productService.getItemsByProductId(productId);
        assertFalse(items.isEmpty());
        assertEquals(1, items.size());
        assertEquals(25, items.get(0).getQuantity());

        List<Item> dbItems = itemRepository.findByProductId(productId);
        assertEquals(1, dbItems.size());
        assertEquals(25, dbItems.get(0).getQuantity());
        assertEquals(productId, dbItems.get(0).getProduct().getId());
    }
}
