FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN apk add --no-cache wget

COPY mcp-task-service/target/*.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --retries=5 CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java","-jar","app.jar"]