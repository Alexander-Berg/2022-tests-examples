FROM registry.yandex.net/vertis-base/ubuntu:trusty

RUN apt-get update -y && mkdir -p /var/lib/push-client && \ 
    mkdir -p /var/log/statbox/ && \
    apt-get install statbox-push-client-daemon && \
    mkdir -p etc/yandex/statbox-push-client/push-client.yaml.d 

CMD /etc/init.d/statbox-push-client start && while (/bin/ps aux | /bin/grep -q [p]ush-client); do sleep 1; done
