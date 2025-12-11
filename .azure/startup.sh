#!/bin/bash
# Azure App Service startup script
# This script is used to start the Spring Boot application

# Find the JAR file in /home/site/wwwroot
JAR_FILE=$(find /home/site/wwwroot -name "*.jar" -type f | head -n 1)

if [ -z "$JAR_FILE" ]; then
  echo "ERROR: No JAR file found in /home/site/wwwroot"
  exit 1
fi

echo "Starting application with JAR: $JAR_FILE"
echo "PORT environment variable: ${PORT:-8080}"

# Start the Spring Boot application
exec java -jar "$JAR_FILE"
