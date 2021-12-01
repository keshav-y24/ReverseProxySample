FROM openjdk:13-jdk-alpine
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} sample-0.0.1-SNAPSHOT.jar
CMD ["java","-jar","sample-0.0.1-SNAPSHOT.jar"]