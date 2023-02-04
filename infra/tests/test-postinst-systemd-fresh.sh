#!/usr/bin/env bash

set -e

ln -s /salt/pkg/yandex-search-salt/usr/share/yandex-search-salt /usr/share/yandex-search-salt

/salt/postinst/postinst configure


if [ ! -f /var/lib/ya-salt/__need_initial_setup__ ]; then
    echo "No marker file found" && exit 1
fi

if [ ! -f /usr/share/yandex-search-salt/runtime.conf ]; then
    echo "No minion config found" && exit 1
fi

if [ ! -f /usr/share/yandex-search-salt/yandex-search-salt.conf ]; then
    echo "No salt config found" && exit 1
fi

if [ ! -f /etc/systemd/system/ya-salt-at-boot.service ]; then
    echo "No at boot service installed" && exit 1
fi
