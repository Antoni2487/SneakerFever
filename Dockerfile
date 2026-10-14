# Backend Spring Boot. Build multi-stage: el JDK y Maven solo existen en la
# etapa de build; la imagen final lleva el JRE y el jar, y corre sin root.
#
#   docker build -t sneakerfever-backend .
#
# Configuración por variables de entorno (Spring las mapea a las propiedades):
# SPRING_DATASOURCE_URL/USERNAME/PASSWORD, JWT_SECRET, MIAPI_TOKEN, ... — ver
# docker-compose.yml. La imagen NO incluye application.properties.

FROM eclipse-temurin:21-jdk AS build
WORKDIR /build
COPY .mvn .mvn
COPY mvnw pom.xml ./
# Capa de dependencias aparte: solo se rehace si cambia el pom.xml.
RUN sh mvnw -q -B dependency:go-offline
COPY src src
RUN sh mvnw -q -B package -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S -G app -u 1001 app \
    && mkdir -p /app/uploads && chown app:app /app/uploads
WORKDIR /app
COPY --from=build /build/target/acceso-*.jar app.jar
USER app
ENV SERVER_PORT=8082 \
    UPLOAD_PATH=/app/uploads
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
