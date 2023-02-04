[ "$virt_mode" != "vm" ] && exit 0

: ${with_rtc_monitoring="1"}

export DEBIAN_FRONTEND="noninteractive"

if [ "$with_rtc_monitoring" = "1" ] ; then
    apt-get --quiet --yes --no-install-recommends install \
	    juggler-client \
	    yandex-search-yasmagent

fi
