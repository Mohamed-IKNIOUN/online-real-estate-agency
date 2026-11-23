ANNOUCEMENTS SERVICE

A Spring Boot microservice responsible for managing buyers' offers on real estate announcements in the Online Real Estate Agency platform.

## Features
- Submit, retrieve, update, and delete offers.
- Retrieve offers by buyer or announcement.
- Manage offer statuses (PENDING, ACCEPTED, REJECTED, WITHDRAWN).
- Validate incoming requests.
- Prevent buyers from submitting multiple offers for the same announcement.
- Handle API exceptions and validation errors.

## Technologies
- Java 25
- Spring Boot
- Spring Data JPA / Hibernate
- PostgreSQL
- Maven

## API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/offers` | Get all offers |
| GET | `/api/offers/{id}` | Get offer by ID |
| POST | `/api/offers` | Submit an offer |
| PUT | `/api/offers/{id}` | Update an offer |
| DELETE | `/api/offers/{id}` | Delete an offer |
| GET | `/api/offers/buyer/{buyerId}` | Get offers by buyer |
| GET | `/api/offers/announcement/{announcementId}` | Get offers by announcement |

## Configuration
Default port: `8082`

Database configuration is managed through `application.properties` and environment variables.