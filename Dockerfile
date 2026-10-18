# Root-level build for hosts that cannot set a build subdirectory (Render demo). Same image as TicketRush/Dockerfile.
FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /app
COPY TicketRush/pom.xml .
COPY TicketRush/src ./src
RUN mvn clean package -DskipTests
RUN java -Djarmode=tools -jar /app/target/ticketRush-0.0.1-SNAPSHOT.jar extract --destination /app/extracted

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/extracted/ /app/

# AppCDS: a training run that only refreshes the context (no database, no seeding) records every
# class the app loads into a shared archive. Booting from it skips most class parsing and
# verification, which dominates cold start on a fraction of a CPU. If the archive is rejected
# (different JVM) the JVM silently falls back to normal loading (-Xshare:auto).
RUN SPRING_PROFILES_ACTIVE=demo java -XX:ArchiveClassesAtExit=/app/app.jsa -XX:+UseSerialGC -XX:TieredStopAtLevel=1       -Dspring.context.exit=onRefresh -Dspring.main.lazy-initialization=false       -Dspring.jpa.hibernate.ddl-auto=none -Dspring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=false       -Dspring.datasource.hikari.initialization-fail-timeout=-1       -jar /app/ticketRush-0.0.1-SNAPSHOT.jar

EXPOSE 8080

# Sized for a 512MB free instance; override JAVA_OPTS if you have more room.
ENV JAVA_OPTS="-Xmx300m -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xss512k -Xshare:auto -XX:SharedArchiveFile=/app/app.jsa"
# SPRING_PROFILES_ACTIVE (e.g. demo) is read by Spring Boot straight from the environment.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/ticketRush-0.0.1-SNAPSHOT.jar"]
