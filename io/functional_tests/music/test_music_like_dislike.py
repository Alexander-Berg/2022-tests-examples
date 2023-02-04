from yandex_io.pylibs.functional_tests.matchers import is_music_playing
from yandex_io.pylibs.functional_tests.utils import common_data_path, regression, test_data_path
import pytest

from testlib import ensure_is_not_playing, wait_music_stop, wait_music_start, get_music_info


@pytest.mark.testpalm("https://testpalm.yandex-team.ru/alice/testsuite/5d09de6f3d42cb5e76b4cf06?testcase=2041")
@pytest.mark.testpalm("https://testpalm.yandex-team.ru/alice/testsuite/5d09de6f3d42cb5e76b4cf06?testcase=2043")
@pytest.mark.testpalm("https://testpalm.yandex-team.ru/alice/testsuite/5d09de6f3d42cb5e76b4cf06?testcase=2044")
@pytest.mark.skip("Broken. If track was already liked it wont be first")
@regression
@pytest.mark.with_yandex_plus
def test_play_personal_playlist(device):
    device.wait_for_authenticate_completion()
    device.wait_for_listening_start()

    with device.get_service_connector("mediad") as mediad:

        # Checking initial conditions
        ensure_is_not_playing(device, mediad)

        # Add song_1 in personal playlist before test
        # This guarantees that user has at least one track in personal playlist
        device.start_conversation()
        device.say_to_mic(test_data_path("play_dejavu.wav"))

        first_track_title = get_music_info(wait_music_start(device, mediad)).title

        device.start_conversation()
        device.say_to_mic(test_data_path("like.wav"))

        device.wait_for_listening_start()

        device.start_conversation()
        device.say_to_mic(test_data_path("next.wav"))

        second_track_title = get_music_info(
            device.wait_for_message(
                mediad,
                lambda m: is_music_playing(m) and get_music_info(m).title != first_track_title,
                "Track should change after next command",
            )
        ).title

        device.start_conversation()
        device.say_to_mic(test_data_path("like.wav"))

        device.wait_for_listening_start()

        device.start_conversation()
        device.say_to_mic(common_data_path("enough.wav"))

        wait_music_stop(device, mediad)

        # Play personal playlist. Liked song should be first in playlist
        device.start_conversation()
        device.say_to_mic(test_data_path("play_playlist_personal.wav"))
        personal_playlist_first_song = get_music_info(wait_music_start(device, mediad)).title
        device.failer.assert_fail(
            second_track_title == personal_playlist_first_song,
            "Last liked song: {} must be first in personal playlist. But got: {}".format(
                second_track_title, personal_playlist_first_song
            ),
        )
        device.stage_logger.test_stage("Liked song successfully saved in personal playlist")

        device.start_conversation()
        device.say_to_mic(test_data_path("dislike.wav"))

        # song should change after dislike
        device.wait_for_message(
            mediad,
            lambda m: is_music_playing(m) and get_music_info(m).title != second_track_title,
            "Disliked song must be skipped after dislike command",
        )

        device.stage_logger.test_stage("Disliked song skipped")

        device.start_conversation()
        device.say_to_mic(common_data_path("enough.wav"))

        wait_music_stop(device, mediad)

        # Play personal playlist again. Dislike song should disappear
        device.start_conversation()
        device.say_to_mic(test_data_path("play_playlist_personal.wav"))
        personal_playlist_first_song = get_music_info(wait_music_start(device, mediad)).title
        device.failer.assert_fail(
            second_track_title != personal_playlist_first_song,
            "Disliked song: {} must disappear from personal playlist after dislike".format(second_track_title),
        )
        device.failer.assert_fail(
            first_track_title == personal_playlist_first_song,
            "First liked song: {} must be first in personal playlist after dislike".format(first_track_title),
        )
        device.stage_logger.test_stage("Disliked song was deleted from personal playlist")
