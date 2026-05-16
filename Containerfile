# syntax=docker/dockerfile:1

ARG JAVA_VERSION=21

FROM eclipse-temurin:${JAVA_VERSION}-jdk-noble AS build
WORKDIR /app

COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle/ gradle/
RUN chmod +x gradlew \
    && ./gradlew dependencies --no-daemon --quiet 2>/dev/null || true

COPY src/ src/
RUN ./gradlew buildFatJar --no-daemon \
    -x test \
    -x ktlintTestSourceSetCheck \
    -x ktlintMainSourceSetCheck

RUN mkdir -p /tmp/jar \
    && cd /tmp/jar \
    && jar xf /app/build/libs/*-all.jar \
    && jdeps \
         --ignore-missing-deps \
         --recursive \
         --multi-release 21 \
         --print-module-deps \
         --class-path '/tmp/jar/BOOT-INF/lib/*' \
         /app/build/libs/*-all.jar > /tmp/modules.txt \
    && jlink \
         --add-modules "$(cat /tmp/modules.txt),jdk.unsupported,jdk.crypto.ec" \
         --strip-debug \
         --no-man-pages \
         --no-header-files \
         --compress=2 \
         --output /opt/jre

FROM ubuntu:26.04 AS runtime

RUN apt-get update \
    && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
       ca-certificates \
       curl \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/* /var/cache/apt/*

RUN groupadd --system app \
    && useradd --system --gid app \
       --shell /usr/sbin/nologin \
       --create-home app

WORKDIR /app

COPY --from=build /opt/jre /opt/jre
COPY --from=build --chown=app:app /app/build/libs/*-all.jar /app/clapper.jar

ENV PATH="/opt/jre/bin:${PATH}"

USER app
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/clapper.jar"]
