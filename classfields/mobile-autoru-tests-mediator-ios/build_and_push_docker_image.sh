#!/bin/sh

# https://wiki.yandex-team.ru/docker-registry/
# To check tags:
#   curl -H "Authorization: OAuth <TOKEN>" -v https://registry.yandex.net/v2/vertis/autoru-ios-mediator/tags/list
# Login to the registry before, run
#   docker login -u <staff login> --password-stdin registry.yandex.net
# then type auth token and press Ctrl+D

image_name=vertis/autoru-ios-mediator
tag=$1

docker build -t $image_name:$1 .
docker tag $image_name:$tag registry.yandex.net/$image_name:$tag
docker push registry.yandex.net/$image_name:$tag
