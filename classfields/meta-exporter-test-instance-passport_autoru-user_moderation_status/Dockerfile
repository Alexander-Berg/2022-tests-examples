FROM registry.yandex.net/vertis/mysql2yt-meta-exporter:latest
LABEL maintainer="Ivan Bogdanov <spooner@yandex-team.ru>"

ENV DEBIAN_FRONTEND=noninteractive

ADD src/ /
ENTRYPOINT [ "/opt/bin/run.sh" ]
