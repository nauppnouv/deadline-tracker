FROM eclipse-temurin:25-jre@sha256:dbb43d81099eec91e0800226ed2296d03146ee7857165b95112372ad0d67e87f
RUN groupadd --gid 10001 tracker && useradd --uid 10001 --gid tracker --no-create-home tracker
WORKDIR /app
COPY --chown=10001:10001 target/deadline-tracker-0.1.0-SNAPSHOT.jar /app/app.jar
USER 10001:10001
ENV JAVA_TOOL_OPTIONS="-Duser.timezone=UTC -XX:MaxRAMPercentage=65"
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
