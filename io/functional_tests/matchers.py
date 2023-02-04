import yandex_io.protos.model_objects_pb2 as P
from .utils import ANY_IDLING_ALICE_STATE


def is_phrase_spotter_begin(msg):
    return msg.HasField("speech_kit_event") and msg.speech_kit_event.HasField("on_phrase_spotter_begin")


def is_alice_listening(msg):
    return is_alice_state(msg, P.AliceState.State.LISTENING)


def is_alice_speaking(msg):
    return is_alice_state(msg, P.AliceState.State.SPEAKING)


def is_alice_busy(msg):
    return is_alice_state(msg, P.AliceState.State.BUSY)


def is_alice_idle(msg):
    return (
        msg.HasField("alice_state")
        and msg.alice_state.HasField("state")
        and msg.alice_state.state in ANY_IDLING_ALICE_STATE
    )


def has_alice_state(msg):
    return msg.HasField("alice_state") and msg.alice_state.HasField("state")


def is_alice_state(msg, state):
    return has_alice_state(msg) and msg.alice_state.state == state


def has_output_speech(msg):
    return (
        msg.HasField("alice_state")
        and msg.alice_state.HasField("vins_response")
        and msg.alice_state.vins_response.HasField("output_speech")
        and len(msg.alice_state.vins_response.output_speech) > 0
    )


def has_app_state(msg):
    return msg.HasField("app_state")


def has_radio_state(msg):
    return has_app_state(msg) and msg.app_state.HasField("radio_state")


def has_music_state(msg):
    return has_app_state(msg) and msg.app_state.HasField("music_state")


def has_multiroom_state(msg):
    return has_app_state(msg) and msg.app_state.HasField("multiroom_state")


def has_audio_player_state(msg):
    return (
        has_app_state(msg)
        and msg.app_state.HasField("audio_player_state")
        and msg.app_state.audio_player_state.HasField("audio")
    )


def has_simple_player_state(msg):
    return msg.HasField("simple_player_state")


def has_platform_volume_state(msg):
    return msg.HasField("testpoint_message") and msg.testpoint_message.HasField("platform_volume_state")


def has_alice_volume_state(msg):
    return msg.HasField("testpoint_message") and msg.testpoint_message.HasField("alice_volume_state")


def is_music_playing(msg):
    return (
        has_music_state(msg)
        and not msg.app_state.music_state.is_paused
        and msg.app_state.music_state.progress > 0
        or has_audio_player_state(msg)
        and msg.app_state.audio_player_state.state == P.AudioClientState.PLAYING
        and msg.app_state.audio_player_state.audio.position_sec > 0
    )


def is_music_stop(msg):
    return (
        (has_music_state(msg) or has_audio_player_state(msg))
        and (not has_music_state(msg) or has_music_state(msg) and msg.app_state.music_state.is_paused)
        and (
            not has_audio_player_state(msg)
            or has_audio_player_state(msg)
            and msg.app_state.audio_player_state.state != P.AudioClientState.PLAYING
            and msg.app_state.audio_player_state.state != P.AudioClientEvent.HEARTBEAT
            and msg.app_state.audio_player_state.state != P.AudioClientState.BUFFERING
        )
    )


def is_radio_playing(msg):
    return has_radio_state(msg) and not msg.app_state.radio_state.is_paused


def is_radio_stop(msg):
    return has_radio_state(msg) and msg.app_state.radio_state.is_paused


def is_multiroom_slave_playing(msg):
    return has_multiroom_state(msg) and (not msg.app_state.multiroom_state.is_paused)
