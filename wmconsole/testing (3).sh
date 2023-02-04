set -ex

echo 'deb http://search.dist.yandex.ru/search unstable/amd64/' >> /etc/apt/sources.list.d/dist.list
echo 'deb http://search.dist.yandex.ru/search stable/all/' >> /etc/apt/sources.list.d/dist.list
echo 'deb http://dist.yandex.ru/yandex-xenial stable/all/' >> /etc/apt/sources.list.d/dist.list
echo 'deb http://dist.yandex.ru/yandex-xenial stable/amd64/' >> /etc/apt/sources.list.d/dist.list
echo 'deb http://dist.yandex.ru/common stable/all/' >> /etc/apt/sources.list.d/dist.list

apt-get update
apt-get install --no-install-recommends  --force-yes -y --allow-unauthenticated \
    yandex-openjdk11 \
    yandex-jdk8 \
    yandex-environment-testing \
    libticket-parser2-java \
    yandex-passport-vault-client \
    jq
