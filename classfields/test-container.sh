#!/bin/bash

VER="18"

docker plugin install registry.yandex.net/vertis/golp-logger:0.0.6-dev$VER --grant-all-permissions --alias golp-logger
docker run --rm --log-driver registry.yandex.net/vertis/golp-logger:0.0.6-dev$VER registry.yandex.net/vertis/postfix echo "test golp-logger"
