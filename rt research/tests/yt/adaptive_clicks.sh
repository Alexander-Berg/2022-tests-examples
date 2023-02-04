#!/bin/bash

YT_PROXY=plato.yt.yandex.net \
     yt map --src '//statbox/yabs-raw-event-log/2015-09-11' --dst '//tmp/bmclient_adaptive_example' \
     --local-file 'mapper.awk' \
     --format '<columns=[eventid;pageid;region_id;selecttype;countertype;devicetype;detaileddevicetype;];enable_escaping=false>schemaful_dsv' \
     --print-statistics \
     './mapper.awk'


#usage: yt map [-h] --src SOURCE_TABLE --dst DESTINATION_TABLE
#              [--file FILE_PATHS] [--local-file FILES] [--job-count JOB_COUNT]
#              [--memory-limit MEMORY_LIMIT] [--spec SPEC] [--format FORMAT]
#              [--input-format INPUT_FORMAT] [--output-format OUTPUT_FORMAT]
#              [--print-statistics]
#              command

