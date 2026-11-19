# 1. Backend framework: Quarkus 3.40

Date: 2026-11-12

## Status

Accepted

## Context

The Learning Hub needs a Java backend for notes, tags, search, progress and accounts
(requirements FR-1 to FR-9). The vision fixes Java 25, PostgreSQL, Docker and GitHub Actions,
and names Quarkus because it is used at work, so the owner can review the code the agent writes.
Hosting must run on free tiers ($0/month, NFR-1). After an idle period the first request must
get a full response within 30 s (NFR-5), so startup time and memory use matter.
Later phases need an MCP server (week 9) and RAG over notes (weeks 14-15).

Options considered: Quarkus 3.40, Spring Boot 4.1, Micronaut 5.2.
Quarkus 4.0 is only a release candidate (4.0.0.CR1) at the time of writing.

## Decision

Use Quarkus 3.40 (latest final release: 3.40.1.2) on Java 25, built with Maven and managed
with the Quarkus platform BOM.

## Consequences

- The owner can review and fix code using skills from work. The project also builds work-relevant skills.
- Fast JVM startup, plus an optional native image, helps with free-tier cold starts and memory limits.
- Quarkus extensions for Flyway, Qute, security, MCP server and LangChain4j cover the later phases
  without a framework change.
- There are fewer public Quarkus examples than Spring examples. The agent may write Spring-style
  code or use outdated Quarkus settings. Reviews and CI must catch this, and agent instructions should
  give the Quarkus version and preferred APIs.
- Native image builds are slow in CI. Build them only for deployment, if at all.
- Quarkus 4.0 is likely to become final during the course. Upgrading to it is a planned task
  with its own ADR, not something to do mid-feature.
