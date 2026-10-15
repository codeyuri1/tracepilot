FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /work
COPY src src
RUN javac -d out src/main/java/DemoLogProducer.java
FROM eclipse-temurin:21-jre-alpine
COPY --from=build /work/out /app
CMD ["java", "-cp", "/app", "DemoLogProducer"]
