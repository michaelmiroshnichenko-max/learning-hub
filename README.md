# Learning Hub

[![CI](https://github.com/michaelmiroshnichenko-max/learning-hub/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/michaelmiroshnichenko-max/learning-hub/actions/workflows/ci.yml)

Personal learning notes and course progress tracker. Quarkus 3.40 on Java 25.
See [docs/vision.md](docs/vision.md), [docs/architecture.md](docs/architecture.md) and
[docs/roadmap.md](docs/roadmap.md).

## Prerequisites

- JDK 25 (`JAVA_HOME` must point to it)
- No Maven install needed: use the Maven wrapper (`./mvnw`, or `.\mvnw.cmd` on Windows)

## Build and test

```shell
./mvnw verify
```

On Windows (PowerShell):

```powershell
$env:JAVA_HOME = 'C:\path\to\jdk-25'
.\mvnw.cmd verify
```

## Run in dev mode

```shell
./mvnw quarkus:dev
```

The app listens on http://localhost:8080 with live reload.

| URL | Purpose |
|-----|---------|
| http://localhost:8080/api/info | Application name and version |
| http://localhost:8080/q/health | Health check (liveness and readiness) |
| http://localhost:8080/q/openapi | OpenAPI document |
| http://localhost:8080/q/swagger-ui | Swagger UI (dev mode only) |
| http://localhost:8080/q/dev-ui | Quarkus Dev UI (dev mode only) |

## Dev data

In dev mode only, Flyway loads `src/main/resources/db/dev-seed/R__dev_seed.sql` into the Dev Services database.
The seed contains invented study notes (Java, Quarkus, PostgreSQL, AI agents, RAG) with Markdown bodies,
dates from the last 90 days, and edge cases: a note with 10 tags, two 200-character titles (one Cyrillic) and
untagged notes. It never runs in tests or production. Flyway reruns it only when the file changes, and each run resets both owners' notes and tags.

| Owner | Account id | Notes | Tags |
|-------|------------|-------|------|
| `dev-user` | `00000000-0000-0000-0000-00000000000d` | 150 | 25 |
| `other-user` | `00000000-0000-0000-0000-00000000000e` | 20 | 5 |

Dev mode acts as `dev-user` (`%dev.learninghub.dev.current-user-id`). `other-user` reuses some tag names and
titles so you can check that one owner never sees the other's data (FR-9).

## Run the packaged app

```shell
./mvnw package
java -jar target/quarkus-app/quarkus-run.jar
```

## Contributing

Agent and contributor rules are in [AGENTS.md](AGENTS.md).
