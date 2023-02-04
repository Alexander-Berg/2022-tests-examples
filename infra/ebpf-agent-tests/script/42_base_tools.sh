export DEBIAN_FRONTEND="noninteractive"

apt-get --quiet --yes --no-install-recommends install \
bc \
binutils \
bsd-mailx \
bsdmainutils \
bzip2 \
cron \
curl \
daemon \
dnsutils \
e2fsprogs=1.44.1-1ubuntu1.2 \
ethtool \
file \
fuse \
iptables \
iputils-ping \
iputils-tracepath \
less \
libext2fs2=1.44.1-1ubuntu1.2 \
lockfile-progs \
logrotate \
lsb-release \
lsof \
m4 \
man \
netbase \
netcat-openbsd \
net-tools \
ntpdate \
openssh-client \
perl \
pigz \
psmisc \
python \
rsync \
rsyslog \
sudo \
tar \
tcpdump \
vim \
vmtouch \
wget \
xz-utils \
zstd


if [ "$virt_mode" == "vm" ] ; then
	apt-get --quiet --yes --no-install-recommends install \
		kmod \
		udev \
		dmsetup
fi
