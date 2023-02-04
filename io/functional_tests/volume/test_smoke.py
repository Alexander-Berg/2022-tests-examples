from yandex_io.pylibs.functional_tests.utils import (
    test_data_path,
    assert_alice_volume,
    assert_platform_volume,
    station_presmoke,
    presmoke,
)
from yandex_io.pylibs.functional_tests.matchers import is_phrase_spotter_begin, has_platform_volume_state, has_alice_volume_state
import pytest


def set_volume_by_voice(device, aliced, command):
    device.start_conversation()
    device.say_to_mic(test_data_path(command))
    device.wait_for_message(aliced, is_phrase_spotter_begin, "Failed to start spotting after start conversation")


@pytest.mark.testpalm("https://testpalm.yandex-team.ru/alice/testsuite/5d09de6f3d42cb5e76b4cf06?testcase=2059")
@presmoke
@station_presmoke
def test_volume_by_voice(device):
    device.wait_for_authenticate_completion()
    device.wait_for_listening_start()

    with device.get_service_connector("aliced") as aliced, device.get_service_connector("testpoint") as testpoint:
        device.wait_for_message(
            testpoint, has_alice_volume_state, "Failed to get initial volume state"
        ).testpoint_message.alice_volume_state

        # Exact volume values:

        set_volume_by_voice(device, aliced, "volume_5.wav")
        assert_alice_volume(device, testpoint, 5, False)

        set_volume_by_voice(device, aliced, "volume_1.wav")
        assert_alice_volume(device, testpoint, 1, False)

        set_volume_by_voice(device, aliced, "volume_3.wav")
        assert_alice_volume(device, testpoint, 3, False)

        # Volume increment and decrement:

        set_volume_by_voice(device, aliced, "volume_up.wav")
        assert_alice_volume(device, testpoint, 4, False)

        set_volume_by_voice(device, aliced, "volume_down.wav")
        assert_alice_volume(device, testpoint, 3, False)

        # Mute/unmute:

        set_volume_by_voice(device, aliced, "volume_mute.wav")
        assert_alice_volume(device, testpoint, 3, True)

        set_volume_by_voice(device, aliced, "volume_unmute.wav")
        assert_alice_volume(device, testpoint, 3, False)

        # Volume command should unmute as well:

        set_volume_by_voice(device, aliced, "volume_mute.wav")
        assert_alice_volume(device, testpoint, 3, True)

        set_volume_by_voice(device, aliced, "volume_5.wav")
        assert_alice_volume(device, testpoint, 5, False)


@pytest.mark.testpalm("https://testpalm.yandex-team.ru/alice/testsuite/5d09de6f3d42cb5e76b4cf06?testcase=2091")
@presmoke
@station_presmoke
def test_volume_by_buttons(device):
    device.wait_for_authenticate_completion()
    device.wait_for_listening_start()

    with device.get_service_connector("testpoint") as testpoint:

        state = device.wait_for_message(
            testpoint, has_platform_volume_state, "Failed to get initial volume state"
        ).testpoint_message.platform_volume_state
        expected_volume = state.cur_volume

        # Test volume increment and decrement:

        device.volume_up()
        expected_volume = min(expected_volume + 1, state.max_volume)
        assert_platform_volume(device, testpoint, expected_volume, False)

        device.volume_down()
        expected_volume -= 1
        assert_platform_volume(device, testpoint, expected_volume, False)

        # Mute/unmute:

        device.mute()
        assert_platform_volume(device, testpoint, expected_volume, True)

        device.unmute()
        assert_platform_volume(device, testpoint, expected_volume, False)

        # Volume command should unmute as well:

        device.mute()
        assert_platform_volume(device, testpoint, expected_volume, True)

        device.volume_up()
        expected_volume = min(expected_volume + 1, state.max_volume)
        assert_platform_volume(device, testpoint, expected_volume, False)

        # Toggle mute

        device.toggle_mute()
        assert_platform_volume(device, testpoint, expected_volume, True)

        device.toggle_mute()
        assert_platform_volume(device, testpoint, expected_volume, False)
