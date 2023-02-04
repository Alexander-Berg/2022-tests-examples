FROM registry.yandex.net/vertis-base/ubuntu:xenial
MAINTAINER Ivan Bogdanov <spooner@yandex-team.ru>

RUN apt-get -qq update ; apt-get -yy --force-yes install unzip tzdata wget python3-pip
RUN wget -q -O levant https://github.com/jrasell/levant/releases/download/0.2.5/linux-amd64-levant && chmod 755 ./levant 
RUN wget -q -O nomad.zip https://releases.hashicorp.com/nomad/0.9.5/nomad_0.9.5_linux_amd64.zip && unzip -o nomad.zip && chmod 755 ./nomad && rm nomad.zip
RUN pip3 install setuptools wheel
RUN pip3 install pyhcl

CMD ["/bin/bash"]
