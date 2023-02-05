#!/usr/bin/env bash
set -e
tags=("yandex" "yandex-team" "outlook" "yandextest")
for (( i = 0; i < 20; ++i )); do
    for tag in "${tags[@]}" ; do
        data="tus_consumer=testopithecus&tags=$tag"
        if [[ "$tag" = "outlook" ]]; then
            ts=$(date +%s)
            sleep 1
            data="$data&login=user$ts&password=simple$ts@outlook.com&env=external"
            curl -H "Authorization: OAuth $1" -d "$data" "https://tus.yandex-team.ru/1/save_account/"
        else
            if [[ "$tag" = "yandextest" ]]; then
                data="$data&env=test"
            else
                data="$data&env=prod"
            fi
            curl -H "Authorization: OAuth $1" -d "$data" "https://tus.yandex-team.ru/1/create_account/portal/"
        fi
    done
done