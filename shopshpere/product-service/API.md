# ShopSphere Product Service API Documentation

**Base URL**: `http://localhost:8082`

---

## 1. Overview & Conventions

### Headers
- `Content-Type: application/json`
- `Accept: application/json`

### Error Response Payload
All non-2xx responses return a consistent error body:
```json
{
  "timestamp": "2026-08-09T06:49:00.123456Z",
  "status": 404,
  "error": "RESOURCE_NOT_FOUND",
  "message": "Product not found with id: 99",
  "path": "/api/products/99",
  "traceId": null
}
```

### Paginated Response Structure
List endpoints return:
```json
{
  "content": [ ... ],
  "page": 0,
  "size": 20,
  "totalElements": 150,
  "totalPages": 8,
  "last": false
}
```

---

## 2. Product APIs

### 2.1 Create Product

- **Method**: `POST`
- **Path**: `/api/products`
- **Status**: `201 Created`

#### Request Body (`CreateProductRequest`)
| Field | Type | Required | Description |
| :--- | :--- | :--- | :--- |
| `sku` | `String` | Yes | Unique product SKU. Must be non-blank |
| `name` | `String` | Yes | Non-blank product name |
| `description` | `String` | No | Product description |
| `price` | `BigDecimal` | Yes | Must be `>= 0.01`. Never `double` or `float` |
| `currency` | `String` | Yes | Non-blank currency code e.g. `USD` |
| `status` | `ProductStatus` | No | `ACTIVE`, `INACTIVE`, or `DISCONTINUED`. Defaults to `ACTIVE` |
| `categoryIds` | `Set<Long>` | Yes | At least one valid category ID |

**Example Request**:
```json
{
  "sku": "LAPTOP-RTX-4090",
  "name": "Gaming Laptop RTX 4090",
  "description": "Flagship gaming laptop with RTX 4090 GPU",
  "price": 2999.99,
  "currency": "USD",
  "status": "ACTIVE",
  "categoryIds": [1, 3]
}
```

**Example Response (`201 Created`)**:
```json
{
  "id": 1,
  "sku": "LAPTOP-RTX-4090",
  "name": "Gaming Laptop RTX 4090",
  "description": "Flagship gaming laptop with RTX 4090 GPU",
  "price": 2999.99,
  "currency": "USD",
  "status": "ACTIVE",
  "createdAt": "2026-08-09T06:49:00.123456Z",
  "updatedAt": "2026-08-09T06:49:00.123456Z",
  "categories": [
    { "id": 1, "name": "Laptops", "status": "ACTIVE", ... },
    { "id": 3, "name": "Gaming", "status": "ACTIVE", ... }
  ]
}
```

**Error Responses**:
- `400 Bad Request` — Validation failure (price <= 0, empty SKU, etc.)
- `409 Conflict` — SKU already registered

---

### 2.2 Get Product by ID

- **Method**: `GET`
- **Path**: `/api/products/{productId}`
- **Status**: `200 OK`

**Example Response**:
```json
{
  "id": 1,
  "sku": "LAPTOP-RTX-4090",
  "name": "Gaming Laptop RTX 4090",
  "price": 2999.99,
  "currency": "USD",
  "status": "ACTIVE",
  "categories": [ ... ],
  "createdAt": "2026-08-09T06:49:00.123456Z",
  "updatedAt": "2026-08-09T06:49:00.123456Z"
}
```

**Error Responses**:
- `404 Not Found` — Product does not exist

---

### 2.3 List Products (Paginated, Filterable)

- **Method**: `GET`
- **Path**: `/api/products`
- **Status**: `200 OK`

#### Query Parameters
| Parameter | Type | Default | Description |
| :--- | :--- | :--- | :--- |
| `page` | `int` | `0` | Zero-based page index |
| `size` | `int` | `20` | Page size |
| `sort` | `String` | `name,asc` | Sort by field and direction e.g. `price,desc` |
| `keyword` | `String` | — | Searches product `name` and `description` (MySQL LIKE) |
| `status` | `ProductStatus` | `ACTIVE` | Filter by `ACTIVE`, `INACTIVE`, or `DISCONTINUED` |
| `categoryId` | `Long` | — | Filter by category ID |

**Example Requests**:
```http
GET /api/products?page=0&size=20&sort=name,asc
GET /api/products?keyword=laptop&categoryId=1
GET /api/products?status=INACTIVE&page=0&size=10
GET /api/products?sort=price,desc
```

**Example Response (`200 OK`)**:
```json
{
  "content": [
    {
      "id": 1,
      "sku": "LAPTOP-RTX-4090",
      "name": "Gaming Laptop RTX 4090",
      "price": 2999.99,
      "currency": "USD",
      "status": "ACTIVE",
      "categories": [ ... ]
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "last": true
}
```

---

### 2.4 Update Product

- **Method**: `PUT`
- **Path**: `/api/products/{productId}`
- **Status**: `200 OK`

**Note**: SKU is immutable and cannot be changed on update.

**Example Request**:
```json
{
  "name": "Gaming Laptop RTX 4090 Ti",
  "description": "Updated description",
  "price": 3199.99,
  "currency": "USD",
  "status": "ACTIVE",
  "categoryIds": [1, 3, 5]
}
```

**Error Responses**:
- `400 Bad Request` — Invalid price or missing name
- `404 Not Found` — Product not found

---

### 2.5 Delete (Deactivate) Product

- **Method**: `DELETE`
- **Path**: `/api/products/{productId}`
- **Status**: `200 OK`

**Note**: Products are **soft-deleted** (status changed to `INACTIVE`) to preserve historical order and audit data.

**Example Response (`200 OK`)**:
```json
{
  "id": 1,
  "sku": "LAPTOP-RTX-4090",
  "status": "INACTIVE",
  ...
}
```

---

## 3. Category APIs

### 3.1 Create Category

- **Method**: `POST`
- **Path**: `/api/categories`
- **Status**: `201 Created`

| Field | Type | Required | Description |
| :--- | :--- | :--- | :--- |
| `name` | `String` | Yes | Unique, non-blank category name |
| `description` | `String` | No | Category description |
| `status` | `CategoryStatus` | No | `ACTIVE` or `INACTIVE`. Defaults to `ACTIVE` |

**Example Request**:
```json
{
  "name": "Laptops",
  "description": "Portable computing devices",
  "status": "ACTIVE"
}
```

**Example Response (`201 Created`)**:
```json
{
  "id": 1,
  "name": "Laptops",
  "description": "Portable computing devices",
  "status": "ACTIVE",
  "createdAt": "2026-08-09T06:48:00.000000Z",
  "updatedAt": "2026-08-09T06:48:00.000000Z"
}
```

**Error Responses**:
- `409 Conflict` — Category name already exists

---

### 3.2 List All Categories

- **Method**: `GET`
- **Path**: `/api/categories`
- **Status**: `200 OK`

**Example Response**:
```json
[
  {
    "id": 1,
    "name": "Laptops",
    "description": "Portable computing devices",
    "status": "ACTIVE",
    "createdAt": "...",
    "updatedAt": "..."
  }
]
```

---

### 3.3 Get Category by ID

- **Method**: `GET`
- **Path**: `/api/categories/{categoryId}`
- **Status**: `200 OK`

---

### 3.4 Update Category

- **Method**: `PUT`
- **Path**: `/api/categories/{categoryId}`
- **Status**: `200 OK`

**Example Request**:
```json
{
  "name": "Laptops & Notebooks",
  "description": "Portable computing devices",
  "status": "ACTIVE"
}
```

---

### 3.5 Delete (Deactivate) Category

- **Method**: `DELETE`
- **Path**: `/api/categories/{categoryId}`
- **Status**: `204 No Content`

**Note**: Category is **soft-deleted** (status set to `INACTIVE`).

---

## 4. Enumerations

### `ProductStatus`
| Value | Description |
| :--- | :--- |
| `ACTIVE` | Visible in normal catalog searches |
| `INACTIVE` | Hidden from standard catalog queries |
| `DISCONTINUED` | Permanently removed from catalog |

### `CategoryStatus`
| Value | Description |
| :--- | :--- |
| `ACTIVE` | Category is visible and in use |
| `INACTIVE` | Category is hidden/deactivated |
