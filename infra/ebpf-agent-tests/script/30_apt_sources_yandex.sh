# For yandex-search-common-apt
tee etc/apt/sources.list.d/yandex-common-temporary.list <<EOF
deb [trusted=yes] http://common.dist.yandex.ru/common stable/all/
deb [trusted=yes] http://common.dist.yandex.ru/common stable/amd64/
EOF

apt-get update

apt-get --yes --no-install-recommends --force-yes install \
	yandex-archive-keyring \
	yandex-search-common-apt

rm etc/apt/sources.list.d/yandex-common-temporary.list

apt-get update
