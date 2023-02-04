from yandex_io.pylibs.functional_tests.utils import (
    test_data_path,
    ANY_IDLING_ALICE_STATE,
    presmoke,
    station_presmoke,
    regression,
)
from yandex_io.pylibs.functional_tests.matchers import (
    has_output_speech,
    is_alice_listening,
    is_alice_speaking,
    is_phrase_spotter_begin,
    is_music_playing,
    is_music_stop,
)

import pytest


@pytest.mark.testpalm("https://testpalm.yandex-team.ru/alice/testsuite/5d09de6f3d42cb5e76b4cf06?testcase=2420")
@pytest.mark.testpalm("https://testpalm.yandex-team.ru/alice/testsuite/5d09de6f3d42cb5e76b4cf06?testcase=2421")
@presmoke
@station_presmoke
def test_news_listening_on_spotter(device, backend_client):
    device.wait_for_authenticate_completion()
    config = backend_client.getAccountConfig()
    config["spotter"] = "alisa"
    backend_client.setAccountConfig(config)

    device.wait_for_listening_start('alisa')

    with device.get_service_connector("aliced") as aliced:

        device.start_conversation()
        device.say_to_mic(test_data_path("ask_news.wav"))

        device.wait_for_message(
            aliced,
            lambda m: has_output_speech(m) and is_alice_speaking(m) and check_has_news_suggest(m),
            "Alice did not start to telling news",
        )

        device.say_to_mic(test_data_path("spotter_alice.wav"))
        device.wait_for_message(aliced, is_alice_listening, "Alice should start listenning after spotter")


@pytest.mark.testpalm("https://testpalm.yandex-team.ru/alice/testsuite/5d09de6f3d42cb5e76b4cf06?testcase=2420")
@pytest.mark.testpalm("https://testpalm.yandex-team.ru/alice/testsuite/5d09de6f3d42cb5e76b4cf06?testcase=2421")
@presmoke
@station_presmoke
def test_news_cancel_on_button(device):
    device.wait_for_authenticate_completion()
    device.wait_for_listening_start()

    with device.get_service_connector("aliced") as aliced:

        device.start_conversation()
        device.say_to_mic(test_data_path("ask_news.wav"))

        device.wait_for_message(
            aliced,
            lambda m: has_output_speech(m) and is_alice_speaking(m) and check_has_news_suggest(m),
            "Alice did not start to telling news",
        )

        device.press_alice_button(target_state=ANY_IDLING_ALICE_STATE)
        device.wait_for_message(aliced, is_phrase_spotter_begin, "Spotter should become active")


def check_has_news_suggest(msg):
    return any(any(keyword in suggest.text for keyword in ["Новости", "новости"]) for suggest in msg.alice_state.vins_response.suggests)


@pytest.mark.testpalm("https://testpalm.yandex-team.ru/alice/testsuite/5d09de6f3d42cb5e76b4cf06?testcase=2548")
@regression
@pytest.mark.with_yandex_plus
@pytest.mark.skip("#FIXME Broken. Music doest no puase on news intent")
def test_news_while_music_playing(device):
    device.wait_for_authenticate_completion()
    device.wait_for_listening_start()

    with device.get_service_connector("aliced") as aliced, device.get_service_connector("mediad") as mediad:

        device.start_conversation()
        device.say_to_mic(test_data_path("play_music.wav"))

        device.wait_for_message(mediad, is_music_playing, "Music did not started to play")

        device.start_conversation()
        device.say_to_mic(test_data_path("ask_news.wav"))

        msg = device.wait_for_message(
            aliced,
            lambda m: has_output_speech(m) and is_alice_speaking(m) and check_has_news_suggest(m),
            "Alice did not start to telling news",
        )
        check_has_news_suggest(msg)

        device.wait_for_message(mediad, is_music_stop, "Music should be paused while telling news")

        # assume next doest not trigger music
        device.start_conversation()
        device.say_to_mic(test_data_path("next.wav"))

        assert mediad.ensure_message_not_received(is_music_playing, "Next command should not trigger music", 15)

        device.start_conversaion()
        device.sat_to_mic(test_data_path("play.wav"))

        device.wait_for_message(mediad, is_music_playing, "Music should start playing after play command")
