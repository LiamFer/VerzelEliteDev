FROM eclipse-temurin:21-jdk
WORKDIR /app

# Copia Maven wrapper e pom
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

# Dá permissão
RUN chmod +x mvnw

# Baixa dependências
RUN ./mvnw dependency:go-offline

# Copia o código
COPY src ./src

# Builda o jar
RUN ./mvnw clean package -DskipTests

EXPOSE 8080

CMD ["java", "-jar", "target/*.jar"]
