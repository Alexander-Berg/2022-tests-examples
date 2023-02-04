if [ "$virt_mode" != "app" -a "$virt_mode" != "os" -a "$virt_mode" != "vm" ] ; then
	echo '$virt_mode must be set to app|os|vm'
	exit 1
fi
