FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY pom.xml pom.xml
COPY user-service/pom.xml user-service/pom.xml
COPY user-service/src user-service/src
RUN --mount=type=cache,target=/root/.m2 \
    apk add --no-cache maven && \
    mvn -f user-service/pom.xml package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S banking && adduser -S banking -G banking
COPY --from=build /app/user-service/target/*.jar app.jar
USER banking
EXPOSE 8082
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
