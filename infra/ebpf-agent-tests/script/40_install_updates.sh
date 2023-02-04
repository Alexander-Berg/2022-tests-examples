# Install security updates
export DEBIAN_FRONTEND="noninteractive"
apt-get update
apt-get --quiet --yes --no-install-recommends dist-upgrade
