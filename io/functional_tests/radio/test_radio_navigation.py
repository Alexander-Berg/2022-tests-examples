import pytest
from yandex_io.pylibs.functional_tests.utils import test_data_path, common_data_path, presmoke, station_presmoke
from yandex_io.pylibs.functional_tests.matchers import (
    is_music_stop,
    is_music_playing,
    is_radio_playing,
    is_radio_stop,
)

from testlib import get_radio_info


@pytest.mark.testpalm("https://testpalm.yandex-team.ru/alice/testsuite/5d09de6f3d42cb5e76b4cf06?testcase=1695")
@pytest.mark.testpalm("https://testpalm.yandex-team.ru/alice/testsuite/5d09de6f3d42cb5e76b4cf06?testcase=1697")
@presmoke
@station_presmoke
@pytest.mark.with_yandex_plus
def test_radio_navigation(device):
    device.wait_for_authenticate_completion()
    device.wait_for_listening_start()

    with device.get_service_connector("aliced") as aliced:

        device.start_conversation()
        device.say_to_mic(test_data_path("play_radio_dfm.wav"))

        init_radio_id = get_radio_info(
            device.wait_for_message(aliced, is_radio_playing, "Radio should start play after play command")
        ).radio_id

        device.start_conversation()
        device.say_to_mic(test_data_path("stop.wav"))
        device.wait_for_message(aliced, is_radio_stop, "Radio should stop after stop command")

        device.start_conversation()
        device.say_to_mic(test_data_path("play.wav"))
        device.wait_for_message(
            aliced,
            lambda m: is_radio_playing(m) and get_radio_info(m).radio_id == init_radio_id,
            "Same radio should play after play command",
        )

        device.start_conversation()
        device.say_to_mic(test_data_path("next.wav"))

        device.wait_for_message(
            aliced,
            lambda m: is_radio_playing(m) and get_radio_info(m).radio_id != init_radio_id,
            "Radio should change after next command",
        )

        device.start_conversation()
        device.say_to_mic(test_data_path("previous.wav"))

        device.wait_for_message(
            aliced,
            lambda m: is_radio_playing(m) and get_radio_info(m).radio_id == init_radio_id,
            "Was waiting for the first radio after prev command",
        )

        device.start_conversation()
        device.say_to_mic(common_data_path("enough.wav"))

        device.wait_for_message(aliced, is_radio_stop, "Radio should stop")


@pytest.mark.testpalm("https://testpalm.yandex-team.ru/alice/testsuite/5d09de6f3d42cb5e76b4cf06?testcase=1660")
@presmoke
@station_presmoke
@pytest.mark.with_yandex_plus
def test_radio_and_music_switching(device):
    device.wait_for_authenticate_completion()
    device.wait_for_listening_start()

    with device.get_service_connector("aliced") as aliced:

        device.start_conversation()
        device.say_to_mic(test_data_path("play_radio_dfm.wav"))

        init_radio_id = get_radio_info(
            device.wait_for_message(aliced, is_radio_playing, "Radio should start to play")
        ).radio_id

        device.start_conversation()
        device.say_to_mic(test_data_path("play_music.wav"))

        device.wait_for_message(
            aliced,
            lambda m: is_music_playing(m) and is_radio_stop(m),
            "Music break in after play music command. Radio should stop playing",
        )

        device.start_conversation()
        device.say_to_mic(test_data_path("play_radio_dfm.wav"))

        radio_id = get_radio_info(
            device.wait_for_message(
                aliced, lambda m: is_radio_playing(m) and is_music_stop(m), "Radio should break in. Music should stop"
            )
        ).radio_id

        device.failer.assert_fail(
            radio_id == init_radio_id,
            "Wrong radio started to play. Expected: {}, Actual: {}".format(init_radio_id, radio_id),
        )

        device.start_conversation()
        device.say_to_mic(common_data_path("enough.wav"))

        device.wait_for_message(aliced, is_radio_stop, "Radio should stop")
