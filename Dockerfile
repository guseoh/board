FROM eclipse-temurin:17-jdk AS build
# 컨테이너에서 작업할 디렉터리
WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
COPY src src

RUN chmod +x gradlew
RUN ./gradlew clean bootJar -x test

FROM eclipse-temurin:17-jdk
WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]