#!/bin/bash

PROM_URL='http://vmselect-agg-api.vrts-slb.prod.vertis.yandex.net/select/0/prometheus/api/v1/query'
PROM_QUERY='rate(sentry_events_processed{docker_host="'$(hostname -f)'"}[1m])<0.1'
CHECK_URL="http://localhost:9090/ping"
CURL_CMD='curl --connect-timeout 2 --max-time 5 -sf'

sentry_healthcheck() {
    if ! $(${CURL_CMD} ${CHECK_URL} > /dev/null); then
        exit 0
    fi
}

prom_check() {
    _prom_out=$(${CURL_CMD} --data-urlencode 'query='${PROM_QUERY} ${PROM_URL})
    if [ "${?}" -ne 0 ]; then
        exit 0
    fi

    _query_result=$(echo ${_prom_out} | jq '.data.result[0].value[1]')

    if [ "${_query_result}" != "null" ]; then
        return 1;
    else
        return 0;
    fi
}

sentry_healthcheck
prom_check || (systemctl restart sentry.service && sleep 300)
