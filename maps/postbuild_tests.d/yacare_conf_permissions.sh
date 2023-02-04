for f in `ls -v /etc/yandex/maps/yacare/*.conf 2> /dev/null || :`; do
    # check read permissions for all
    printf '%o' $(( 0`stat -c %a $f` & 04 )) | grep -q 4
done
