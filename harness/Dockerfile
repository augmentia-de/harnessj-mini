FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S harness && adduser -S harness -G harness \
    && mkdir -p /app /data /log \
    && chown -R harness:harness /app /data /log
COPY --chown=harness:harness target/harness-0.1.0.jar /app/harness.jar
USER harness
EXPOSE 18000
ENV LOG_DIR=/data/log
ENTRYPOINT ["java", "-jar", "/app/harness.jar"]