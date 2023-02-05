#!/bin/bash
set -e
TEST_COMMAND="Acceptance"
ANDROID_TARGET="27"
SWARMER_VERSION=0.2.4
TEST_CLASS="NONE"
BRANCH="next-release"
TEST_LOGIN="NONE"
TEST_PASSWORD="NONE"
USE_IPV4="false"
TABLET="false"

# WiKi: https://wiki.yandex-team.ru/yaphone/automation

OPTS=$(getopt -o '' -l help -l ipv4: -l test-class: -l branch: -l command: -l target: -l tablet: -l login: -l password: -- "$@")
eval set -- "${OPTS}"

while true ; do
    case "$1" in
        --help)
            echo 'EXAMPLE 1: ./tools/ci/run_espresso_tests.sh --command "Acceptance"'
            echo 'EXAMPLE 2: ./tools/ci/run_espresso_tests.sh --ipv4="false" --test-class="NONE" --target=28 --tablet=true --branch="next-release" --command="Regression"'
            echo 'EXAMPLE 3: ./tools/ci/run_espresso_tests.sh --test-class "AllAppsMenuTest#shouldOpenAllAppsBySwipe"'
            exit 0
            ;;
        --ipv4)
            USE_IPV4="$2"
            shift 2
            ;;
        --test-class)
            TEST_CLASS="$2"
            shift 2
            ;;
        --branch)
            BRANCH="$2"
            shift 2
            ;;
        --command)
            TEST_COMMAND="$2"
            shift 2
            ;;
        --target)
            ANDROID_TARGET="$2"
            shift 2
            ;;
        --tablet)
            TABLET="$2"
            shift 2
            ;;
        --login)
            TEST_LOGIN="$2"
            shift 2
            ;;
        --password)
            TEST_PASSWORD="$2"
            shift 2
            ;;
        -- )
            shift;
            break ;;
        * )
            break ;;
    esac
done

function echoTitle {
    echo ""
    echo "*************************************************************************"
    echo "   $1:"
    echo "*************************************************************************"
}

function javaVersion() {
    echoTitle "Java version"
    java -version
}

javaVersion
echoTitle "Container env"
echo "TEAMCITY_VERSION = ${TEAMCITY_VERSION}"
echo "ANDROID_HOME = ${ANDROID_HOME}"
echo "PATH = ${PATH}"
echo "USE_IPV4 = ${USE_IPV4}"

echoTitle "Test options"
echo TEST_COMMAND="$TEST_COMMAND"
echo ANDROID_TARGET="$ANDROID_TARGET"
echo TEST_CLASS="$TEST_CLASS"
echo BRANCH="$BRANCH"
echo TABLET="$TABLET"
CLASS="class"
CLASS_ARGUMENT="com.yandex.launcher.$TEST_CLASS"
ANNOTATION="annotation"
ANNOTATION_ARGUMENT="com.yandex.launcher.suites.$TEST_COMMAND"

if [[ "$USE_IPV4" == "true" ]]; then
    export _JAVA_OPTIONS=-Djava.net.preferIPv4Stack=true
fi

export PATH="${ANDROID_HOME}/tools/emulator:${ANDROID_HOME}/tools:${ANDROID_HOME}/platform-tools:${ANDROID_HOME}/tools/bin:${PATH}"

function startEmulator {
    echo ""
    echo "<---Start $1--->"
    java -jar tools/ci/swarmer_${SWARMER_VERSION}.jar start \
    --emulator-name "$1" \
    --package "$2" \
    --android-abi google_apis/x86 \
    --path-to-config-ini "$3" \
    --emulator-start-options -no-window -noaudio -prop persist.sys.language=en -prop persist.sys.country=US
}

function startEmulatorNewApi {
    GPU_MODE="swiftshader_indirect"
    echo ""
    echo "<---Start $1--->"
    java -jar tools/ci/swarmer_${SWARMER_VERSION}.jar start \
    --emulator-name "$1" \
    --package "$2" \
    --android-abi google_apis/x86 \
    --path-to-config-ini "$3" \
    --emulator-start-options -gpu ${GPU_MODE} -no-window -noaudio -prop persist.sys.language=en -prop persist.sys.country=US
}

function emulatorApi22 {
    PACKAGE="system-images;android-22;google_apis;x86"
    if [[ "$TABLET" == "true" ]]; then
        EMULATOR_NAME="lnchr_Tablet_10.1"
        CONFIG_PATH="tools/ci/avd-configs/Tablet_10.1_WXGA_API_22.avd.ini"
    else
        EMULATOR_NAME="lnchr_Nexus_4"
        CONFIG_PATH="tools/ci/avd-configs/Nexus_4_API_22.avd.ini"
    fi
    startEmulator ${EMULATOR_NAME} ${PACKAGE} ${CONFIG_PATH}
}

function emulatorApi23 {
    PACKAGE="system-images;android-23;google_apis;x86"
    if [[ "$TABLET" == "true" ]]; then
        EMULATOR_NAME="lnchr_Tablet_7"
        CONFIG_PATH="tools/ci/avd-configs/Tablet_7_WSVGA_API_23.avd.ini"
    else
        EMULATOR_NAME="lnchr_Nexus_5"
        CONFIG_PATH="tools/ci/avd-configs/Nexus_5_API_23.avd.ini"
    fi
    startEmulator ${EMULATOR_NAME} ${PACKAGE} ${CONFIG_PATH}
}

function emulatorApi24 {
    PACKAGE="system-images;android-24;google_apis;x86"
    if [[ "$TABLET" == "true" ]]; then
        EMULATOR_NAME="lnchr_Nexus_7_2012"
        CONFIG_PATH="tools/ci/avd-configs/Nexus_7_2012_API_24.avd.ini"
    else
        EMULATOR_NAME="lnchr_Nexus_6"
        CONFIG_PATH="tools/ci/avd-configs/Nexus_6_API_24.avd.ini"
    fi
    startEmulator ${EMULATOR_NAME} ${PACKAGE} ${CONFIG_PATH}
}

function emulatorApi25 {
    PACKAGE="system-images;android-25;google_apis;x86"
    if [[ "$TABLET" == "true" ]]; then
        EMULATOR_NAME="lnchr_Nexus_7_2013"
        CONFIG_PATH="tools/ci/avd-configs/Nexus_7_2013_API_25.avd.ini"
    else
        EMULATOR_NAME="lnchr_Pixel"
        CONFIG_PATH="tools/ci/avd-configs/Pixel_API_25.avd.ini"
    fi
    startEmulator ${EMULATOR_NAME} ${PACKAGE} ${CONFIG_PATH}
}

function emulatorApi26 {
    PACKAGE="system-images;android-26;google_apis;x86"
    if [[ "$TABLET" == "true" ]]; then
        EMULATOR_NAME="lnchr_Nexus_10_API26"
        CONFIG_PATH="tools/ci/avd-configs/Nexus_10_API_26.avd.ini"
    else
        EMULATOR_NAME="lnchr_Pixel_2"
        CONFIG_PATH="tools/ci/avd-configs/Pixel_2_API_26.avd.ini"
    fi
    startEmulator ${EMULATOR_NAME} ${PACKAGE} ${CONFIG_PATH}
}

function emulatorApi27 {
    PACKAGE="system-images;android-27;google_apis;x86"
    if [[ "$TABLET" == "true" ]]; then
        EMULATOR_NAME="lnchr_Nexus_9"
        CONFIG_PATH="tools/ci/avd-configs/Nexus_9_API_27.avd.ini"
    else
        EMULATOR_NAME="lnchr_Pixel_2_XL"
        CONFIG_PATH="tools/ci/avd-configs/Pixel_2_XL_API_27.avd.ini"
    fi
    startEmulatorNewApi ${EMULATOR_NAME} ${PACKAGE} ${CONFIG_PATH}
}

function emulatorApi28 {
    PACKAGE="system-images;android-28;google_apis;x86"
    if [[ "$TABLET" == "true" ]]; then
        EMULATOR_NAME="lnchr_Pixel_C"
        CONFIG_PATH="tools/ci/avd-configs/Pixel_C_API_28.avd.ini"
    else
        EMULATOR_NAME="lnchr_Pixel_XL"
        CONFIG_PATH="tools/ci/avd-configs/Pixel_XL_API_28.avd.ini"
    fi
    startEmulatorNewApi ${EMULATOR_NAME} ${PACKAGE} ${CONFIG_PATH}
}

function emulatorApi29 {
    PACKAGE="system-images;android-29;google_apis;x86"
    if [[ "$TABLET" == "true" ]]; then
        EMULATOR_NAME="lnchr_Nexus_10_API29"
        CONFIG_PATH="tools/ci/avd-configs/Nexus_10_API_29.avd.ini"
    else
        EMULATOR_NAME="lnchr_Pixel_3"
        CONFIG_PATH="tools/ci/avd-configs/Pixel_3_API_29.avd.ini"
    fi
    startEmulatorNewApi ${EMULATOR_NAME} ${PACKAGE} ${CONFIG_PATH}
}

function createAndRunEmulator {
    echoTitle "Create and Run emulators"
    if [[ ${ANDROID_TARGET} == *"22"* ]]; then
        emulatorApi22
    fi
    if [[ ${ANDROID_TARGET} == *"23"* ]]; then
        emulatorApi23
    fi
    if [[ ${ANDROID_TARGET} == *"24"* ]]; then
        emulatorApi24
    fi
    if [[ ${ANDROID_TARGET} == *"25"* ]]; then
        emulatorApi25
    fi
    if [[ ${ANDROID_TARGET} == *"26"* ]]; then
        emulatorApi26
    fi
    if [[ ${ANDROID_TARGET} == *"27"* ]]; then
        emulatorApi27
    fi
    if [[ ${ANDROID_TARGET} == *"28"* ]]; then
        emulatorApi28
    fi
    if [[ ${ANDROID_TARGET} == *"29"* ]]; then
        emulatorApi29
    fi
}

function waitForEmulator {
    BOOTANIM=""
    FAILCOUNTER=0
    FAILTIME_IN_SEC=60
    TIMEOUT_IN_SEC=5

    until [[ "$BOOTANIM" =~ "1" ]]; do
        BOOTANIM=$(adb -e shell getprop dev.bootcomplete 2>&1 &)
        echo "<--->"
          echo "${x[0]} - $BOOTANIM"
        if [[ "$BOOTANIM" =~ "0" || -z "$BOOTANIM" ]]; then
            FAILCOUNTER+=TIMEOUT_IN_SEC
            echo "Waiting for ${x[0]} to start"
        if [[ ${FAILCOUNTER} -gt ${FAILTIME_IN_SEC} ]]; then
            echo "!!!WARNING!!! Timeout reached; failed to start ${x[0]}"
            break
        fi
        else echo "${x[0]} is ready"
            break
        fi
        sleep $TIMEOUT_IN_SEC
    done
}

function waitingForEmulatorsStopBooting {
    if [[ ${ANDROID_TARGET} == *"28"* ]] || [[ ${ANDROID_TARGET} == *"29"* ]]; then
        echoTitle "Waiting for emulator stop booting"
        for i in $(adb devices |  cut -sf 1)
        do
            eval x="$i"
            waitForEmulator "${x[0]}"
        done
    fi
}

function prepareEmulator {
    echoTitle "Prepare emulator"
    echo "Disable window animation scale"
    adb shell settings put global window_animation_scale 0
    echo "Disable transition animation scale"
    adb shell settings put global transition_animation_scale 0
    echo "Disable animator duration scale"
    adb shell settings put global animator_duration_scale 0
    if [[ "$TEST_COMMAND" == "Regression" ]]; then
        set +e
        echo "Disable preinstalled Wallpapers apk"
        adb shell pm disable-user --user 0 com.google.android.apps.wallpaper
        echo "Install demo_apps_for_espresso"
        adb push tools/ci/demo-apps/testwallpaper1.jpg sdcard/Pictures
        adb push tools/ci/demo-apps/testwallpaper2.jpg sdcard/Pictures
        set -e
        adb install -r tools/ci/demo-apps/demo_app_for_espresso.apk
        adb install -r tools/ci/demo-apps/demo_app_nafanya.apk
        adb install -r tools/ci/demo-apps/test_app01_for_espresso.apk
        adb install -r tools/ci/demo-apps/test_app02_for_espresso.apk
        adb install -r tools/ci/demo-apps/demo_app_shtorka.apk
        adb install -r tools/ci/demo-apps/demo_widget.apk
        adb install -r tools/ci/demo-apps/browser_for_tests.apk
        adb install -r tools/ci/demo-apps/app_with_blue_icon.apk
        # Выдаём пермишен во время установки чтобы решить проблему https://stackoverflow.com/questions/61362916/disable-accept-test-storage-service-in-espresso
        adb install -r -g tools/ci/demo-apps/test-services-1.3.0-beta01.apk
    fi
}

function performTests {
    ARGUMENT="$1='$2'"
    echo "${ARGUMENT}"
    ./gradlew connectedQaMarketLoggedDebugAndroidTest \
    -Pyandex_account.test.login="${TEST_LOGIN}" \
    -Pyandex_account.test.password="${TEST_PASSWORD}" \
    -Pdisable_speech_kit_log \
    -Pandroid.testInstrumentationRunnerArguments."${ARGUMENT}" \
    -Plauncher.forceBuildVariant=true \
    -Plauncher.selectedBuildVariants="['logged','debug','qa', 'market','dev','noperf']" \
    -Porchestrator=true \
    -p "." \
    -Dorg.gradle.java.home=/usr/local/jdk-11
}

function getLogs {
    echoTitle "Get logs"
    mkdir -p "build/outputs/orchestrator/logs"
    echo "Start logcat..."
    adb logcat -v threadtime >> "build/outputs/orchestrator/logs/emulator.logcat" &
}

function runTests {
    echoTitle "Run tests"
    if [[ "$TEST_COMMAND" == "Acceptance" ]]; then
        echo "com.yandex.launcher.suites.Acceptance"
        ./gradlew connectedQaMarketLoggedDebugAndroidTest \
        -Pyandex_account.test.login="${TEST_LOGIN}" \
        -Pyandex_account.test.password="${TEST_PASSWORD}" \
        -Pdisable_speech_kit_log \
        -Pandroid.testInstrumentationRunnerArguments.annotation='com.yandex.launcher.suites.Acceptance' \
        -Plauncher.forceBuildVariant=true \
        -Plauncher.selectedBuildVariants="['logged','debug','qa', 'market','dev','noperf']" \
        -Porchestrator=true \
        -p "." \
        -Dorg.gradle.java.home=/usr/local/jdk-11
    else
        echo "com.yandex.launcher.suites.Regression"
        ./gradlew connectedQaMarketLoggedDebugAndroidTest \
        -Pyandex_account.test.login="${TEST_LOGIN}" \
        -Pyandex_account.test.password="${TEST_PASSWORD}" \
        -Pdisable_speech_kit_log \
        -Pandroid.testInstrumentationRunnerArguments.annotation='com.yandex.launcher.suites.Regression' \
        -Plauncher.forceBuildVariant=true \
        -Plauncher.selectedBuildVariants="['logged','debug','qa', 'market','dev','noperf']" \
        -Porchestrator=true \
        -p "." \
        -Dorg.gradle.java.home=/usr/local/jdk-11
    fi
}

function install_build_tools {
    wget https://dl.google.com/android/repository/commandlinetools-linux-8092744_latest.zip
    unzip commandlinetools-linux-8092744_latest.zip
    mkdir -p android-sdk/cmdline-tools
    mv cmdline-tools android-sdk/cmdline-tools/tools
}

#install_build_tools
createAndRunEmulator
waitingForEmulatorsStopBooting
prepareEmulator
getLogs
runTests
