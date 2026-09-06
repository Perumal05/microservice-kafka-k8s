# ShopSphere Notification Service API Documentation

**Base URL**: `http://localhost:4007`
**Swagger UI**: `http://localhost:4007/swagger-ui.html`

---

## Notification State Lifecycle

```
[*] ──► PENDING ──► SENT
            │
            ├──► FAILED
            │
            └──► CANCELLED
```

**Rules:**
- Only `PENDING` notifications can be sent or cancelled.
- Attempting to act on `SENT`, `FAILED`, or `CANCELLED` returns `400 INVALID_NOTIFICATION_STATE`.

---

## Supported Enums

### `NotificationType`
| Value |
|---|
| `ORDER_CREATED` |
| `ORDER_CONFIRMED` |
| `ORDER_CANCELLED` |
| `PAYMENT_SUCCESS` |
| `PAYMENT_FAILED` |
| `ORDER_SHIPPED` |
| `ORDER_DELIVERED` |

### `NotificationChannel`
| Value | Recipient Format |
|---|---|
| `EMAIL` | Email address (e.g. `user@example.com`) |
| `SMS` | Phone number (e.g. `+919876543210`) |
| `PUSH` | Device token (e.g. `device-token-abc123`) |

### `NotificationStatus`
| Value |
|---|
| `PENDING` |
| `SENT` |
| `FAILED` |
| `CANCELLED` |

---

## Endpoints

### `POST /api/notifications` — Create Notification

**Request Body:**
```json
{
  "userId": 10,
  "type": "PAYMENT_SUCCESS",
  "channel": "EMAIL",
  "recipient": "john.doe@example.com",
  "subject": "Payment Successful",
  "message": "Your payment of ₹299.99 was processed successfully.",
  "simulateFailure": false
}
```

**Response `201 Created`:**
```json
{
  "id": 1,
  "userId": 10,
  "type": "PAYMENT_SUCCESS",
  "channel": "EMAIL",
  "recipient": "john.doe@example.com",
  "subject": "Payment Successful",
  "message": "Your payment of ₹299.99 was processed successfully.",
  "status": "PENDING",
  "createdAt": "2026-08-09T10:30:00.000000Z",
  "sentAt": null,
  "failureReason": null
}
```

---

### `GET /api/notifications/{notificationId}` — Get Notification by ID

**Response `200 OK`:**
```json
{
  "id": 1,
  "userId": 10,
  "type": "PAYMENT_SUCCESS",
  "channel": "EMAIL",
  "recipient": "john.doe@example.com",
  "subject": "Payment Successful",
  "message": "Your payment of ₹299.99 was processed successfully.",
  "status": "PENDING",
  "createdAt": "2026-08-09T10:30:00.000000Z",
  "sentAt": null,
  "failureReason": null
}
```

**Error `404 Not Found`:**
```json
{
  "timestamp": "2026-08-09T10:30:00.000000Z",
  "status": 404,
  "error": "RESOURCE_NOT_FOUND",
  "message": "Notification not found with id: 999",
  "path": "/api/notifications/999"
}
```

---

### `GET /api/notifications/user/{userId}` — Get User Notification History

**Query Parameters:**

| Param | Type | Default | Description |
|---|---|---|---|
| `page` | int | `0` | Page number (0-based) |
| `size` | int | `20` | Page size |

**Response `200 OK`:**
```json
{
  "content": [
    {
      "id": 3,
      "userId": 10,
      "type": "ORDER_SHIPPED",
      "channel": "SMS",
      "recipient": "+919876543210",
      "subject": null,
      "message": "Your order #456 has shipped!",
      "status": "SENT",
      "createdAt": "2026-08-09T10:25:00.000000Z",
      "sentAt": "2026-08-09T10:25:01.000000Z",
      "failureReason": null
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

### `POST /api/notifications/{notificationId}/send` — Send Notification

**Query Parameters:**

| Param | Type | Default | Description |
|---|---|---|---|
| `simulateFailure` | boolean | `false` | Set `true` to simulate a failed delivery |

**Response `200 OK` (success):**
```json
{
  "id": 1,
  "status": "SENT",
  "sentAt": "2026-08-09T10:31:00.000000Z",
  "failureReason": null
}
```

**Response `200 OK` (simulated failure):**
```json
{
  "id": 1,
  "status": "FAILED",
  "sentAt": null,
  "failureReason": "Simulated EMAIL delivery failure for recipient: john.doe@example.com"
}
```

**Error `400 Bad Request` (invalid state):**
```json
{
  "status": 400,
  "error": "INVALID_NOTIFICATION_STATE",
  "message": "Cannot send notification with status 'SENT'. Only PENDING notifications can be sent.",
  "path": "/api/notifications/1/send"
}
```

---

### `POST /api/notifications/{notificationId}/cancel` — Cancel Notification

**Response `200 OK`:**
```json
{
  "id": 1,
  "status": "CANCELLED",
  "sentAt": null,
  "failureReason": null
}
```

**Error `400 Bad Request` (already sent):**
```json
{
  "status": 400,
  "error": "INVALID_NOTIFICATION_STATE",
  "message": "Cannot cancel notification with status 'SENT'. Only PENDING notifications can be cancelled.",
  "path": "/api/notifications/1/cancel"
}
```

---

## Health & Metrics

| Endpoint | Description |
|---|---|
| `GET /actuator/health` | Service health check |
| `GET /actuator/metrics` | Metrics summary |
