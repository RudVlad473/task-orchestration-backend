FROM gradle:8.7-jdk21 AS build
WORKDIR /app

COPY . .

RUN chmod +x gradlew
RUN ./gradlew bootJar

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

# Run as the numeric nobody user and group, as the backup image does. Numeric because kubelet
# cannot check runAsNonRoot against a user name. The app writes only under /tmp, which stays
# world-writable, and app.jar stays root-owned.
USER 65534:65534

ENTRYPOINT ["java", "-jar", "app.jar"]
