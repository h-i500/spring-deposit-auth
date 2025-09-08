FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /ws
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /ws/target/quarkus-app/ ./
ENV JAVA_OPTS="-Dquarkus.http.host=0.0.0.0"
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/quarkus-run.jar"]
