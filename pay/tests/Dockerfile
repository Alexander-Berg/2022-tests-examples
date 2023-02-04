FROM registry.yandex.net/rtc-base/bionic:stable

RUN apt-get update \
    && apt-get install -y python3.6 python3-pip \
    && apt-get clean

COPY requirements.txt /app/requirements.txt
COPY tests/requirements-tests.txt /app/requirements-tests.txt
RUN pip3 install -i https://pypi.yandex-team.ru/simple/ -r /app/requirements.txt -r /app/requirements-tests.txt

COPY kkt_srv /app/kkt_srv
COPY tests /app/tests

# it is a hack for teamcity-messages in docker:
ENV TEAMCITY_VERSION 'any value'

WORKDIR /app
ENTRYPOINT python3 -m pytest
