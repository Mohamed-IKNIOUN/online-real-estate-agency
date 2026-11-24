VISITS SERVICE

A Spring Boot microservice responsible for managing property viewing requests and visit scheduling in the Online Real Estate Agency platform.

## Features
- Schedule, retrieve, and manage property visit requests.
- Filter visits by client, agent, or announcement.
- Manage visit statuses (`DEMANDEE`, `CONFIRMEE`, `REFUSEE`, `ANNULEE`).
- Validate incoming requests and prevent double-booking conflicts on the same time slot.
- Handle API exceptions and validation errors.

## Technologies
- Java 21
- Spring Boot
- Spring Data JPA / Hibernate
- PostgreSQL
- Maven
- Docker

## API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/visits` | Get all visits (supports optional `clientId`, `announcementId`, and `agentId` query parameters) |
| GET | `/api/visits/{id}` | Get visit by ID |
| POST | `/api/visits` | Schedule a new visit |
| PATCH | `/api/visits/{id}/status` | Update visit status (`DEMANDEE`, `CONFIRMEE`, `REFUSEE`, `ANNULEE`) |
| DELETE | `/api/visits/{id}` | Delete or cancel a visit |
| GET | `/api/visits/client/{clientId}` | Get visits by client |
| GET | `/api/visits/agent/{agentId}` | Get visits by agent |
| GET | `/api/visits/announcement/{announcementId}` | Get visits by announcement |

*(Note: Verify your `VisitController.java` to remove any route rows above that you did not explicitly define, such as `DELETE` or path-based filters if you only used query parameters).*

## Configuration
Default port: `8084`

Database configuration is managed through `application.properties` and environment variables (`POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`).