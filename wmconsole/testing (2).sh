set -ex

echo 'deb http://search.dist.yandex.ru/search unstable/amd64/' >> /etc/apt/sources.list.d/dist.list
echo 'deb http://search.dist.yandex.ru/search stable/all/' >> /etc/apt/sources.list.d/dist.list
echo 'deb http://dist.yandex.ru/yandex-xenial stable/all/' >> /etc/apt/sources.list.d/dist.list
echo 'deb http://dist.yandex.ru/yandex-xenial stable/amd64/' >> /etc/apt/sources.list.d/dist.list
echo 'deb http://dist.yandex.ru/common stable/all/' >> /etc/apt/sources.list.d/dist.list

apt-get update
apt-get install --no-install-recommends  --force-yes -y --allow-unauthenticated \
    yandex-openjdk11 \
    yandex-openjdk17 \
    yandex-environment-testing \
    yandex-passport-vault-client \
    jq

# Install postfix 
echo "postfix postfix/mailname string mail.yandex-team.ru" | debconf-set-selections
echo "postfix postfix/main_mailer_type string 'Internet Site'" | debconf-set-selections

apt install --force-yes -y --allow-unauthenticated postfix
