# Imagen de user-service. Se construye desde la raiz del repo porque el pom padre
# declara todos los modulos:  docker build -f user-service/Dockerfile .
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY gateway/pom.xml gateway/
COPY source-service/pom.xml source-service/
COPY analysis-service/pom.xml analysis-service/
COPY report-service/pom.xml report-service/
COPY user-service/pom.xml user-service/
COPY user-service/src user-service/src
RUN mvn -B -q -pl user-service -am -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/user-service/target/user-service.jar app.jar
# H2 y workspaces viven en ./data (relativo a /app); montar un volumen para conservarlos.
VOLUME /app/data
ENV JAVA_OPTS="-Xms64m -Xmx384m"
EXPOSE 8084
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
