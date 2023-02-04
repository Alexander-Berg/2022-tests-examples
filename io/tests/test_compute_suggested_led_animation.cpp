#include <yandex_io/modules/device_state/extended_device_state.h>

#include <yandex_io/sdk/yandex_iosdk.h>
#include <yandex_io/tests/testlib/test_utils.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace YandexIO;

Y_UNIT_TEST_SUITE(testComputeSuggestedLedAnimation) {

    Y_UNIT_TEST(testComputeSuggestedLedAnimationDataResetState) {
        ExtendedDeviceState deviceState;
        deviceState.dataResetState = ExtendedDeviceState::DataResetState::DATA_RESET_WAIT_CONFIRM;
        UNIT_ASSERT_EQUAL(computeSuggestedLedAnimation(deviceState), ExtendedDeviceState::SuggestedLedAnimation::DATA_RESET_WAIT_CONFIRM);
        deviceState.dataResetState = ExtendedDeviceState::DataResetState::DATA_RESET_IN_PROGRESS;
        UNIT_ASSERT_EQUAL(computeSuggestedLedAnimation(deviceState), ExtendedDeviceState::SuggestedLedAnimation::DATA_RESET_IN_PROGRESS);
    }

    Y_UNIT_TEST(testComputeSuggestedLedAnimationIsMicOn) {
        ExtendedDeviceState deviceState;
        deviceState.isMicOn = false;
        UNIT_ASSERT_EQUAL(computeSuggestedLedAnimation(deviceState), ExtendedDeviceState::SuggestedLedAnimation::MUTE);
    }

    // FIXME: need more tests

    Y_UNIT_TEST(testComputeSuggestedLedAnimationPriority) {
        ExtendedDeviceState deviceState;
        UNIT_ASSERT_EQUAL(computeSuggestedLedAnimation(deviceState), ExtendedDeviceState::SuggestedLedAnimation::IDLE);
        deviceState.isMicOn = false;
        deviceState.dataResetState = ExtendedDeviceState::DataResetState::DATA_RESET_WAIT_CONFIRM;
        UNIT_ASSERT_EQUAL(computeSuggestedLedAnimation(deviceState), ExtendedDeviceState::SuggestedLedAnimation::DATA_RESET_WAIT_CONFIRM);
        deviceState.sdkState.updateState.isCritical = true;
        deviceState.sdkState.updateState.state = YandexIO::SDKState::UpdateState::State::DOWNLOADING;
        UNIT_ASSERT_EQUAL(computeSuggestedLedAnimation(deviceState), ExtendedDeviceState::SuggestedLedAnimation::UPDATE_DOWNLOADING);
        deviceState.sdkState.isTimerPlaying = true;
        UNIT_ASSERT_EQUAL(computeSuggestedLedAnimation(deviceState), ExtendedDeviceState::SuggestedLedAnimation::TIMER);
        deviceState.sdkState.isAlarmPlaying = true;
        UNIT_ASSERT_EQUAL(computeSuggestedLedAnimation(deviceState), ExtendedDeviceState::SuggestedLedAnimation::ALARM);
        deviceState = ExtendedDeviceState();
        deviceState.sdkState.aliceState.state = YandexIO::SDKState::AliceState::State::SPEAKING;
        UNIT_ASSERT_EQUAL(computeSuggestedLedAnimation(deviceState), ExtendedDeviceState::SuggestedLedAnimation::SPEAKING);
        deviceState.isMicOn = true;
        UNIT_ASSERT_EQUAL(computeSuggestedLedAnimation(deviceState), ExtendedDeviceState::SuggestedLedAnimation::SPEAKING);
        deviceState.isMicOn = false;
        UNIT_ASSERT_EQUAL(computeSuggestedLedAnimation(deviceState), ExtendedDeviceState::SuggestedLedAnimation::MUTE);
    }

}
