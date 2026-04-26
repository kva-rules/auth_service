# Auth Service

Authentication & authorization microservice. Issues JWT access + refresh tokens (HS384), manages users, roles, password resets, and token validation. The **API Gateway calls `/api/auth/validate` on every protected request** to decode tokens — every other downstream service simply trusts the `X-User-Id` / `X-User-Role` headers that the gateway injects after validation.

---

## At a glance
| | |
|---|---|
| **Port** | 8081 |
| **Database** | postgres-auth (`auth_db`) |
| **Kafka topics (out)** | `user.registered`, `user.password-reset` |
| **Kafka topics (in)** | none |
| **Swagger UI (direct)** | http://localhost:8081/swagger-ui.html |
| **Swagger UI (via gateway)** | http://localhost:8080/swagger-ui.html?urls.primaryName=auth-service |
| **OpenAPI JSON** | http://localhost:8081/v3/api-docs |
| **Java** | 21 (Temurin) |
| **Spring Boot** | 3.3.5 |

---

## What it does
- **Registers** users (`POST /api/auth/register`) — bcrypts password, assigns default role
- **Issues JWT on login** (`POST /api/auth/login`) — HS384 signed, default 1 h expiry
- **Refreshes tokens** (`POST /api/auth/refresh`) — rotating refresh tokens stored in `refresh_tokens` table
- **Validates tokens for the gateway** (`POST /api/auth/validate`) — the only endpoint the gateway calls unauthenticated
- **Password reset** (`POST /api/auth/reset-password`) — emits `user.password-reset` event
- **Manages roles** (`/api/roles/**`) — seed USER / ENGINEER / MANAGER / ADMIN
- **Admin user operations** (`/api/admin/users/**`) — list / lock / unlock / delete
- **Internal lookups** (`/internal/**`) — service-to-service (no JWT required)

---

## API surface

### Authentication (`/api/auth/**`) — mostly public
| Method | Path | Auth | Purpose |
|---|---|---|---|
| POST | `/api/auth/register` | public | Create user + return JWT |
| POST | `/api/auth/login` | public | Exchange credentials for JWT |
| POST | `/api/auth/refresh` | public | Swap refresh token for new access token |
| POST | `/api/auth/reset-password` | public | Trigger password reset flow |
| POST | `/api/auth/validate` | public | Decode + validate an access token (called by gateway) |
| POST | `/api/auth/logout` | JWT | Revoke current refresh token |
| GET | `/api/auth/me` | JWT | Current user profile |

### Internal (service-to-service, `/internal/**`)
| Method | Path | Auth | Purpose |
|---|---|---|---|
| GET | `/internal/users/{id}` | none | Lookup user by ID for reward/notification |
| GET | `/internal/users/by-email` | none | Lookup user by email |

### Roles (`/api/roles/**`)
| Method | Path | Auth | Purpose |
|---|---|---|---|
| GET | `/api/roles` | JWT | List all roles |
| POST | `/api/roles` | JWT + ADMIN | Create a new role |
| PUT | `/api/roles/{id}` | JWT + ADMIN | Update a role |

### Admin users (`/api/admin/users/**`)
| Method | Path | Auth | Purpose |
|---|---|---|---|
| GET | `/api/admin/users` | JWT + ADMIN | List all users |
| PUT | `/api/admin/users/{id}/lock` | JWT + ADMIN | Lock account |
| PUT | `/api/admin/users/{id}/unlock` | JWT + ADMIN | Unlock account |

Full live schema: **http://localhost:8081/swagger-ui.html** (or aggregated via gateway).

---

## Configuration

| Env var | Yaml key | Default | Purpose |
|---|---|---|---|
| `SERVER_PORT` | `server.port` | `8081` | HTTP listener |
| `SPRING_DATASOURCE_URL` | `spring.datasource.url` | `jdbc:postgresql://postgres-auth:5432/auth_db` | Postgres JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | | `postgres` | DB user |
| `SPRING_DATASOURCE_PASSWORD` | | `postgres` | DB password |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | `spring.kafka.bootstrap-servers` | `kafka:9092` | Kafka brokers |
| `JWT_SECRET` | `jwt.secret` | (shared HS384 key) | **MUST** match all other services |
| `JWT_EXPIRATION` | `jwt.expiration` | `3600000` | Access token expiry (ms) |
| `JWT_REFRESH_EXPIRATION` | `jwt.refresh-expiration` | `86400000` | Refresh token expiry (ms) |

Use `SPRING_PROFILES_ACTIVE=local` to target `localhost` Postgres/Kafka instead of Docker DNS.

---

## Kafka events produced
- **`user.registered`** — `{ userId, email, role, registeredAt }` — consumed by user-service to create an empty profile row
- **`user.password-reset`** — `{ userId, email, resetAt }` — consumed by notification-service

---

## Build & run

From project root (uses the repo's Java-21-aware wrapper):
```bash
./services.sh start auth-service
./services.sh logs auth-service
./services.sh stop auth-service
```

Direct Maven (requires Java 21):
```bash
export JAVA_HOME=/opt/homebrew/Cellar/openjdk@21/21.0.11
cd auth_service
mvn -DskipTests -Dmaven.test.skip=true spring-boot:run
```

---

## Docker

```bash
docker build -t auth-service:latest .
docker run --rm -p 8081:8081 \
  -e SPRING_PROFILES_ACTIVE=docker \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/auth_db \
  auth-service:latest
```

Base image: `eclipse-temurin:21-jre`.

---

## Kubernetes
- Manifest: `k8s/auth-service.yaml`
- Service DNS: `auth-service.default.svc.cluster.local`
- Gateway reaches it via `AUTH_SERVICE_URI=http://auth-service:8081`

---

## Troubleshooting

**"Role USER not found" → 500 on register**
Seed the roles table:
```sql
INSERT INTO roles (role_name) VALUES ('USER'),('ENGINEER'),('MANAGER'),('ADMIN');
```

**JWT signature mismatch across services**
`jwt.secret` must be identical in every service's yaml. Check all 8 services.

**`LazyInitializationException` in `JwtAuthenticationFilter`**
`User.userRoles` and `UserRole.role` must be `FetchType.EAGER`. Already fixed in this repo — don't revert to LAZY.

**Gateway returns 401 for valid tokens**
Gateway calls `POST /api/auth/validate`. Make sure `validate` is in `permitAll()` — it is by default in this repo's `SecurityConfig`.

---

## Tech stack
- Java 21 (Temurin)
- Spring Boot 3.3.5
- Spring Security + JJWT 0.12.6 (HS384)
- Spring Data JPA + PostgreSQL 16
- Spring Kafka
- springdoc-openapi 2.6.0
- Lombok 1.18.34, MapStruct 1.6.3
- `com.kva:common-library` 1.0.0 (shared `ApiResponse<T>` envelope)
