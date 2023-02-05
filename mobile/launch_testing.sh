#!/usr/bin/env bash

WORKSPACE_PATH=$1
SCHEME=$2 #UITestingUITest
WORKSPACE_NAME=$(echo $WORKSPACE_PATH | rev | cut -d '/' -f1 | rev | cut -d '.' -f1)

## date format ##
CURRENT_DATE=$(date +"%F")
CURRENT_TIME=$(date +"%T")

## device parameters ##
DEVICE_HASH=$3
DEVICE_TO_LAUNCH=$(instruments -s devices | grep $DEVICE_HASH)
OS_VERSION=$(echo $DEVICE_TO_LAUNCH | cut -d "(" -f2 | cut -d ")" -f1)
DEVICE_NAME=$(echo $DEVICE_TO_LAUNCH | cut -d "(" -f1 | cut -d " " -f1-2)
echo "START TEST SUITE FOR: $DEVICE_TO_LAUNCH"

## path ##
TEST_SUITE_FOLDER="${DEVICE_HASH}_${CURRENT_DATE}_${CURRENT_TIME}"

CRASH_LOGS_ROOT_PATH="~/Library/Logs/DiagnosticReports"
DEVICE_LOGS_READ_PATH="~/Library/Logs/CoreSimulator/${DEVICE_HASH}/system.log"

TEST_CASE_INDEX=0
TEST_CASE_PATH="${TEST_SUITE_FOLDER}/test${TEST_CASE_INDEX}"

## read params ##
# while getopts "d" opt
# do
# case $opt in
#     # d) DEVICE_HASH=${OPTARG};;
# d) echo "Nice $OPTARG" ;;
# esac
# done

declare -i videoPID
declare -i deviceLoggerPID
declare -i uiTestingPID

## functions ##
function launchUITesting {
    xcodebuild \
        -workspace $WORKSPACE_PATH \
        -scheme $SCHEME \
        -destination "platform=iOS Simulator,name=$DEVICE_NAME,OS=$OS_VERSION" \
        test >> "${TEST_CASE_PATH}/uitestLogs.log" &
    uiTestingPID=$!

    echo "---UI TESTING STARTED (PID: ${uiTestingPID})"
}

function stopUITesting {
    kill -INT $uiTestingPID

    echo "---UI TESTING FINISHED"
}

function startRecording {
    echo "---START RECORDING"

    xcrun simctl io booted recordVideo "${TEST_CASE_PATH}/recordedVideo.mov" &
    videoPID=$!
    echo "---VIDEO RECORDING STARTED (PID: ${videoPID})"

    eval "tail -f $DEVICE_LOGS_READ_PATH >> ${TEST_CASE_PATH}/deviceLogs.log &"
    deviceLoggerPID=$!
    echo "---DEVICE LOGGER STARTED (PID: ${deviceLoggerPID})"
}

function stopRecording {
    kill -INT $videoPID
    echo "---VIDEO RECORDING FINISHED"

    kill $deviceLoggerPID
    echo "---DEVICE LOGGER FINISHED"

    echo "---RECORDING FINISHED"
}

## crash log ##

XCODE_CONTENTS_PATH="/Applications/Xcode.app/Contents"
XCODE_DEVELOPER_DIR_PATH="${XCODE_CONTENTS_PATH}/Developer"
SYMBOLICATECRASH="${XCODE_CONTENTS_PATH}/SharedFrameworks/DVTFoundation.framework/Versions/A/Resources/symbolicatecrash"
DSYM_PATH=$4

function pullCrashlog {

    CRASH_LOG_READ_PATH=$(eval find $CRASH_LOGS_ROOT_PATH -name '${WORKSPACE_NAME}*.crash' -cmin -1 | tail -n 1)
    CRASH_LOG_SAVE_PATH="${TEST_CASE_PATH}/crashlog.crash"
    CRASH_LOG_SYMBOLICATED_PATH="${TEST_CASE_PATH}/symbolicated.crash"
    
    if [[ $CRASH_LOG_READ_PATH ]]; then
        if [ -z "$DSYM_PATH" ]; then
            return 0  
        fi
        
        echo "Start crash symbolicating"

        echo $CRASH_LOG_READ_PATH
        echo $CRASH_LOG_SAVE_PATH
        echo $CRASH_LOG_SYMBOLICATED_PATH

        cp $CRASH_LOG_READ_PATH $CRASH_LOG_SAVE_PATH

        export "DEVELOPER_DIR=${XCODE_DEVELOPER_DIR_PATH}/"
        echo "eval $SYMBOLICATECRASH --dsym=$DSYM_PATH -o $CRASH_LOG_SYMBOLICATED_PATH $CRASH_LOG_SAVE_PATH"
        eval $SYMBOLICATECRASH --dsym=$DSYM_PATH -o $CRASH_LOG_SYMBOLICATED_PATH $CRASH_LOG_SAVE_PATH -v

        echo "End crash symbolicating"
    fi

}

## exit trap ##

function ctrl_c() {
    stopUITesting
}

## main ##

echo "TEST SUITE STARTED"

trap ctrl_c INT

# boot device
xcrun instruments -w "$DEVICE_TO_LAUNCH"

# wait until device booted
while [ $(xcrun simctl list | grep $DEVICE_HASH | grep Booted | wc -l | sed -e 's/ //g') -lt 1 ]
do
    sleep 1
done

echo "DEVICE INFO: $(xcrun simctl list | grep $DEVICE_HASH | grep Booted)"

mkdir $TEST_SUITE_FOLDER

# while true
# do
    ((TEST_CASE_INDEX++))

    echo "--------------------------"
    echo "TEST CASE #${TEST_CASE_INDEX} START"
    echo "--------------------------"

    TEST_CASE_PATH="${TEST_SUITE_FOLDER}/test${TEST_CASE_INDEX}"
    mkdir $TEST_CASE_PATH

    startRecording
    launchUITesting
    
    # wait until UITesting is completed (check if pid exists in ps)
    ps -o time,%cpu,%mem,rss | head -n 1 >> "${TEST_CASE_PATH}/memory.log" # save memory log
    while [ $(ps -p $uiTestingPID | wc -l | sed -e 's/ //g') -eq 2 ]
    do

        APPLICATION_PID=$(pgrep $WORKSPACE_NAME)
        if [[ $APPLICATION_PID ]]; then
            ps -o time,%cpu,%mem,rss -p $APPLICATION_PID | tail -n 1 >> "${TEST_CASE_PATH}/memory.log" # save memory log
        fi

        sleep 1
    done

    sleep 5
    pullCrashlog
    stopRecording

    echo "--------------------------"
    echo "TEST CASE #${TEST_CASE_INDEX} END"
    echo "--------------------------"

# done

    echo "TEST SUITE FINISHED"
