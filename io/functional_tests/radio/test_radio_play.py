import pytest
from yandex_io.pylibs.functional_tests.utils import (
    test_data_path,
    common_data_path,
    regression,
    presmoke,
    station_presmoke,
    tus_mark,
)
from yandex_io.pylibs.functional_tests.matchers import has_output_speech, is_radio_playing, is_radio_stop

from testlib import assert_player_state, get_radio_info, ensure_radio_playing
from time import sleep

import re

tus = tus_mark()


@pytest.mark.testpalm("https://testpalm.yandex-team.ru/alice/testsuite/5d09de6f3d42cb5e76b4cf06?testcase=1530")
@pytest.mark.testpalm("https://testpalm.yandex-team.ru/alice/testsuite/5d09de6f3d42cb5e76b4cf06?testcase=1543")
@presmoke
@station_presmoke
@pytest.mark.with_yandex_plus
@pytest.mark.parametrize("sound_input", ["play_radio_dfm.wav", "play_radio_102.1.wav", "play_radio.wav"])
def test_radio_play(device, sound_input):
    device.wait_for_authenticate_completion()
    device.wait_for_listening_start()

    with device.get_service_connector("aliced") as aliced, device.get_service_connector("testpoint") as testpointd:

        device.start_conversation()
        device.say_to_mic(test_data_path(sound_input))

        device.wait_for_message(aliced, is_radio_playing, "Radio should start")

        assert_player_state(device, testpointd)

        device.start_conversation()
        device.say_to_mic(common_data_path("enough.wav"))

        device.wait_for_message(aliced, is_radio_stop, "Radio should stop")


@pytest.mark.testpalm("https://testpalm.yandex-team.ru/alice/testsuite/5d09de6f3d42cb5e76b4cf06?testcase=1542")
@regression
@pytest.mark.with_yandex_plus
def test_vins_response(device):
    device.wait_for_authenticate_completion()
    device.wait_for_listening_start()

    with device.get_service_connector("aliced") as aliced:

        device.start_conversation()
        device.say_to_mic(test_data_path("play_radio_dfm.wav"))

        title = get_radio_info(
            device.wait_for_message(aliced, is_radio_playing, "Radio should start to play")
        ).radio_title

        device.start_conversation()
        device.say_to_mic(test_data_path("whats_playing.wav"))

        device.wait_for_message(
            aliced,
            lambda m: has_output_speech(m) and re.search(title, m.alice_state.vins_response.output_speech),
            "Radio title should appear in alice response on whats playing request",
        )

        device.start_conversation()
        device.say_to_mic(common_data_path("enough.wav"))

        device.wait_for_message(aliced, is_radio_stop, "Radio should stop")


@pytest.mark.testpalm("https://testpalm.yandex-team.ru/alice/testsuite/5d09de6f3d42cb5e76b4cf06?testcase=2082")
@tus
@regression
@pytest.mark.no_plus
def test_radio_play_no_plus(device):
    device.wait_for_authenticate_completion()
    device.wait_for_listening_start()

    with device.get_service_connector("aliced") as aliced, device.get_service_connector("testpoint") as testpointd:

        device.start_conversation()
        device.say_to_mic(test_data_path("play_radio_dfm.wav"))

        device.wait_for_message(aliced, is_radio_playing, "Radio should start to play")

        assert_player_state(device, testpointd)

        device.start_conversation()
        device.say_to_mic(common_data_path("enough.wav"))

        device.wait_for_message(aliced, is_radio_stop, "Radio should stop")


@pytest.mark.testpalm("https://testpalm.yandex-team.ru/alice/testsuite/5d09de6f3d42cb5e76b4cf06?testcase=2249")
@regression
@pytest.mark.with_yandex_plus
def test_radio_continue_playing(device, is_remote_test):
    device.wait_for_authenticate_completion()
    device.wait_for_listening_start()

    # sleep timout
    if is_remote_test:
        TIMEOUT = 180
    else:
        TIMEOUT = 60

    with device.get_service_connector("aliced") as aliced:

        device.start_conversation()
        device.say_to_mic(test_data_path("play_radio_dfm.wav"))

        device.wait_for_message(aliced, is_radio_playing, "Radio should start to play")

        sleep(TIMEOUT)

        # dont use wait_for_message. To recieve state new connector must be created
        ensure_radio_playing(device)

        device.start_conversation()
        device.say_to_mic(common_data_path("enough.wav"))

        device.wait_for_message(aliced, is_radio_stop, "Radio should stop")
