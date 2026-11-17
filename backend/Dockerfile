FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /backend
COPY pom.xml ./
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q package -DskipTests

FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S rbm && adduser -S rbm -G rbm
WORKDIR /app
COPY --from=build /backend/target/rbm.jar app.jar
RUN mkdir -p /app/uploads && chown -R rbm:rbm /app
USER rbm
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]