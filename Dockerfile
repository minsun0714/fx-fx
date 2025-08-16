# =======================
# 1단계: 빌드
# =======================
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /workspace

# 1) Gradle Wrapper/설정 먼저 (캐시)
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
RUN chmod +x gradlew

# 2) 의존성 프리페치 (실패해도 빌드는 계속)
RUN ./gradlew --no-daemon dependencies || true

# 3) 전체 소스 복사
COPY . .

# 4) Spring Boot JAR 빌드 (테스트 생략은 상황에 따라)
RUN ./gradlew --no-daemon clean bootJar -x test

# =======================
# 2단계: 실행
# =======================
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# (선택) 컨테이너 타임존을 KST로
ENV TZ=Asia/Seoul
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 빌드된 boot JAR 복사 (bootJar만 집게 더 구체화)
# 필요에 따라 패턴 조정: *-plain.jar 제외
COPY --from=builder /workspace/build/libs/*-SNAPSHOT.jar /app/app.jar

EXPOSE 8080

# 기본 프로필을 docker로 맞추거나 이 줄을 아예 제거하고 compose에서만 지정
ENV SPRING_PROFILES_ACTIVE=docker

# JAVA_TOOL_OPTIONS는 자바 표준 관례 (UseContainerSupport는 21부터 기본 활성화라 생략 가능)
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -Duser.timezone=Asia/Seoul"

# exec로 신호 전달 깔끔하게
ENTRYPOINT ["sh", "-c", "exec java $JAVA_TOOL_OPTIONS -jar /app/app.jar --spring.profiles.active=${SPRING_PROFILES_ACTIVE}"]
