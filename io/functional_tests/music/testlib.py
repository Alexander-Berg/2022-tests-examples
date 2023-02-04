import yandex_io.protos.model_objects_pb2 as P

from yandex_io.pylibs.functional_tests.matchers import (
    has_simple_player_state,
    is_music_stop,
    is_music_playing,
    has_music_state,
    has_audio_player_state,
)


def ensure_is_not_playing(device, aliced, timeout=10):
    device.ensure_message_not_recieved(aliced, is_music_playing, "Music is still playing", timeout=timeout)


def wait_music_start(device, aliced, timeout=30):
    return device.wait_for_message(aliced, is_music_playing, "Music did not start to play", timeout)


def wait_music_stop(device, aliced, timeout=30):
    device.wait_for_message(aliced, is_music_stop, "Music did not stop to play", timeout)


def wait_player_state(device, testpointd, timeout=30):
    def matcher(state):
        return state.has_pause and state.has_next and state.has_prev and state.has_progress_bar and state.has_like and state.has_dislike

    device.wait_for_message(
        testpointd, lambda msg: has_simple_player_state(msg) and matcher(msg.simple_player_state), "Failed to receive player state", timeout
    )


def wait_next_track(device, aliced, prev_track_title, timeout=30):
    return device.wait_for_message(
        aliced,
        lambda m: is_music_playing(m) and get_music_info(m).title != prev_track_title,
        "Song should change after next command",
    )


class MusicInfo(object):
    def __init__(self, msg):
        if has_music_state(msg) and not msg.app_state.music_state.is_paused:
            self.title = msg.app_state.music_state.title
            self.duration = msg.app_state.music_state.duration_ms // 1000
            self.cur_position = msg.app_state.music_state.progress
        elif has_audio_player_state(msg) and msg.app_state.audio_player_state.state == P.AudioClientState.PLAYING:
            self.title = msg.app_state.audio_player_state.audio.metadata.title
            self.duration = max(msg.app_state.audio_player_state.audio.duration_sec, 30)
            self.cur_position = msg.app_state.audio_player_state.audio.position_sec
        else:
            self.title = None
            self.duration = None
            self.cur_position = None


def get_music_info(msg):
    return MusicInfo(msg)
