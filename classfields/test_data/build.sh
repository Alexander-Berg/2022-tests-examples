#!/usr/bin/env bash

set -e

docker build -t registry.vertis.yandex.net/vs-billing/create_orders .
docker push registry.vertis.yandex.net/vs-billing/create_orders

