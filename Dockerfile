FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -B -ntp -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S porteria && adduser -S porteria -G porteria && mkdir -p /app/data/fotos && chown -R porteria:porteria /app
COPY --from=build /workspace/target/porteria-api-*.jar /app/porteria-api.jar
USER porteria
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/porteria-api.jar"]
