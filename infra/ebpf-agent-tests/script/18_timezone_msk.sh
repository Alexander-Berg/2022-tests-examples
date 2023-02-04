# Time zone: MSK
[ -s /usr/share/zoneinfo/Europe/Moscow ] && ln -fs /usr/share/zoneinfo/Europe/Moscow /etc/localtime

# https://st.yandex-team.ru/RUNTIMECLOUD-6324
[ -L /usr/share/zoneinfo/UTC ] && [ -f /usr/share/zoneinfo/Etc/UTC ] && ln -fs /usr/share/zoneinfo/Etc/UTC /usr/share/zoneinfo/UTC

echo "Europe/Moscow" > /etc/timezone

dpkg-reconfigure -f noninteractive tzdata
