#  Build stage
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app

# Cache deps first (faster rebuilds)
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp -DskipTests dependency:go-offline

# Build
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp -DskipTests clean package

# Runtime stage (small + secure)
FROM eclipse-temurin:21-jre-alpine AS runtime

RUN apk update && apk upgrade
WORKDIR /app

# Create non-root user
RUN addgroup -S app && adduser -S app -G app

# Copy jar with a fixed name (avoids wildcard surprises)
COPY --from=builder /app/target/*.jar /app/app.jar


RUN mkdir -p /tmp && chown -R app:app /app /tmp

USER app


ENTRYPOINT ["java","-jar","/app/app.jar"]
