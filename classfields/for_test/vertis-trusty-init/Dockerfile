FROM registry.yandex.net/vertis-base/ubuntu:trusty

#RUN apt-get update -y
#
#CMD ["/sbin/init"]
#
#LABEL product="vertis"
#####

RUN apt-get update -y && mkdir -p /var/log/vertis-trusty-init 

CMD while true; do echo "`hostname`   `date` "  >> /alloc/logs/vertis-trusty-init.log; sleep 1; done
