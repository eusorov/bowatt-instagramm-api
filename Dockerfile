# --- Build: JDK + Gradle wrapper
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

COPY gradlew gradlew
COPY gradle gradle
COPY settings.gradle gradle.properties ./

RUN chmod +x gradlew

# Dependency resolution layer (invalidates when Gradle files change)
COPY app/build.gradle app/build.gradle
RUN ./gradlew --no-daemon :app:dependencies

COPY app/src app/src

# build JAR (zip), skip tests
RUN set -e \
    && ./gradlew --no-daemon :app:bootJar -x test \
    && jar_path="$(find app/build/libs -maxdepth 1 -name '*.jar' ! -name '*-plain.jar' -print -quit)" \
    && test -n "$jar_path" \
    && test -s "$jar_path" \
    && jar tf "$jar_path" >/dev/null \
    && cp "$jar_path" /workspace/application.jar

# --- Runtime: JRE only
FROM eclipse-temurin:25-jre

RUN groupadd --system spring && useradd --system --gid spring spring
WORKDIR /app

COPY --from=build /workspace/application.jar /app/application.jar

RUN mkdir -p /app/uploads \
    && chown spring:spring /app/application.jar \
    && chown -R spring:spring /app/uploads
USER spring:spring

EXPOSE 8080
ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/application.jar"]
