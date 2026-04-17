# Auth Service

A comprehensive authentication and authorization microservice built with Spring Boot 3, providing JWT-based authentication, role-based access control, and security event publishing via Kafka.

## Features

- **JWT Token Management**: Access token generation, validation, and refresh token rotation
- **Role-Based Authorization**: Assign/remove roles, role-based endpoint protection
- **Password Management**: Secure password hashing, change password, password reset
- **Account Security**: Account locking after failed attempts, manual lock/unlock
- **Session Tracking**: Login history with pagination
- **Kafka Events**: Security event publishing for audit and monitoring
- **Metrics**: Login success/failure rates, active sessions, token refresh counts

## Tech Stack

- Java 17
- Spring Boot 3.x
- Spring Security
- Spring Data JPA
- PostgreSQL
- Apache Kafka
- JWT (jjwt)
- Micrometer (metrics)
- Docker

## API Endpoints

### Authentication (`/api/auth`)

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/register` | Register new user | No |
| POST | `/login` | User login | No |
| POST | `/logout` | User logout | Yes |
| POST | `/refresh` | Refresh access token | No |
| POST | `/validate` | Validate JWT token | No |
| PUT | `/change-password` | Change user password | Yes |
| POST | `/reset-password` | Request password reset | No |

### User Management (`/users/{userId}`)

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| PUT | `/lock` | Lock user account | Admin |
| PUT | `/unlock` | Unlock user account | Admin |
| GET | `/login-history` | Get login history (paginated) | Admin/Owner |

### Role Management (`/users/{userId}/roles`)

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/` | Get user roles | Admin/Owner |
| POST | `/` | Assign role to user | Admin |
| DELETE | `/{roleId}` | Remove role from user | Admin |

### Internal APIs (`/internal/auth`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/validate` | Validate token (service-to-service) |
| GET | `/users/{userId}` | Get user info (service-to-service) |

## JWT Token Structure

### Access Token Claims

```json
{
  "sub": "user@example.com",
  "authUserId": "uuid",
  "userId": 123,
  "roles": ["ROLE_USER", "ROLE_ADMIN"],
  "iat": 1234567890,
  "exp": 1234571490
}
```

### Token Expiration

- **Access Token**: 1 hour (configurable)
- **Refresh Token**: 24 hours (configurable)

## Authentication Flow

### Login Flow

```
1. Client sends POST /api/auth/login with email/password
2. Server validates credentials
3. If valid:
   - Generate access token (JWT)
   - Generate refresh token (stored in DB)
   - Publish user.login event to Kafka
   - Return tokens to client
4. If invalid:
   - Increment failed login attempts
   - Lock account if max attempts exceeded
   - Publish user.account.locked event if locked
   - Return 401 Unauthorized
```

### Token Refresh Flow

```
1. Client sends POST /api/auth/refresh with refresh token
2. Server validates refresh token (not expired, not revoked)
3. If valid:
   - Revoke old refresh token
   - Generate new access token
   - Generate new refresh token (rotation)
   - Return new tokens
4. If invalid:
   - Return 401 Unauthorized
```

## Kafka Events

Events are published to the `user-events` topic:

| Event Type | Description |
|------------|-------------|
| `user.registered` | New user registration |
| `user.login` | Successful login |
| `user.logout` | User logout |
| `user.password.changed` | Password changed |
| `user.password.reset.requested` | Password reset requested |
| `user.account.locked` | Account locked |
| `user.account.unlocked` | Account unlocked |

### Event Schema

```json
{
  "eventType": "user.login",
  "authUserId": "uuid",
  "userId": 123,
  "email": "user@example.com",
  "timestamp": "2024-01-01T12:00:00",
  "additionalInfo": "optional"
}
```

## Security Configuration

### Password Requirements

- Minimum 8 characters
- At least 1 uppercase letter
- At least 1 number

### Account Locking

- Account locks after 5 failed login attempts (configurable)
- Locked accounts require admin intervention to unlock

### Endpoint Security

```
/api/auth/register, /login, /refresh, /reset-password → Public
/api/auth/logout, /change-password, /validate → Authenticated
/users/{userId}/lock, /unlock → Admin only
/users/{userId}/roles (POST, DELETE) → Admin only
/internal/** → Internal services (no auth)
/actuator/** → Public (health checks)
```

## Configuration

### application.yaml

```yaml
server:
  port: 8081

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/auth_db
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}

jwt:
  secret: ${JWT_SECRET}
  expiration: 3600000        # 1 hour
  refresh-expiration: 86400000  # 24 hours

kafka:
  bootstrap-servers: localhost:9092
  topic:
    user-events: user-events

auth:
  max-failed-attempts: 5
```

### Environment Variables

| Variable | Description | Required |
|----------|-------------|----------|
| `DB_USERNAME` | Database username | Yes |
| `DB_PASSWORD` | Database password | Yes |
| `JWT_SECRET` | JWT signing secret (min 64 chars) | Yes |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka broker addresses | Yes |

## Running Locally

### Prerequisites

- Java 17+
- PostgreSQL 14+
- Apache Kafka
- Maven 3.8+

### Build

```bash
mvn clean package
```

### Run

```bash
java -jar target/auth_service-0.0.1-SNAPSHOT.jar
```

### Run with Docker

```bash
# Build image
docker build -t auth-service:latest .

# Run container
docker run -p 8081:8081 \
  -e DB_USERNAME=postgres \
  -e DB_PASSWORD=secret \
  -e JWT_SECRET=your-very-long-secret-key-at-least-64-characters \
  -e KAFKA_BOOTSTRAP_SERVERS=kafka:9092 \
  auth-service:latest
```

## Testing

### Run Tests

```bash
mvn test
```

### Run with Coverage

```bash
mvn test jacoco:report
```

Coverage report available at: `target/site/jacoco/index.html`

### Coverage Requirements

- Minimum 80% line coverage enforced by JaCoCo

## CI/CD

GitHub Actions workflow (`.github/workflows/ci-cd.yml`) includes:

1. **Build**: Compile and run tests
2. **Coverage**: Generate and check JaCoCo coverage
3. **Docker Build**: Build and push Docker image to GHCR
4. **Deploy**: Deploy to INT → UAT → PROD environments

## Metrics

Available at `/actuator/prometheus`:

| Metric | Description |
|--------|-------------|
| `auth.login.success` | Successful login count |
| `auth.login.failure` | Failed login count |
| `auth.registration` | Registration count |
| `auth.token.refresh` | Token refresh count |
| `auth.sessions.active` | Active session gauge |

## Health Check

```bash
curl http://localhost:8081/actuator/health
```

## Project Structure

```
src/main/java/com/example/auth_service/
├── application/
│   ├── dto/           # Request/Response DTOs
│   ├── mapper/        # Entity mappers
│   └── service/       # Business logic services
├── domain/
│   ├── event/         # Event models
│   └── model/         # JPA entities
├── infrastructure/
│   ├── config/        # Spring configurations
│   ├── event/         # Kafka producers
│   ├── exception/     # Custom exceptions
│   ├── persistence/   # JPA repositories
│   └── security/      # JWT, filters, security
└── presentation/
    └── rest/          # REST controllers
```

## License

MIT License
