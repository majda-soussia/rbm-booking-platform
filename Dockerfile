# ---------- Étape 1 : build Angular ----------
FROM node:16-alpine AS frontend
WORKDIR /frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci --legacy-peer-deps
COPY frontend/ ./
RUN npx ng build --configuration production

# ---------- Étape 2 : build Spring Boot (avec Angular intégré) ----------
FROM maven:3.9-eclipse-temurin-17 AS backend
WORKDIR /backend
COPY backend/pom.xml ./
RUN mvn -q dependency:go-offline
COPY backend/src ./src
COPY --from=frontend /frontend/dist/ ./src/main/resources/static/
RUN mvn -q package -DskipTests

# ---------- Étape 3 : image finale ----------
FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S rbm && adduser -S rbm -G rbm
WORKDIR /app
COPY --from=backend /backend/target/rbm.jar app.jar
RUN mkdir -p /app/uploads && chown -R rbm:rbm /app
USER rbm
ENV UPLOAD_DIR=/app/uploads
VOLUME /app/uploads
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]