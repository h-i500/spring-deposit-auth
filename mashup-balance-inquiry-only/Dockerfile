FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/target/quarkus-app/ ./quarkus-app/
EXPOSE 8180
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/quarkus-app/quarkus-run.jar"]
