FROM registry.yandex.net/tools/raw-ubuntu:18.04

COPY deps/debian-main.txt deps/debian-build.txt /
RUN apt-get update \
    && cat debian-main.txt | xargs apt-get install -y  \
    && cat debian-build.txt | xargs apt-get install -y  \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

COPY deps/python-main.txt deps/python-dev.txt /
RUN pip3 install --disable-pip-version-check -i https://pypi.yandex-team.ru/simple/ -r python-main.txt
RUN pip3 install --disable-pip-version-check -i https://pypi.yandex-team.ru/simple/ -r python-dev.txt

COPY setup.py src/
COPY pytest.ini src/
COPY tests src/tests
COPY at src/at

RUN pip3 install --disable-pip-version-check -e /src/

EXPOSE 80
CMD cd src && py.test
