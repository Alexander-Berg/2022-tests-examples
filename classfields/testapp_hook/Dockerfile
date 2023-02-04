FROM registry.yandex.net/vertis/golang:1.17.6 as builder
ARG VERSION=dev
ENV CGO_ENABLED 0
WORKDIR /build
COPY . .
RUN go build -mod=vendor -o /testapp_hook -ldflags="-w -s -X config.Version=$VERSION" ./cmd/testapp_hook

FROM registry.yandex.net/vertis/alpine:3.8
MAINTAINER Aleksey Danilov <danevge@yandex-team.ru>
MAINTAINER Alexander Saltykov <alexander-s@yandex.team.ru>
COPY --from=builder /testapp_hook /
CMD ["/testapp_hook"]
