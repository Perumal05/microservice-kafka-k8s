# Product Service

Owns the product catalog and categories. This is the **only** authoritative
source for a product's price, SKU, name, and status anywhere in ShopSphere
— every other service that needs product data calls this service (or, for
Order Service, gets that data at checkout time) rather than trusting a
client-supplied value.

⬅ [Back to root README](../README.md)

---

## Service Overview

**Responsible for:** the product catalog and its categories — create, read,
update, delete for both.

**Role in the architecture:** Order Service calls this service synchronously
over REST during checkout to resolve the real price/SKU/name for every item
a customer is ordering (`items` in a checkout request only ever carry a
`productId` and `quantity` — never a price). This is the one piece of the
checkout flow that stayed synchronous even after Kafka was introduced,
because an order can't be priced without a real-time answer.

---

## Architecture

```
Controller (ProductController, CategoryController)
      │
      ▼
Service (ProductService, CategoryService)
      │
      ▼
Repository (Spring Data JPA)
      │
      ▼
MySQL (shopsphere_product)
```

Products and categories are a many-to-many relationship (a product can
belong to multiple categories); resolved via a join table.

---

## Database

- **Technology:** MySQL 8, schema managed by **Flyway**, applied automatically on startup.
- **Database name:** `shopsphere_product`

| Table | Purpose | Key columns |
|---|---|---|
| `products` | The catalog | `sku`, `name`, `description`, `price`, `currency`, `status` (`ACTIVE`/`INACTIVE`/`DISCONTINUED`) |
| `categories` | Category list | `name`, `description`, `status` (`ACTIVE`/`INACTIVE`) |
| `product_categories` | Join table | `product_id`, `category_id` |

---

## APIs

- **Base URL:** `http://localhost:4002`
- **Swagger UI:** http://localhost:4002/swagger-ui.html
- **Health check:** http://localhost:4002/actuator/health
- **Authentication:** none.

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/products` | Paginated list — supports `keyword`, `status`, `categoryId`, `page`, `size`, `sort` |
| GET | `/api/products/{productId}` | Get one product |
| POST | `/api/products` | Create a product |
| PUT | `/api/products/{productId}` | Update a product |
| DELETE | `/api/products/{productId}` | Delete a product |
| GET | `/api/categories` | List all categories |
| GET | `/api/categories/{categoryId}` | Get one category |
| POST | `/api/categories` | Create a category |
| PUT | `/api/categories/{categoryId}` | Update a category |
| DELETE | `/api/categories/{categoryId}` | Delete a category |

---

## Kafka

Product Service does not produce or consume any Kafka events at this stage
of the project — it remains synchronous-only by design (see the root
README's Kafka Architecture section for why).

---

## Communication With Other Services

| Direction | Service | Mechanism | What flows |
|---|---|---|---|
| Inbound | Order Service | REST (`GET /api/products/{productId}`) | Called once per order item during checkout, to resolve authoritative price/SKU/name/status |
| Outbound | — | — | Product Service makes no outbound calls to any other service |

---

## Configuration

| Property | Default | Notes |
|---|---|---|
| `server.port` | `4002` | |
| `spring.datasource.url` | `jdbc:mysql://localhost:3306/shopsphere_product?...` | `mysql:3306` when run via Docker Compose |
| `spring.datasource.username` | `root` | |
| `spring.datasource.password` | *(local dev placeholder)* | See `.env.example` at the project root |
| `management.endpoints.web.exposure.include` | `health,info,metrics` | |

---

## Running the Service

Standalone:
```bash
cd product-service
mvn spring-boot:run
```

Or as part of the full stack:
```bash
docker compose up -d --build product-service
```

Run its tests:
```bash
mvn clean test
```
