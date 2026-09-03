# Product Inventory Service

A production-grade RESTful web service built with **Spring Boot 4 / Java 21**, implementing secure product and inventory item management with **Role-Based Access Control (RBAC)**, **JWT Authentication**, and **Opaque Refresh Token Rotation**.

---

## Table of Contents
1. [Project Overview](#1-project-overview)
2. [Features](#2-features)
3. [Tech Stack](#3-tech-stack)
4. [Architecture & Project Structure](#4-architecture--project-structure)
5. [API Versioning](#5-api-versioning)
6. [Authentication & Authorization](#6-authentication--authorization)
7. [JWT Access Token](#7-jwt-access-token)
8. [Refresh Token + Rotation](#8-refresh-token--rotation)
9. [Role-Based Access Control (RBAC)](#9-role-based-access-control-rbac)
10. [Product APIs](#10-product-apis)
11. [Item APIs](#11-item-apis)
12. [Validation](#12-validation)
13. [Pagination](#13-pagination)
14. [Standardized Error Responses](#14-standardized-error-responses)
15. [Database Schema & Relationships](#15-database-schema--relationships)
16. [CORS Configuration](#16-cors-configuration)
17. [HTTPS Enforcement & Production Readiness](#17-https-enforcement--production-readiness)
18. [Async Processing Decision](#18-async-processing-decision)
19. [Running Locally](#19-running-locally)
20. [Running with Docker](#20-running-with-docker)
21. [Test Execution](#21-test-execution)
22. [Swagger / OpenAPI Documentation](#22-swagger--openapi-documentation)
23. [Default Evaluation Admin Credentials](#23-default-evaluation-admin-credentials)
24. [Example API Flow & Payloads](#24-example-api-flow--payloads)

---

## 1. Project Overview
The **Product Inventory Service** is designed to provide robust product catalog and stock inventory tracking capabilities for enterprise applications. It adheres to RESTful best practices, stateless security, comprehensive database normalization, and containerized deployment with Docker and Docker Compose.

---

## 2. Features
- **Stateless Authentication**: Signed JWT Bearer tokens for short-lived authorization.
- **Refresh Token Rotation**: Opaque database-persisted refresh tokens with strict single-use rotation and reuse detection.
- **Role-Based Authorization (RBAC)**: Fine-grained access control segregating `USER` and `ADMIN` privileges.
- **Product & Inventory Management**: CRUD operations on products and 1-to-many relationship tracking with inventory items.
- **Pagination**: Built-in Spring Data pagination for product catalog browsing.
- **Jakarta Bean Validation**: Strict request body validation with descriptive error reporting.
- **Global Exception Handling**: Standardized, clean API response envelope for both success and error outcomes.
- **OpenAPI / Swagger UI**: Interactive API documentation pre-configured with JWT Bearer authentication.
- **Zero-Config Docker Compose**: Starts PostgreSQL and the Spring Boot application simultaneously with health check dependency.

---

## 3. Tech Stack
- **Java**: 21 (LTS)
- **Framework**: Spring Boot 4.1.1
- **Security**: Spring Security 6 & JJWT (io.jsonwebtoken 0.13.0)
- **Data & Persistence**: Spring Data JPA, Hibernate 7
- **Database (Production/Docker)**: PostgreSQL 16
- **Database (Testing)**: H2 In-Memory Database
- **API Documentation**: SpringDoc OpenAPI 3.1.0 / Swagger UI
- **Testing**: JUnit 5, Mockito, Spring Boot Test, Spring Test (MockMvc)
- **Containerization**: Docker & Docker Compose (Multi-stage build)

---

## 4. Architecture & Project Structure
The application follows standard Layered Architecture:

```
src/main/java/com/harsh/product/inventory/
├── config/                  # Security, Swagger, CORS, and Data Initializer
│   ├── DataInitializer.java
│   ├── OpenApiConfig.java
│   └── SecurityConfig.java
├── controller/              # REST Controllers (Auth, Product)
│   ├── AuthController.java
│   └── ProductController.java
├── dto/
│   ├── request/             # Request payloads (Login, Register, Refresh, Product, Item)
│   └── response/            # Standardized API and data transfer envelopes
├── entity/                  # JPA Entities (User, Product, Item, RefreshToken)
├── enums/                   # Role enum (ADMIN, USER)
├── exception/               # Custom Domain Exceptions & @RestControllerAdvice
├── filter/                  # JwtAuthenticationFilter
├── repository/              # Spring Data JPA Repositories
├── security/                # CustomUserDetails and UserDetails implementation
└── service/                 # Core Business Logic (Auth, Jwt, Product, RefreshToken)
```

---

## 5. API Versioning
All business domain resources are namespaced under the `/api/v1/` prefix:
- `/api/v1/products/**`
- `/api/v1/products/{productId}/items`

Public authentication endpoints are grouped under `/auth/`:
- `/auth/register`
- `/auth/login`
- `/auth/refresh`

---

## 6. Authentication & Authorization
- Passwords are encrypted using **BCrypt** with adaptive salting.
- Upon login, the user receives an `accessToken` (JWT) and a `refreshToken` (opaque UUID string).
- Authenticated requests send the token in the HTTP Header:
  ```http
  Authorization: Bearer <accessToken>
  ```
- Unauthenticated requests to protected endpoints return `401 Unauthorized`.
- Authenticated requests lacking necessary privileges return `403 Forbidden`.

---

## 7. JWT Access Token
- **Algorithm**: HMAC-SHA256 (`HS256`)
- **Subject**: User email address
- **Custom Claims**:
  - `role`: Role of the user (`USER` or `ADMIN`)
- **Expiration**: Short-lived (15 minutes) for minimal blast radius.

---

## 8. Refresh Token + Rotation
To prevent replay attacks and token theft, the service enforces **Refresh Token Rotation**:
1. **Opaque Tokens**: 128-bit cryptographically secure random UUID tokens stored in the `refresh_tokens` database table.
2. **Revocation & Expiration Check**:
   - If a refresh token does not exist $\rightarrow$ returns `401 Unauthorized` (`Invalid refresh token`).
   - If a refresh token was already revoked $\rightarrow$ returns `401 Unauthorized` (`Refresh token has already been used or revoked`).
   - If a refresh token is expired $\rightarrow$ returns `401 Unauthorized` (`Refresh token has expired. Please log in again`).
3. **Rotation Sequence**:
   - When a valid refresh token $R_1$ is submitted to `/auth/refresh`:
     - $R_1$ is immediately flagged as `revoked = true` in PostgreSQL.
     - A new JWT access token $A_2$ is generated.
     - A brand new refresh token $R_2$ is persisted and returned.
   - Any subsequent attempt to present $R_1$ is instantly rejected with `TokenAlreadyUsedException`.

---

## 9. Role-Based Access Control (RBAC)

| Endpoint | Method | Permitted Roles | Description |
|---|---|---|---|
| `/auth/**` | POST | Public | Registration, Login, Token Refresh |
| `/swagger-ui/**`, `/v3/api-docs/**` | GET | Public | OpenAPI Documentation |
| `/api/v1/products` | GET | `USER`, `ADMIN` | List paginated products |
| `/api/v1/products/{id}` | GET | `USER`, `ADMIN` | Retrieve single product |
| `/api/v1/products/{id}/items` | GET | `USER`, `ADMIN` | View inventory items for product |
| `/api/v1/products` | POST | `ADMIN` only | Create a new product |
| `/api/v1/products/{id}` | PUT | `ADMIN` only | Update product metadata |
| `/api/v1/products/{id}` | DELETE | `ADMIN` only | Remove product from catalog |
| `/api/v1/products/{id}/items` | POST | `ADMIN` only | Add inventory item to product |

---

## 10. Product APIs
- `POST /api/v1/products`: Creates a product associated with the authenticated user (`createdBy`).
- `GET /api/v1/products`: Returns paginated product cards with pagination metadata.
- `GET /api/v1/products/{id}`: Returns complete details of a specific product.
- `PUT /api/v1/products/{id}`: Updates product name and assigns `modifiedBy` and `modifiedOn`.
- `DELETE /api/v1/products/{id}`: Deletes product (returns HTTP 204).

---

## 11. Item APIs
Items represent stock quantity units associated with a parent `Product` ($1 \text{ Product} \rightarrow * \text{ Items}$):
- `POST /api/v1/products/{productId}/items`: Adds an inventory stock record with `quantity > 0`.
- `GET /api/v1/products/{productId}/items`: Lists all stock items linked to that product ID.

---

## 12. Validation
Jakarta Bean Validation (`@Valid`) is applied to incoming request bodies:
- `RegisterRequest`:
  - `fullname`: `@NotBlank`
  - `email`: `@NotBlank`, `@Email`
  - `password`: `@NotBlank`, `@Size(min = 6)`
- `LoginRequest`:
  - `email`: `@NotBlank`, `@Email`
  - `password`: `@NotBlank`
- `ProductRequest`:
  - `name`: `@NotBlank(message = "name is required")`
- `ItemRequest`:
  - `quantity`: `@NotNull`, `@Min(value = 1, message = "quantity must be greater than 0")`

Validation failures return **HTTP 400 Bad Request** with structured field-level errors.

---

## 13. Pagination
The product listing endpoint supports Spring Data `Pageable` query parameters:
- `page`: 0-indexed page number (default: `0`)
- `size`: Items per page (default: `10`)

Example:
`GET /api/v1/products?page=0&size=5`

---

## 14. Standardized Error Responses
All API responses follow consistent DTO models.

### Standard Success Response (`ApiResponse<T>`):
```json
{
  "success": true,
  "message": "Operation description",
  "data": { ... }
}
```

### Standard Error Response (`ErrorDetails`):
```json
{
  "success": false,
  "message": "Product not found with id: 42"
}
```

### Validation Error Response:
```json
{
  "success": false,
  "message": "Validation failed",
  "errors": [
    {
      "field": "quantity",
      "message": "quantity must be greater than 0"
    }
  ]
}
```

---

## 15. Database Schema & Relationships

```
+--------------------+        1 : N         +-------------------+
|      products      |--------------------->|       item        |
+--------------------+                      +-------------------+
| id (PK)            |                      | id (PK)           |
| product_name       |                      | product_id (FK)   |
| created_by (FK)    |                      | quantity          |
| created_on         |                      +-------------------+
| modified_by (FK)   |
| modified_on        |
+--------------------+
          |
          | N : 1
          v
+--------------------+        1 : N         +-------------------+
|       Users        |--------------------->|  refresh_tokens   |
+--------------------+                      +-------------------+
| id (PK)            |                      | id (PK)           |
| email (Unique)     |                      | token (Unique)    |
| password (BCrypt)  |                      | user_id (FK)      |
| fullname           |                      | expires_at        |
| role (ADMIN/USER)  |                      | revoked (boolean) |
| created_at         |                      +-------------------+
+--------------------+
```

---

## 16. CORS Configuration
Cross-Origin Resource Sharing (CORS) is explicitly configured in `SecurityConfig`:
- **Allowed Origin Patterns**: `*` (flexible support for any future frontend host, e.g., React/Vue/Angular on localhost or deployed domains).
- **Allowed Methods**: `GET`, `POST`, `PUT`, `DELETE`, `OPTIONS`, `PATCH`.
- **Allowed Headers**: `*` (supports `Authorization`, `Content-Type`, etc.).
- **Allow Credentials**: `true` (supports cookie/auth header propagation).

---

## 17. HTTPS Enforcement & Production Readiness
- **Development & Evaluation Environment**: Runs over standard HTTP port `8080` out-of-the-box so evaluators can inspect and test without configuring SSL certificates or trusting self-signed certificates.
- **Production Deployment Pattern**:
  In modern cloud architectures (AWS ECS/ALB, Kubernetes Ingress, Nginx reverse proxy), TLS termination is performed at the reverse proxy / API Gateway layer. The Spring Boot backend sits behind the reverse proxy with:
  ```properties
  server.forward-headers-strategy=native
  ```
  Spring Security sends the `Strict-Transport-Security` (HSTS) header on secure channels to enforce browser-side HTTPS redirection.

---

## 18. Async Processing Decision
- **Architecture Evaluation**: The current service consists of synchronous, atomic transactional CRUD operations (User registration, authentication, Product & Item persistence, and Refresh token rotation).
- **Decision**: Asynchronous processing (`@Async`) was **deliberately not introduced** into the core CRUD flow. Adding `@Async` arbitrarily to database transactions introduces unnecessary race conditions, potential uncommitted reads, and complexity without actual performance benefit. If long-running I/O jobs (such as email delivery or invoice generation) are added in the future, Spring TaskExecutor/async messaging (RabbitMQ/Kafka) should be introduced.

---

## 19. Running Locally

### Prerequisites:
- Java 21 JDK installed
- PostgreSQL running locally on port 5432 (or H2 test profile)

### Steps:
1. Clone the repository:
   ```bash
   git clone <repository-url>
   cd product-inventory-service
   ```
2. Set environment variables (or rely on default local configuration):
   ```bash
   # Windows PowerShell
   $env:DATABASE_URL="jdbc:postgresql://localhost:5432/product_inventory_db"
   $env:DATABASE_USERNAME="postgres"
   $env:DATABASE_PASSWORD="postgres"
   ```
3. Run with Maven Wrapper:
   ```bash
   # Linux/macOS
   ./mvnw spring-boot:run

   # Windows
   .\mvnw spring-boot:run
   ```

---

## 20. Running with Docker

The project provides a fully automated **Docker Compose** configuration with PostgreSQL and Spring Boot.

### Quick Start:
```bash
git clone <repository-url>
cd product-inventory-service
docker compose up --build
```

The database container will start, verify its health check, and the Spring Boot application container will launch and automatically initialize the schema and default admin account.

### Stopping Containers:
```bash
docker compose down
```

### Stopping and Clearing Database Volume:
```bash
docker compose down -v
```

---

## 21. Test Execution
The test suite includes **35 tests** covering:
- **Unit Tests (JUnit 5 + Mockito)**: Services and token rotation logic.
- **Integration Tests (Spring Boot Test + H2)**: Real database persistence, encoding, and pagination.
- **Controller Tests (MockMvc)**: HTTP endpoint status codes and JSON response envelopes.

### Run All Tests:
```bash
# Linux/macOS
./mvnw test

# Windows
.\mvnw test
```

---

## 22. Swagger / OpenAPI Documentation
Once the application is running, open your web browser:

**Swagger UI URL**:
[http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)

**OpenAPI v3 JSON**:
[http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

To test authenticated endpoints in Swagger:
1. Call `POST /auth/login` and copy the `accessToken`.
2. Click the green **Authorize** button at the top right of Swagger UI.
3. Enter the token into the Bearer input and click **Authorize**.

---

## 23. Default Evaluation Admin Credentials
When the application starts in Docker or with default configuration, a demo Administrator account is automatically provisioned by `DataInitializer`:

- **Email / Username**: `admin@inventory.com`
- **Password**: `Admin@123`
- **Role**: `ADMIN`

> **Note**: These are strictly for evaluation/demo purposes. In production, override `DEF_ADMIN_USERNAME` and `DEF_ADMIN_PASSWORD` via environment variables.

---

## 24. Example API Flow & Payloads

### Step 1: Register a New User (`USER` role)
- **URL**: `POST /auth/register`
- **Request Body**:
```json
{
  "fullname": "John Doe",
  "email": "john@example.com",
  "password": "Password123!"
}
```
- **Response** (`201 Created`):
```json
{
  "success": true,
  "message": "User Registered successfully."
}
```

---

### Step 2: Login
- **URL**: `POST /auth/login`
- **Request Body**:
```json
{
  "email": "john@example.com",
  "password": "Password123!"
}
```
- **Response** (`200 OK`):
```json
{
  "status": true,
  "message": "Logged in successfully",
  "accessToken": "eyJhbGciOiJIUzI1NiIsIn...",
  "refreshToken": "e30e12d4-1a3b-4876-b9dc-6901fdf79a48",
  "fullName": "John Doe",
  "email": "john@example.com"
}
```

---

### Step 3: Refresh Access Token (Token Rotation)
- **URL**: `POST /auth/refresh`
- **Request Body**:
```json
{
  "refreshToken": "e30e12d4-1a3b-4876-b9dc-6901fdf79a48"
}
```
- **Response** (`200 OK`):
```json
{
  "success": true,
  "message": "Token refreshed successfully",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsIn...",
    "refreshToken": "7c8808d7-567a-4299-8e41-0f72782b7dc1"
  }
}
```
*(Notice that presenting `e30e12d4-1a3b-4876-b9dc-6901fdf79a48` a second time will fail with HTTP 401 Unauthorized).*

---

### Step 4: Create a Product (`ADMIN` only)
- **URL**: `POST /api/v1/products`
- **Headers**: `Authorization: Bearer <ADMIN_JWT>`
- **Request Body**:
```json
{
  "name": "Wireless Mechanical Keyboard"
}
```
- **Response** (`201 Created`):
```json
{
  "success": true,
  "message": "Product created successfully",
  "data": {
    "id": 1,
    "name": "Wireless Mechanical Keyboard"
  }
}
```

---

### Step 5: Get Products with Pagination (`USER` or `ADMIN`)
- **URL**: `GET /api/v1/products?page=0&size=10`
- **Headers**: `Authorization: Bearer <JWT>`
- **Response** (`200 OK`):
```json
{
  "success": true,
  "message": "Products fetched successfully",
  "data": {
    "products": [
      {
        "id": 1,
        "name": "Wireless Mechanical Keyboard",
        "createdBy": "Default Admin",
        "createdOn": "2026-09-04T01:00:00",
        "modifiedBy": null,
        "modifiedOn": null
      }
    ],
    "page": 0,
    "isLast": true,
    "totalElements": 1
  }
}
```

---

### Step 6: Get Product by ID (`USER` or `ADMIN`)
- **URL**: `GET /api/v1/products/1`
- **Headers**: `Authorization: Bearer <JWT>`
- **Response** (`200 OK`):
```json
{
  "success": true,
  "message": "Product fetched successfully",
  "data": {
    "id": 1,
    "name": "Wireless Mechanical Keyboard",
    "createdBy": "Default Admin",
    "createdOn": "2026-09-04T01:00:00",
    "modifiedBy": null,
    "modifiedOn": null
  }
}
```

---

### Step 7: Update Product (`ADMIN` only)
- **URL**: `PUT /api/v1/products/1`
- **Headers**: `Authorization: Bearer <ADMIN_JWT>`
- **Request Body**:
```json
{
  "name": "RGB Wireless Mechanical Keyboard"
}
```
- **Response** (`200 OK`):
```json
{
  "success": true,
  "message": "Product updated successfully",
  "data": {
    "id": 1,
    "name": "RGB Wireless Mechanical Keyboard",
    "modifiedBy": "Default Admin",
    "modifiedOn": "2026-09-04T01:30:00"
  }
}
```

---

### Step 8: Add Inventory Items to Product (`ADMIN` only)
- **URL**: `POST /api/v1/products/1/items`
- **Headers**: `Authorization: Bearer <ADMIN_JWT>`
- **Request Body**:
```json
{
  "quantity": 25
}
```
- **Response** (`201 Created`):
```json
{
  "success": true,
  "message": "Item created successfully",
  "data": {
    "id": 1,
    "quantity": 25
  }
}
```

---

### Step 9: Get Items for Product (`USER` or `ADMIN`)
- **URL**: `GET /api/v1/products/1/items`
- **Headers**: `Authorization: Bearer <JWT>`
- **Response** (`200 OK`):
```json
{
  "success": true,
  "message": "Items fetched successfully",
  "data": [
    {
      "id": 1,
      "quantity": 25
    }
  ]
}
```

---

### Step 10: Delete Product (`ADMIN` only)
- **URL**: `DELETE /api/v1/products/1`
- **Headers**: `Authorization: Bearer <ADMIN_JWT>`
- **Response** (`204 No Content` - Empty Body)
