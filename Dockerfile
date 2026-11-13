# Build en dos etapas: la imagen final no carga el JDK ni el repositorio de Maven.
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

# Primero el wrapper y el pom: si no cambian, Docker reutiliza la capa de
# dependencias y no las vuelve a descargar en cada build.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src/ src/
# Los tests necesitan PostgreSQL y aqui no hay: corren en el pipeline, no en la imagen.
RUN ./mvnw -B package -DskipTests
RUN cp target/*.jar /app/app.jar

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
# Usuario sin privilegios: si alguien escapa del proceso, no es root.
RUN useradd --system --create-home --uid 1000 spring
COPY --from=build /app/app.jar app.jar
USER spring
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]
