#!/usr/bin/env bash

set -Eeuo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."

if [[ " $* " =~ ' --help ' || " $* " =~ ' -h ' ]]; then
    echo 'Fetches debug/full.js from production and testing to show the diff between them.'
    exit 0
fi

fetch-all() {
    local env="$1"
    mkdir -p $dir/$env
    echo "fetching info for $env"

    $(npm bin)/qtools hosts $env | grep -Eo '[^ ]*.yp-c.yandex.net' > $dir/$env/hosts # qtools can't handle EPIPE
    local host="$(head -n1 $dir/$env/hosts)"

    ssh root@app_box.$host cat /usr/local/app/build/debug/bundles.json > $dir/$env/bundles.json
    ssh root@app_box.$host cat /usr/local/app/.qtools.json > $dir/$env/.qtools.json

    local version=$(jq -r .registry.tag "$dir/$env/.qtools.json" | tee $dir/$env/version)

    echo "fetching bundle for $env"
    local full="$(jq -r .full $dir/$env/bundles.json)"
    local url="https://yastatic.net/s3/front-maps-static/maps-front-jsapi-v2-1/$version/build/$full"
    wget -q -O $dir/$env/full.js "$url"
}

wip=tmp/wip
dir=$wip

fetch-all testing
fetch-all production

dir="tmp/diff_$(<$dir/production/version)_$(<$dir/testing/version)"
rm -rf $dir
mv $wip $dir

git diff $dir/production/full.js $dir/testing/full.js
