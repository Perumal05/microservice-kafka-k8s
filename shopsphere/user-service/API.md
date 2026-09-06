# ShopSphere User Service API Documentation

**Base URL**: `http://localhost:8081`

This document details all available RESTful endpoints for the **ShopSphere User Service**.

---

## 1. Overview & Conventions

### Headers
- `Content-Type: application/json`
- `Accept: application/json`

### Error Response Payload
All non-2xx HTTP responses return a consistent error body:

```json
{
  "timestamp": "2026-08-09T05:43:00.123456Z",
  "status": 404,
  "error": "RESOURCE_NOT_FOUND",
  "message": "User not found with id: 99",
  "path": "/api/users/99",
  "traceId": null
}
```

---

## 2. User Management APIs

### 2.1 Create User

- **HTTP Method**: `POST`
- **Path**: `/api/users`
- **Success Status**: `201 Created`

#### Request Body (`CreateUserRequest`)
| Field | Type | Required | Description / Constraints |
| :--- | :--- | :--- | :--- |
| `email` | `String` | Yes | Valid email address, unique across system |
| `password` | `String` | Yes | Minimum 6 characters |
| `firstName` | `String` | Yes | Non-blank |
| `lastName` | `String` | Yes | Non-blank |
| `phone` | `String` | No | Optional phone number |
| `status` | `UserStatus` | No | `ACTIVE`, `INACTIVE`, or `BLOCKED` (Defaults to `ACTIVE`) |

**Example Request**:
```json
{
  "email": "jane.doe@shopsphere.com",
  "password": "Password123!",
  "firstName": "Jane",
  "lastName": "Doe",
  "phone": "+1-555-0199",
  "status": "ACTIVE"
}
```

**Example Response (`201 Created`)**:
```json
{
  "id": 1,
  "email": "jane.doe@shopsphere.com",
  "firstName": "Jane",
  "lastName": "Doe",
  "phone": "+1-555-0199",
  "status": "ACTIVE",
  "createdAt": "2026-08-09T05:43:00.123456Z",
  "updatedAt": "2026-08-09T05:43:00.123456Z"
}
```

#### Error Responses
- `400 Bad Request`: Field validation failures (`VALIDATION_ERROR`).
- `409 Conflict`: Email already registered (`DUPLICATE_RESOURCE`).

---

### 2.2 Get User by ID

- **HTTP Method**: `GET`
- **Path**: `/api/users/{userId}`
- **Success Status**: `200 OK`

#### Path Parameters
| Parameter | Type | Description |
| :--- | :--- | :--- |
| `userId` | `Long` | Unique Identifier of the user |

**Example Response (`200 OK`)**:
```json
{
  "id": 1,
  "email": "jane.doe@shopsphere.com",
  "firstName": "Jane",
  "lastName": "Doe",
  "phone": "+1-555-0199",
  "status": "ACTIVE",
  "createdAt": "2026-08-09T05:43:00.123456Z",
  "updatedAt": "2026-08-09T05:43:00.123456Z"
}
```

#### Error Responses
- `404 Not Found`: User does not exist (`RESOURCE_NOT_FOUND`).

---

### 2.3 Update User Profile

- **HTTP Method**: `PUT`
- **Path**: `/api/users/{userId}`
- **Success Status**: `200 OK`

#### Request Body (`UpdateUserRequest`)
| Field | Type | Required | Description |
| :--- | :--- | :--- | :--- |
| `firstName` | `String` | Yes | Non-blank updated first name |
| `lastName` | `String` | Yes | Non-blank updated last name |
| `phone` | `String` | No | Updated phone number |

**Example Request**:
```json
{
  "firstName": "Janet",
  "lastName": "Doe-Smith",
  "phone": "+1-555-9999"
}
```

**Example Response (`200 OK`)**:
```json
{
  "id": 1,
  "email": "jane.doe@shopsphere.com",
  "firstName": "Janet",
  "lastName": "Doe-Smith",
  "phone": "+1-555-9999",
  "status": "ACTIVE",
  "createdAt": "2026-08-09T05:43:00.123456Z",
  "updatedAt": "2026-08-09T05:43:30.987654Z"
}
```

#### Error Responses
- `400 Bad Request`: Validation failure.
- `404 Not Found`: User does not exist.

---

## 3. Address Management APIs

### 3.1 Get User Addresses

- **HTTP Method**: `GET`
- **Path**: `/api/users/{userId}/addresses`
- **Success Status**: `200 OK`

**Example Response (`200 OK`)**:
```json
[
  {
    "id": 10,
    "userId": 1,
    "addressLine1": "742 Evergreen Terrace",
    "addressLine2": "Apt 2B",
    "city": "Springfield",
    "state": "OR",
    "postalCode": "97477",
    "country": "USA",
    "type": "HOME",
    "isDefault": true,
    "createdAt": "2026-08-09T05:43:10.111222Z",
    "updatedAt": "2026-08-09T05:43:10.111222Z"
  }
]
```

#### Error Responses
- `404 Not Found`: User does not exist.

---

### 3.2 Create User Address

- **HTTP Method**: `POST`
- **Path**: `/api/users/{userId}/addresses`
- **Success Status**: `201 Created`

#### Request Body (`CreateAddressRequest`)
| Field | Type | Required | Description |
| :--- | :--- | :--- | :--- |
| `addressLine1` | `String` | Yes | Non-blank street address line 1 |
| `addressLine2` | `String` | No | Suite / Apartment / Unit |
| `city` | `String` | Yes | Non-blank city |
| `state` | `String` | Yes | Non-blank state / province |
| `postalCode` | `String` | Yes | Non-blank postal / ZIP code |
| `country` | `String` | Yes | Non-blank country |
| `type` | `AddressType` | No | `HOME`, `WORK`, or `OTHER` (Defaults to `HOME`) |
| `isDefault` | `Boolean` | No | Flag indicating default status. If `true`, unsets previous default address. |

**Example Request**:
```json
{
  "addressLine1": "100 Broadway",
  "addressLine2": "Suite 500",
  "city": "New York",
  "state": "NY",
  "postalCode": "10005",
  "country": "USA",
  "type": "WORK",
  "isDefault": true
}
```

**Example Response (`201 Created`)**:
```json
{
  "id": 11,
  "userId": 1,
  "addressLine1": "100 Broadway",
  "addressLine2": "Suite 500",
  "city": "New York",
  "state": "NY",
  "postalCode": "10005",
  "country": "USA",
  "type": "WORK",
  "isDefault": true,
  "createdAt": "2026-08-09T05:43:15.333444Z",
  "updatedAt": "2026-08-09T05:43:15.333444Z"
}
```

---

### 3.3 Update User Address

- **HTTP Method**: `PUT`
- **Path**: `/api/users/{userId}/addresses/{addressId}`
- **Success Status**: `200 OK`

**Example Request**:
```json
{
  "addressLine1": "100 Broadway",
  "addressLine2": "Floor 12",
  "city": "New York",
  "state": "NY",
  "postalCode": "10005",
  "country": "USA",
  "type": "WORK",
  "isDefault": true
}
```

**Example Response (`200 OK`)**:
```json
{
  "id": 11,
  "userId": 1,
  "addressLine1": "100 Broadway",
  "addressLine2": "Floor 12",
  "city": "New York",
  "state": "NY",
  "postalCode": "10005",
  "country": "USA",
  "type": "WORK",
  "isDefault": true,
  "createdAt": "2026-08-09T05:43:15.333444Z",
  "updatedAt": "2026-08-09T05:43:20.555666Z"
}
```

#### Error Responses
- `400 Bad Request`: Cross-user violation (Address belonging to another user).
- `404 Not Found`: Address or User not found.

---

### 3.4 Delete User Address

- **HTTP Method**: `DELETE`
- **Path**: `/api/users/{userId}/addresses/{addressId}`
- **Success Status**: `204 No Content`

**Example Response**: Empty response body (`204 No Content`).

---

## 4. Enumerations

### `UserStatus`
- `ACTIVE`: Normal operating account.
- `INACTIVE`: Deactivated account.
- `BLOCKED`: Suspended / blocked account.

### `AddressType`
- `HOME`: Residential address.
- `WORK`: Commercial / office address.
- `OTHER`: Alternative address.
