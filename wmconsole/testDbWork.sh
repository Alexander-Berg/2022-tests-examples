USER_DB_URL="user_db_rw:user_db_rw@vconsole.yandex.ru:3306/user_db"
HOST_DB_URLS="robot_v_rw:robot_v_rw@vconsole.yandex.ru:3306/robot_v","robot_f_rw:robot_f_rw@fryazino.yandex.ru:3306/robot_f"

cat hosts.txt | sort | ./dbwork -ud $USER_DB_URL -hd $HOST_DB_URLS