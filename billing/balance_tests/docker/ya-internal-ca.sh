#!/bin/bash

mkdir -p /usr/local/share/ca-certificates/
wget https://crls.yandex.net/YandexInternalRootCA.crt -O /usr/local/share/ca-certificates/YandexInternalRootCA.crt
update-ca-certificates
