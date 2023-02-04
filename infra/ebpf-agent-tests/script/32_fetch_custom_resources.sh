[ "$virt_mode" != "vm" ] && exit 0

: ${KERN_TEST="0"}

if [ "$KERN_TEST" != "0" ] ; then
    export DEBIAN_FRONTEND="noninteractive"
    apt-get --quiet --yes --no-install-recommends install curl

    for rid in ${CUSTOM_RESOURCE_LIST}; do
        curl -JO "https://proxy.sandbox.yandex-team.ru/${rid}"
    done

    apt-get --quiet --yes remove --purge curl
    apt-get --quiet --yes autoremove
fi
