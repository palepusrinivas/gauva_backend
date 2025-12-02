Gauva Backend
A Spring Boot (Java 17) backend for the Ride Fast application. Built with Maven, uses MySQL, Redis, JWT-based security, Flyway migrations, WebSockets for realtime, and integrations including Firebase Storage and Razorpay.

## Tech Stack
- Spring Boot 3.2.x (Web, Security, Validation, Data JPA, WebSocket, Actuator)
- Java 17
- Maven
- MySQL (production), H2 (tests)
- Redis (caching/pub-sub)
- JWT (jjwt)
- Flyway (DB migrations)
- springdoc-openapi (Swagger UI)
- Firebase Admin SDK + Google Cloud Storage
- Razorpay SDK
- Lombok, ModelMapper, OkHttp

## Prerequisites
- Java 17+
- Maven 3.9+
- MySQL 8.x (or compatible) running and accessible
- Redis server
- Optional: Docker and Docker Compose

## Project Structure
- `src/main/java/com/ridefast/ride_fast_backend` – Application source (entry: `RideFastBackendApplication`)
- `src/test/resources/application-test.properties` – Test configuration
- `pom.xml` – Dependencies and build plugins
- `Dockerfile` – Containerization file

## Configuration
This service is configured primarily via environment variables or Spring properties. Common properties:

- Security/CORS
  - `app.cors.allowed-origins` (default `*`)

- Admin seeding
  - `app.admin.username`
  - `app.admin.password`
  - `app.admin.role` (default `ROLE_ADMIN`)
  - `app.admin.seed-json-path` (optional, file path with admin seed JSON)
  - `app.admin.seed-json-b64` (optional, base64 of admin seed JSON)

- Payments (Razorpay)
  - `app.razorpay.key-id`
  - `app.razorpay.key-secret`
  - `app.razorpay.webhook-secret`
  - `app.wallet.commission-rate` (default `0.10`)

- Firebase / Storage
  - `app.firebase.project-id`
  - `app.firebase.storage-bucket`
  - `app.firebase.credentials-b64` (recommended) or `app.firebase.credentials-path`
  - `app.firebase.documents-gs-path` (e.g. `gs://<bucket>/documents`)
  - `app.firebase.logs-gs-path` (e.g. `gs://<bucket>/app_logs`)
  - `app.firebase.limits.doc-max-bytes` (default `2097152`)
  - `app.firebase.limits.log-max-bytes` (default `5242880`)

- Logging
  - `app.logging.log-path` (default `logs`)
  - `app.logging.upload-enabled` (default `false`)

- Email/SMS Notifications
  - `app.notify.email.from` (default `no-reply@example.com`)
  - `app.notify.sms.provider` (default `dummy`)
  - `app.notify.sms.api-key`
  - `app.notify.sms.sender-id` (default `RIDFST`)

- Redis
  - `spring.data.redis.host` (default `127.0.0.1`)
  - `spring.data.redis.port` (default `6379`)
  - `spring.data.redis.password` (optional)

- Database (MySQL)
  - `spring.datasource.url` (e.g. `jdbc:mysql://localhost:3306/ridefast?useSSL=false&serverTimezone=UTC`)
  - `spring.datasource.username`
  - `spring.datasource.password`
  - `spring.jpa.hibernate.ddl-auto` (recommended `validate`/`update` per environment)
  - Flyway properties as needed (defaults usually fine)

- Server
  - `server.port` (default `8080`)

Notes:
- Prefer supplying Firebase credentials via base64 env `app.firebase.credentials-b64` rather than committing JSON files.
- Ensure Razorpay secrets are provided securely as environment variables.

## Running Locally

### 1) Configure environment
Create an `.env` or export variables in your shell. Example (adjust for your setup):

```properties
# Server
server.port=8080

# DB
spring.datasource.url=jdbc:mysql://localhost:3306/ridefast?useSSL=false&serverTimezone=UTC
spring.datasource.username=ridefast
spring.datasource.password=ridefast
spring.jpa.hibernate.ddl-auto=update

# Redis
spring.data.redis.host=127.0.0.1
spring.data.redis.port=6379

# CORS
app.cors.allowed-origins=http://localhost:3000,https://guava-adminpanel.vercel.app/

# Admin seed
app.admin.username=admin
app.admin.password=admin123

# Firebase (example)
app.firebase.project-id=your-project-id
app.firebase.storage-bucket=your-bucket
app.firebase.credentials-b64=BASE64_OF_SERVICE_ACCOUNT_JSON

# Razorpay
app.razorpay.key-id=rzp_test_xxx
app.razorpay.key-secret=xxx
app.razorpay.webhook-secret=whsec_xxx
```

### 2) Build

- With tests:
  ```bash
  mvn clean package
  ```
- Skip tests:
  ```bash
  mvn clean package -DskipTests
  ```

### 3) Run

- Via Maven:
  ```bash
  mvn spring-boot:run
  ```

- Via JAR:
  ```bash
  java -jar target/ride_fast_backend-0.0.1-SNAPSHOT.jar
  ```

Application will start on `https://gauva-b7gaf7bwcwhqa0c6.canadacentral-01.azurewebsites.net` by default.

## Docker

Build the JAR first:
```bash
mvn clean package -DskipTests
```

Build image:
```bash
docker build -t ride-fast-backend:latest .
```

Run container (example envs):
```bash
docker run --name ride-fast-backend \
  -p 8080:8080 \
  -e SPRING_DATASOURCE_URL="jdbc:mysql://host.docker.internal:3306/ridefast?useSSL=false&serverTimezone=UTC" \
  -e SPRING_DATASOURCE_USERNAME=ridefast \
  -e SPRING_DATASOURCE_PASSWORD=ridefast \
  -e SPRING_DATA_REDIS_HOST=host.docker.internal \
  -e APP_CORS_ALLOWED_ORIGINS=http://localhost:3000 \
  -e APP_RAZORPAY_KEY_ID=rzp_test_xxx \
  -e APP_RAZORPAY_KEY_SECRET=xxx \
  -e APP_RAZORPAY_WEBHOOK_SECRET=whsec_xxx \
  -e APP_FIREBASE_PROJECT_ID=your-project-id \
  -e APP_FIREBASE_STORAGE_BUCKET=your-bucket \
  -e APP_FIREBASE_CREDENTIALS_B64=BASE64_OF_SERVICE_ACCOUNT_JSON \
  ride-fast-backend:latest
```

Note: Spring will map `APP_*` environment variables to `app.*` properties automatically.

## API Documentation
- Swagger UI: `https://gauva-b7gaf7bwcwhqa0c6.canadacentral-01.azurewebsites.net/swagger-ui/index.html`
- OpenAPI JSON: `https://gauva-b7gaf7bwcwhqa0c6.canadacentral-01.azurewebsites.net/v3/api-docs`

## Health and Monitoring
- Actuator health: `https://gauva-b7gaf7bwcwhqa0c6.canadacentral-01.azurewebsites.net/actuator/health`
- Other Actuator endpoints may require auth; configure as needed in security.

## Development Notes
- Default port: 8080 (Dockerfile exposes 8080)
- Logs directory: `logs` (configurable via `app.logging.log-path`)
- WebSocket is enabled for real-time features (STOMP over WebSocket)
- Flyway plugin is configured; ensure DB is reachable on startup for migrations

## Troubleshooting
- Cannot connect to DB: verify `spring.datasource.*` and DB is reachable
- Redis errors: ensure Redis host/port and network connectivity
- Firebase Storage errors: check service account credentials and bucket name
- Razorpay validation errors: verify key id/secret and webhook secret
- CORS issues: set `app.cors.allowed-origins` to your frontend origin(s)

## License
Proprietary. All rights reserved unless otherwise specified.
