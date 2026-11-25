# Energy Management System

Distributed Systems – Assignment 1 (request-reply communication), Assignment 2 (asynchronous communication) and Project P1 (Docker deployment + reverse proxy).

Authenticated users access, monitor and manage smart energy metering devices. The system is a set of loosely coupled,
containerized microservices behind a reverse proxy / API gateway.

## Architecture

UML deployment diagram: [`docs/deployment-diagram.puml`](docs/deployment-diagram.puml)
(render with PlantUML, e.g. the IntelliJ or VS Code plugin, or <https://www.plantuml.com/plantuml>).

| Component | Technology | Responsibility |
|---|---|---|
| **Reverse proxy / API gateway** | Traefik v3.6 + `forwardAuth` middleware | Only published entry point (port 80). Routes requests and asks the Authorization Service to validate the token and the permissions of every protected request before forwarding it. |
| **Frontend** | React 18 (Vite) served by nginx | Login / register, role-based pages. Admin: CRUD on users and devices, assign devices to users. Client: list of own devices. |
| **Authorization Service** (`auth-service`) | Spring Boot, JWT (HS256), PostgreSQL | Login, register, token generation, token + permission validation for the gateway. Credential Database. |
| **User Management** (`user-service`) | Spring Boot, JPA, PostgreSQL | CRUD on users. User Database. Keeps the credentials in sync with the Authorization Service. |
| **Device Management** (`device-service`) | Spring Boot, JPA, PostgreSQL | CRUD on devices, device ↔ user mapping. Device Database. |

| **Monitoring** (`monitoring-service`) | Spring Boot, JPA, PostgreSQL, Spring AMQP | *Consumer.* Reads the smart meter readings from the Data Collection Broker, computes the **hourly totals** and stores them in the Monitoring Database. Exposes the hourly consumption of a device for one day. |
| **Synchronization Broker** (`sync-broker`) | RabbitMQ 3.13 | Topic exchange `sync.events`. User and device events travel through it; every consuming service owns a queue bound to it. Not reachable from the host except its management UI. |
| **Data Collection Broker** (`data-broker`) | RabbitMQ 3.13 | Durable queue `device-measurements` (point-to-point). Its AMQP port is published so the desktop simulator can connect. |
| **Device Data Simulator** (`device-simulator/`) | Java 21, Swing, RabbitMQ client | *Producer.* Standalone desktop application (not a container). Sends `{"timestamp","device_id","measurement_value"}` every 10 minutes. |

Each service has its own database on its own Docker network; the databases are not reachable from anywhere else.

### Request flow

1. The browser sends `POST /api/auth/login` (public route) and receives a JWT plus `userId`, `username` and `role`.
2. For every other API call the frontend sends `Authorization: Bearer <token>`.
3. Traefik calls `GET /api/auth/validate` on the Authorization Service (forwardAuth) with the original method and URI
   (`X-Forwarded-Method`, `X-Forwarded-Uri`). The service checks the signature and expiry of the token, that the user
   still exists, and applies the access rules below.
   - `401` – token missing, invalid or expired; `403` – role not allowed. Traefik returns that response to the browser.
   - `200` – Traefik forwards the request and adds `X-User-Id`, `X-User-Name`, `X-User-Role`.
4. `/api/auth/validate` and `/internal/**` have **no** route in Traefik, so they can only be called from inside the
   Docker network.

### Access rules

| Role | Allowed |
|---|---|
| `ADMIN` | everything under `/api/users/**`, `/api/devices/**` and `/api/monitoring/**` |
| `CLIENT` | `GET /api/users/{ownId}`, `GET /api/devices/user/{ownId}` and `GET /api/monitoring/devices/{id}/hourly` (only for a device assigned to them – checked by the Monitoring service) |
| none | `POST /api/auth/login`, `POST /api/auth/register`, Swagger UIs |

Self-registration always creates a `CLIENT`. The first administrator is created at startup from `ADMIN_USERNAME` /
`ADMIN_PASSWORD` (see below).

### Keeping users and credentials consistent

The User service owns the user data; the Authorization Service owns the credentials (BCrypt hashes). When a user is
created, updated or deleted, the User service calls `PUT/DELETE /internal/credentials/{userId}` on the Authorization
Service. If that call fails, the operation fails with `503` (a user that was just created is rolled back). Because the
gateway also checks that the user still exists in the Credential Database, deleting a user invalidates their token
immediately.

## Asynchronous communication (Assignment 2)

```
 Device Data Simulator --(JSON, AMQP)--> [ Data Collection Broker: queue device-measurements ] --> Monitoring service --> Monitoring DB

 User service   --user.created|updated|deleted-->  [ Synchronization Broker: topic exchange sync.events ]
 Device service --device.created|updated|deleted-->        |  queue sync.device-service.users      (binding user.*)   --> Device service
                                                           |  queue sync.monitoring-service.devices (binding device.*) --> Monitoring service
```

**Measurements.** The simulator sends one reading per device every 10 minutes. `measurement_value` is the energy (kWh)
consumed in that 10-minute interval. The Monitoring service logs every message it extracts from the queue
(`[data-broker] message extracted from queue: ...`), stores the raw reading and adds it to the total of its full hour
(`hourly_consumption`). A reading with the same device and timestamp is ignored, so a redelivered message is never counted
twice. Readings of unknown devices and malformed messages are logged and dropped (no endless redelivery).

**Synchronization.** Every synchronization event is published to the Synchronization Broker, as JSON, with the routing key
`user.*` / `device.*`:

| Event | Published by | Consumed by | Effect |
|---|---|---|---|
| `user.created` / `user.updated` | User service | Device service | user id (+ username, role) is upserted into the local `user_refs` table |
| `user.deleted` | User service | Device service | local copy removed, devices of that user are unassigned (and `device.updated` is published) |
| `device.created` / `device.updated` (also assign / unassign) | Device service | Monitoring service | device id, name, max consumption and owner are upserted into `device_refs` |
| `device.deleted` | Device service | Monitoring service | device and its measurements are removed |

The password never leaves the User service. At startup the User and Device services re-publish everything they own
(consumers upsert, so this is idempotent), which repairs the other databases after a restore from a dump or a lost message.

**Queue or topic?** The readings use a plain *queue* (point-to-point): each reading must be processed by exactly one consumer.
The synchronization events use a *topic exchange* with one queue per consuming service (publish/subscribe): if all services
read one shared queue they would compete for the messages and each event would reach only one of them.

**Who may see what.** `GET /api/monitoring/devices/{deviceId}/hourly?date=yyyy-MM-dd&tz=Europe/Bucharest`
(24 values, kWh). The gateway lets a `CLIENT` call it; the Monitoring service then checks that the device is assigned to the
caller (`X-User-Id`, set by the gateway) and answers `403` otherwise. An `ADMIN` can read every device.

## Prerequisites

- Docker with Docker Compose v2 (the only mandatory tool)
- For the simulator: JDK 21+ and Maven 3.9+ (or run the prebuilt jar)
- For local development without Docker: JDK 25, Maven 3.9+, Node 22

## Build and run

```bash
cp .env.example .env        # then edit: passwords, JWT_SECRET (>= 32 chars), ADMIN_PASSWORD
docker compose up --build
```

| What | URL |
|---|---|
| Web application | <http://localhost/> |
| Traefik dashboard | <http://localhost:8090/> |
| Swagger – auth | <http://localhost/api/auth/swagger-ui.html> |
| Swagger – users | <http://localhost/api/users/swagger-ui.html> |
| Swagger – devices | <http://localhost/api/devices/swagger-ui.html> |
| Swagger – monitoring | <http://localhost/api/monitoring/swagger-ui.html> |
| RabbitMQ UI – synchronization broker | <http://localhost:15672/> (`SYNC_MQ_USER` / `SYNC_MQ_PASSWORD`) |
| RabbitMQ UI – data collection broker | <http://localhost:15673/> (`DATA_MQ_USER` / `DATA_MQ_PASSWORD`) |

Sign in as `ADMIN_USERNAME` / `ADMIN_PASSWORD` from `.env`. The administrator is created a few seconds after startup,
once the Authorization Service is ready (the User service retries until then).

To call protected endpoints from Swagger UI: log in with `POST /api/auth/login`, copy the `token`, click **Authorize**
and paste it.

Stop with `docker compose down` (add `-v` to delete the database volumes).

If port 80 or 8090 is taken, change `GATEWAY_PORT` / `TRAEFIK_DASHBOARD_PORT` in `.env`.

### Run the Device Data Simulator

```bash
cd device-simulator
mvn -B package                      # builds target/device-simulator.jar
java -jar target/device-simulator.jar
```

1. In the web application (admin) create a client and a device and assign the device to the client; copy the **device id**
   (Swagger UI of the device service, or the table in the admin page).
2. In the simulator window enter the broker (`localhost`, port `5672`, user/password = `DATA_MQ_USER` / `DATA_MQ_PASSWORD`
   from `.env`), paste one or more device ids (comma separated) and press **Start**.
   * *Seconds between readings* = 600 is real time (one reading every 10 minutes); use a small value (e.g. 5) for a fast demo.
   * *Send today's readings so far* generates all the 10-minute readings from 00:00 until now immediately, so the chart of
     today is already filled.
3. Watch the messages arrive: `docker compose logs -f monitoring-service`.
4. Sign in as the client and open the chart under *My devices* (pick a device and a day from the calendar).

Headless mode (no window): `java -jar target/device-simulator.jar --headless --devices <id1>,<id2> --password <DATA_MQ_PASSWORD> --interval 600 --backfill`

### Configuration (`.env`)

| Variable | Description |
|---|---|
| `GATEWAY_PORT`, `TRAEFIK_DASHBOARD_PORT` | published ports of the proxy |
| `AUTH_DB_*`, `USER_DB_*`, `DEVICE_DB_*`, `MONITORING_DB_*` | database name, user and password of each service |
| `SYNC_MQ_USER`, `SYNC_MQ_PASSWORD`, `SYNC_MQ_MANAGEMENT_PORT` | Synchronization Broker credentials and management UI port |
| `DATA_MQ_USER`, `DATA_MQ_PASSWORD`, `DATA_MQ_PORT`, `DATA_MQ_MANAGEMENT_PORT` | Data Collection Broker credentials, AMQP port (used by the simulator) and management UI port |
| `JWT_SECRET` | HS256 signing key, at least 32 characters |
| `JWT_EXPIRATION_MINUTES` | token lifetime (default 60) |
| `ADMIN_USERNAME`, `ADMIN_PASSWORD` | bootstrap administrator (password at least 6 characters) |

`.env` is git-ignored; never commit real secrets.

### Frontend development

```bash
docker compose up -d --build        # backend through the proxy on :80
cd frontend && npm install && npm run dev    # http://localhost:5173, /api is proxied to the gateway
```

## REST API (summary – full documentation in Swagger)

| Method | Path | Description | Role |
|---|---|---|---|
| POST | `/api/auth/login` | log in, returns JWT | public |
| POST | `/api/auth/register` | create a client account | public |
| GET | `/api/users` | list users | ADMIN |
| GET | `/api/users/{id}` | get a user | ADMIN, owner |
| POST | `/api/users` | create a user | ADMIN |
| PUT | `/api/users/{id}` | update a user (password optional) | ADMIN |
| DELETE | `/api/users/{id}` | delete a user, unassigns their devices | ADMIN |
| GET | `/api/devices` | list devices | ADMIN |
| GET | `/api/devices/{id}` | get a device | ADMIN |
| GET | `/api/devices/user/{userId}` | devices of a user | ADMIN, owner |
| POST | `/api/devices` | create a device | ADMIN |
| PUT | `/api/devices/{id}` | update a device | ADMIN |
| DELETE | `/api/devices/{id}` | delete a device | ADMIN |
| PUT | `/api/devices/{id}/user` | assign a device to a user `{ "userId": "..." }` | ADMIN |
| DELETE | `/api/devices/{id}/user` | remove the assignment | ADMIN |
| GET | `/api/monitoring/devices/{id}/hourly?date=&tz=` | hourly consumption (kWh) of a device for one day | ADMIN, owner of the device |

Errors have the form `{ timestamp, status, error, message, details }` (`400` validation, `401`, `403`, `404`,
`409` duplicate username, `503` another service is down).

Data model: **user** – id (UUID), username, password (BCrypt), name, email, role; **device** – id (UUID), name,
description, maximum consumption (kWh), owner user id (nullable); **credential** – user id, username, password hash, role.
Local copies filled by the synchronization queue: **user_refs** (Device DB), **device_refs** (Monitoring DB). Monitoring DB:
**measurements** (raw 10-minute readings) and **hourly_consumption** (device, full hour, total kWh).

## Testing

With the stack running, `ADMIN_PASSWORD=<value from .env> ./docs/smoke-test.sh` checks the whole flow through the
proxy: 401 without token, login, admin CRUD and assignment, client restrictions (200 for own devices, 403 otherwise),
and that a deleted user's token stops working. It also publishes one reading through the Data Collection Broker
(management API) and checks that the client sees it in the hourly consumption (`DATA_MQ_PASSWORD` must be set too).

## Database dumps

`docs/auth_db_dump.sql`, `docs/user_db_dump.sql`, `docs/device_db_dump.sql`, `docs/monitoring_db_dump.sql`. Regenerate them from the running
containers with `./docs/export-db-dumps.sh`.

## Repository layout

```
auth-service/      Authorization Service (login, register, JWT, gateway validation)
user-service/      User Management microservice
device-service/    Device Management microservice
monitoring-service/ Monitoring microservice (message consumer, hourly totals)
device-simulator/  Device Data Simulator (desktop application, message producer)
frontend/          React web client + nginx
docs/              deployment diagram, DB dumps, smoke test
docker-compose.yml Traefik + services + databases
.env.example       configuration template
```
