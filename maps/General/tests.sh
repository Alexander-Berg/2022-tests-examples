#!/bin/bash -e

SERVER=mr01f.tst.maps.yandex.ru:8013
TABLES="jams_archive"

get_test_file_name()
{
    echo "tests/$1"
}

get_file_mtime()
{
    filename=/tmp/test_backup/$1/tmp/test_tables/$2
    echo $(stat -c "%Y" $filename)
}

touch_file()
{
    filename=/tmp/test_backup/$1/tmp/test_tables/$2
    touch $filename
}


get_restored_table_name()
{
    echo "restored/tmp/test_tables/$1"
}

get_table_name()
{
    echo "tmp/test_tables/$1"
}

upload_table()
{
    restored_table_name=$(get_restored_table_name $1)
    /usr/sbin/mapreduce -server $SERVER -drop $restored_table_name
    table_name=$(get_table_name $1)
    /usr/sbin/mapreduce -server $SERVER -drop $table_name
    file_name=$(get_test_file_name $1)
    cat $file_name | /usr/sbin/mapreduce -server $SERVER -subkey -$2 -write $table_name
}

check_table()
{
    table_name=$(get_table_name $1)
    restored_table_name=$(get_restored_table_name $1)
    diff=$(mr_diff -s $SERVER $table_name $restored_table_name)
    if [ -z "$diff" ] ; then
        echo "Passed"
    else
        echo "Failed" $table_name $restored_table_name
        exit 1
    fi
}

check_file_time()
{
    filename=/tmp/test_backup/$1/tmp/test_tables/$2
    first_t=$(stat $filename -c "%Y")
    second_t=$3
    if (( first_t == second_t )) ; then
        echo "Passed date " $1
    else
        echo "Failed date" $1
        exit 1
    fi
}

init_backup()
{
    mkdir -p /tmp/test_backup
    ./backup.py --config=backup-test-mr.conf --test --init
    touch_file daily signals_month_201112 
    touch_file test signals_month_201112 
    touch_file test_hidden signals_month_201112 
}

daily_backup_restore()
{
    mkdir -p /tmp/test_backup
    ./backup.py --config=backup-test-mr.conf --init
    ./backup.py --config=backup-test-mr.conf --restore daily
}

do_backup()
{
    ./backup.py --config=backup-test-mr.conf --test --update
}


trap "rm -rf /tmp/test_backup" EXIT
rm -rf /tmp/test_backup

upload_table jams_archive "-lenval"
upload_table signals_month_201112 ""
daily_backup_restore
check_table jams_archive
check_table signals_month_201112

rm -rf /tmp/test_backup
echo "********************* Initializing"
init_backup
orig=$(get_file_mtime daily signals_month_201112)
check_file_time test signals_month_201112 $orig
check_file_time test_hidden signals_month_201112 $orig
echo "********************* Update"
do_backup
check_file_time test signals_month_201112 $orig
check_file_time test_hidden signals_month_201112 $orig
sleep 4
echo "********************* Update"
touch_file daily signals_month_201112 
do_backup
check_file_time test signals_month_201112 $orig
check_file_time test_hidden signals_month_201112 $orig
sleep 4
echo "********************* Update"
touch_file daily signals_month_201112 
do_backup
check_file_time test signals_month_201112 $orig
check_file_time test_hidden signals_month_201112 $orig
sleep 4
echo "********************* Update"
touch_file daily signals_month_201112 
tmp=$(get_file_mtime daily signals_month_201112)
do_backup
check_file_time test signals_month_201112 $orig
check_file_time test_hidden signals_month_201112 $tmp
sleep 4
echo "********************* Update"
touch_file daily signals_month_201112 
do_backup
check_file_time test signals_month_201112 $orig
sleep 4
echo "********************* Update"
touch_file daily signals_month_201112 
do_backup
check_file_time test signals_month_201112 $orig
sleep 4
echo "********************* Update"
touch_file daily signals_month_201112 
do_backup
check_file_time test signals_month_201112 $tmp

