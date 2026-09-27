# README.md

# Log Analyzer Parser

Spring Boot backend for uploading, parsing, storing, searching, and AI-analyzing application logs.

## What It Does

- Authenticates users with JWT.
- Accepts `.log` and `.txt` log files.
- Parses log files into structured entries.
- Stores parsed log entries in Elasticsearch.
- Stores session metadata and AI results in PostgreSQL.
- Provides AI-powered:
  - log summarization
  - root-cause diagnosis
  - natural-language log querying
  - aggregation queries
  - fact extraction from logs

## Tech Stack

| Area | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5.6 |
| Build | Maven |
| Web | Spring Web |
| Security | Spring Security + JWT + JJWT 0.12.6 |
| Relational DB | PostgreSQL + Spring Data JPA |
| Search | Elasticsearch + Spring Data Elasticsearch |
| AI | Spring AI 1.0.1 + OpenAI-compatible providers |
| Kafka | Dependency present, current implementation commented out |
| Testing | Spring Boot Test, Spring Kafka Test, H2 |
| Utilities | Lombok, Jackson |

## Main Modules

```text
com.logAnalyzer.parser
├── ai
│   ├── config
│   ├── entity
│   ├── enums
│   ├── model
│   ├── provider
│   ├── repository
│   └── service
├── auth
│   ├── config
│   ├── controller
│   ├── dto
│   ├── entity
│   ├── filter
│   ├── repository
│   └── service
├── config
├── controller
├── entity
├── enums
├── exception
├── mapper
├── model
├── repository
├── service
├── util
└── ParserApplication
```

## Key Endpoints

| Method | Path | Purpose |
|---|---|---|
| POST | `/auth/register` | Register user |
| POST | `/auth/login` | Login and receive JWT |
| POST | `/api/logs/upload` | Upload a log file |
| GET | `/api/logs/sessions` | List current user's sessions |
| POST | `/api/logs/ai/summarise/{sessionId}` | AI summary |
| POST | `/api/logs/ai/diagnose/{sessionId}` | AI root-cause diagnosis |
| POST | `/api/logs/ai/query/{sessionId}` | Natural-language log query |

## Configuration Summary

- Server port: `8081`
- PostgreSQL: `jdbc:postgresql://localhost:5432/logdb`
- Elasticsearch: `http://localhost:9200`
- Upload directory: `parser/src/main/resources/uploads`
- JWT expiration: `86400000` ms
- JWT secret: expected from `secret.yaml` or environment

## Quick Start

1. Install Java 21, Maven, PostgreSQL, Elasticsearch.
2. Create PostgreSQL database `logdb`.
3. Create `secret.yaml` with `jwt.secret` and any Spring AI/OpenAI keys.
4. Start PostgreSQL and Elasticsearch.
5. Run:

```bash
mvn spring-boot:run
```

## Documentation

- [Architecture](../docs/ARCHITECTURE.md)
- [API Reference](../docs/API_REFERENCE.md)
- [Setup](../docs/SETUP.md)
- [Configuration](../docs/CONFIGURATION.md)
- [Data Model](../docs/DATA_MODEL.md)
- [AI Features](../docs/AI_FEATURES.md)
- [Parser Pipeline](../docs/PARSER_PIPELINE.md)
- [Security](../docs/SECURITY.md)
- [Known Issues](../docs/KNOWN_ISSUES.md)

## Important Warning

The supplied consolidated repository file states that security validation was bypassed. Treat it as sensitive. Do not commit `secret.yaml`, real JWT secrets, database passwords, or API keys.