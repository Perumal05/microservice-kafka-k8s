# User Service

Manages user accounts and their saved addresses. This is ShopSphere's
source of truth for "who a customer is" — every other service that needs a
user only ever stores a `userId`, never a copy of the user's own data.

⬅ [Back to root README](../README.md)

---

## Service Overview

**Responsible for:** creating and updating user accounts, and managing each
user's saved shipping addresses.

**Role in the architecture:** User Service is intentionally **not** part of
the checkout flow. Order Service receives `userId` directly from the
client's checkout request and never calls User Service to look anyone up —
this keeps checkout from depending on yet another service being available.
User Service is used directly by clients (e.g. to manage a profile or an
address book) and has no Kafka involvement at all.

---

## Architecture

Standard layered structure, no unusual patterns:

```
Controller (UserController, AddressController)
      │
      ▼
Service (UserService / AddressService)
      │
      ▼
Repository (Spring Data JPA)
      │
      ▼
MySQL (shopsphere_user)
```

Requests are validated (Jakarta Bean Validation) before reaching the
service layer; the service layer contains the actual business rules
(uniqueness checks, etc.); the repository layer is a thin Spring Data JPA
interface per entity.

---

## Database

- **Technology:** MySQL 8, schema managed by **Flyway** (`src/main/resources/db/migration`), applied automatically on startup.
- **Database name:** `shopsphere_user`

| Table | Purpose | Key columns |
|---|---|---|
| `users` | One row per account | `email` (unique), `password_hash`, `first_name`, `last_name`, `phone`, `status` |
| `addresses` | Saved addresses, many per user | `user_id` (FK → `users.id`, cascade delete), `address_line1/2`, `city`, `state`, `postal_code`, `country`, `type`, `is_default` |

`addresses.user_id` is a foreign key to `users.id` with `ON DELETE CASCADE`
— deleting a user removes their saved addresses too.

---

## APIs

- **Base URL:** `http://localhost:4001`
- **Swagger UI:** http://localhost:4001/swagger-ui.html
- **Health check:** http://localhost:4001/actuator/health
- **Authentication:** none — there is no login/token system anywhere in
  ShopSphere at this stage.

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/users` | Create a user |
| GET | `/api/users/{userId}` | Get a user by ID |
| PUT | `/api/users/{userId}` | Update a user |
| GET | `/api/users/{userId}/addresses` | List a user's addresses |
| POST | `/api/users/{userId}/addresses` | Add an address |
| PUT | `/api/users/{userId}/addresses/{addressId}` | Update an address |
| DELETE | `/api/users/{userId}/addresses/{addressId}` | Delete an address |

There is intentionally no "list all users" endpoint — users are always
created or looked up by ID.

---

## Kafka

User Service does not produce or consume any Kafka events. It has no
`spring-kafka` dependency and no `event` package.

---

## Communication With Other Services

| Direction | Service | Mechanism | What flows |
|---|---|---|---|
| — | — | — | User Service has **no outbound calls** to any other ShopSphere service |
| Inbound (potential) | Any client | REST | User Service only ever receives direct client requests; no other backend service calls it |

User Service is fully standalone within the system — see the root README's
architecture diagram for how it sits apart from the checkout flow.

---

## Configuration

| Property | Default | Notes |
|---|---|---|
| `server.port` | `4001` | |
| `spring.datasource.url` | `jdbc:mysql://localhost:3306/shopsphere_user?...` | Points at `mysql:3306` instead when run via Docker Compose |
| `spring.datasource.username` | `root` | |
| `spring.datasource.password` | *(local dev placeholder)* | Set your own value — see `.env.example` at the project root for the Docker Compose flow |
| `management.endpoints.web.exposure.include` | `health,info,metrics` | |

No environment-variable overrides are wired for this service's own
settings today — values are set directly in `application.properties` (see
the root README's note on why: consistency with the rest of the project's
"hardcode for local dev, override only what Docker needs" approach).

---

## Running the Service

Standalone (with MySQL already running, e.g. via `docker compose up -d mysql` from the project root):
```bash
cd user-service
mvn spring-boot:run
```

Or as part of the full stack:
```bash
docker compose up -d --build user-service
```

Run its tests:
```bash
mvn clean test
```
