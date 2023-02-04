FROM registry.yandex.net/rtc-base/bionic:stable

RUN apt-get update \
    && apt-get install -y openjdk-8-jre\
    && apt-get clean


RUN curl -o /wiremock-jre8-standalone.jar  -k https://artifactory.yandex.net/public/com/github/tomakehurst/wiremock-jre8-standalone/2.30.1/wiremock-jre8-standalone-2.30.1.jar


EXPOSE 80