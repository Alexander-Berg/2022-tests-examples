#!/bin/bash

# Если есть параметр --verbose - печатать отладочную информацию
vrb=0;
if [[ "$1" = "--verbose" ]]; then
	vrb=1;
fi

# Захардкоженные координаты жестов/элементов/итд

coord_main_menu_tap_x=100;
coord_main_menu_tap_y=500;

coord_burger_tap_x=100;
coord_burger_tap_y=100;

coord_custom_button_tap_x=100;
coord_custom_button_tap_y=1600;


open_page(){
wait_time="1s";

adb shell input touchscreen tap $coord_main_menu_tap_x $coord_main_menu_tap_y
sleep $wait_time;
adb shell input touchscreen tap $coord_burger_tap_x $coord_burger_tap_y
sleep $wait_time;
adb shell input touchscreen tap $coord_custom_button_tap_x $coord_custom_button_tap_y
sleep $wait_time;

if [[ "$vrb" -eq 1 ]]; then
  echo "Customization config input opened.";
fi

# Тут магия эскейпинга символов для андроида. Паста из интернета.
escape() {
    # Encapsulate the string in $'', which enables interpretation of
    # \xnn escapes in the string. This is not POSIX-sh, but an extension
    # documented by bash and also supported by the Android sh.
    echo -n "$'"

    # Process each character in $1 one by one
    for (( i=0 ; i<${#1}; i++ )); do
        # Extract the i'th character
        C="${1:$i:1}"
        if [ "$C" = ' ' ]; then
            # Encode spaces as %s, which is needed for Android's
            # "input text" command below 6.0 Marshmellow
            # See https://stackoverflow.com/documentation/android/9408/adb-shell/3958/send-text-key-pressed-and-touch-events-to-android-device-via-adb
            echo -n '%s'
        else
            # Encode everything else as \xnn, to prevent them from being
            # interpreted by the Android shell
            printf '\\x%02x' "'$C"
        fi
    done
    # Terminate the $''
    echo -n "'"
}

ESCAPED_TEXT=`escape "$(cat $CUSTOM)"`

node ./test-customization/send-text-adb.js "$ESCAPED_TEXT";

sleep $wait_time

# Костальная паста из интернета (иначе либо никак, либо тотальный оверхед)
# Тут мы лезем представление интерфейса (предположительно)
adb pull $(adb shell uiautomator dump | grep -oP '[^ ]+.xml') /tmp/view.xml
# Перловая темная магия. Ищем координаты кнопки с текстом "Apply"
coords=$(perl -ne 'printf "%d %d\n", ($1+$3)/2, ($2+$4)/2 if /text="Apply"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"/' /tmp/view.xml)
echo ${coords}
adb shell input touchscreen tap $coords

}

node ./test-customization/web-gen.js $CUSTOM
yandex-browser ./test-customization/temp.html
open_page;
