FROM eclipse-temurin:21-jre-jammy
RUN groupadd -r metriq && useradd -r -g metriq metriq
RUN mkdir -p /app/segmented /app/merged /app/controller-uploads /app/external /app/file-uploads /var/log/metriq \
    && chown -R metriq:metriq /app /var/log/metriq
WORKDIR /app
COPY target/metriq-*-runner.jar app.jar
RUN chown metriq:metriq app.jar
USER metriq
EXPOSE 8080 38708
ENTRYPOINT ["java", "--add-opens=java.base/java.lang=ALL-UNNAMED", "--add-opens=java.base/java.lang.invoke=ALL-UNNAMED", "-jar", "app.jar"]
