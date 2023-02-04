FROM registry.yandex.net/tools/uhura-deps:latest

ENV DJANGO_SECRET_KEY 12345678900987654321
CMD bash entrypoint-tests.sh

COPY deps/python-dev.txt /
RUN pip install --disable-pip-version-check -i https://pypi.yandex-team.ru/simple/ -r /python-dev.txt

COPY entrypoint-tests.sh /app/
COPY uhura/ uhura/
RUN pip install -e .
