FROM registry.vertis.yandex.net/ubuntu:trusty
MAINTAINER Ivan Bogdanov <spooner@yandex-team.ru>

COPY create_orders.xml /
COPY realty.sh / 
COPY realty_commercial.sh /

CMD bash /realty.sh ; bash realty_commercial.sh

