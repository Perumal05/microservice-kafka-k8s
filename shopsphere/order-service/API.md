# ShopSphere Order Service API Documentation

**Base URL**: `http://localhost:4004`  
**Swagger UI**: `http://localhost:4004/swagger-ui.html`

---

## 1. State Transition Lifecycle & Rules

```text
PENDING ──► PAYMENT_PENDING ──► PAID ──► PROCESSING ──► SHIPPED ──► DELIVERED
   │               │              │          │
   ├───────────────┼──────────────┴──────────┴──► CANCELLED
   │               │
   └──► FAILED ────┴──► FAILED
```

- **Cancellation Rules**: Orders can be cancelled from `PENDING`, `PAYMENT_PENDING`, `CONFIRMED`, `PAID`, or `PROCESSING` states. Orders in `SHIPPED`, `DELIVERED`, `CANCELLED`, or `FAILED` states **cannot** be cancelled.

---

## 2. API Endpoint Specifications

### 2.1 Create Order

- **Method**: `POST`
- **Path**: `/api/orders`
- **Status**: `201 Created`

**Example Request**:
```json
{
  "userId": 1,
  "currency": "USD",
  "items": [
    {
      "productId": 10,
      "productSku": "LAPTOP-001",
      "productName": "Gaming Laptop",
      "quantity": 2,
      "unitPrice": 750.00
    }
  ],
  "shippingAddress": {
    "addressLine1": "123 Main Street",
    "addressLine2": "Apt 4B",
    "city": "Chennai",
    "state": "Tamil Nadu",
    "postalCode": "600001",
    "country": "India"
  }
}
```

**Example Response (`201 Created`)**:
```json
{
  "id": 1,
  "orderNumber": "ORD-20260809-A1B2C3",
  "userId": 1,
  "status": "PENDING",
  "currency": "USD",
  "subtotal": 1500.00,
  "shippingAmount": 0.00,
  "taxAmount": 0.00,
  "discountAmount": 0.00,
  "totalAmount": 1500.00,
  "shippingAddress": {
    "addressLine1": "123 Main Street",
    "addressLine2": "Apt 4B",
    "city": "Chennai",
    "state": "Tamil Nadu",
    "postalCode": "600001",
    "country": "India"
  },
  "items": [
    {
      "id": 1,
      "productId": 10,
      "productSku": "LAPTOP-001",
      "productName": "Gaming Laptop",
      "quantity": 2,
      "unitPrice": 750.00,
      "totalPrice": 1500.00,
      "createdAt": "2026-08-09T09:15:00.000000Z"
    }
  ],
  "createdAt": "2026-08-09T09:15:00.000000Z",
  "updatedAt": "2026-08-09T09:15:00.000000Z"
}
```

---

### 2.2 Get Order by ID

- **Method**: `GET`
- **Path**: `/api/orders/{orderId}`
- **Status**: `200 OK`

---

### 2.3 Get Order by Order Number

- **Method**: `GET`
- **Path**: `/api/orders/number/{orderNumber}`
- **Status**: `200 OK`

---

### 2.4 Get User Orders (Paginated)

- **Method**: `GET`
- **Path**: `/api/orders/user/{userId}?page=0&size=20&sort=createdAt,desc`
- **Status**: `200 OK`

---

### 2.5 Update Order Status

- **Method**: `PATCH`
- **Path**: `/api/orders/{orderId}/status`
- **Status**: `200 OK`

**Example Request**:
```json
{
  "status": "CONFIRMED"
}
```

---

### 2.6 Cancel Order

- **Method**: `POST`
- **Path**: `/api/orders/{orderId}/cancel`
- **Status**: `200 OK`

---

## 3. Error Responses (`400 Bad Request`)

```json
{
  "timestamp": "2026-08-09T09:16:00.000000Z",
  "status": 400,
  "error": "INVALID_ORDER_STATE",
  "message": "Cannot cancel order in status 'SHIPPED'",
  "path": "/api/orders/1/cancel",
  "traceId": null
}
```
