# ShopSphere Cart Service API Documentation

**Service:** `cart-service`  
**Version:** `1.0.0-SNAPSHOT`  
**Framework:** Spring Boot 3.3.5  
**Java:** 21  
**API style:** REST / JSON  
**Configured server port:** `4003`  
**Base URL:** `http://localhost:4003`  
**Primary API base path:** `/api/cart`

> This document is generated from the current `cart-service` source code, including its controller, DTOs, services, repositories, exception handling, database migration, application configuration, and Maven dependencies. It describes the behavior that is actually implemented rather than an assumed generic cart API.

---

# 1. Overview

The ShopSphere Cart Service manages a user's shopping cart and its items.

The service currently supports:

- Getting or creating a user's active cart
- Adding products to a cart
- Increasing quantity when the same product is added again
- Updating an existing cart item's quantity
- Removing an individual cart item
- Clearing a cart
- Calculating per-item totals
- Calculating the complete cart total
- Active / checked-out / abandoned cart statuses
- User identification through the `X-User-Id` HTTP header
- Bean Validation for cart-item requests
- Consistent JSON error responses
- Actuator health, info, and metrics endpoints
- Swagger/OpenAPI dependency support

The service does **not** currently authenticate the user itself. The caller supplies the user ID through the request header.

---

# 2. API Conventions

## 2.1 Base URL

For the current local configuration:

```text
http://localhost:4003
```

All cart endpoints are under:

```text
/api/cart
```

Therefore the complete base API URL is:

```text
http://localhost:4003/api/cart
```

## 2.2 Content Type

Requests containing JSON bodies should use:

```http
Content-Type: application/json
```

Responses from the REST controllers are JSON except for the `DELETE /api/cart` endpoint, which returns no body.

## 2.3 User Identification

Every cart endpoint requires:

```http
X-User-Id: <user-id>
```

Example:

```http
X-User-Id: 101
```

The controller declares the header as optional at the Spring MVC level, but explicitly rejects requests where the header is missing.

The user ID is parsed as a `Long`.

Example:

```http
X-User-Id: 101
```

is valid.

A missing header results in:

```text
400 Bad Request
```

---

# 3. Cart Statuses

The service defines three cart statuses.

| Status | Meaning |
|---|---|
| `ACTIVE` | Current shopping cart for a user |
| `CHECKED_OUT` | Cart has been checked out |
| `ABANDONED` | Cart has been cleared/abandoned |

The API currently creates carts with:

```text
ACTIVE
```

There is no public endpoint in the current controller for transitioning a cart to `CHECKED_OUT`.

The clear-cart operation transitions the current active cart to:

```text
ABANDONED
```

---

# 4. Endpoint Summary

| Method | Endpoint | Success | Purpose |
|---|---|---:|---|
| `GET` | `/api/cart` | `200 OK` | Get or create the user's active cart |
| `POST` | `/api/cart/items` | `200 OK` | Add a product to the cart |
| `PUT` | `/api/cart/items/{itemId}` | `200 OK` | Update item quantity |
| `DELETE` | `/api/cart/items/{itemId}` | `200 OK` | Remove one cart item |
| `DELETE` | `/api/cart` | `204 No Content` | Clear/abandon the active cart |

All five endpoints require:

```http
X-User-Id: <userId>
```

---

# 5. Get Current Cart

Returns the user's active cart.

If the user does not currently have an active cart, the service automatically creates one.

## Request

```http
GET /api/cart
X-User-Id: 101
```

## Headers

| Header | Required | Type | Description |
|---|---:|---|---|
| `X-User-Id` | Yes | Long | Identifies the user whose cart is being accessed |

## Behavior

The service executes the equivalent of:

```text
find cart where userId = supplied user ID
and status = ACTIVE
```

If found, it returns the existing cart.

If not found, it creates:

```text
CartStatus.ACTIVE
```

and returns the newly created empty cart.

## Success

**HTTP 200 OK**

Example:

```json
{
  "cartId": 10,
  "userId": 101,
  "status": "ACTIVE",
  "items": [],
  "totalAmount": 0,
  "createdAt": "2026-08-09T08:00:00Z",
  "updatedAt": "2026-08-09T08:00:00Z"
}
```

## Cart with items

```json
{
  "cartId": 10,
  "userId": 101,
  "status": "ACTIVE",
  "items": [
    {
      "itemId": 25,
      "productId": 501,
      "quantity": 2,
      "unitPrice": 49.99,
      "totalPrice": 99.98,
      "createdAt": "2026-08-09T08:02:00Z",
      "updatedAt": "2026-08-09T08:02:00Z"
    }
  ],
  "totalAmount": 99.98,
  "createdAt": "2026-08-09T08:00:00Z",
  "updatedAt": "2026-08-09T08:02:00Z"
}
```

## cURL

```bash
curl -X GET http://localhost:4003/api/cart \
  -H "X-User-Id: 101"
```

---

# 6. Add Item to Cart

Adds a product to the user's active cart.

## Request

```http
POST /api/cart/items
X-User-Id: 101
Content-Type: application/json
```

## Request body

```json
{
  "productId": 501,
  "quantity": 2,
  "unitPrice": 49.99
}
```

## Request fields

| Field | Type | Required | Validation |
|---|---|---:|---|
| `productId` | Long | Yes | Must not be null |
| `quantity` | Integer | Yes | Must be at least `1` |
| `unitPrice` | BigDecimal | Yes | Must be at least `0.01` |

## Important price behavior

The current implementation accepts `unitPrice` from the client.

The source code explicitly notes that this is a temporary development behavior.

The service does **not** currently call the Product Service to obtain or verify the product's current price.

Therefore:

```json
{
  "productId": 501,
  "quantity": 2,
  "unitPrice": 49.99
}
```

stores `49.99` as the item's unit price.

For production, the recommended architecture is to retrieve the authoritative price from the Product Service or another trusted pricing component rather than trusting the client.

---

# 7. Add Item — New Product

If the product does not already exist in the user's active cart, a new cart item is created.

## Example

```http
POST /api/cart/items
X-User-Id: 101
```

```json
{
  "productId": 501,
  "quantity": 2,
  "unitPrice": 49.99
}
```

The resulting item is conceptually:

```text
productId = 501
quantity  = 2
unitPrice = 49.99
```

## Success

**HTTP 200 OK**

The endpoint returns the **complete updated cart**, not just the newly created item.

Example:

```json
{
  "cartId": 10,
  "userId": 101,
  "status": "ACTIVE",
  "items": [
    {
      "itemId": 25,
      "productId": 501,
      "quantity": 2,
      "unitPrice": 49.99,
      "totalPrice": 99.98,
      "createdAt": "2026-08-09T08:02:00Z",
      "updatedAt": "2026-08-09T08:02:00Z"
    }
  ],
  "totalAmount": 99.98,
  "createdAt": "2026-08-09T08:00:00Z",
  "updatedAt": "2026-08-09T08:02:00Z"
}
```

## cURL

```bash
curl -X POST http://localhost:4003/api/cart/items \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 101" \
  -d '{
    "productId": 501,
    "quantity": 2,
    "unitPrice": 49.99
  }'
```

---

# 8. Add Existing Product to Cart

If the specified product is already in the user's active cart, the service **accumulates the quantity** instead of creating another cart item.

For example, assume the cart contains:

```json
{
  "productId": 501,
  "quantity": 2,
  "unitPrice": 49.99
}
```

The client sends:

```json
{
  "productId": 501,
  "quantity": 3,
  "unitPrice": 49.99
}
```

The resulting quantity becomes:

```text
2 + 3 = 5
```

The resulting item is:

```json
{
  "productId": 501,
  "quantity": 5,
  "unitPrice": 49.99
}
```

## Important behavior

When an existing product is found, the service changes only the quantity.

It does **not** replace the existing `unitPrice` with the `unitPrice` supplied by the second request.

Therefore, the original stored price remains authoritative for that cart item.

This means the second request's `unitPrice` is effectively ignored when the product already exists in the cart.

---

# 9. Add Item Automatically Creates an Active Cart

`POST /api/cart/items` does not require the client to create a cart first.

If the user has no active cart, the service creates one automatically.

Therefore this sequence is valid:

```text
POST /api/cart/items
```

without first calling:

```text
GET /api/cart
```

The service performs:

```text
Find active cart
    ↓
Not found
    ↓
Create ACTIVE cart
    ↓
Add item
    ↓
Return cart
```

---

# 10. Update Cart Item Quantity

Updates the quantity of an existing cart item.

## Request

```http
PUT /api/cart/items/{itemId}
```

Example:

```http
PUT /api/cart/items/25
X-User-Id: 101
Content-Type: application/json
```

## Request body

```json
{
  "quantity": 5
}
```

## Request fields

| Field | Type | Required | Validation |
|---|---|---:|---|
| `quantity` | Integer | Yes | Must be at least `1` |

## Important behavior

Only the quantity can be updated.

The unit price cannot be changed through this endpoint.

## Ownership validation

The service first resolves the user's active cart.

It then loads the cart item by `itemId`.

The item must belong to the user's active cart.

If it belongs to another cart, the service returns:

**HTTP 400 Bad Request**

with:

```json
{
  "timestamp": "2026-08-09T08:10:00Z",
  "status": 400,
  "error": "BAD_REQUEST",
  "message": "Cart item with id: 25 does not belong to your cart",
  "path": "/api/cart/items/25",
  "traceId": null
}
```

## Success

**HTTP 200 OK**

The complete updated cart is returned.

## cURL

```bash
curl -X PUT http://localhost:4003/api/cart/items/25 \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 101" \
  -d '{
    "quantity": 5
  }'
```

---

# 11. Remove Cart Item

Removes one item from the user's active cart.

## Request

```http
DELETE /api/cart/items/{itemId}
```

Example:

```http
DELETE /api/cart/items/25
X-User-Id: 101
```

## Ownership validation

The service verifies that the item belongs to the current user's active cart.

If it does not:

**HTTP 400 Bad Request**

```json
{
  "timestamp": "2026-08-09T08:12:00Z",
  "status": 400,
  "error": "BAD_REQUEST",
  "message": "Cart item with id: 25 does not belong to your cart",
  "path": "/api/cart/items/25",
  "traceId": null
}
```

## Success

**HTTP 200 OK**

The endpoint returns the complete cart after removing the item.

Example:

```json
{
  "cartId": 10,
  "userId": 101,
  "status": "ACTIVE",
  "items": [],
  "totalAmount": 0,
  "createdAt": "2026-08-09T08:00:00Z",
  "updatedAt": "2026-08-09T08:12:00Z"
}
```

## cURL

```bash
curl -X DELETE http://localhost:4003/api/cart/items/25 \
  -H "X-User-Id: 101"
```

---

# 12. Clear Cart

Clears the user's active cart.

## Request

```http
DELETE /api/cart
X-User-Id: 101
```

## Behavior

If an active cart exists, the service:

1. Deletes all cart items belonging to the cart.
2. Changes the cart status from `ACTIVE` to `ABANDONED`.
3. Saves the cart.
4. Returns no response body.

Conceptually:

```text
ACTIVE CART
    |
    +-- delete all cart items
    |
    +-- status = ABANDONED
    |
    +-- save cart
```

## Success

**HTTP 204 No Content**

There is no response body.

## cURL

```bash
curl -X DELETE http://localhost:4003/api/cart \
  -H "X-User-Id: 101"
```

## No active cart

If the user has no active cart, the service performs no operation and still returns:

```text
204 No Content
```

It does not return a `404`.

---

# 13. Behavior After Clearing a Cart

After:

```http
DELETE /api/cart
```

the previous cart becomes:

```text
ABANDONED
```

If the user subsequently calls:

```http
GET /api/cart
```

the service looks specifically for:

```text
status = ACTIVE
```

It will not reuse the abandoned cart.

Instead, it creates a new active cart.

Therefore:

```text
Cart #10 -> ABANDONED

GET /api/cart

Cart #11 -> ACTIVE
```

This allows a user to start a fresh cart after clearing the previous one.

---

# 14. Cart Response

The service returns a `CartResponse`.

Schema:

```json
{
  "cartId": 10,
  "userId": 101,
  "status": "ACTIVE",
  "items": [],
  "totalAmount": 0,
  "createdAt": "2026-08-09T08:00:00Z",
  "updatedAt": "2026-08-09T08:00:00Z"
}
```

## Fields

| Field | Type | Description |
|---|---|---|
| `cartId` | Long | Cart database ID |
| `userId` | Long | User associated with the cart |
| `status` | CartStatus | `ACTIVE`, `CHECKED_OUT`, or `ABANDONED` |
| `items` | array | Items currently in the cart |
| `totalAmount` | BigDecimal | Sum of all item total prices |
| `createdAt` | Instant | Cart creation timestamp |
| `updatedAt` | Instant | Last cart update timestamp |

---

# 15. Cart Item Response

Each item is returned as a `CartItemResponse`.

```json
{
  "itemId": 25,
  "productId": 501,
  "quantity": 2,
  "unitPrice": 49.99,
  "totalPrice": 99.98,
  "createdAt": "2026-08-09T08:02:00Z",
  "updatedAt": "2026-08-09T08:02:00Z"
}
```

## Fields

| Field | Type | Description |
|---|---|---|
| `itemId` | Long | Cart item database ID |
| `productId` | Long | Product ID |
| `quantity` | Integer | Number of units |
| `unitPrice` | BigDecimal | Stored unit price |
| `totalPrice` | BigDecimal | `unitPrice × quantity` |
| `createdAt` | Instant | Item creation timestamp |
| `updatedAt` | Instant | Last item update timestamp |

---

# 16. Cart Total Calculation

The service calculates each item total using:

```text
totalPrice = unitPrice × quantity
```

For example:

```text
unitPrice = 25.50
quantity  = 4

totalPrice = 102.00
```

The complete cart total is:

```text
totalAmount = sum(all item totalPrice values)
```

Example:

```text
Item A = 100.00
Item B =  50.00
Item C =  25.00
----------------
Cart   = 175.00
```

The implementation uses `BigDecimal` for these monetary calculations.

---

# 17. Validation

The API uses Jakarta Bean Validation.

## CreateCartItemRequest

### `productId`

```java
@NotNull
```

Error message:

```text
Product ID is required
```

### `quantity`

```java
@NotNull
@Min(1)
```

Possible messages:

```text
Quantity is required
Quantity must be at least 1
```

### `unitPrice`

```java
@NotNull
@DecimalMin("0.01")
```

Possible messages:

```text
Unit price is required
Unit price must be greater than zero
```

---

# 18. UpdateCartItemRequest

The update request contains only:

```json
{
  "quantity": 5
}
```

Validation:

```java
@NotNull
@Min(1)
```

Possible errors:

```text
Quantity is required
Quantity must be at least 1
```

A quantity of `0` is rejected.

Negative quantities are rejected.

---

# 19. Missing User Header

All endpoints require:

```http
X-User-Id
```

If it is missing:

**HTTP 400 Bad Request**

Example:

```json
{
  "timestamp": "2026-08-09T08:15:00Z",
  "status": 400,
  "error": "MISSING_HEADER",
  "message": "Required header 'X-User-Id' is missing",
  "path": "/api/cart",
  "traceId": null
}
```

This applies to:

```text
GET    /api/cart
POST   /api/cart/items
PUT    /api/cart/items/{itemId}
DELETE /api/cart/items/{itemId}
DELETE /api/cart
```

---

# 20. Cart Item Not Found

If an item ID does not exist:

```http
PUT /api/cart/items/999999
X-User-Id: 101
```

or:

```http
DELETE /api/cart/items/999999
X-User-Id: 101
```

the service throws `CartItemNotFoundException`.

## Response

**HTTP 404 Not Found**

```json
{
  "timestamp": "2026-08-09T08:16:00Z",
  "status": 404,
  "error": "RESOURCE_NOT_FOUND",
  "message": "Cart item not found with id: 999999",
  "path": "/api/cart/items/999999",
  "traceId": null
}
```

---

# 21. Cart Item Ownership Error

A cart item cannot be modified or deleted merely by knowing its ID.

The service verifies:

```text
cartItem.cartId == currentUserActiveCart.id
```

If the item belongs to another cart:

**HTTP 400 Bad Request**

```json
{
  "timestamp": "2026-08-09T08:17:00Z",
  "status": 400,
  "error": "BAD_REQUEST",
  "message": "Cart item with id: 25 does not belong to your cart",
  "path": "/api/cart/items/25",
  "traceId": null
}
```

This is an important ownership check in the current implementation.

---

# 22. Validation Error Response

For invalid request bodies, the global exception handler returns:

```text
400 Bad Request
```

with:

```text
error = VALIDATION_ERROR
```

Example request:

```json
{
  "productId": null,
  "quantity": 0,
  "unitPrice": 0
}
```

Possible response:

```json
{
  "timestamp": "2026-08-09T08:18:00Z",
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "Product ID is required; Quantity must be at least 1; Unit price must be greater than zero",
  "path": "/api/cart/items",
  "traceId": null
}
```

Multiple validation messages are joined with:

```text
; 
```

---

# 23. Error Response Schema

All handled application errors use:

```json
{
  "timestamp": "2026-08-09T08:18:00Z",
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "Quantity must be at least 1",
  "path": "/api/cart/items",
  "traceId": null
}
```

## Fields

| Field | Type | Description |
|---|---|---|
| `timestamp` | Instant | Time the error response was created |
| `status` | integer | HTTP status code |
| `error` | string | Application-level error code |
| `message` | string | Human-readable error description |
| `path` | string | Request URI |
| `traceId` | string/null | Trace ID field; currently returned as `null` |

---

# 24. Error Codes

## `MISSING_HEADER`

HTTP:

```text
400 Bad Request
```

Used when `X-User-Id` is absent.

---

## `VALIDATION_ERROR`

HTTP:

```text
400 Bad Request
```

Used when Jakarta Bean Validation fails.

---

## `BAD_REQUEST`

HTTP:

```text
400 Bad Request
```

Used for application-level request errors, including cart-item ownership violations.

---

## `RESOURCE_NOT_FOUND`

HTTP:

```text
404 Not Found
```

Used when a requested cart item does not exist.

---

## `INTERNAL_SERVER_ERROR`

HTTP:

```text
500 Internal Server Error
```

Used by the generic exception handler for unexpected exceptions.

The current generic handler returns the exception's message when available.

---

# 25. Complete API Reference

## 25.1 Get Cart

```text
GET /api/cart
```

Headers:

```text
X-User-Id: Long
```

Response:

```text
200 OK
CartResponse
```

---

## 25.2 Add Item

```text
POST /api/cart/items
```

Headers:

```text
X-User-Id: Long
Content-Type: application/json
```

Body:

```json
{
  "productId": 501,
  "quantity": 2,
  "unitPrice": 49.99
}
```

Response:

```text
200 OK
CartResponse
```

---

## 25.3 Update Item

```text
PUT /api/cart/items/{itemId}
```

Headers:

```text
X-User-Id: Long
Content-Type: application/json
```

Body:

```json
{
  "quantity": 5
}
```

Response:

```text
200 OK
CartResponse
```

---

## 25.4 Remove Item

```text
DELETE /api/cart/items/{itemId}
```

Headers:

```text
X-User-Id: Long
```

Response:

```text
200 OK
CartResponse
```

---

## 25.5 Clear Cart

```text
DELETE /api/cart
```

Headers:

```text
X-User-Id: Long
```

Response:

```text
204 No Content
```

---

# 26. End-to-End Example

The following demonstrates a complete typical cart workflow.

Assume:

```text
User ID    = 101
Product ID = 501
Unit Price = 49.99
```

## Step 1 — Get the user's cart

```bash
curl -X GET http://localhost:4003/api/cart \
  -H "X-User-Id: 101"
```

If no active cart exists, one is automatically created.

Response:

```json
{
  "cartId": 10,
  "userId": 101,
  "status": "ACTIVE",
  "items": [],
  "totalAmount": 0,
  "createdAt": "2026-08-09T08:00:00Z",
  "updatedAt": "2026-08-09T08:00:00Z"
}
```

## Step 2 — Add a product

```bash
curl -X POST http://localhost:4003/api/cart/items \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 101" \
  -d '{
    "productId": 501,
    "quantity": 2,
    "unitPrice": 49.99
  }'
```

Result:

```text
quantity = 2
total    = 99.98
```

## Step 3 — Add the same product again

```bash
curl -X POST http://localhost:4003/api/cart/items \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 101" \
  -d '{
    "productId": 501,
    "quantity": 3,
    "unitPrice": 49.99
  }'
```

The service accumulates the quantity:

```text
2 + 3 = 5
```

Result:

```text
quantity = 5
total    = 249.95
```

## Step 4 — Update quantity

Assume item ID is `25`.

```bash
curl -X PUT http://localhost:4003/api/cart/items/25 \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 101" \
  -d '{
    "quantity": 4
  }'
```

Result:

```text
quantity = 4
total    = 199.96
```

## Step 5 — Remove the item

```bash
curl -X DELETE http://localhost:4003/api/cart/items/25 \
  -H "X-User-Id: 101"
```

Result:

```json
{
  "cartId": 10,
  "userId": 101,
  "status": "ACTIVE",
  "items": [],
  "totalAmount": 0,
  "createdAt": "2026-08-09T08:00:00Z",
  "updatedAt": "2026-08-09T08:10:00Z"
}
```

## Step 6 — Clear the cart

```bash
curl -X DELETE http://localhost:4003/api/cart \
  -H "X-User-Id: 101"
```

Response:

```text
204 No Content
```

The cart becomes:

```text
ABANDONED
```

---

# 27. Database Model

The service uses two primary tables.

## 27.1 `carts`

```text
carts
├── id
├── user_id
├── status
├── created_at
└── updated_at
```

### Constraints

```text
PRIMARY KEY (id)
UNIQUE (user_id, status)
```

Indexes:

```text
idx_carts_user_id
idx_carts_status
```

---

# 28. Cart Item Database Model

## `cart_items`

```text
cart_items
├── id
├── cart_id
├── product_id
├── quantity
├── unit_price
├── created_at
└── updated_at
```

### Constraints

```text
PRIMARY KEY (id)
UNIQUE (cart_id, product_id)
CHECK (quantity > 0)
```

Foreign key:

```text
cart_items.cart_id
    ->
carts.id
```

with:

```text
ON DELETE CASCADE
```

Indexes:

```text
idx_cart_items_cart_id
idx_cart_items_product_id
```

---

# 29. Cart-to-Item Relationship

The database models:

```text
Cart 1 ---- N CartItems
```

A cart can contain multiple items.

Each cart item belongs to exactly one cart.

A product is represented by:

```text
product_id
```

The current cart service does not maintain a local product table.

---

# 30. Product Service Integration

There is currently **no Product Service client implementation** in this project.

The cart service stores only:

```text
product_id
unit_price
```

It does not verify:

- Whether the product exists
- Whether the product is active
- Whether the product is currently available
- Whether the supplied price matches the Product Service
- Whether the product is in stock

The `CreateCartItemRequest` source code explicitly identifies the client-supplied unit price as a temporary development measure.

For a production microservice architecture, these concerns should normally be handled by a trusted product/pricing/inventory flow.

---

# 31. Cart Creation Rules

A cart is created automatically by:

```text
GET /api/cart
```

or:

```text
POST /api/cart/items
```

when the user has no active cart.

New carts are created with:

```text
status = ACTIVE
```

The entity's `@PrePersist` method also defaults a null status to:

```text
ACTIVE
```

---

# 32. Active Cart Uniqueness

The database defines:

```sql
UNIQUE (user_id, status)
```

This means a user cannot have two carts with the same status.

In particular, it prevents multiple:

```text
(user_id, ACTIVE)
```

records.

The application normally works with one active cart per user.

It also means the database uniqueness rule applies to all statuses, not only `ACTIVE`.

For example, the database constraint prevents two carts for the same user from both having:

```text
CHECKED_OUT
```

or both having:

```text
ABANDONED
```

---

# 33. Important Cart Lifecycle

The currently implemented lifecycle is:

```text
                   +----------------+
                   |                |
                   |  GET /api/cart |
                   |                |
                   +-------+--------+
                           |
                           v
                    +-------------+
                    |   ACTIVE    |
                    +-------------+
                      |    |    |
          add/update  |    |    | remove item
                      |    |    |
                      v    |    |
                    ACTIVE  |    |
                           |    |
                  DELETE /api/cart
                           |
                           v
                    +-------------+
                    |  ABANDONED  |
                    +-------------+
```

`CHECKED_OUT` exists as a domain status but is not currently exposed through a controller operation.

---

# 34. Timestamp Behavior

Both carts and cart items have:

```text
createdAt
updatedAt
```

On creation:

```text
createdAt = current time
updatedAt = current time
```

On entity update:

```text
updatedAt = current time
```

Timestamps use Java:

```text
java.time.Instant
```

and are represented as ISO-8601 timestamps in JSON.

---

# 35. Transaction Behavior

Both cart service implementations use:

```java
@Transactional
```

Therefore cart operations are intended to execute transactionally.

This is especially important for:

```text
clear cart
```

where the service:

1. Deletes all items.
2. Changes cart status.
3. Saves the cart.

These operations are contained within the service transaction.

`CartItemServiceImpl` also uses transactions for:

```text
addItem
updateItem
removeItem
```

---

# 36. API Response Status Matrix

| Scenario | HTTP Status | Error Code |
|---|---:|---|
| Get/create cart succeeds | `200` | — |
| Add item succeeds | `200` | — |
| Update item succeeds | `200` | — |
| Remove item succeeds | `200` | — |
| Clear cart succeeds | `204` | — |
| Missing `X-User-Id` | `400` | `MISSING_HEADER` |
| Invalid request body | `400` | `VALIDATION_ERROR` |
| Item belongs to another cart | `400` | `BAD_REQUEST` |
| Cart item does not exist | `404` | `RESOURCE_NOT_FOUND` |
| Unexpected server exception | `500` | `INTERNAL_SERVER_ERROR` |

---

# 37. Swagger / OpenAPI

The Maven project includes:

```text
springdoc-openapi-starter-webmvc-ui
version 2.6.0
```

No custom Springdoc configuration is present in the source inspected.

With the default Springdoc configuration, the application should expose the standard OpenAPI resources when the application is running.

Typical locations are:

```text
/v3/api-docs
/swagger-ui/index.html
```

Therefore, with the configured port:

```text
http://localhost:4003/v3/api-docs
```

and:

```text
http://localhost:4003/swagger-ui/index.html
```

The actual generated OpenAPI document will be based on Springdoc's runtime introspection of the controller.

---

# 38. Actuator Endpoints

The application exposes:

```text
health
info
metrics
```

through Spring Boot Actuator.

Typical endpoints:

```http
GET /actuator/health
GET /actuator/info
GET /actuator/metrics
```

Examples:

```bash
curl http://localhost:4003/actuator/health
```

```bash
curl http://localhost:4003/actuator/info
```

```bash
curl http://localhost:4003/actuator/metrics
```

---

# 39. Security

The current project does not implement authentication or authorization inside the cart service.

The application uses:

```text
X-User-Id
```

as the user identity supplied to the controller.

This should not be considered secure authentication by itself.

In a production deployment, the header should normally be supplied by a trusted authentication layer, such as:

```text
Client
  |
  v
API Gateway / Identity Layer
  |
  +-- validates JWT
  |
  +-- derives authenticated user ID
  |
  v
Cart Service
  |
  +-- X-User-Id / authenticated principal
```

The cart service should not blindly trust arbitrary user-supplied identity headers when directly exposed to untrusted clients.

---

# 40. Recommended Production Improvements

The current implementation is suitable as a development-oriented cart service, but several areas should be strengthened before production.

## 40.1 Do not trust client-supplied price

Current request:

```json
{
  "productId": 501,
  "quantity": 2,
  "unitPrice": 49.99
}
```

allows the caller to provide the price.

Recommended:

```json
{
  "productId": 501,
  "quantity": 2
}
```

The cart service should retrieve the authoritative price from the Product Service or pricing service.

---

## 40.2 Add product validation

The current service does not verify that:

```text
productId
```

exists.

A Product Service call or trusted event/read model could verify the product.

---

## 40.3 Add inventory validation

The current cart service does not check stock availability.

For example, a user could request:

```text
quantity = 1,000,000
```

as long as it passes the `quantity >= 1` validation.

Inventory validation should normally occur before checkout and, depending on business requirements, possibly when adding/updating the cart.

---

## 40.4 Use authenticated identity

Instead of trusting:

```http
X-User-Id
```

from an arbitrary external caller, use an authenticated principal/JWT claim.

---

## 40.5 Consider optimistic locking

Concurrent cart updates could potentially cause lost updates.

For example:

```text
Request A reads quantity = 2
Request B reads quantity = 2

A adds 3 -> 5
B adds 4 -> 6
```

Depending on timing, one update could overwrite another.

An optimistic locking strategy such as JPA `@Version` could help protect concurrent modifications.

---

## 40.6 Consider explicit checkout support

The domain already defines:

```text
CHECKED_OUT
```

but there is currently no endpoint that transitions a cart to that state.

A future checkout workflow could be implemented outside or inside this service depending on the overall ShopSphere architecture.

---

# 41. Environment Configuration

The current `application.properties` configures:

```text
spring.application.name=cart-service
server.port=4003
```

Database:

```text
MySQL
Database: shopsphere_cart
Host: localhost
Port: 3306
```

The current source contains database credentials directly in `application.properties`.

For production, credentials should be moved to:

- Environment variables
- Docker/Kubernetes secrets
- Cloud secret manager
- Vault
- Other secure configuration providers

---

# 42. Database Migration

Flyway is enabled:

```properties
spring.flyway.enabled=true
```

Migration location:

```text
classpath:db/migration
```

The initial schema is:

```text
V1__create_cart_schema.sql
```

It creates:

```text
carts
cart_items
```

and their indexes, constraints, and foreign key relationship.

---

# 43. Testing Configuration

The project includes H2 for tests.

Test configuration uses:

```text
H2 in-memory database
```

with:

```text
server.port=0
```

The test configuration also enables Flyway migrations.

This allows the service to use the same database migration structure during testing without requiring the configured MySQL instance.

---

# 44. Quick Client Reference

## Get cart

```bash
curl http://localhost:4003/api/cart \
  -H "X-User-Id: 101"
```

## Add item

```bash
curl -X POST http://localhost:4003/api/cart/items \
  -H "X-User-Id: 101" \
  -H "Content-Type: application/json" \
  -d '{"productId":501,"quantity":2,"unitPrice":49.99}'
```

## Update item

```bash
curl -X PUT http://localhost:4003/api/cart/items/25 \
  -H "X-User-Id: 101" \
  -H "Content-Type: application/json" \
  -d '{"quantity":5}'
```

## Remove item

```bash
curl -X DELETE http://localhost:4003/api/cart/items/25 \
  -H "X-User-Id: 101"
```

## Clear cart

```bash
curl -X DELETE http://localhost:4003/api/cart \
  -H "X-User-Id: 101"
```

## Swagger UI

```text
http://localhost:4003/swagger-ui/index.html
```

## OpenAPI JSON

```text
http://localhost:4003/v3/api-docs
```

## Health

```text
http://localhost:4003/actuator/health
```

---

# 45. Final API Contract

The current Cart Service contract can be summarized as:

```text
                    ShopSphere Cart Service
                           |
                    X-User-Id Header
                           |
          +----------------+----------------+
          |                |                |
          v                v                v
       Get Cart         Add Item       Clear Cart
          |                |                |
          |                +--> create cart |
          |                |                |
          v                v                v
       ACTIVE CART <---- Cart Items ----> ABANDONED
          |
          +--> Update Item
          |
          +--> Remove Item
          |
          +--> Calculate Total
```

The key rules are:

1. **Every endpoint requires `X-User-Id`.**
2. **An active cart is automatically created when needed.**
3. **Only one active cart exists per user according to the database uniqueness constraint.**
4. **Adding an existing product accumulates quantity.**
5. **Updating an item changes quantity only.**
6. **Removing an item returns the updated cart.**
7. **Clearing a cart deletes its items and changes the cart status to `ABANDONED`.**
8. **Getting a cart after clearing creates a new active cart.**
9. **Cart totals are calculated from `unitPrice × quantity`.**
10. **The current implementation accepts unit price from the client and does not validate products against the Product Service.**
11. **`CHECKED_OUT` exists as a status but has no current API transition.**
12. **Errors use a common `ErrorResponse` structure.**
