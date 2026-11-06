# syntax=docker/dockerfile:1

# 페어카 크루 서버 이미지
# 빌드 컨텍스트는 저장소 루트: docker build -f infra/Dockerfile -t paircar-server .

# ---------- 1. 빌드 ----------
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace

# 의존성 레이어: 빌드 설정이 바뀌지 않으면 캐시를 그대로 씀
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
RUN --mount=type=cache,target=/root/.gradle ./gradlew dependencies --no-daemon -q > /dev/null

COPY src src
RUN --mount=type=cache,target=/root/.gradle ./gradlew bootJar -x test --no-daemon \
    && cp build/libs/*-SNAPSHOT.jar app.jar \
    && java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted

# ---------- 2. 실행 ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S paircar && adduser -S paircar -G paircar
USER paircar

# 자주 바뀌는 순서대로 아래에 둠 (의존성 → 로더 → 스냅샷 의존성 → 애플리케이션 코드)
COPY --from=build /workspace/extracted/dependencies/ ./
COPY --from=build /workspace/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/extracted/application/ ./

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=3s --start-period=40s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health/readiness || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
