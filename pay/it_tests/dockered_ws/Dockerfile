FROM registry.yandex.net/rtc-base/bionic:stable

RUN sed -e s/'archive.ubuntu'/'ru.archive.ubuntu'/g -i /etc/apt/sources.list \
    && apt-get update \
    && apt-get install -y python3.6 python3-pip ssh \
    && apt-get clean

RUN mkdir -p /var/lib/tvmtool && echo "600e1f045193d1b9cd21a232a37b4e59" > /var/lib/tvmtool/local.auth

RUN useradd -m yb

RUN mkdir -p /var/log/yandex/whitespirit && chown yb /var/log/yandex/whitespirit

USER yb

RUN ssh-keygen -P "" -f /home/yb/.ssh/id_rsa

COPY requirements.txt /app/requirements.txt
RUN pip3 install -i https://pypi.yandex-team.ru/simple/ -r /app/requirements.txt

COPY proto_grammars /app/proto_grammars
COPY kkt_srv /app/kkt_srv
COPY it_tests/dockered_ws/run.py /app/run.py
COPY kkt_srv_config.yaml /app/kkt_srv_config.yaml
COPY swagger.yaml /app
COPY tvm_config.yaml /app
COPY it_tests/dockered_ws/ssh_kkt_password /etc/yandex/balance-whitespirit/secrets/ssh_kkt_password

WORKDIR /app

EXPOSE 8080
