if [ -f etc/rc.conf.local ] ; then
	cat /etc/rc.conf.local
	sed 's/[0-9\.\ ]*//g' -i /etc/rc.conf.local
fi
