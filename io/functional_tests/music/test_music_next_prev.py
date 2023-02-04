import pytest

from testlib import ensure_is_not_playing, wait_music_start, get_music_info, wait_music_stop, wait_next_track

from yandex_io.pylibs.functional_tests.matchers import has_music_state, has_audio_player_state, is_music_playing
from yandex_io.pylibs.functional_tests.utils import (
    test_data_path,
    regression,
    common_data_path,
    presmoke,
    station_presmoke,
)


@pytest.mark.testpalm("https://testpalm.yandex-team.ru/alice/testsuite/5d09de6f3d42cb5e76b4cf06?testcase=2058")
@presmoke
@station_presmoke
@pytest.mark.with_yandex_plus
def test_music_rewind(device):
    device.wait_for_authenticate_completion()
    device.wait_for_listening_start()

    with device.get_service_connector("aliced") as aliced:

        # Checking initial conditions
        ensure_is_not_playing(device, aliced, 10)

        # Play some music
        device.start_conversation()
        device.say_to_mic(test_data_path("play_artist.wav"))
        wait_music_start(device, aliced)

        # Rewind forward and check
        device.start_conversation()
        device.say_to_mic(test_data_path("rewind_forward_30_sec.wav"))
        device.wait_for_message(
            aliced,
            lambda msg: has_music_state(msg)
            and msg.app_state.music_state.progress > 30
            or has_audio_player_state(msg)
            and msg.app_state.audio_player_state.audio.position_sec > 30,
            "Music posistion must be more than 30 seconds after forward rewind",
            timeout=15,
        )

        device.stage_logger.test_stage("Rewind forward successed")

        device.start_conversation()
        device.say_to_mic(test_data_path("rewind_backward_30_sec.wav"))
        device.wait_for_message(
            aliced,
            lambda msg: has_music_state(msg)
            and msg.app_state.music_state.progress < 30
            or has_audio_player_state(msg)
            and msg.app_state.audio_player_state.audio.position_sec < 30,
            "Music position must be less than 30 seconds after backward rewind",
        )

        device.stage_logger.test_stage("Rewind backward successed")


@pytest.mark.testpalm("https://testpalm.yandex-team.ru/alice/testsuite/5d09de6f3d42cb5e76b4cf06?testcase=2045")
@pytest.mark.testpalm("https://testpalm.yandex-team.ru/alice/testsuite/5d09de6f3d42cb5e76b4cf06?testcase=2058")
@regression
@pytest.mark.with_yandex_plus
def test_music_next_prev(device):
    device.wait_for_authenticate_completion()
    device.wait_for_listening_start()

    with device.get_service_connector("aliced") as aliced:

        # Checking initial conditions
        ensure_is_not_playing(device, aliced, timeout=10)

        # Play some music
        device.start_conversation()
        device.say_to_mic(test_data_path("play_music.wav"))

        title = get_music_info(wait_music_start(device, aliced)).title

        # next
        device.start_conversation()
        device.say_to_mic(test_data_path("next.wav"))

        # wait next track
        device.wait_for_message(
            aliced,
            lambda m: is_music_playing(m) and get_music_info(m).title != title,
            "Wating for next track after next command failed",
        )

        # prev
        device.start_conversation()
        device.say_to_mic(test_data_path("prev.wav"))

        # wait initial track
        device.wait_for_message(
            aliced,
            lambda m: is_music_playing(m) and get_music_info(m).title == title,
            "Wating for prev track after prev command failed",
        )


@pytest.mark.testpalm("https://testpalm.yandex-team.ru/alice/testsuite/5d09de6f3d42cb5e76b4cf06?testcase=489")
@regression
@pytest.mark.with_yandex_plus
def test_music_shuffle(device):
    device.wait_for_authenticate_completion()
    device.wait_for_listening_start()

    with device.get_service_connector("aliced") as aliced:

        # Checking initial conditions
        ensure_is_not_playing(device, aliced)

        # Play daily playlist
        device.start_conversation()
        device.say_to_mic(test_data_path("play_daily_playlist.wav"))

        first_track = get_music_info(wait_music_start(device, aliced)).title

        device.start_conversation()
        device.say_to_mic(test_data_path("next.wav"))

        second_track = get_music_info(wait_next_track(device, aliced, first_track)).title

        device.start_conversation()
        device.say_to_mic(common_data_path("enough.wav"))

        wait_music_stop(device, aliced)

        # Play daily playlist shuffled
        device.start_conversation()
        device.say_to_mic(test_data_path("play_daily_playlist_shuffled.wav"))

        # assert first 2 songs are not the same
        first_suffled_track = get_music_info(wait_music_start(device, aliced)).title
        if first_suffled_track == first_track:
            device.start_conversation()
            device.say_to_mic(test_data_path("next.wav"))
            device.failer.assert_fail(
                second_track != get_music_info(wait_next_track(device, aliced, first_suffled_track)).title,
                "Playlist was not actually shuffled. First 2 tracks are the same",
            )


@pytest.mark.testpalm("https://testpalm.yandex-team.ru/alice/testsuite/5d09de6f3d42cb5e76b4cf06?testcase=606")
@pytest.mark.testpalm("https://testpalm.yandex-team.ru/alice/testsuite/5d09de6f3d42cb5e76b4cf06?testcase=2631")
@regression
@pytest.mark.with_yandex_plus
@pytest.mark.parametrize(
    "args",
    [
        # (wav_file, should_next_be_different_song, error_msg, success_msg)
        (
            "play_short_song.wav",
            True,
            "Automatic song change after end failed",
            "Next track started to play after initial track end",
        ),
        ("play_short_song_repeat.wav", False, "Song was not repeated after switching track", "Song reapeted after end"),
    ],
)
def test_music_auto_next(device, args):
    device.wait_for_authenticate_completion()
    device.wait_for_listening_start()

    wav_file, should_next_be_different_song, error_msg, success_msg = args

    EPS = 5

    with device.get_service_connector("aliced") as aliced:

        # Checking initial conditions
        ensure_is_not_playing(device, aliced)

        # Play some music
        device.start_conversation()
        device.say_to_mic(test_data_path(wav_file))

        music_info = get_music_info(wait_music_start(device, aliced))

        # wait next track for music duration
        device.wait_for_message(
            aliced,
            lambda m: is_music_playing(m)
            and (get_music_info(m).title != music_info.title) == should_next_be_different_song,
            error_msg,
            timeout=music_info.duration + EPS,
        )

        device.stage_logger.test_stage(success_msg)
