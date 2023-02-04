FROM python:2.7.17-stretch
RUN apt update

RUN apt install -y curl wget

COPY /requirements.txt /requirements.txt

RUN pip install -r /requirements.txt

COPY /docker/ya.sh /ya.sh
RUN sh /ya.sh

COPY /docker/ya-internal-ca.sh /ya-internal-ca.sh
RUN sh /ya-internal-ca.sh

COPY /balance /balance
COPY /btestlib /btestlib
COPY /simpleapi /simpleapi
COPY /cashmachines /cashmachines
COPY /temp /temp

EXPOSE 4000

COPY /docker/run.sh /run.sh
RUN chmod +x /run.sh
# CMD sh /run.sh
