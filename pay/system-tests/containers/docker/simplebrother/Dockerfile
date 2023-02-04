FROM registry.yandex.net/ubuntu:trusty
LABEL maintainer="spirit-1984@yandex-team.ru"

RUN apt-get update && apt-get install -y dpkg software-properties-common \
&& add-apt-repository ppa:longsleep/python2.7-backports \
&& apt-get update && apt-get install -y git \
oracleclient=19.3.0.0.0-yb1 \
python2.7 \
python-pip \
dpkg \
yandex-balance-common-conf-dev \
&& apt-get clean

RUN mkdir -p /var/log/yb /var/run/yb

COPY simplebrother_balance_bo/requirements.txt /code/requirements.txt
COPY requirements-dev.txt /code/requirements-dev.txt
RUN pip install --upgrade pip && pip install setuptools==42.0.2 && \
   pip install -i https://pypi.yandex-team.ru/simple/ -r /code/requirements.txt && \
   pip install -i https://pypi.yandex-team.ru/simple/ -r /code/requirements-dev.txt

COPY code code
COPY docker-servant.cfg.xml /etc/yandex/simplebrother_balance_bo/servant.cfg.xml
COPY docker/simplebrother/deploy /

ENTRYPOINT ["/opt/entrypoint.sh"]
