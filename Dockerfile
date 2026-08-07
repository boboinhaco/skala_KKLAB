# 빌드 산출물 jar를 실행하는 단일 컨테이너
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY build/libs/skala-shop-api-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
