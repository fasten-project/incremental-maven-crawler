FROM adoptopenjdk/openjdk11:alpine

COPY lib/crawler.jar /app/

WORKDIR /app/

ENTRYPOINT ["java", "-jar", "crawler.jar"]