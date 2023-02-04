#!/usr/bin/env bash

[ ! -z "$TRACE" ] && set -x

SITEMAP_PROTOCOL=${SITEMAP_PROTOCOL:-https}
SITEMAP_HOST=${SITEMAP_HOST:-auto.ru}

SITEMAP_PATHS=( 0 10 20 )

SITEMAP_MAXBRANCHES_TEST=5

log() {
    echo >&2 $*
}

get_url() {
    local url="${SITEMAP_PROTOCOL}://$SITEMAP_HOST"
    if [ -z "$1" ]; then
        url="${url}/sitemap.xml"
    else
        url="${url}/sitemap.$1.xml"
    fi
    echo $url
}

test_headers() {
    local proto=
    local st=
    local msg=
    local len=
    while IFS=':' read key value; do
        # trim whitespace in "value"
        value=${value##+([[:space:]])}; value=${value%%+([[:space:]])}

        case "$key" in
            HTTP*) read proto st msg <<< "$key{$value:+:$value}"
                ;;
            Content-Length) len="$value"
                ;;
        esac
    done < <(curl --insecure -sI -H "Accept-Encoding: gzip" "$1")

    if [ -z "$st" ] || [ "$st" != "200" ]; then
        exit 1
    fi

    #if [ -z "$len" ] || [ "$len" = "0" ]; then
    #    exit 3
    #fi
}

test_map_headers() {
    local url=$1

    log "Checking headers $url..."

    ( test_headers $url )
    ret=$?
    if [ "$ret" -gt "0" ]; then
        log "headers test for $url exited with ${ret}"
        exit $ret
    fi

    log "ok headers"
}

test_maps() {
    local url=`get_url`
    test_map_headers $url
    for map in ${SITEMAP_PATHS[@]}; do
        url=`get_url $map`
        test_map_headers $url
    done
}

test_maps

