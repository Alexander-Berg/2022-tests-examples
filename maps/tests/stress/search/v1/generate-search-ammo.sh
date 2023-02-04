#!/usr/bin/env bash

APIKEY=`npx ts-node -e "import {config} from '../../../config';console.log(config.apikey)"`
USERNAME=`whoami`
FILENAME=search.ammo
echo "[Connection: Close]" > $FILENAME
cat yt.log | sed "s/apikey=[^&$]*/apikey=$APIKEY/g" | awk -F 'ip=|\t|request=' '{print "[X-Forwarded-For: "$2"]\n" $4}' >> $FILENAME

read -p "Do you want to upload the generated ammo to MDS? (y/N) " answer

if [ "$answer" == "y" ] ;then
    gzip -c -9 $FILENAME > $FILENAME.gz
    curl https://lunapark.yandex-team.ru/api/addammo.json -F"login=$USERNAME" -F'dsc=search-api' -F"file=@$FILENAME"
fi
