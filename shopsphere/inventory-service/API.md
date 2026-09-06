# ShopSphere Inventory Service API Documentation

**Base URL**: `http://localhost:4005`  
**Swagger UI**: `http://localhost:4005/swagger-ui.html`

---

## 1. Inventory & Reservation Lifecycle

```text
Salable Quantity = availableQuantity - reservedQuantity

[POST /reserve] ──► status: RESERVED (reservedQuantity += qty)
                          │
          ┌───────────────┴───────────────┐
          ▼                               ▼
[POST /confirm] ──► CONFIRMED     [POST /release] ──► RELEASED
(available -= qty,                (reserved -= qty)
 reserved -= qty)
```

---

## 2. API Endpoint Specifications

### 2.1 Initialize Inventory

- **Method**: `POST`
- **Path**: `/api/inventory`
- **Status**: `201 Created`

**Example Request**:
```json
{
  "productId": 10,
  "sku": "LAPTOP-RTX-4090",
  "availableQuantity": 100,
  "reorderLevel": 10
}
```

**Example Response (`201 Created`)**:
```json
{
  "id": 1,
  "productId": 10,
  "sku": "LAPTOP-RTX-4090",
  "availableQuantity": 100,
  "reservedQuantity": 0,
  "reorderLevel": 10,
  "salableQuantity": 100,
  "createdAt": "2026-08-09T10:15:00.000000Z",
  "updatedAt": "2026-08-09T10:15:00.000000Z"
}
```

---

### 2.2 Reserve Inventory

- **Method**: `POST`
- **Path**: `/api/inventory/{productId}/reserve`
- **Status**: `200 OK`

**Example Request**:
```json
{
  "orderId": 500,
  "quantity": 2
}
```

**Example Response (`200 OK`)**:
```json
{
  "id": 1,
  "inventoryId": 1,
  "orderId": 500,
  "quantity": 2,
  "status": "RESERVED",
  "createdAt": "2026-08-09T10:16:00.000000Z",
  "updatedAt": "2026-08-09T10:16:00.000000Z"
}
```

---

### 2.3 Confirm Reservation (Consume Stock)

- **Method**: `POST`
- **Path**: `/api/inventory/reservations/{reservationId}/confirm`
- **Status**: `200 OK`

---

### 2.4 Release Reservation (Return Stock)

- **Method**: `POST`
- **Path**: `/api/inventory/reservations/{reservationId}/release`
- **Status**: `200 OK`

---

### 2.5 Check Availability

- **Method**: `GET`
- **Path**: `/api/inventory/{productId}/availability`
- **Status**: `200 OK`

**Example Response (`200 OK`)**:
```json
{
  "productId": 10,
  "sku": "LAPTOP-RTX-4090",
  "availableQuantity": 98,
  "reservedQuantity": 0,
  "salableQuantity": 98,
  "isAvailable": true
}
```
