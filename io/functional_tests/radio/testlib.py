from yandex_io.pylibs.functional_tests.matchers import is_radio_playing


def has_radio_playing_simple_player_state(msg):
    if not msg.HasField("simple_player_state"):
        return False
    player_state = msg.simple_player_state
    return (
        player_state.has_pause
        and not player_state.has_next
        and not player_state.has_prev
        and not player_state.has_progress_bar
        and not player_state.has_like
        and not player_state.has_dislike
        and not player_state.show_player
    )


def assert_player_state(device, testpointd):
    device.wait_for_message(
        testpointd, has_radio_playing_simple_player_state, "Was waiting for simple player state message with radio"
    )


class RadioInfo(object):
    def __init__(self, radio_state):
        self.radio_id = radio_state.radio_id
        self.radio_title = radio_state.radio_title


def get_radio_info(mediad_message):
    return RadioInfo(mediad_message.app_state.radio_state)


def ensure_radio_playing(device):
    with device.get_service_connector("aliced") as aliced:
        device.wait_for_message(aliced, is_radio_playing, "Radio should still playing")
