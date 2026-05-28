# Instagramm API

This a mvp backend service which behaves like instagram application.

User can upload images (post /api/uploads) and gets list of images (get /api/images).

User can get also a list of tags (get /api/tags).

## Features and Decisions
- user can upload images via POST api/uploads
- persistence of image metadata is in MySQL DB
- user can see images and the client (UI) gets a paginageable list: GET api/images
- user can see tags via GET api/tags 
- the db schema is set via sql and flyway, so that db schema has a version
- persistence of image blob is inside application / container and is configurable via env: application.yml: app.storage.upload-dir
- the image serving path is configurable via application.yml: app.storage.upload-path
- Websockets are provided by spring and when an image is uploaded a new event "IMAGE_CREATED" is broadcasted to clients (UI)
- For containarization a Dockerfile with docker-compose file is provided, it can start both mysql and app. But it is also possible to start them separately.
- For caching use spring cache with caffeine as a simple in memory storage
- API documentation is automatically generated with swagger with use of springdoc lib 

## Out of scope
- authentification: all users can anonymously make requests (auth is not set)
- security for react UI: all clients can send requests (JWT token not set) 
- delete images and tags


## Stack

| Layer | Technology |
|--------|------------|
| Language | Java **25** |
| Framework | **Spring Boot 4.0.5** |
| HTTP | **Spring Web MVC** — JSON REST (`spring-boot-starter-webmvc`) |
| Build | **Gradle** (wrapper) |
| Persistence | **MySQL**, **Spring Data JPA**, **Flyway** (`src/main/resources/db/migration`) |
| Object storage | is internally in the application |
| cache | **Spring Cache + Caffeine** |
| API docs | **springdoc-openapi** — `/v3/api-docs`, **Swagger UI** at `/swagger-ui.html` |
| Other | **Spring Actuator** (health), **nullaway**, **JSpecify** (NPE handling) ** |

## API conventions

- Base path: **`/api/...`**
- Version header: **`API-Version`** (optional; defaults to **`1.00`** when omitted). Controllers are mapped at version **`1.00`**.
- Documentation UI: **`http://localhost:8080/swagger-ui/index.html`**
- Documentation JSON: **`http://localhost:8080/v3/api-docs`**
- Documentation YML: **`http://localhost:8080/v3/api-docs.yml`**

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

5. Docker
There is a Docker file for building an image. There is a docker-compose.yml file for starting the mysql and the app inside a container.

For running the app on a [VPS insta.api.usomi.de](https://insta.api.usomi.de) with Letsencrypt there is docker-compose.deploy.yml, which overrides a few parameters, like mysql url, because it is installed locally on VPS. And it provides parameters for letsencrypt service. 

6. Github Actions
There are github actions to run tests, create image and deploy to VPS.