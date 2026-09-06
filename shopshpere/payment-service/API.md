# ShopSphere Payment Service API Documentation

**Base URL**: `http://localhost:4006`  
**Swagger UI**: `http://localhost:4006/swagger-ui.html`

---

## 1. Payment State Lifecycle & Transitions

```text
PENDING ──► PROCESSING ──► SUCCESS ──► REFUNDED
   │             │
   └─────────────┼───────► FAILED
                 │
                 └───► CANCELLED
```

- **Refund Rule**: Only `SUCCESS` payments can be transitioned to `REFUNDED`. Attempting to refund a `PENDING`, `FAILED`, or `CANCELLED` payment returns `400 Bad Request`.

---

## 2. API Endpoint Specifications

### 2.1 Create Payment Intent

- **Method**: `POST`
- **Path**: `/api/payments`
- **Status**: `201 Created`

**Example Request**:
```json
{
  "orderId": 100,
  "userId": 50,
  "amount": 299.99,
  "currency": "USD",
  "paymentMethod": "CARD"
}
```

**Example Response (`201 Created`)**:
```json
{
  "id": 1,
  "paymentReference": "PAY-20260809-A1B2C3",
  "orderId": 100,
  "userId": 50,
  "amount": 299.99,
  "currency": "USD",
  "status": "PENDING",
  "paymentMethod": "CARD",
  "failureReason": null,
  "createdAt": "2026-08-09T10:20:00.000000Z",
  "updatedAt": "2026-08-09T10:20:00.000000Z"
}
```

---

### 2.2 Process Payment (Simulation)

- **Method**: `POST`
- **Path**: `/api/payments/{paymentId}/process`
- **Status**: `200 OK`

**Example Request (`SUCCESS`)**:
```json
{
  "simulationMode": "SUCCESS"
}
```

**Example Request (`FAILURE`)**:
```json
{
  "simulationMode": "FAILURE",
  "failureReason": "Insufficient funds in bank account"
}
```

**Example Request (`TIMEOUT`)**:
```json
{
  "simulationMode": "TIMEOUT"
}
```

---

### 2.3 Refund Payment

- **Method**: `POST`
- **Path**: `/api/payments/{paymentId}/refund`
- **Status**: `200 OK`

---

### 2.4 Cancel Payment

- **Method**: `POST`
- **Path**: `/api/payments/{paymentId}/cancel`
- **Status**: `200 OK`

---

### 2.5 Get Payment by Reference

- **Method**: `GET`
- **Path**: `/api/payments/reference/{paymentReference}`
- **Status**: `200 OK`

---

### 2.6 Get Payments by Order ID

- **Method**: `GET`
- **Path**: `/api/payments/order/{orderId}`
- **Status**: `200 OK`
