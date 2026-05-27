# Instagramm API

This a mvp backend service which behaves like instagram application.

User can upload images (post /api/images) and gets list of images (get /api/images).

User can get also a list of tags (get /api/tags).

## Decisions
- persistence of image metadata is in MySQL DB
- the db schema is set via sql and flyway, that db schema has a version
- persistence of image blob is in application and is configurable via application.yml: app.storage.upload-dir
- the image serving is via static folder and the path is configurable via application.yml: app.storage.upload-path



## Stack

| Layer | Technology |
|--------|------------|
| Language | Java **25** |
| Framework | **Spring Boot 4.0.5** |
| HTTP | **Spring Web MVC** — JSON REST (`spring-boot-starter-webmvc`) |
| Build | **Gradle** (wrapper) |
| Persistence | **MySQL**, **Spring Data JPA**, **Flyway** (`src/main/resources/db/migration`) |
| Security | **Spring Security** (stateless), **JWT** (**JJWT**)|
| Object storage | is internally in the application |
| API docs | **springdoc-openapi** — `/v3/api-docs`, **Swagger UI** at `/swagger-ui.html` |
| Other | **Spring Actuator** (health), ** nullaway, Jspecify (NPE handling) ** |

## API conventions

- Base path: **`/api/...`**
- Version header: **`API-Version`** (optional; defaults to **`1.00`** when omitted). Controllers are mapped at version **`1.00`**.

## Local development

1. Start **MySQL** (for example `docker compose up` from the repo root; default user/database match `application.yml`).
2. Run the app:

```bash
./gradlew bootRun
```

4. Run tests — requires PostgreSQL with database **`challengestest`** (see `docker-compose.yml`). **`./gradlew test`** runs Flyway clean on that database first, then the suite.

```bash
./gradlew test
```