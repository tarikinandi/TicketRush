# Root-level build for hosts that cannot set a build subdirectory (Render demo). Same image as TicketRush/Dockerfile.
FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /app
COPY TicketRush/pom.xml .
COPY TicketRush/src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

# Sized for a 512MB free instance; override JAVA_OPTS if you have more room.
ENV JAVA_OPTS="-Xmx300m -XX:+UseSerialGC"
# SPRING_PROFILES_ACTIVE (e.g. demo) is read by Spring Boot straight from the environment.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
