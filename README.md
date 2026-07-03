# EventSphere 🎟️

A production-grade distributed event ticketing platform — modelled after Ticketmaster — built as a microservices monorepo. Designed to demonstrate senior-level backend skills: distributed locking, event-driven architecture, idempotency, the Saga pattern, and zero-downtime deployments.

---

## Architecture

```
                        ┌──────────────────────────────────────┐
  Browser / Mobile ───▶ │  Next.js Frontend  (port 3000)       │
                        └────────────────┬─────────────────────┘
                                         │ HTTP
                        ┌────────────────▼─────────────────────┐
                        │  API Gateway  Go/chi  (port 8000)    │
                        │  • JWT validation                     │
                        │  • Rate limiting (Redis INCR)         │
                        │  • Reverse proxy per route            │
                        └──┬─────┬─────┬──────┬────────────────┘
                           │     │     │      │
              ┌────────────▼┐ ┌──▼──┐ ┌▼───┐ ┌▼────────────┐
              │ user-service│ │event│ │book│ │ticket/notif│
              │  Java/SB    │ │ -svc│ │-svc│ │   service  │
              │  JWT auth   │ │     │ │    │ │            │
              └─────────────┘ └──┬──┘ └──┬─┘ └────────────┘
                                 │       │
                        ┌────────▼───────▼──────────────────┐
                        │  RabbitMQ (topic exchange fan-out) │
                        │  eventsphere.bookings              │
                        │  eventsphere.events                │
                        │  eventsphere.payments              │
                        └──────────────┬────────────────────┘
                                       │
                        ┌─────────────▼──────────────────────┐
                        │  payment-service  Go/chi (port 8004)│
                        │  • Stripe integration               │
                        │  • Idempotent payments              │
                        │  • Webhook deduplication            │
                        └────────────────────────────────────┘
```

### Services

| Service | Lang | Port | Responsibility |
|---------|------|------|----------------|
| api-gateway | Go | 8000 | JWT auth, rate limiting, reverse proxy |
| user-service | Java 21 / Spring Boot 3.3 | 8001 | Auth, JWT, refresh tokens |
| event-service | Java 21 / Spring Boot 3.3 | 8002 | Events, venues, ticket types |
| booking-service | Java 21 / Spring Boot 3.3 | 8003 | Reservations with Redis distributed locking |
| payment-service | Go | 8004 | Stripe payments, idempotency |
| ticket-service | Java 21 / Spring Boot 3.3 | 8005 | QR-coded tickets, scan validation |
| notification-service | Java 21 / Spring Boot 3.3 | 8006 | Event-driven email via RabbitMQ |
| frontend | Next.js 14 / TypeScript | 3000 | Browse, book, manage events |

### Database isolation

All 5 Java services share one physical Postgres instance but each owns its **own schema** (`user_schema`, `event_schema`, `booking_schema`, `ticket_schema`, `notification_schema`), set via `currentSchema=<schema>,public` on the JDBC URL. This gives every service an independent Flyway migration history — each can ship its own `V1__create_*.sql` without colliding with another service's version history in `flyway_schema_history`. The `,public` fallback in the search path keeps `uuid_generate_v4()` and `pg_trgm` operators (installed once in `public` by `scripts/init-db.sql`) resolvable from every schema.

### Key Design Patterns

**1. Distributed Seat Locking (BookingService)**
- Named seats → `Redis SETNX` per seat with 15-minute TTL
- General admission → `Redis DECRBY` on quota counter
- Both paths roll back atomically on any failure before persisting to Postgres

**2. Payment Idempotency (PaymentService)**
- Every payment carries a client-supplied `idempotencyKey`
- `FindByIdempotencyKey` guard before any Stripe API call
- Stripe webhook events deduplicated via `processed_webhook_events` table

**3. Exactly-once Event Consumption**
- `ticket-service` and `notification-service` both maintain `processed_events` tables
- Before processing any RabbitMQ message, check `EXISTS(event_key)` — skip if seen before
- Failures rethrow to trigger RabbitMQ retry → DLQ after max-attempts

**4. Saga (Choreography)**
```
reserve → payment.initiated → payment.completed
       → booking.confirmed  → ticket.issued + email.sent
```
Each step publishes a domain event; downstream services react independently.

---

## Quick Start

### Prerequisites
- Docker Desktop with Compose V2
- 8 GB RAM (all services + infra)

### 1. Clone and configure
```bash
git clone https://github.com/yourname/eventsphere.git
cd eventsphere
cp .env.example .env
# Edit .env — at minimum set JWT_SECRET (32+ chars).
# Leave STRIPE_SECRET_KEY empty to run in stub mode (payments auto-succeed).
```

### 2. Start everything
```bash
docker-compose up --build
```

First build downloads ~1 GB of base images and Maven/Go dependencies. Subsequent builds are cached and take ~30 seconds.

### 3. Verify health
```bash
curl http://localhost:8000/health
# {"status":"healthy","service":"api-gateway"}

curl http://localhost:8001/actuator/health  # user-service
curl http://localhost:8002/actuator/health  # event-service
# ... 8003, 8004, 8005, 8006
```

### 4. Open the app
- **Frontend**: http://localhost:3000
- **RabbitMQ management**: http://localhost:15672 (guest/guest)
- **API docs** (Swagger): not yet wired — use the endpoints below directly

---

## API Reference

### Auth
```
POST /api/v1/auth/register   { email, password, firstName, lastName }
POST /api/v1/auth/login      { email, password }
POST /api/v1/auth/refresh    { refreshToken }
POST /api/v1/auth/logout     🔒
```

### Events
```
GET  /api/v1/events                  ?category=music&city=NYC&search=rock&page=0&size=20
GET  /api/v1/events/:id
GET  /api/v1/events/:id/seats
POST /api/v1/events                  🔒 ORGANIZER  { title, startTime, endTime, venue, ticketTypes[] }
PUT  /api/v1/events/:id              🔒 ORGANIZER
POST /api/v1/events/:id/publish      🔒 ORGANIZER
POST /api/v1/events/:id/cancel       🔒 ORGANIZER
GET  /api/v1/events/my               🔒 ORGANIZER
```

### Bookings (high-concurrency critical path)
```
POST /api/v1/bookings/reserve            🔒 { eventId, ticketTypeId, quantity, totalPrice }
GET  /api/v1/bookings/:id                🔒
POST /api/v1/bookings/:id/confirm        🔒 { paymentId }
POST /api/v1/bookings/:id/cancel         🔒
GET  /api/v1/users/:userId/bookings      🔒
```

### Payments
```
POST /api/v1/payments/initiate           🔒 { bookingId, amount, idempotencyKey }
GET  /api/v1/payments/:id/status         🔒
POST /api/v1/payments/:id/refund         🔒
POST /api/v1/payments/webhook               ← Stripe calls this (public)
```

### Tickets
```
GET  /api/v1/tickets/:id                 🔒 (returns base64 QR code PNG)
GET  /api/v1/users/:userId/tickets       🔒
POST /api/v1/tickets/:ticketNumber/validate  🔒 STAFF { location, deviceId }
```

---

## Running tests

### Booking service unit tests (mocked Redis + Rabbit)
```bash
cd booking-service
mvn test
```

The tests cover:
- `reserveSeats` — acquires N Redis locks, creates DB rows, publishes event
- Lock rollback — if seat 2 is already locked, seat 1's lock is released atomically
- `confirmBooking` — transitions `AWAITING_PAYMENT → CONFIRMED`
- `expireStaleReservations` — releases locks, marks `EXPIRED`

### Go services
```bash
cd api-gateway   && go test ./...
cd payment-service && go test ./...
```

---

## Environment variables

| Variable | Default | Description |
|----------|---------|-------------|
| `JWT_SECRET` | *(required)* | HMAC-SHA256 signing key, min 32 chars |
| `DB_PASSWORD` | `eventsphere_dev_pass` | Postgres password |
| `STRIPE_SECRET_KEY` | empty | Leave empty for stub/demo mode |
| `STRIPE_WEBHOOK_SECRET` | empty | Stripe webhook signing secret |
| `SENDGRID_API_KEY` | empty | Leave empty to log emails instead of sending |
| `REDIS_HOST` | `redis` | Redis hostname |
| `RABBITMQ_HOST` | `rabbitmq` | RabbitMQ hostname |

---

## Kubernetes

```bash
# Create namespace
kubectl create namespace eventsphere

# Apply manifests in order
kubectl apply -f kubernetes/configmaps/
kubectl apply -f kubernetes/secrets/        # edit values first!
kubectl apply -f kubernetes/deployments/infrastructure.yaml
kubectl apply -f kubernetes/services/services.yaml
kubectl apply -f kubernetes/deployments/microservices.yaml

# Check rollout
kubectl get pods -n eventsphere -w
```

---

## Known limitations / production gaps

1. **Notification email enrichment** — `notification-service` uses `userId@placeholder.eventsphere.com` as recipient email because no inter-service HTTP client to `user-service` was built. In production: add a feign/httpx call to look up the user's real email.

2. **Ticket quota initialisation** — The Redis quota key (`event:quota:{eventId}:{ticketTypeId}`) is used by `booking-service` but never seeded from `event-service`'s actual inventory. In production: seed the key when an event is published (via the `event.published` RabbitMQ message).

3. **No Swagger/OpenAPI** — Add `springdoc-openapi` to each Java service's pom for auto-generated docs.

4. **Single Postgres instance** — All services share one Postgres container, isolated via per-service schemas (see "Database isolation" above). This is fine for local dev/demo; in production use a managed cloud Postgres (RDS, Cloud SQL, Supabase) or split into per-service databases entirely.

5. **Secret management** — Never commit `kubernetes/secrets/eventsphere-secrets.yaml` with real values. Use [external-secrets](https://external-secrets.io/) + Vault or AWS Secrets Manager in production.

---

## Tech stack

**Backend**
- Java 21 + Spring Boot 3.3 (web, data-jpa, security, amqp, redis, actuator)
- Go 1.21 + chi/v5 (api-gateway, payment-service)
- PostgreSQL 15 (primary datastore, Flyway migrations)
- Redis 7 (distributed locking, rate limiting, caching)
- RabbitMQ 3.12 (event bus, topic exchanges, DLQ)
- ZXing 3.5 (QR code generation)
- jjwt 0.12 (JWT signing/verification)
- Stripe Go SDK v76 (payment processing)

**Frontend**
- Next.js 14 (App Router)
- React 18 + TypeScript
- TanStack Query v5
- Tailwind CSS

**Infrastructure**
- Docker Compose (local dev)
- Kubernetes (production)
- GitHub Actions (CI/CD)
