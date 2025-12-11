FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY target/ride_fast_backend-0.0.1-SNAPSHOT.jar app.jar

# Azure App Service requires port 80 internally
EXPOSE 80

CMD ["java", "-Dserver.port=80", "-jar", "app.jar"]
