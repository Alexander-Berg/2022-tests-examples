if [ "$virt_mode" != "vm" ] ; then
	systemctl mask console-getty.service
fi
