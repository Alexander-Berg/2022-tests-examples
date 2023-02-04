[ "$virt_mode" != "vm" ] && exit 0

export DEBIAN_FRONTEND="noninteractive"

#apt-get --quiet --yes --no-install-recommends install debconf-utils
#debconf-set-selections <<EOF
#cloud-init	cloud-init/datasources	multiselect	NoCloud, ConfigDrive
#EOF

apt-get --quiet --yes --no-install-recommends install isc-dhcp-client

# do not rewrite /etc/resolv.conf from dhcp
tee etc/dhcp/dhclient-enter-hooks.d/keep_resolv_conf <<EOF
#!/bin/sh
make_resolv_conf() { : ; }
EOF
chmod +x etc/dhcp/dhclient-enter-hooks.d/keep_resolv_conf


apt-get --quiet --yes --no-install-recommends install cloud-init

# source-directory /etc/network/interfaces.d does not work for files with '.'
# https://bugs.debian.org/cgi-bin/bugreport.cgi?bug=867921
tee etc/network/interfaces <<EOF
source /etc/network/interfaces.d/*
EOF

tee etc/network/interfaces.d/50-cloud-init.cfg <<EOF
# By default setup ipv4 by DHCP, ipv6 by SLAAC
# This config can be overwritten by cloud-init

auto lo
iface lo inet loopback

auto eth0
iface eth0 inet dhcp
EOF

# do not overwrite /etc/apt/sources.list
tee etc/cloud/cloud.cfg.d/98_apt_preserve_sources_list.cfg <<EOF
apt:
    preserve_sources_list: true
EOF

# for 41_set_root_secrets.sh
tee etc/cloud/cloud.cfg.d/99_enable_root.cfg <<EOF
disable_root: false
EOF
