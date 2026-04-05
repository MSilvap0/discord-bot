FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn -B package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/target/discord-bot-reminder-1.0-SNAPSHOT.jar ./discord-bot-reminder.jar

CMD ["java", "-jar", "/app/discord-bot-reminder.jar"]
