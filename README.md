# Order Service

Event-driven order processing service built with Spring Boot, MySQL, Flyway, Kafka, Docker Compose, and manual-auth-ready Spring Security.

The service accepts orders, publishes an order-created Kafka event, reserves inventory asynchronously, publishes the inventory result, and updates the order status from that result.

## Tech Stack

- Java 21
- Spring Boot 3
- Spring Web, Spring Data JPA, Validation, Actuator
- MySQL 8
- Flyway
- Apache Kafka
- Docker Compose
- Swagger UI / OpenAPI

## Architecture

```text
Client
  |
  v
Spring Boot app in IntelliJ (:8081)
  |
  v
Docker infra: MySQL, Kafka
  ^
  |
Kafka topics:
  order-created     -> InventoryConsumer reserves stock
  inventory-result  -> OrderStatusConsumer updates order status
```

## Main Flow

1. Client creates an order through `POST /api/v1/orders`.
2. The service stores an order snapshot with status `CREATED`.
3. The service publishes an `order-created` Kafka event.
4. The inventory consumer reserves stock using pessimistic locking.
5. The inventory reservation is stored in `inventory_reservations` so duplicate Kafka deliveries do not deduct stock twice.
6. The service publishes an `inventory-result` event.
7. The order status consumer updates the order to `INVENTORY_RESERVED` or `INVENTORY_REJECTED`.

## API

Swagger UI is available after startup:

- `http://localhost:8081/swagger-ui.html`

Useful endpoints:

```http
GET /api/v1/products?page=0&size=20&sort=id,desc
POST /api/v1/products
GET /api/v1/products/{id}
PUT /api/v1/products/{id}
DELETE /api/v1/products/{id}

GET /api/v1/orders?page=0&size=20&sort=id,desc
POST /api/v1/orders
GET /api/v1/orders/{id}
PUT /api/v1/orders/{id}
DELETE /api/v1/orders/{id}

GET /api/v1/inventory-reservations?page=0&size=20&sort=id,desc
GET /api/v1/inventory-reservations/{orderId}
DELETE /api/v1/inventory-reservations/{orderId}

GET /actuator/health
```

Successful API responses use a consistent wrapper:

```json
{
  "success": true,
  "message": "Products fetched",
  "data": {},
  "timestamp": "2026-06-12T00:00:00Z"
}
```

List endpoints return paging metadata inside `data`.

Manual authentication is being introduced in phase 1.5. Until the JWT filter is wired, existing product/order APIs remain open for local development.

Create order example:

```bash
curl -X POST http://localhost:8081/api/v1/orders \
  -H "Content-Type: application/json" \
  -d "{\"productId\":1,\"quantity\":2}"
```

Create product example:

```bash
curl -X POST http://localhost:8081/api/v1/products \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"Mechanical Keyboard\",\"price\":99.00,\"stockQuantity\":10}"
```

## Run Locally

Create your local environment file:

```bash
cp .env.example .env
```

On Windows PowerShell:

```powershell
Copy-Item .env.example .env
```

Start the infrastructure services:

```bash
docker compose up -d
```

Services:

- Kafka UI: `http://localhost:8080`
- MySQL: `localhost:3306`

Run the Spring Boot app from IntelliJ:

```text
com.example.ecommerce.OrderServiceApplication
```

The default local config expects:

```text
MySQL: jdbc:mysql://localhost:3306/orderdb
Kafka: localhost:29092
API: http://localhost:8081
```

Frontend dev origins allowed by default:

- `http://localhost:3000`
- `http://localhost:5173`
- `http://localhost:5174`

Change `CORS_ALLOWED_ORIGINS` in `.env` if your frontend runs on a different origin.

Stop the stack:

```bash
docker compose down
```

Remove database volume:

```bash
docker compose down -v
```

Run this when migrations changed or when your local database was created from an older schema.

Frontend base URL:

```text
http://localhost:8081/api/v1
```

## Test

```bash
./mvnw test
```

On Windows PowerShell:

```powershell
.\mvnw.cmd test
```

## Notes For Reviewers

- Inventory reservation uses pessimistic locking to prevent concurrent stock overselling.
- Kafka inventory processing is idempotent per order through the `inventory_reservations` table.
- Flyway owns the schema and JPA runs with `ddl-auto=validate`.
- Docker Compose starts only infrastructure services. Run the Spring Boot app from IntelliJ on port `8081`.
