#!/bin/bash

export DEF_MR_SERVER=sakura
export MR_USER=bmclient
export MR=mapreduce

echo "test ts_upload/download"
echo -e "#key\tvalue\na\t1" | ts_upload.py -t users/bmclient/test/simple_table
ts_download.py -t users/bmclient/test/simple_table

echo "test mrkit"
mrkit_cat users/bmclient/test/simple_table | mrkit_read

echo "OK"
