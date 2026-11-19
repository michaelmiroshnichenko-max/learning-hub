# AGENTS.md

## Commands
- Build and run all tests: `./mvnw verify` (Windows: `.\mvnw.cmd verify` with `JAVA_HOME` = JDK 25)
- Run one test class: `./mvnw test -Dtest=InfoResourceTest`
- Run tests for one FR: `./mvnw verify -Dgroups=FR-3`
- Dev mode with live reload: `./mvnw quarkus:dev` -> http://localhost:8080
- Health: `GET /q/health` · OpenAPI: `/q/openapi` · Swagger UI (dev only): `/q/swagger-ui`
- Package: `./mvnw package` -> `target/quarkus-app/quarkus-run.jar`

## Stack
- Java 25, Maven (wrapper), Quarkus 3.40.1.2 (platform BOM; do not upgrade to 4.x without an ADR)
- Extensions: `rest-jackson`, `smallrye-health`, `smallrye-openapi`
- Tests: JUnit 5, `@QuarkusTest`, RestAssured
- Planned in later stages (do not add early): PostgreSQL + Flyway, Qute + HTMX, SmallRye JWT, Docker, CI
- Use Quarkus APIs (Jakarta REST, CDI, MicroProfile Config). No Spring-style code or annotations.

## Project structure
```
src/main/java/dev/learninghub/
  shared/      cross-cutting: problem details, current user, layout, /api/info
  account/     FR-2  (later)
  auth/        FR-1  (later)
  note/        FR-3..FR-7, FR-9 (later)
  progress/    FR-8  (later)
src/main/resources/application.properties
src/test/java/dev/learninghub/<module>/   tests mirror the main package
docs/        vision, requirements, architecture, roadmap, adr/
```
- Package by feature. A module exposes only its `api` sub-package; everything else is internal.
- Allowed dependencies: `auth -> account`, `progress -> note`, every module -> `shared`.
  `shared` depends on no feature module. No cycles. See docs/architecture.md section 2.

## Code style
- Constructor injection, `final` fields, records for response bodies, explicit `@Produces`.
- Config through `@ConfigProperty`; no hard-coded deploy values. Example (`shared/InfoResource.java`):
```java
@Path("/api/info")
public class InfoResource {

    public record Info(String name, String version) {
    }

    private final Info info;

    public InfoResource(
            @ConfigProperty(name = "quarkus.application.name") String name,
            @ConfigProperty(name = "quarkus.application.version") String version) {
        this.info = new Info(name, version);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Info get() {
        return info;
    }
}
```
- Formatting follows `.editorconfig` (UTF-8, LF, 4 spaces). Comment only non-obvious code.

## Testing
- Test first: write a failing test, see it fail, then implement.
- Endpoint tests: `@QuarkusTest` + RestAssured, named `<Class>Test`, in the same package.
- Every FR test has a JUnit tag with the FR id, e.g. `@Tag("FR-3")`.
- `./mvnw verify` must pass before every commit.

## Git workflow
- One roadmap stage = one branch = one PR into `main`; CI green before merge.
- Small commits with imperative messages. Record AI credits used in the PR description.
- Changes to FR-1, FR-2, FR-9, cookies or tokens need a human review before merge.

## Boundaries
- Never commit secrets, `.env` files or a `plan/` folder. Config secrets come from env variables.
- Never edit a merged Flyway migration; add a new `V<n>__<description>.sql` instead.
- Do not add dependencies, extensions or modules outside the current stage's scope.
- Do not change docs/adr decisions in code; propose a new ADR instead.
- Ask before deleting files, changing `pom.xml` versions or the module dependency rules.
