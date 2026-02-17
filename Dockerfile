FROM eclipse-temurin:17-jre

WORKDIR /app

ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar

EXPOSE 26125

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
