# Runtime image - uses pre-built JAR from CI pipeline
FROM eclipse-temurin:25-jre
WORKDIR /app

# Copy the pre-built JAR (built by CI and placed in target/)
COPY target/*.jar app.jar

# Cloud Run sets PORT env var
ENV PORT=8090
EXPOSE 8090

# JVM settings for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
