FROM amazoncorretto:21-alpine AS builder

ARG APP=split-audio
ENV APP=$APP

WORKDIR /app/$APP

COPY ./.mvn ./.mvn
COPY ./mvnw .
COPY ./pom.xml .

RUN chmod +x ./mvnw

RUN ./mvnw clean package -Dmaven.test.skip -Dmaven.main.skip -Dspring-boot.repackage.skip && rm -r ./target/

COPY ./src ./src

RUN ./mvnw clean package -DskipTests

FROM amazoncorretto:21-alpine

WORKDIR /app

RUN mkdir -p /app/logs /tmp/split_audio

COPY --from=builder /app/split-audio/target/*.jar split-audio.jar

ARG PORT_APP=8002
ENV PORT=$PORT_APP

EXPOSE $PORT

CMD ["java", "-jar", "split-audio.jar"]