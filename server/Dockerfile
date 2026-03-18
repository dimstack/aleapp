FROM gradle:9.0.0-jdk21 AS build
WORKDIR /app
COPY server/ ./
RUN gradle --no-daemon installDist

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/install/callapp-server /app
RUN mkdir -p /app/data
EXPOSE 3000 3478 3478/udp
CMD ["bin/callapp-server"]
