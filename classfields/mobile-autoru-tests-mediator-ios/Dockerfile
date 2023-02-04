# Build
FROM swift:5.5 as build

WORKDIR /build
COPY ./Package.* ./
RUN swift package resolve
COPY . .
RUN swift build -c release

# Run
FROM registry.yandex.net/vertis-base/ubuntu:bionic
RUN apt-get update && apt-get install libcurl4 -y

WORKDIR /app

COPY --from=build /usr/lib/swift/ /usr/lib/swift/
COPY --from=build /build/.build/release /app

CMD ["./Run"]
