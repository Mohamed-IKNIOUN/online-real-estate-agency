# ==========================================
# Étape 1 : Construction (Build) du JAR
# ==========================================
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Copier les fichiers de configuration Maven et le code source
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src src

# Rendre le wrapper Maven exécutable et compiler le projet en ignorant les tests
RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests

# ==========================================
# Étape 2 : Exécution (Runtime) de l'application
# ==========================================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copier uniquement le fichier JAR généré depuis l'étape de build
COPY --from=build /app/target/*.jar app.jar

# Exposer le port du service d'identité (généralement 8085 ou 8080, à ajuster si besoin)
EXPOSE 8085

# Lancer l'application Spring Boot
ENTRYPOINT ["java", "-jar", "app.jar"]