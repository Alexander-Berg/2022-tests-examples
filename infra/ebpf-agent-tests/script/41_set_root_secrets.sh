# do not log secrets
set +x

# hint: encrypt with mkpasswd -m sha-512
if [ -n "$root_password_encrypted" ] ; then
	echo "root:$root_password_encrypted" | chpasswd --encrypted
fi

if [ -n "$root_authorized_keys" ] ; then
	mkdir -p root/.ssh
	chmod 0700 root/.ssh
	echo "$root_authorized_keys" >> root/.ssh/authorized_keys
	chmod 0600 root/.ssh/authorized_keys
fi

set -x
