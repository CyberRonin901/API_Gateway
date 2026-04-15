# Use the JDK 25 image as the starting point
FROM eclipse-temurin:25-jdk-noble AS builder

# Update the package list and install Maven to build .jar file (maven image was not working with Java 25)
RUN apt-get update && apt-get install -y maven

# Create a variable for the folder path of the specific microservice we want to build
ARG SERVICE_PATH
# Set the main working folder inside the container where the build will happen
WORKDIR /build

# Copy only the pom.xml file first to keep things organized
COPY ${SERVICE_PATH}/pom.xml ./service/
# Download all the required libraries and cache so we don't have to re-download
RUN mvn -f ./service/pom.xml dependency:go-offline -B

# Copy the actual source code (Java files) into the container
COPY ${SERVICE_PATH}/src ./service/src
# Build the application into a JAR file while skipping unit tests to make the build process faster
RUN mvn -f ./service/pom.xml clean package -DskipTests -Dmaven.test.skip=true

# Switch to a smaller JRE (Java Runtime Environment) image to keep the final container size tiny
FROM eclipse-temurin:25-jre-noble
# Install the wget tool so Docker Compose can check if this service is healthy and running
RUN apt-get update && apt-get install -y wget && rm -rf /var/lib/apt/lists/*
# Set the folder where the application will live and run
WORKDIR /app
# Move .jar file from the builder stage into this final clean image
COPY --from=builder /build/service/target/app.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]