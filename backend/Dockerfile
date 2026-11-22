# Etapa 1: compilar el JAR con Maven y Java 21.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package

# Etapa 2: imagen liviana solo con el JRE.
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --create-home parkingspot
COPY --from=build /app/target/*.jar app.jar
USER parkingspot
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
