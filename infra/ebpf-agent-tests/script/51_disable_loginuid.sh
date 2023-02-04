# https://st.yandex-team.ru/RUNTIMECLOUD-5421
if [ "$virt_mode" = "app" ] ; then
	sed -e '/pam_loginuid.so/ s/^/#/' -i /etc/pam.d/login
fi
