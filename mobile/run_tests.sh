#!/bin/bash
set -x
set -e


TARGET="android-21"
TEST_COMMAND="./gradlew connectedBetaDebugAndroidTest"

## parse options to extract optional target and command to run
## provide 'command' to run tests on different flavor/annotation/test suite

OPTS=`getopt -o '' -l help -l target: -l command: -- "$@"`
eval set -- "$OPTS"

while true ; do
    case "$1" in
        --help)
            echo 'USAGE EXAMPLE: ./run_tests.sh --target "android-22" --command "./gradlew connectedBetaDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.annotation=com.yandex.mail.suites.Acceptance"'
            exit 0
            ;;
        --target)
            TARGET="$2"
            shift 2
            ;;
        --command)
            TEST_COMMAND="$2"
            shift 2
            ;;
        --)
            shift
            break
            ;;
    esac
done

export PATH="${ANDROID_HOME}/tools:${ANDROID_HOME}/platform-tools:${PATH}"

# due to virtual agents sharing the same buildserver, we have to give unique names to the emulators
if [[ -v AGENT_NAME ]]
then
    SUFFIX="_${AGENT_NAME}"
else
    SUFFIX=""
fi

EMULATOR_PORT= # fill be selected right before running the emulator
ANDROID_SERIAL= # depends on emulator port, will be selected later as well

function select_emulator_port {
    MIN_PORT=5566
    MAX_PORT=6000
    # choose some unused port
    for PORT in $(seq ${MIN_PORT} 2 ${MAX_PORT})  # emulator port has to be even number
    do
        echo "Trying port ${PORT}"
        if ! nc -z localhost ${PORT}
        then
            EMULATOR_PORT=${PORT}
            ANDROID_SERIAL="emulator-${EMULATOR_PORT}" # android tools hardcode 'emulator' prefix
            export ANDROID_SERIAL # to make adb/gradlew connect to our device only
            return 0
        fi
    done
    echo "Couldn't find open port to run the emulator!"
    exit 1
}

EMULATOR_NAME="yamail_test_${TARGET}${SUFFIX}"
SDCARD_NAME="${EMULATOR_NAME}_sdcard"
SDCARD_FILE="${SDCARD_NAME}.img"


function cleanup {
    set +e
    rm "$SDCARD_FILE"
    adb emu kill
    android delete avd --name "$EMULATOR_NAME"
    set -e
}


function install_emulator_if_necessary {
    # install emulator if necessary
    target_installed="`android list target -c | grep ${TARGET}`"
    # unfortunally, no simple way to check for installed ABIS :(
    # anyway, this step takes just a few seconds in case everything is installed

    #if [[ -z $target_installed ]]
    #then
    #    # accept the license
    #    echo 'yes' | android update sdk -a --no-ui --filter "sys-img-x86-$TARGET"
    #fi

    echo 'yes' | android update sdk -a --no-ui --filter "sys-img-x86-${TARGET}"
}


function create_emulator_configuration {
    # Create emulator configuration
    # answer 'no' to 'Create custom hardware config?'
    echo 'no' | android create avd --force --name "$EMULATOR_NAME" --target "$TARGET" --abi x86

    # For debug: list all available virtual devices
    android list avd

    # To delete the device, run:
    # android delete avd --name yamail_test
}


function run_emulator {
    mksdcard -l "$SDCARD_NAME" 128M "$SDCARD_FILE"
    select_emulator_port # TODO: we should do that in a loop, until emulator command succeeds, but it's kinda hard with a subshell...
    emulator -avd "$EMULATOR_NAME" -skin WXGA720 -no-audio -no-window -sdcard "$SDCARD_FILE" -port "$EMULATOR_PORT" &
    # remove -no-window if you want to run emulator on machine with GUI
}


function wait_for_emulator {
    bootanim=""
    failcounter=0
    timeout_in_sec=360

    until [[ "$bootanim" =~ "stopped" ]]; do
      bootanim=`adb -e shell getprop init.svc.bootanim 2>&1 &`
      if [[ "$bootanim" =~ "device not found" || "$bootanim" =~ "device offline"
        || "$bootanim" =~ "running" ]]; then
        let "failcounter += 1"
        echo "Waiting for emulator to start"
        if [[ $failcounter -gt timeout_in_sec ]]; then
          echo "Timeout ($timeout_in_sec seconds) reached; failed to start emulator"
          exit 1
        fi
      fi
      sleep 1
    done

    echo "Emulator ${ANDROID_SERIAL} is ready"

    # For debug: list all running emulators
    emulator -list-avds
}


function prepare_emulator {
    # unlock screen in case it was locked
    adb shell input keyevent 82

    # disable animations
    adb shell settings put global window_animation_scale 0
    adb shell settings put global transition_animation_scale 0
    adb shell settings put global animator_duration_scale 0
}


function run_tests {
    ./gradlew uninstallAll
    # tests might fail, but execution has to continue, CI will determine failure from test reports
    set +e
    eval ${TEST_COMMAND}
    set -e

    # following commands are left here for reference
    # ./gradlew connectedBetaDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.annotation=com.yandex.mail.suites.Acceptance
    # ./gradlew installBetaDebug
    # ./gradlew installBetaDebugAndroidTest
    # adb shell am instrument -w -e class "com.yandex.mail.tests.MessageListTest#seeGroupModeIsOn" ru.yandex.mail.test/com.yandex.mail.UiTestsRunner
    # adb shell am instrument -w -e annotation com.yandex.mail.suites.Acceptance ru.yandex.mail.test/com.yandex.mail.UiTestsRunner
}


install_emulator_if_necessary
create_emulator_configuration
run_emulator
wait_for_emulator
prepare_emulator
run_tests
cleanup