FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY pom.xml pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY api-gateway/src api-gateway/src
RUN --mount=type=cache,target=/root/.m2 \
    apk add --no-cache maven && \
    mvn -f api-gateway/pom.xml package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S banking && adduser -S banking -G banking
COPY --from=build /app/api-gateway/target/*.jar app.jar
USER banking
EXPOSE 8080
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
