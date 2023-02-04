FROM registry.yandex.net/rtc-base/bionic:stable

COPY it_tests/dockered_tvmtool/deploy /

RUN sed -e s/'archive.ubuntu'/'ru.archive.ubuntu'/g -i /etc/apt/sources.list

RUN apt-get update \
  && apt install -y software-properties-common \
  && apt-get install -y yandex-passport-tvmtool=1.3.7 \
  && apt-get clean

EXPOSE 18030
ENTRYPOINT ["tvmtool", "--unittest", "--dangerous-bind-all-interfaces", "-c", "/etc/tvmtool/tvmtool.conf", "--auth", "600e1f045193d1b9cd21a232a37b4e59"]
