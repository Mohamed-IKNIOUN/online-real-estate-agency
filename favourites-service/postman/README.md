# Local Postman tests for favourites-service

Use an isolated local stack and disposable data. Favourites requires a signed
access token from identity-service and allows a BUYER to access only their own
favourites. The three endpoint paths and success responses are preserved.

| Method | Path | Success |
| --- | --- | --- |
| POST | `/api/favoris/{clientId}/{annonceId}` | 201 with `id`, `clientId`, and `annonceId` |
| GET | `/api/favoris/client/{clientId}` | 200 with a JSON array |
| DELETE | `/api/favoris/{clientId}/{annonceId}` | 204 with an empty body |

## Start the local stack

Run identity-service, favourites-service, announcements-service, and PostgreSQL
in the isolated environment. The collection defaults to identity on port 8085,
favourites on 8083, and announcements on 8081. Set the local Postman
`identityUrl`, `baseUrl`, and `announcementsUrl` variables if those ports differ.

Configure the favourites PostgreSQL URL, username, and password in the process
environment. Supply `FAVOURITES_JWT_SECRET` from your local secret manager in
the same environment. It must be the Base64-encoded 256-bit HS256 key used by
the identity instance. Use a dedicated test key and keep it out of source
control, command history, and exported Postman files.

## Run the collection

1. Import [the collection](favourites-service.postman_collection.json). In a
   private local Postman environment, set `favouritesBuyerEmail` and
   `favouritesBuyerPassword` for a dedicated BUYER. Keep their values local
   and mark the password sensitive. Set `otherClientId` to the ID of a distinct
   test user in the same identity database.
2. Set `annonceId` to an existing disposable listing ID and
   `missingAnnonceId` to a different ID confirmed to return 404. Announcement
   GETs are public. Creating a fixture requires an authorized AGENT or ADMIN.
   Keep IDs within JavaScript's safe integer range.
3. Verify that the dedicated buyer has neither test favourite before running
   the main suite. Run the folder in the Collection Runner in order. Login
   stores the access token and owner ID in run-local variables. The suite
   checks authentication and ownership, creates the test favourite, checks
   duplicates and listing, and deletes the pair.
4. If the run stops after creation, remove only the pair created for this run.
   The outage folder logs in separately. Run it only against an isolated stack
   where announcements can be safely unavailable.

Keep the Postman environment and run results private. Login responses contain
tokens, so avoid exporting the environment, persisting response bodies, or
sharing the collection runner logs.
