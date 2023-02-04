FROM registry.yandex.net/maps/core-base-bionic:9754240

ARG SERVICE

COPY wait-for.sh /
COPY install /
RUN mkdir -p /var/log/yandex/maps/${SERVICE}/

RUN mkdir -p /var/log/yandex/tvmtool
RUN chmod -R 777 /etc/tvmtool /var/log/yandex/tvmtool || true

RUN sed -e 's/.*d_auth.*/#&/' -i /etc/syslog-ng/syslog-ng-default.tpl
RUN echo "" > /etc/nginx/locations/404-any
RUN echo "" > /etc/nginx/locations/500-html

RUN yacare enable ${SERVICE}
