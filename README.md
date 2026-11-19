# Learning Hub

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

## Run the packaged app

```shell
./mvnw package
java -jar target/quarkus-app/quarkus-run.jar
```

## Contributing

Agent and contributor rules are in [AGENTS.md](AGENTS.md).
