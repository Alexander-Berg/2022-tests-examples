set +e

cat /etc/os-release

cat /etc/lsb-release

dpkg -l

systemctl list-unit-files --no-pager

cat etc/resolv.conf

ls -la usr/share/zoneinfo/UTC

cat etc/fstab

cat lib/init/fstab

du -shx .
