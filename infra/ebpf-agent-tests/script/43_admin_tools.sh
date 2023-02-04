export DEBIAN_FRONTEND="noninteractive"

# atop: taskstats netlink does not work in netns
systemctl mask atopacct.service || true

apt-get --quiet --yes --no-install-recommends install \
atop \
bash-completion \
dstat \
htop \
ltrace \
strace \
sysstat \
screen \
tmux
