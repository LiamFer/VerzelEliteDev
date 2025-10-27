# Imagem base com JDK 21
FROM eclipse-temurin:21-jdk

# Diretório de trabalho
WORKDIR /app

# Copia Maven wrapper e POM
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

# Baixa dependências sem compilar
RUN ./mvnw dependency:go-offline

# Copia o código-fonte
COPY src ./src

# Builda o .jar
RUN ./mvnw clean package -DskipTests

# Expõe a porta padrão do Spring Boot
EXPOSE 8080

# Comando para rodar a API
CMD ["java", "-jar", "target/*.jar"]
