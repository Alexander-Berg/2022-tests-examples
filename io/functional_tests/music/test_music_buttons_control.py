import pytest

from testlib import wait_music_start, get_music_info, wait_music_stop
from yandex_io.pylibs.functional_tests.matchers import is_music_playing
from yandex_io.pylibs.functional_tests.utils import regression, test_data_path


@regression
@pytest.mark.with_yandex_plus
def test_buttons_control(device):
    device.wait_for_authenticate_completion()
    device.wait_for_listening_start()

    with device.get_service_connector("aliced") as aliced:

        # ensure current player is music
        device.start_conversation()
        device.say_to_mic(test_data_path("play_song.wav"))

        wait_music_start(device, aliced)

        device.toggle_play_pause()
        wait_music_stop(device, aliced)

        device.toggle_play_pause()
        title = get_music_info(wait_music_start(device, aliced)).title

        device.next()
        device.wait_for_message(
            aliced,
            lambda m: is_music_playing(m) and title != get_music_info(m).title,
            "Music should change after pressing next button",
        )

        device.prev()
        device.wait_for_message(
            aliced,
            lambda m: is_music_playing(m) and title == get_music_info(m).title,
            "Music should change to initial track after pressing prev button",
        )

        device.toggle_play_pause()
        wait_music_stop(device, aliced)
