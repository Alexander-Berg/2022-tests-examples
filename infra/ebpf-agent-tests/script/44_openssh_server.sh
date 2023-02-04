if [ -n "$with_openssh_server" -o "$virt_mode" = "vm" ] ; then
	apt-get --quiet --yes --no-install-recommends install openssh-server
fi
