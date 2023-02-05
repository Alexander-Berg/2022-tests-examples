#!/bin/bash

SED_LINE='s+<dict>+<dict><key>TestBucketIndex</key><integer>'$TEST_BUCKET_INDEX'</integer><key>TestBucketTotal</key><integer>'$TEST_BUCKET_TOTAL'</integer><key>TestIsRequired</key><'$TEST_IS_REQUIRED'/>+g'

if [[ -z $1 ]]; then
	OUTPUT_FILE="autotests.plist"
else
	OUTPUT_FILE="$1"
fi
sed -i'.old' $SED_LINE $OUTPUT_FILE
