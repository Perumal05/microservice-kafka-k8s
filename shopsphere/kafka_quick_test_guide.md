# ShopSphere — Quick Kafka Testing Guide

**Goal of this doc:** get you from zero to watching a real order flow
through Kafka in about 10 minutes, with copy-pasteable API calls and exact
JSON payloads. This is the hands-on companion to the full
[Onboarding Guide](./SHOPSPHERE_ONBOARDING_GUIDE.md) — read that one for
concepts and background; read this one when you just want to *do it*.

---

## Table of Contents

1. [Before You Start](#1-before-you-start)
2. [The Full Flow, Start to Finish](#2-the-full-flow-start-to-finish)
3. [Step-by-Step: Make the API Calls](#3-step-by-step-make-the-api-calls)
4. [Now Go Look at Kafka UI](#4-now-go-look-at-kafka-ui)
5. [How the Producer Side Works](#5-how-the-producer-side-works)
6. [How Consumer Groups Work (and How to Watch Them)](#6-how-consumer-groups-work-and-how-to-watch-them)
7. [How Topics Get Created](#7-how-topics-get-created)
8. [Quick Reference Card](#8-quick-reference-card)
9. [If Something Doesn't Progress](#9-if-something-doesnt-progress)

---

## 1. Before You Start

Make sure everything is up:

```bash
docker compose ps
```

You should see `mysql`, `kafka`, and all seven `shopsphere-*-service`
containers as `Up`. If you're running services outside Docker instead,
just make sure MySQL, Kafka, and all seven services are running on their
usual ports.

Open two things in your browser, in separate tabs:

- **Swagger UI** for Order Service: http://localhost:4004/swagger-ui.html
  (you'll also want Product Service's and Inventory Service's Swagger —
  ports 4002 and 4005)
- **Kafka UI**: http://localhost:9099

---

## 2. The Full Flow, Start to Finish

Here's what you're about to trigger, in order:

```
You: POST a product ──────────► Product Service
You: POST stock for it ───────► Inventory Service
You: POST an order ───────────► Order Service
                                     │
                                     │ publishes OrderCreated
                                     ▼
                                  Kafka topic: shopsphere.order.events
                                     │
                       ┌─────────────┴─────────────┐
                       ▼                             ▼
              Inventory Service              Notification Service
              (reserves stock)                (logs it)
                       │
                       │ publishes InventoryReserved
                       ▼
                  Kafka topic: shopsphere.inventory.events
                       │
                       ▼
                Payment Service
              (processes payment)
                       │
                       │ publishes PaymentSucceeded
                       ▼
                  Kafka topic: shopsphere.payment.events
                       │
              ┌────────┼────────┐
              ▼        ▼        ▼
        Order Service  Cart Service  Notification Service
        (status→PAID)  (clears cart)  (logs it)
```

Three topics, three "hops," and your one API call triggers all of it
without you doing anything else.

---

## 3. Step-by-Step: Make the API Calls

### Step 1 — Create a product

**Where:** Product Service Swagger → `POST /api/products`

```json
{
  "sku": "SKU-TEST-001",
  "name": "Test Wireless Mouse",
  "description": "A product created for Kafka testing",
  "price": 25.00,
  "currency": "USD",
  "status": "ACTIVE",
  "categoryIds": [1]
}
```

> **Note:** `categoryIds` must reference a category that already exists. If
> you don't have one yet, create one first via `POST /api/categories`:
> ```json
> { "name": "Electronics", "description": "Test category", "status": "ACTIVE" }
> ```
> Use the returned category `id` in `categoryIds` above.

**Response (`201 Created`)** — note the `id`, you'll need it next:
```json
{
  "id": 101,
  "sku": "SKU-TEST-001",
  "name": "Test Wireless Mouse",
  "description": "A product created for Kafka testing",
  "price": 25.00,
  "currency": "USD",
  "status": "ACTIVE",
  "categories": [{ "id": 1, "name": "Electronics", "description": "Test category", "status": "ACTIVE", "createdAt": "...", "updatedAt": "..." }],
  "createdAt": "2026-09-05T10:00:00Z",
  "updatedAt": "2026-09-05T10:00:00Z"
}
```

### Step 2 — Give it stock

**Where:** Inventory Service Swagger → `POST /api/inventory`

```json
{
  "productId": 101,
  "sku": "SKU-TEST-001",
  "availableQuantity": 50,
  "reorderLevel": 5
}
```

**Response (`201 Created`)**:
```json
{
  "id": 55,
  "productId": 101,
  "sku": "SKU-TEST-001",
  "availableQuantity": 50,
  "reservedQuantity": 0,
  "reorderLevel": 5,
  "createdAt": "2026-09-05T10:01:00Z",
  "updatedAt": "2026-09-05T10:01:00Z"
}
```

### Step 3 — Check out (this is the one that triggers Kafka)

**Where:** Order Service Swagger → `POST /api/orders`

```json
{
  "userId": 16,
  "currency": "USD",
  "paymentMethod": "CARD",
  "items": [
    {
      "productId": 101,
      "quantity": 2
    }
  ],
  "shippingAddress": {
    "addressLine1": "221B Baker Street",
    "addressLine2": "",
    "city": "London",
    "state": "Greater London",
    "postalCode": "NW16XE",
    "country": "UK"
  }
}
```

**Response (`201 Created`)** — **write down the `id` and the
`x-correlation-id` response header**, you'll use both in Kafka UI next:
```json
{
  "id": 42,
  "orderNumber": "ORD-20260905-A1B2C3",
  "userId": 16,
  "status": "PENDING",
  "currency": "USD",
  "paymentMethod": "CARD",
  "subtotal": 50.00,
  "shippingAmount": 0.00,
  "taxAmount": 0.00,
  "discountAmount": 0.00,
  "totalAmount": 50.00,
  "shippingAddress": {
    "addressLine1": "221B Baker Street",
    "addressLine2": "",
    "city": "London",
    "state": "Greater London",
    "postalCode": "NW16XE",
    "country": "UK"
  },
  "items": [
    {
      "id": 77,
      "productId": 101,
      "productSku": "SKU-TEST-001",
      "productName": "Test Wireless Mouse",
      "quantity": 2,
      "unitPrice": 25.00,
      "totalPrice": 50.00,
      "createdAt": "2026-09-05T10:02:00Z"
    }
  ],
  "createdAt": "2026-09-05T10:02:00Z",
  "updatedAt": "2026-09-05T10:02:00Z"
}
```

Look in the **response headers** (Swagger shows these below the response
body) for:
```
x-correlation-id: 7c9e6679-7425-40de-944b-e07fc1f90ae7
```
Copy that value — it's how you'll find "your" messages in Kafka UI in a
sea of other test data.

### Step 4 — Poll until it's done

**Where:** Order Service Swagger → `GET /api/orders/42` (use your real `id`)

Run this every couple of seconds. Watch `status` change:
```
PENDING  →  PAYMENT_PENDING  →  PAID
```
This usually takes well under a second in practice, but polling a few
times gives you a chance to actually catch it mid-flight in Kafka UI if
you're fast (see Step 3 in the next section).

---

## 4. Now Go Look at Kafka UI

Open **http://localhost:9099**.

### 4a. Confirm the topics exist

Click **Topics** in the sidebar. You should see:
```
shopsphere.order.events
shopsphere.inventory.events
shopsphere.payment.events
```

### 4b. Find your order's messages

Click into `shopsphere.order.events` → **Messages** tab.

- In the search/filter box, search for your order's `id` (e.g. `42`) — this
  is the message **Key** column.
- You should see exactly one message. Click to expand it — the JSON should
  look like this:

```json
{
  "eventId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "eventType": "OrderCreated",
  "eventVersion": 1,
  "occurredAt": "2026-09-05T10:02:00.123Z",
  "correlationId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "orderId": 42,
  "orderNumber": "ORD-20260905-A1B2C3",
  "userId": 16,
  "currency": "USD",
  "totalAmount": 50.00,
  "paymentMethod": "CARD",
  "items": [
    { "productId": 101, "sku": "SKU-TEST-001", "quantity": 2, "unitPrice": 25.00 }
  ]
}
```

Check that `correlationId` matches the header you copied in Step 3. ✅

Now do the same in `shopsphere.inventory.events` (search key `42`):

```json
{
  "eventId": "a1c3e5f7-....",
  "eventType": "InventoryReserved",
  "eventVersion": 1,
  "occurredAt": "2026-09-05T10:02:00.456Z",
  "correlationId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "orderId": 42,
  "orderNumber": "ORD-20260905-A1B2C3",
  "userId": 16,
  "currency": "USD",
  "totalAmount": 50.00,
  "paymentMethod": "CARD",
  "reservations": [
    { "productId": 101, "reservationId": 200, "quantity": 2 }
  ]
}
```

Same `correlationId` again. And finally `shopsphere.payment.events` (search
key `42`):

```json
{
  "eventId": "b2d4f6a8-....",
  "eventType": "PaymentSucceeded",
  "eventVersion": 1,
  "occurredAt": "2026-09-05T10:02:00.789Z",
  "correlationId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "orderId": 42,
  "orderNumber": "ORD-20260905-A1B2C3",
  "userId": 16,
  "paymentId": 300,
  "amount": 50.00,
  "currency": "USD"
}
```

Same `correlationId` a third time — that's your whole checkout's journey
across three services, tied together by one string.

### 4c. Watch it happen live (optional, but satisfying)

On the Messages tab, look for a **Live**/tail toggle (varies by Kafka UI
version — sometimes an icon near the filter bar, sometimes labeled
"Live mode"). Turn it on for `shopsphere.order.events`, then fire another
`POST /api/orders` from Swagger — you'll watch the message land in real
time within a second or two.

---

## 5. How the Producer Side Works

Every service that publishes events has one dedicated producer class (e.g.
`OrderEventProducer` in Order Service). It does exactly two things:

1. Turn the event object into a JSON string (using Jackson).
2. Call `kafkaTemplate.send(topicName, key, jsonString)`.

The **key** is always the `orderId`, turned into a string — e.g. `"42"`,
not the number `42`. That's what you saw in the Key column in Kafka UI.

```
OrderServiceImpl.createOrder()
        │
        │  order persisted, status = PENDING
        ▼
OrderEventProducer.publishOrderCreated(event)
        │
        │  event → JSON string
        │  kafkaTemplate.send("shopsphere.order.events", "42", jsonString)
        ▼
      Kafka
```

Nothing calls Kafka directly from a controller — publishing always goes
through one of these small producer classes, and it always happens *after*
the relevant database write has already succeeded (see the Onboarding
Guide's section on the "dual-write problem" for why that ordering still
isn't perfectly safe).

---

## 6. How Consumer Groups Work (and How to Watch Them)

Click **Consumer Groups** in Kafka UI's sidebar. You'll see five:

| Group name | Belongs to | Subscribed to |
|---|---|---|
| `shopsphere-inventory-service` | Inventory Service | `shopsphere.order.events` |
| `shopsphere-payment-service` | Payment Service | `shopsphere.inventory.events` |
| `shopsphere-order-service` | Order Service | `shopsphere.inventory.events`, `shopsphere.payment.events` |
| `shopsphere-cart-service` | Cart Service | `shopsphere.payment.events` |
| `shopsphere-notification-service` | Notification Service | all three topics |

**Why five groups instead of one?** Because a message on a topic is
delivered **once per consumer group**, not once total. Three different
services all need to see `shopsphere.payment.events` (Order Service, Cart
Service, Notification Service) — if they shared one group name, Kafka
would split those messages between them and each service would only see
some of them. Giving each service its own group name means each one gets
its own complete copy of every message it cares about.

**Click into a group**, e.g. `shopsphere-inventory-service`, and you'll see
a table with columns like:

| Topic | Partition | Current Offset | End Offset | Lag |
|---|---|---|---|---|

- **Current Offset** — how far this group has read.
- **End Offset** — how many messages exist in total.
- **Lag** = End Offset − Current Offset — how many messages this group
  hasn't processed yet.

Right after you create an order, refresh this page a couple of times — you
should see the offset tick up by 1 and lag return to `0` almost instantly.
**Lag that stays stuck above 0** means that service isn't keeping up or has
crashed — see §9 below.

---

## 7. How Topics Get Created

You don't need to create topics yourself. `docker-compose.yml` sets
`KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"` on the broker, so the **first
time any service tries to publish to a topic name that doesn't exist yet,
Kafka creates it automatically** with default settings (1 partition, per
this project's single-broker dev setup).

Practically: the very first time you ever run a checkout after a fresh
`docker compose up`, you're also implicitly creating all three topics. If
you check Kafka UI's Topics list *before* placing your first order, it may
be empty (or missing some topics) — that's expected, not a problem.

---

## 8. Quick Reference Card

| Thing | Value |
|---|---|
| Order Service Swagger | http://localhost:4004/swagger-ui.html |
| Product Service Swagger | http://localhost:4002/swagger-ui.html |
| Inventory Service Swagger | http://localhost:4005/swagger-ui.html |
| Kafka UI | http://localhost:9099 |
| Topic: order events | `shopsphere.order.events` |
| Topic: inventory events | `shopsphere.inventory.events` |
| Topic: payment events | `shopsphere.payment.events` |
| Message key (all topics) | `orderId`, as a string |
| Happy-path status progression | `PENDING` → `PAYMENT_PENDING` → `PAID` |
| Header to copy after checkout | `x-correlation-id` (in the response headers) |

---

## 9. If Something Doesn't Progress

Order stuck at `PENDING` and never moves?

1. **Check `shopsphere.order.events` in Kafka UI** — is there a message
   keyed by your order's `id`?
   - **No message at all** → check Order Service's own console log right
     after your `POST /api/orders` call for a line like
     `Failed to publish OrderCreated for orderId=42: ...`. This is logged,
     never thrown, so your `201 Created` response still looked successful
     even though the event never made it to Kafka.
   - **Message is there** → the publish worked; move to step 2.
2. **Check the `shopsphere-inventory-service` consumer group's lag** — is
   it stuck above 0? If so, check Inventory Service's own console log for
   an error right around the time you placed the order (its own database
   being unreachable is the most common cause).
3. **Check `shopsphere.inventory.events`** for an `InventoryReserved` (or
   `InventoryReservationFailed` — also valid progress, just a different
   outcome) message keyed by your order's `id`. Repeat the same "message
   there vs. not there" logic from step 1 to figure out if Inventory
   Service published successfully.
4. **Repeat for Payment Service** and `shopsphere.payment.events`.

This same message-by-message, topic-by-topic tracing is how you'd debug
*any* stuck order — the whole point of the correlation ID and the message
key is that you never have to guess which step broke.