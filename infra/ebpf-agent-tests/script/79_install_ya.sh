
export DEBIAN_FRONTEND="noninteractive"

: ${with_ya="1"}
: ${YA_PACKAGE="yandex-fakeya"}

if [ "$with_ya" = "1" ] ; then
    apt-get --quiet --yes --no-install-recommends install ${YA_PACKAGE}
fi
