FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN apk add --no-cache bash \
    && addgroup -S harness && adduser -S harness -G harness \
    && mkdir -p /app /data /log \
    && chown -R harness:harness /app /data /log
COPY --chown=harness:harness target/harness-0.1.0.jar /app/harness.jar
RUN mkdir -p /workspace && chown harness:harness /workspace
USER harness
WORKDIR /workspace
EXPOSE 18000
ENV LOG_DIR=/data/log
ENTRYPOINT ["java", "-jar", "/app/harness.jar"]