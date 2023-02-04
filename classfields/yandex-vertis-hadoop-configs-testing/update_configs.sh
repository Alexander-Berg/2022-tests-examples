#/bin/bash

auth=hadoop:hadoop
ambari_server=hdp-01-sas.test.vertis.yandex.net
archive=hadoop_configs.tar.gz
echo Downloading configs

curl -u $auth -H 'X-Requested-By:admin' 'http://'$ambari_server':8080/api/v1/clusters/vertis/components?format=client_config_tar' -o /tmp/$archive

echo Extracting

tar -xvf /tmp/$archive  -C src/etc/hadoop

rm /tmp/$archive
echo Done

