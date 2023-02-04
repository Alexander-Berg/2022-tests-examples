cat etc/resolv.conf || true
rm -f etc/resolv.conf

if [ -n "$with_nat64" ] ; then

# ns64-cache.yandex.net
# dns-cache.yandex.net
# ns-cache.yandex.net

tee etc/resolv.conf <<EOF
nameserver 2a02:6b8:0:3400::5005
nameserver 2a02:6b8:0:3400::1
nameserver 2a02:6b8::1:1
options timeout:1 attempts:1
EOF

else

# dns-cache.yandex.net
# ns-cache.yandex.net

tee etc/resolv.conf <<EOF
nameserver 2a02:6b8:0:3400::1
nameserver 2a02:6b8::1:1
options timeout:1 attempts:1
EOF

fi
