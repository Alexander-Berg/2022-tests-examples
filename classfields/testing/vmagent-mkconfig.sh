#!/bin/bash

SCRAPE_FILE="/etc/scrape.yml"
SCRAPE_TEMP_FILE="/tmp/scrape.yml"
MIN_TARGETS_ENRICH_PERCENTAGE_ON_RELOAD=85
TOKENS_SECRET='sec-01fzanhx0xbv5hw2f0kmssdgy7'
YAV_ROBOT_RSA_FILE='/root/.ssh/id_rsa_datasources'
YAV_ROBOT_RSA_LOGIN='robot-vertis-yav-tst'

SCRAPE_CONFIG_URL="https://s3.mds.yandex.net/adm/prometheus-configuration/testing/scrape.yml"

check_targets() {
    curl -s "http://localhost:8429/targets" | wc -l
}

count_diff_perc() {
    FIRST=${1}
    SECOND=${2}
    echo "${SECOND}/${FIRST}*100" | bc -l | awk -F '.' '{print $1}'
}

get_yav_tokens() {
  grep -i '${TOKEN:.*}' ${SCRAPE_TEMP_FILE} | while read _str; do
    _key=$(echo ${_str} | awk -F 'TOKEN:' '{print $2}' | awk -F "}'" '{print $1}')
    _token=$(yav get version ${TOKENS_SECRET} --rsa-private-key ${YAV_ROBOT_RSA_FILE} --rsa-login ${YAV_ROBOT_RSA_LOGIN} -o ${_key})
    sed -i "s/\${TOKEN\:${_key}}/${_token}/g" ${SCRAPE_TEMP_FILE}
  done
}

if ! wget -t 2 -6 ${SCRAPE_CONFIG_URL} -O ${SCRAPE_TEMP_FILE} -o /tmp/wgetlog; then
        cat /tmp/wgetlog
        exit 1
fi

#sed -i "s/{consul_addr}/localhost\:8500/g" ${SCRAPE_TEMP_FILE}
sed -i "s/{consul_addr}/vm-consul-agent-api.vrts-slb.test.vertis.yandex.net/g" ${SCRAPE_TEMP_FILE}

[ -f ${YAV_ROBOT_RSA_FILE} ] && get_yav_tokens

if [ ! -f "${SCRAPE_FILE}" ]; then
        mv ${SCRAPE_TEMP_FILE} ${SCRAPE_FILE}
else
        if ! diff ${SCRAPE_TEMP_FILE} ${SCRAPE_FILE} > /dev/null; then
                mv ${SCRAPE_TEMP_FILE} ${SCRAPE_FILE}

                if [ "${1}" == "--wait-consul-enrichment" ]; then
                    TARGETS_BEFORE=$(check_targets)
                fi

                curl -s "http://localhost:8429/-/reload"

                if [ "${1}" == "--wait-consul-enrichment" ]; then
                    while true; do
                        ACTUAL_ENRICH_PERC=$(count_diff_perc ${TARGETS_BEFORE} $(check_targets))
                        [ "${ACTUAL_ENRICH_PERC}" -gt "${MIN_TARGETS_ENRICH_PERCENTAGE_ON_RELOAD}" ] && break
                        sleep 5;
                    done
                fi
        fi
fi
