#!/usr/bin/env bash

APIKEY=`npx ts-node -e "import {config} from '../../../config';console.log(config.apikey)"`
USERNAME=`whoami`
FILENAME=geocode.ammo
echo "[Connection: Close]" > $FILENAME
cat yt.log |
    sed "s/apikey=[^&$]*//g;s/ /%20/g;s/\$/\&apikey=$APIKEY/g" |
    awk -F '\t' '{split($1,ip,"ip="); split($2,request,"request="); print "[X-Forwarded-For: "ip[2]"]\n" "/geocode"request[2]}' >> $FILENAME

read -p "Do you want to upload the generated ammo to MDS? (y/N) " answer

if [ "$answer" == "y" ] ;then
    gzip -c -9 $FILENAME > $FILENAME.gz
    curl https://lunapark.yandex-team.ru/api/addammo.json -F"login=$USERNAME" -F'dsc=search-api' -F"file=@$FILENAME"
fi
