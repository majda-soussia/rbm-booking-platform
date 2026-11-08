FROM jenkins/jenkins:lts-jdk17
USER root

# Node 16 (même version que le frontend Angular du projet)
RUN apt-get update && apt-get install -y --no-install-recommends xz-utils ca-certificates \
    && rm -rf /var/lib/apt/lists/* \
    && curl -fsSL https://nodejs.org/dist/v16.20.2/node-v16.20.2-linux-x64.tar.xz \
       | tar -xJ -C /usr/local --strip-components=1

# Client Docker et plugin Compose, copiés depuis l'image officielle docker:cli
COPY --from=docker:27-cli /usr/local/bin/docker /usr/local/bin/docker
COPY --from=docker:27-cli /usr/local/libexec/docker/cli-plugins/docker-compose /usr/local/libexec/docker/cli-plugins/docker-compose