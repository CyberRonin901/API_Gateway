# Build the service using base JDK 25 image
FROM eclipse-temurin:25-jdk-noble AS builder

# Install Maven manually in the container (cuz image was not working with Java 25)
RUN apt-get update && apt-get install -y maven

ARG SERVICE_PATH
WORKDIR /build

# Copy and build
COPY ${SERVICE_PATH}/pom.xml ./service/
RUN mvn -f ./service/pom.xml dependency:go-offline -B

COPY ${SERVICE_PATH}/src ./service/src
RUN mvn -f ./service/pom.xml clean package -DskipTests -Dmaven.test.skip=true

# Run the service
FROM eclipse-temurin:25-jre-noble
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=builder /build/service/target/app.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]