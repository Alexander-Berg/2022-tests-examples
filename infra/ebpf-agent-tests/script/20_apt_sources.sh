umask 0022

tee etc/apt/sources.list <<EOF
deb http://mirror.yandex.ru/ubuntu xenial main restricted universe multiverse
deb http://mirror.yandex.ru/ubuntu xenial-security main restricted universe multiverse
deb http://mirror.yandex.ru/ubuntu xenial-updates main restricted universe multiverse
deb http://mirror.yandex.ru/ubuntu xenial-backports main restricted universe multiverse
EOF
