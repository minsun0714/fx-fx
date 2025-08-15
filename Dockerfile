# =======================
# 1단계: 빌드
# =======================
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /app

# 캐시 최적화를 위해 의존성 먼저 다운로드
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN ./mvnw dependency:go-offline

# 전체 소스 복사
COPY src ./src

# Spring Boot JAR 빌드
RUN ./mvnw clean package -DskipTests

# =======================
# 2단계: 실행
# =======================
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# 빌드된 JAR 복사
COPY --from=builder /app/target/*.jar app.jar

# 기본 포트
EXPOSE 8080

# Spring Profile 환경변수 (기본값: prod)
ENV SPRING_PROFILES_ACTIVE=prod

# JVM 최적화 옵션
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --spring.profiles.active=${SPRING_PROFILES_ACTIVE}"]
