# SecureNotesAPI

Thanks for your time in reviewing yet another SecureNotesAPI Readme.

## My design rationale

I looked at the stack that seemed like a good fit for BlueStaq, Tikia mentioned you are a Java shop and use AWS and Kubernetes.
So I went with Quarkus, a modern framework that's cloud-native, lightweight and was built specifically for Kubernetes.   
It also has great support for SmallRye JWT for bearer-token authentication and JUnit/REST Assured for unit tests.

I used PostgreSQL for persistence (with Hibernate ORM), Flyway for schema migrations, Swagger/OpenAPI for API documentation, plus things wouldn't be complete without the JavaDocs.

- `Quarkus`: fast startup, low memory use, strong container/Kubernetes fit, and good support for REST APIs.
- `SmallRye JWT`: validates bearer tokens and maps token groups to Quarkus security roles.
- `PostgreSQL`: production-grade relational database suitable for ownership and sharing authorization checks.
- `Flyway`: repeatable schema management for local, test, and production deployments.
- `Swagger/OpenAPI`: generated endpoint documentation for reviewers and client developers. Plus this just makes it super easy to test and use the API.
- `JUnit 5` and `REST Assured`: API behavior tests for authentication, ownership, sharing, and read-only access.

In terms of this stack in production, I think it's a solid design.  It helps scaling because the API is stateless, containerized, and database-backed.
It has excellent extensions for AWS IAM Roles for Service Accounts (IRSA), letting your pods securely assume GovCloud IAM roles.
Quarkus is great for lower cost on Kubernetes than Spring Boot (though Spring Boot would offer more on intensive security integrations).  Also, Quarkus would do well with compliance running on Iron Bank.
The slow startup of Spring Boot is another reason why Quarkus would be a better choice, as it starts in milliseconds.

The main bottleneck to manage in production will be PostgreSQL connections and query performance, not the Quarkus HTTP layer, but we'll get to that at the end section.


## Security Details - Role-Based Access Control (RBAC) & Group Restrictions

1. Clients authenticate through `/auth/register` or `/auth/login`.
2. The API returns a signed JWT containing the user ID in `sub` and the `user` group.
3. Protected `/notes` endpoints require a valid bearer token and the user must be a part of the `user` group.
4. The service layer resolves the current user from the token subject.  (Claims-Based Access Control)
5. PostgreSQL ownership and share records determine whether the request is authorized.  (Database Driven Authorization)
6. Importantly, I added a token generation step in the Docker Compose so that there are NO keys commited in this repo. (Secrets Management)

## Main components with Clean Architecture

I've separated the logic into the services and kept the endpoints as a clean controller, spearating concerns.

- `AuthResource`: registration and login endpoints.
- `NoteResource`: note and sharing endpoints.
- `AuthService`: password hashing and JWT creation.
- `NoteService`: ownership, sharing, and read-only enforcement.
- `AuditFilterService` :  filters audit
- `AuditLoggerService` :



## How To Run the Project


The app is run with this command:  

```powershell
docker compose up --build
```

The API starts at:

- API host: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/q/swagger-ui`
- OpenAPI document: `http://localhost:8080/q/openapi`


## How To Run the Tests

Tests are run with this command:

```powershell
mvn test
```

Docker Compose starts PostgreSQL, generates local development JWT keys in the ignored `secrets/` directory if they do not already exist, and starts the API.


Flyway applies the database schema automatically when the API starts.


The tests cover:

- Registration and duplicate username handling.
- Login failure behavior.
- Note create/read/update/delete by owner.
- Unshared note isolation.
- Read-only access for shared recipients.
- Authentication required for protected note endpoints.

I added two more tests that are helpful:





## API Behavior

Should be no surprises here.
All successful and error responses are JSON. Validation errors return HTTP `400` with a consistent error shape:

```json
{
  "timestamp": "2026-06-03T00:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "details": ["register.request.password: password must be between 12 and 128 characters"]
}
```

Authentication endpoints (see the Swagger):

| Method | Endpoint | Description |
| --- | --- | --- |
| `POST` | `/auth/register` | Register a new user and return a bearer token |
| `POST` | `/auth/login` | Authenticate and return a bearer token |

Notes endpoints:

| Method | Endpoint | Description |
| --- | --- | --- |
| `POST` | `/notes` | Create a new note |
| `GET` | `/notes` | List notes owned by or shared with the authenticated user |
| `GET` | `/notes/{id}` | Get a note if owned by or shared with the authenticated user |
| `PUT` | `/notes/{id}` | Update a note if owned by the authenticated user |
| `DELETE` | `/notes/{id}` | Delete a note if owned by the authenticated user |
| `POST` | `/notes/{id}/share` | Share a note read-only with another user |

Status code behavior:

- `200 OK`: successful login, registration, reads, updates, or idempotent share.
- `201 Created`: note created or new share created.
- `204 No Content`: note deleted.
- `400 Bad Request`: invalid request body or invalid sharing request.
- `401 Unauthorized`: missing, invalid, or expired JWT.
- `403 Forbidden`: authenticated user has read-only shared access but attempts to update or delete.
- `404 Not Found`: note or share target is not visible to the requester.
- <s>`409 Conflict`: duplicate username registration.</s>  I prefer to just return 404 here and not disclose if the user already exists, to slow user enumeration attacks.

## Example requests

Register:

```powershell
curl -X POST http://localhost:8080/auth/register `
  -H "Content-Type: application/json" `
  -d "{\"username\":\"alice\",\"password\":\"strong-password-123\"}"
```

Create a note:

```powershell
curl -X POST http://localhost:8080/notes `
  -H "Authorization: Bearer <token>" `
  -H "Content-Type: application/json" `
  -d "{\"content\":\"Mission notes\"}"
```

Share a note:

```powershell
curl -X POST http://localhost:8080/notes/1/share `
  -H "Authorization: Bearer <token>" `
  -H "Content-Type: application/json" `
  -d "{\"username\":\"bob\"}"
```

## Data model

The requested model is implemented with one naming adjustment: `User` is stored as `app_users` and `Share` is stored as `note_shares`. 
In Entity Framework and other ORMs, I've run into trouble with those names.  So I made them more specific to avoid conflicts with reserved or ambiguous SQL terms and make the database intent clearer.

| Table | Key columns |
| --- | --- |
| `app_users` | `id`, `username`, `password_hash`, `created_at` |
| `notes` | `id`, `owner_id`, `content`, `created_at`, `updated_at` |
| `note_shares` | `id`, `note_id`, `shared_with_user_id`, `created_at` |

Additional constraints and indexes:

- `app_users.username` is unique.
- `note_shares` has a unique `(note_id, shared_with_user_id)` constraint to prevent duplicate shares.
- Indexes exist for `notes.owner_id`, `note_shares.note_id`, and `note_shares.shared_with_user_id`.

## Security model

SmallRye JWT handles authentication and role recognition. The token identifies the user through the JWT `sub` claim and assigns the `user` group. Note ownership and sharing are intentionally enforced from PostgreSQL on each request instead of being stored inside JWT claims.

Access rules:

- Owners can create, read, update, delete, and share their notes.
- Shared recipients can read shared notes.
- Shared recipients cannot update, delete, or re-share notes.
- Notes that are neither owned by nor shared with the requester return `404` to avoid exposing private note existence.

Password handling:

- Passwords are hashed with BCrypt before storage.
- Plaintext passwords are never returned by the API.

JWT key handling:

- Runtime JWT keys are loaded from files outside the tracked application source.
- Local Docker Compose generates missing development keys into the ignored `secrets/` directory and mounts them read-only into the API container.
- Tests generate an ephemeral JWT key pair under `target/test-jwt-keys`; no test signing keys are committed.
- Regenerate local development keys manually with `.\scripts\generate-jwt-keys.ps1 -Force` if token-signing keys need to be rotated during development.
- Production should inject keys from a secret manager or use an external OIDC provider such as Amazon Cognito, Keycloak, Microsoft Entra ID Government, or Okta for Government.


## JavaDoc - Because Documentation Matters

No reason not to have this as auto-generated.  Generate JavaDoc with:

```powershell
mvn javadoc:javadoc
```

Output is written to `target/reports/apidocs/index.html`.

## How Would You Deploy this to Production?  Production deployment options

I stubbed out a simple CI.yml file with steps for intgration in Github, etc.

Recommended production paths:

- `AWS GovCloud ECS Fargate`: run the Docker image behind an Application Load Balancer and use Amazon RDS PostgreSQL.
- `AWS GovCloud EKS`: deploy the same container with Kubernetes manifests or Helm and use RDS PostgreSQL.
- `OpenShift`: suitable for organizations already standardized on Red Hat-supported Kubernetes.
- `EC2 with Docker`: viable for strict host-control environments, but it increases patching and operations burden.

Production changes should include:

- Managed PostgreSQL with encryption, backups, Multi-AZ where required, and least-privilege credentials.
- Externalized secrets through AWS Secrets Manager, SSM Parameter Store, Kubernetes Secrets, or an approved vault.
- Centralized identity through OIDC/SAML where enterprise SSO, MFA, CAC, or PIV is required.
- TLS termination, private networking, WAF/rate limiting, audit logging, metrics, tracing, and vulnerability scanning.
- Container image scanning and signed images in a trusted registry such as ECR.


## What would you monitor or alert on?

For compliance, we need an audit pipeline.  Security signals are going to be the most important to monitor for access to our Secure Notes.
I went ahead and stubbed this out in the app.  We use an AuditFilterService to generate an audit record for each access attempt and the AuditLoggerService logs the record.
The AuditFilterService will filter out any secrets, full tokens, passwords or unnecessary PII from the audit records.  The AuditLoggerService sends the the re ords to the audit pipeline.

We need to watch for things like when a normally active service is dark for a certain amount of minutes.

I'd set a threshold for errors to be notified when an error condition is happening repeatedly or consistently.
I'd monitor request volume to make sure the API isn't being abused.  Rate limiting should help with that, but it doesn't tell the whole story.  
We want to know if the application is being tested or brute forced.  We want to look for patterns in the logs like a burst of 401 responses from an IP (especially if followed by a 200) indicating a successful breakin-in.

Other things to alert for:
Certificate expiry - this one is classic.  Thankfully with certs being rotated on a tighetr schedule these days we can avoid what used to be a yearly issue.
Common error conditions like "sink unreachable", or "disk full", etc.



## How would you handle database migrations over time?

Here's where our use of Flyway shines.  We decouple the migration execution from the core application runtime and treat our schema updates entirely as code. 
Then as a part of CI/CD, we trigger an AWS CodeBuild project or an Amazon ECS task as a formal gate for updating the database.

BUT, running migrations inside an auto-scaling group during application startup introduces risks, such as split-brain scenarios where concurrent application instances attempt to modify the flyway_schema_history table at the exact same time.  So AWS recommends orchestrating migrations through a pipeline to ensure reliability.  After several years of development, your project might compile hundreds of legacy V1__init.sql, V2__add_users.sql type of files, slowing down initial spin-up.  At that time, you can baseline the database to combine all those historical stripts.  Flyway has good support for this.

## Scaling to 10,000 concurrent users

To support 10,000 concurrent users, the application would need production sizing and load testing based on requests per second, p95/p99 latency, and database utilization.

Required scaling changes:

- Run multiple stateless API instances behind a load balancer.
- Use ECS/EKS autoscaling based on CPU, memory, request rate, and latency.
- Store all persistent state in managed PostgreSQL.
- Tune Quarkus and database connection pools so API scaling does not exhaust PostgreSQL connections.
     OR we could look to integrate Amazon Aurora for the managed solution.
- Consider PgBouncer or RDS Proxy if connection counts become a bottleneck.
- Add and validate indexes for ownership and sharing queries under realistic data volume.
- Add rate limiting and throttling, especially for authentication and write endpoints. - I considered adding this currently, it's trivial but hugely important.
- Add centralized logs, metrics, traces, dashboards, and alerts.
- Run load tests before making capacity claims.

