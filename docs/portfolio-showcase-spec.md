# Portfolio Showcase Specification

## Goal

Make the repository understandable, runnable and demonstrable to a recruiter or technical reviewer in under ten minutes, without presenting unimplemented capabilities as complete.

## Acceptance criteria

- The README identifies the product problem, core technical decisions, architecture and trade-offs.
- A new contributor can start dependencies, obtain a development JWT and run a verified end-to-end API walkthrough.
- The walkthrough demonstrates incident intake, analysis and safe failure handling using only synthetic data.
- Public API contracts are discoverable from an OpenAPI document and reproducible through checked-in HTTP examples.
- CI status, test scope and the Docker/Testcontainers requirement are accurately documented.
- The repository includes a concise recorded-demo script and instructions for capturing screenshots/GIFs, without committing generated media.
- Existing security guarantees are explained accurately: roles, tenant isolation, redaction and metrics restrictions.

## Non-goals

- A browser UI, public cloud deployment, real credentials, a real production identity provider or Kafka transport.
- Changing model behavior or weakening authorization only to make a demo easier.
