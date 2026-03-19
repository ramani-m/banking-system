FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY pom.xml pom.xml
COPY account-service/pom.xml account-service/pom.xml
COPY account-service/src account-service/src
RUN --mount=type=cache,target=/root/.m2 \
    apk add --no-cache maven && \
    mvn -f account-service/pom.xml package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S banking && adduser -S banking -G banking
COPY --from=build /app/account-service/target/*.jar app.jar
USER banking
EXPOSE 8083
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
