# Dockerfile

# 1. Build Stage
FROM gradle:8.5.0-jdk17 AS build
WORKDIR /app
COPY . .
RUN gradle bootJar

# 2. Run Stage
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/build/libs/nonstop-0.0.1-SNAPSHOT.jar .
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "nonstop-0.0.1-SNAPSHOT.jar"]
