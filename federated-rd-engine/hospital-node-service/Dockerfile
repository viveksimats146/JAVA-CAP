FROM eclipse-temurin:17-jdk-alpine as builder
WORKDIR /app
COPY . .
RUN ./gradlew :hospital-node-service:bootJar --no-daemon

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/federated-rd-engine/hospital-node-service/build/libs/*.jar app.jar
EXPOSE 8080 1099
ENTRYPOINT ["java", "-jar", "app.jar"]
