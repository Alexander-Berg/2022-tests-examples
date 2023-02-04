FROM registry.yandex.net/rtc-base/xenial:stable
LABEL author="spirit-1984"

# Using exact version of haproxy available on whitespirit*.paysys.yandex.net:8081
# liblua dev is required in order to create lua-cjson binary
RUN sed -e s/'archive.ubuntu'/'ru.archive.ubuntu'/g -i /etc/apt/sources.list \
    && apt-get update \
    && apt-get install -y lua5.3 haproxy=2.1.4-1 wget patch build-essential liblua5.3-dev

RUN apt-get install -y supervisor python3

COPY it_tests/dockered_hudsucker/deploy/ .

WORKDIR  /src

COPY hudsucker .

RUN mkdir -v clib run && wget http://s3.mds.yandex.net/sandbox-469/1674673388/lua-cjson-2.1.0.tar.gz \
  && tar -xvf lua-cjson-2.1.0.tar.gz --directory clib \
  && patch clib/lua-cjson-2.1.0/Makefile < debian/cjson_debian.patch \
  && make -C clib/lua-cjson-2.1.0 install \
  && mkdir -p /var/log/supervisor

ENTRYPOINT ["/opt/entrypoint.sh"]
CMD ["/usr/bin/supervisord", "-n", "-c", "/etc/supervisor/supervisord.conf"]
