[ "$virt_mode" != "vm" ] && exit 0

: ${with_porto="1"}

: ${PORTO_PACKAGE="yandex-porto"}

export DEBIAN_FRONTEND="noninteractive"

if [ "$with_porto" = "1" ] ; then
	apt-get --quiet --yes --no-install-recommends install ${PORTO_PACKAGE}
fi
