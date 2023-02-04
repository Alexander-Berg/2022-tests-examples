#!/bin/sh

auth_string="admin:admin"


cr_alerts=`curl -s -u $auth_string "http://hdp-01-sas.test.vertis.yandex.net:8080/api/v1/clusters/vertis/alerts?Alert/state=CRITICAL" | jq -r '.items[].Alert.definition_name' | wc -l`

warn_alerts=`curl -s -u $auth_string "http://hdp-01-sas.test.vertis.yandex.net:8080/api/v1/clusters/vertis/alerts?Alert/state=WARNING" | jq -r '.items[].Alert.definition_name' | wc -l`


if [ "$cr_alerts" -gt "0" ] 
 then
    echo "2; Cluster status unhealthy (Critical alerts:" $cr_alerts "|" "Warning alerts" $warn_alerts ")"
    exit 0
 fi



if [ "$warn_alerts" -gt "0" ]
 then
     echo "1; Cluster status unhealthy (Critical alerts:"$cr_alerts "|" "Warning alerts:"$warn_alerts ")"
 else 
     echo "0; Cluster status healthy"
fi
