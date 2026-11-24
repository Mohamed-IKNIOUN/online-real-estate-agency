# Local Postman tests for favourites-service

| Method | Path | Success | Dependencies |
| --- | --- | --- | --- |
| POST | `/api/favoris/{clientId}/{annonceId}` | 201 with a favourite object | PostgreSQL and a reachable announcements service for a new pair |
| GET | `/api/favoris/client/{clientId}` | 200 with an array of favourite objects | PostgreSQL |
| DELETE | `/api/favoris/{clientId}/{annonceId}` | 204 with an empty body | PostgreSQL |

No request body or authentication is required by the current favourites API. A favourite object exposes `id`, `clientId`, and `annonceId`. IDs in the path must be positive integers that fit a Java `Long`. Add checks PostgreSQL for duplicates before looking up the announcement. The HTTP client maps an upstream 404 to 404, a connection failure to 503, and other upstream HTTP errors to 502.

## Start the local stack

Use Docker Compose and a local Java 21 installation with Maven. From the repository root, start PostgreSQL and the announcements container.

```powershell
docker compose up -d postgres announcements-service
docker compose ps
```

Compose publishes PostgreSQL on port 5433 and provisions the `agence_immobiliere` database. In a dedicated PowerShell terminal opened at the repository root, provide `SPRING_DATASOURCE_PASSWORD` through your local secret manager or the masked prompt below. The environment overrides apply to this shell. The standalone favourites configuration defaults to a different database named `favourites_db`.

```powershell
$password = Read-Host 'Local PostgreSQL password' -AsSecureString
try {
    $env:SPRING_DATASOURCE_PASSWORD = [System.Net.NetworkCredential]::new('', $password).Password
} finally {
    $password.Dispose()
}
```

Start favourites on port 8083 with the Compose database and local announcement URL. Close the dedicated terminal after testing to clear its environment.

```powershell
Set-Location favourites-service
$env:SPRING_DATASOURCE_URL = 'jdbc:postgresql://localhost:5433/agence_immobiliere'
$env:SPRING_DATASOURCE_USERNAME = 'agence_admin'
if (-not $env:SPRING_DATASOURCE_PASSWORD) { throw 'Set SPRING_DATASOURCE_PASSWORD for this shell first.' }
$env:ANNOUNCEMENTS_SERVICE_URL = 'http://localhost:8081/api/announcements'
mvn spring-boot:run
```

Wait for the application startup message. If PostgreSQL is already running on port 5433, use its database and credentials instead of starting another instance on the same port. If announcements is already running locally, use that instance.

## Import and run

1. Import [the Postman collection](favourites-service.postman_collection.json) in the Postman desktop app. Use a disposable local database and a dedicated test `clientId`. The default client ID is an example. Before running the collection, ensure the client has no favourite with either fixture announcement ID.
2. Get a valid `annonceId` from `GET http://localhost:8081/api/announcements`. If the list is empty, send a JSON POST to the same URL with `Content-Type: application/json` and copy the returned `id`. Example request body follows.

   ```json
   {
     "title": "Postman fixture",
     "description": "Disposable local listing",
     "price": 1200.00,
     "propertyType": "APARTMENT",
     "transactionType": "RENT",
     "city": "Casablanca",
     "area": 65.00
   }
   ```

3. Choose a different positive integer for `missingAnnonceId` and confirm that `GET /api/announcements/{missingAnnonceId}` returns 404 on port 8081. Set both fixture IDs in the collection variables. Keep the fixture IDs and test client ID within JavaScript's safe integer range so the response assertions can compare IDs exactly.
4. Run **Main suite — announcements running** in the Collection Runner in the listed order. It checks both fixtures, runs invalid-input and missing-announcement requests, creates a favourite, verifies that a duplicate returns 409, checks the list, and deletes the test favourite. If the run stops after creation, send **Delete favourite** for the dedicated test pair before rerunning. A listing created as a fixture can be removed separately through the announcements API once testing is finished.
5. To run **Outage check — run separately**, first complete the main suite so the pair is absent. From the repository root, run `docker compose stop announcements-service` while PostgreSQL and favourites stay up. Send the outage request and check for 503. Then restore announcements with `docker compose start announcements-service`. A duplicate pair would return 409 before the HTTP lookup, so keep the pair absent.
