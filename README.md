# Instagramm API

This a mvp backend service which behaves like instagram application.

User can upload images (post /api/images) and gets list of images (get /api/images).

User can get also a list of tags (get /api/tags).

## Decisions
- persistence of image metadata is in MySQL DB
- the db schema is set via sql and flyway, so that db schema has a version
- persistence of image blob is inside application / container and is configurable via env: application.yml: app.storage.upload-dir
- the image serving path is configurable via application.yml: app.storage.upload-path
- There is a Dockerfile with docker-compose file, which start both mysql and app. But it is also possible to start them separately.



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
| Other | **Spring Actuator** (health), **nullaway**, **JSpecify** (NPE handling) ** |

## API conventions

- Base path: **`/api/...`**
- Version header: **`API-Version`** (optional; defaults to **`1.00`** when omitted). Controllers are mapped at version **`1.00`**.

## Local development

1. To start onyl **Mysql** service from the repo root:
```bash
docker compose up mysql -d
```
2. Run the app locally:

```bash
./gradlew bootRun
```

2.1 Alternatively you can run both mysql and app inside a container. It will build a Docker image and run inside container.
```bash
docker compose up -d
 ```


4. Run tests
```bash
./gradlew test
```