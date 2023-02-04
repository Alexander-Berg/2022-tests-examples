#!/bin/bash

geocoder_split_dir="./local_execute/storage/offline_search_cache_local_execution/geocoder"

function test-present()
{
    files=$1
    pattern=$2
    country=$3

    if [ -z "$country" ]; then
        cat $geocoder_split_dir/$files | grep -q $pattern
    else
        cat $geocoder_split_dir/$files | grep $pattern | grep -q $country
    fi;

    if [ $? != "0" ]; then
        exit 1;
    fi;
}

function test-absent()
{
    files=$1
    pattern=$2

    cat $geocoder_split_dir/$files | grep -q $pattern
    if [ $? == "0" ]; then
        exit 1;
    fi;
}



test-present RU/geosrc.split.977.xml Крым Россия
test-present US/geosrc.split.977.xml Крым Россия # as at maps.yandex.com
test-present UA/geosrc.split.977.xml Крым Украина
test-present TR/geosrc.split.977.xml Крым Украина

test-present RU/geosrc.split.15.xml Тула

test-absent RU/geosrc.split.*.xml Владимир
