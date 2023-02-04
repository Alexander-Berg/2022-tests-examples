from testlib import ensure_is_not_playing, wait_music_start, wait_music_stop, wait_player_state, get_music_info
from yandex_io.pylibs.functional_tests.utils import test_data_path, regression, tus_mark, presmoke, station_presmoke
from yandex_io.pylibs.functional_tests.matchers import has_output_speech, is_alice_state

import yandex_io.protos.model_objects_pb2 as P
import pytest
import re


tus = tus_mark()


@pytest.mark.testpalm("https://testpalm.yandex-team.ru/alice/testsuite/5d09de6f3d42cb5e76b4cf06?testcase=472")
@pytest.mark.testpalm("https://testpalm.yandex-team.ru/alice/testsuite/5d09de6f3d42cb5e76b4cf06?testcase=667")
@pytest.mark.testpalm("https://testpalm.yandex-team.ru/alice/testsuite/5d09de6f3d42cb5e76b4cf06?testcase=576")
@pytest.mark.testpalm("https://testpalm.yandex-team.ru/alice/testsuite/5d09de6f3d42cb5e76b4cf06?testcase=2039")
@pytest.mark.testpalm("https://testpalm.yandex-team.ru/alice/testsuite/5d09de6f3d42cb5e76b4cf06?testcase=2042")
@pytest.mark.testpalm("https://testpalm.yandex-team.ru/alice/testsuite/5d09de6f3d42cb5e76b4cf06?testcase=2056")
@pytest.mark.testpalm("https://testpalm.yandex-team.ru/alice/testsuite/5d09de6f3d42cb5e76b4cf06?testcase=2057")
@pytest.mark.testpalm("https://testpalm.yandex-team.ru/alice/testsuite/5d09de6f3d42cb5e76b4cf06?testcase=2382")
@pytest.mark.testpalm("https://testpalm.yandex-team.ru/alice/testsuite/5d09de6f3d42cb5e76b4cf06?testcase=3334")
@presmoke
@station_presmoke
@pytest.mark.with_yandex_plus
@pytest.mark.parametrize(
    "sound_input",
    [
        "play_artist.wav",
        "play_genre.wav",
        "play_playlist.wav",
        "play_song.wav",
        "play_podcast.wav",
        "play_morning_show.wav",
        "play_fairytail.wav",
        "play_waterfall_sound.wav",
    ],
)
def test_music_play_pause(device, sound_input):
    device.wait_for_authenticate_completion()
    device.wait_for_listening_start()

    if sound_input == "play_morning_show.wav":
        pytest.skip("FIXME morning show is currently broken in case of pause bug SK-5389")

    with device.get_service_connector("aliced") as aliced, device.get_service_connector("testpoint") as testpointd:

        # Checking initial conditions
        ensure_is_not_playing(device, aliced, timeout=10)

        # Ask Alice to play music
        device.start_conversation()
        device.say_to_mic(test_data_path(sound_input))

        device.wait_for_message(aliced, has_output_speech, "Alice did not respond to play music intent")

        wait_music_start(device, aliced)
        wait_player_state(device, testpointd)

        # Ask Alice to pause music
        device.start_conversation()
        device.say_to_mic(test_data_path("pause.wav"))
        wait_music_stop(device, aliced)

        # Ask Alice to play music
        device.start_conversation()
        device.say_to_mic(test_data_path("play.wav"))
        wait_music_start(device, aliced)

        # Ask Alice to play music
        device.start_conversation()
        device.say_to_mic(test_data_path("stop.wav"))
        wait_music_stop(device, aliced)


@pytest.mark.testpalm("https://testpalm.yandex-team.ru/alice/testsuite/5d09de6f3d42cb5e76b4cf06?testcase=460")
@tus
@regression
@pytest.mark.no_plus
def test_music_no_plus(device):
    device.wait_for_authenticate_completion()
    device.wait_for_listening_start()

    with device.get_service_connector("aliced") as aliced:

        # Checking initial conditions
        ensure_is_not_playing(device, aliced, timeout=10)

        device.start_conversation()
        device.say_to_mic(test_data_path("play_song.wav"))

        device.wait_for_message(aliced, has_output_speech, "Alice did not respond")

        ensure_is_not_playing(device, aliced, timeout=10)

        device.stage_logger.test_stage("Music did not start without yandex plus account")


@pytest.mark.testpalm("https://testpalm.yandex-team.ru/alice/testsuite/5d09de6f3d42cb5e76b4cf06?testcase=1169")
@regression
@pytest.mark.with_yandex_plus
def test_what_playing(device):
    device.wait_for_authenticate_completion()
    device.wait_for_listening_start()

    with device.get_service_connector("aliced") as aliced:
        ensure_is_not_playing(device, aliced, timeout=10)

        device.start_conversation()
        device.say_to_mic(test_data_path("play_song.wav"))

        title = get_music_info(wait_music_start(device, aliced)).title

        aliced.clear_message_queue()

        device.start_conversation()
        device.say_to_mic(test_data_path("whats_playing.wav"))

        response = device.wait_for_message(
            aliced, has_output_speech, "Alice did not respond"
        ).alice_state.vins_response.output_speech
        device.failer.assert_fail(
            re.search(title, response), "Song title should be in alice response to \"Whats playing\" request"
        )


@pytest.mark.testpalm("https://testpalm.yandex-team.ru/alice/testsuite/5d09de6f3d42cb5e76b4cf06?testcase=1313")
@pytest.mark.testpalm("https://testpalm.yandex-team.ru/alice/testsuite/5d09de6f3d42cb5e76b4cf06?testcase=1314")
@regression
@pytest.mark.with_yandex_plus
@pytest.mark.parametrize(
    "args", [("whats_playing_with_song.wav", [r"Toto", r"Africa"]), ("whats_playing.wav", [r"[Нн]ичего"])]
)
def test_shazam(device, args):
    device.wait_for_authenticate_completion()
    device.wait_for_listening_start()

    wav, regexps = args

    with device.get_service_connector("aliced") as aliced:

        ensure_is_not_playing(device, aliced, timeout=10)

        device.start_conversation()
        device.say_to_mic(test_data_path(wav))

        device.wait_for_message(
            aliced, lambda m: is_alice_state(m, P.AliceState.State.SHAZAM), "Alice state was not set to SHAZAM"
        )

        response = device.wait_for_message(
            aliced, has_output_speech, "Alice did not respond"
        ).alice_state.vins_response.output_speech

        device.failer.assert_fail(
            all(map(lambda reg: re.search(reg, response), regexps)),
            "Failed to find {} regexps in alice response on shazam request".format(",".join(regexps)),
        )
