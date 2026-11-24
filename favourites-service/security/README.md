# Local favourites-service security checks

## Scope

Branch `quality-favourites`. The ZAP plan sends one ordinary GET to
`http://127.0.0.1:8083/api/favoris/client/2026112401`, waits for passive
analysis, and writes JSON and HTML reports. The test client has an empty
favourites list. The scan covers the sampled response only.

## ZAP results

ZAP 2.17.0 completed the plan successfully and reported one Low alert
with Medium confidence (rule 10021). The HTTP 200 JSON response lacked
`X-Content-Type-Options`. Serve API responses with the correct
`Content-Type` and add `X-Content-Type-Options: nosniff` through the
application or gateway response policy.

The scan writes local reports as `zap-passive.html` and `zap-passive.json`.

## Manual review

**High before external exposure — missing authentication and ownership checks
at the service boundary.** The direct GET returned HTTP 200 with no
credentials. The controller passes the caller-supplied client ID to the
service, and the read queries favourites by that ID. The observed response
contained an empty list. Require an authenticated identity and enforce
client ownership on read, add, and delete, or enforce equivalent controls
at a trusted gateway and restrict direct service access.

## Snyk dependency scan

The Spring Boot 4.2.0 after-scan covered 84 dependencies
and reported two findings. The supplied 4.1.2 baseline had 14 findings
(1 Critical, 4 High, 8 Medium, 1 Low). The baseline JSON is unavailable,
so the comparison is by severity totals.

| Severity | Before | After |
| --- | ---: | ---: |
| Critical | 1 | 0 |
| High | 4 | 1 |
| Medium | 8 | 0 |
| Low | 1 | 1 |
| **Total** | **14** | **2** |

- **High · JAXB XXE (CVE-2026-92877).** `jaxb-core:4.0.9` is pulled in
  by Hibernate ORM through `jaxb-runtime`. Snyk lists no fixed version
  or upgrade path. Track a compatible upstream fix. The current
  favourites routes take numeric path parameters and expose no
  application path that unmarshals untrusted XML.
- **Low · Improper input validation (SNYK-JAVA-JAKARTAVALIDATION-20936881).**
  `jakarta.validation-api:3.1.1` comes through Hibernate Validator.
  Snyk lists `4.0.0-M1` as fixed, with no managed upgrade path.
  Prefer a compatible stable version managed by Spring Boot and
  Hibernate Validator.

Both findings remain open. Snyk uses its hosted vulnerability service.
From the repository root, use browser-based OAuth and scan only the
favourites-service Maven project:

```powershell
npx.cmd --yes snyk auth
Push-Location .\favourites-service
try {
    npx.cmd --yes snyk test --file=pom.xml --json-file-output=security/snyk-after.json
} finally {
    Pop-Location
}
```

Complete the sign-in and authorization in the browser. OAuth credentials
are managed by the CLI in the user profile. Exit code 1 means Snyk found
vulnerabilities. Check the command output and JSON for any scan errors.

[Snyk CLI authentication](https://docs.snyk.io/developer-tools/snyk-cli/authenticate-to-use-the-cli)
