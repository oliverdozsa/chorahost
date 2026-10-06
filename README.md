# chorahost

Backend API for a gamified household chore tracker. Teams (a household, a flat
share) post chores as **cards** with a rarity, XP and gold rewards. Members
claim cards, complete them, and an admin verifies the work, which pays out the
rewards and levels the member up.

Built with Quarkus 3 (Java 21), PostgreSQL, Flyway and Google OIDC.

## Concepts

- **Teams and roles.** A user can belong to many teams, with an independent
  role in each: `USER`, `ADMIN` or `TEAM_ADMIN` (the team creator). Level, XP
  and gold are tracked per team membership, not globally.
- **Invitation-only joining.** An `ADMIN` or `TEAM_ADMIN` invites an email
  address and gets back a one-time invite code (the API returns it; delivery is
  up to the caller). The invitee must be signed in with that same email to
  accept, by code or by invitation id. Invites expire after 7 days, and
  re-inviting an email revokes the earlier invite.
- **Chore cards.** Cards have a rarity (`COMMON`, `RARE`, `EPIC`, `LEGENDARY`),
  XP and gold rewards, an optional due date and an optional image. Lifecycle:
  `AVAILABLE` → `CLAIMED` → `COMPLETED` → `VERIFIED`. A claimed card can be
  unclaimed, and claimed cards past their due date are handled by a background
  job.
- **Templates and recurrence.** Card templates let admins respawn cards
  automatically, either on a schedule (daily, weekly, monthly, or every N
  minutes) or N minutes after the previous card was verified.
- **Levelling.** Reaching the next level takes `level × 1000` XP, with overflow
  carried into the following level.

## Getting started

Requirements: JDK 21 and Docker (Quarkus Dev Services starts a throwaway
PostgreSQL container for dev and tests, so no local database is needed).

```shell
./mvnw quarkus:dev
```

- API: <http://localhost:8080/api/v1>
- Dev UI: <http://localhost:8080/q/dev/>
- Health: <http://localhost:8080/q/health>

Dev mode seeds two teams with members and cards (alice, bob, carol, dave and
emily at `@example.com`).

### Authenticating

All `/api/*` endpoints require a Google-issued ID token as a bearer token. In
dev mode you can skip Google entirely and impersonate any user with a header:

```shell
curl -H 'X-Dev-User: alice@example.com' http://localhost:8080/api/v1/users/me
```

The header is only honored under the dev profile.

## API overview

Everything is under `/api/v1` and returns JSON. An OpenAPI description is
served by the SmallRye OpenAPI extension at `/q/openapi`.

| Area | Endpoints |
| --- | --- |
| Users | `GET /users/me`, `GET /users/{id}`, `GET /users/{id}/hand` |
| Teams | `POST /teams`, `GET /teams/{id}/leaderboard`, `GET /teams/{id}/cards`, `GET /teams/{id}/card-templates`, `POST /teams/{id}/role`, `PUT /teams/{id}/members/{userId}/gold` |
| Invitations (admin side) | `POST`/`GET /teams/{id}/invitations`, `DELETE /teams/{id}/invitations/{invitationId}` |
| Invitations (invitee side) | `GET /invitations`, `POST /invitations/accept` (body `{"code": "..."}`), `POST /invitations/{id}/accept` |
| Cards | `POST /cards`, `PUT`/`DELETE /cards/{id}`, `POST /cards/{id}/claim`, `/unclaim`, `/complete`, `/acknowledge-reward`, `/verify` |
| Card templates | `POST /card-templates`, `PUT`/`DELETE /card-templates/{id}` |

Creating, editing, deleting and verifying cards, and managing invitations,
require `ADMIN` or `TEAM_ADMIN` in the card's team. Assigning roles requires
`TEAM_ADMIN`.

Example: invite someone and accept as them.

```shell
curl -X POST -H 'X-Dev-User: alice@example.com' -H 'Content-Type: application/json' \
  -d '{"email":"frank@example.com"}' \
  http://localhost:8080/api/v1/teams/<teamId>/invitations
# -> {"id": "...", "code": "<invite code>", ...}

curl -X POST -H 'X-Dev-User: frank@example.com' -H 'Content-Type: application/json' \
  -d '{"code":"<invite code>"}' \
  http://localhost:8080/api/v1/invitations/accept
```

## Configuration

Set these for production (`%prod` profile); see `application.properties` for
the full list.

| Variable | Purpose | Default |
| --- | --- | --- |
| `DB_URL` | JDBC URL | `jdbc:postgresql://localhost:5432/chorahost` |
| `DB_USERNAME` / `DB_PASSWORD` | Database credentials | `chorahost` / `chorahost` |
| `CHORAHOST_OIDC_CLIENT_ID` | Google OAuth client id the tokens are issued for | (empty) |
| `CHORAHOST_OIDC_CLIENT_SECRET` | Google OAuth client secret | (empty) |
| `CORS_ORIGINS` | Allowed browser origins | `http://localhost:4200` |

The schema is owned by Flyway (`src/main/resources/db/migration`) and applied
at startup. Hibernate only validates it.

## Testing

```shell
./mvnw test
```

Tests are `@QuarkusTest` integration tests against a real PostgreSQL started by
Dev Services, so Docker must be running. Within a test class, methods run in a
fixed order and share state, and the database is wiped once per class.

## Packaging

```shell
./mvnw package                                   # target/quarkus-app/quarkus-run.jar
./mvnw package -Dquarkus.package.jar.type=uber-jar
./mvnw package -Dnative                          # native executable (needs GraalVM)
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

Run the default build with `java -jar target/quarkus-app/quarkus-run.jar`.
Dockerfiles for JVM and native images are in `src/main/docker`.

## Project layout

```
src/main/java/pub/galactic/chorahost/
  resource/    JAX-RS endpoints
  service/     business logic (teams, invitations, cards, recurrence, gamification)
  repository/  Panache repositories
  entity/      JPA entities
  dto/         request/response records
  security/    current-user resolution from the OIDC token
  dev/         dev-only data seeding and header auth
```
