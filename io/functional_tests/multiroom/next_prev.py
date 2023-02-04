import yandex_io.protos.model_objects_pb2 as P
import logging
import pytest
from yandex_io.pylibs.functional_tests.multiroom import MultiroomState
from yandex_io.pylibs.functional_tests.multiroom import MultiroomStates
from yandex_io.pylibs.functional_tests.multiroom import AppStates
from yandex_io.pylibs.functional_tests.utils import test_data_path

logger = logging.getLogger(__name__)


@pytest.mark.skip(reason="Mulriroom tests are broken. TICKET: SK-6160")
@pytest.mark.with_yandex_plus
@pytest.mark.multidevice(2)
@pytest.mark.parametrize(
    "device_index",
    [
        0,
        1,
    ],
)
def test_next_prev(devices, device_index):
    devices.wait_devices_ready()

    device_name = "MASTER" if device_index == 0 else "SLAVE"
    logger.info(
        f"****\nAnnotation:\n1. Play music at MASTER device\n2. Next music at {device_name} device\n3. Prev music at {device_name} device"
    )

    with AppStates(devices) as app_states, MultiroomStates(devices) as multiroom_states:
        multiroom_states.wait_any_peers()
        devices[0].start_conversation()
        devices[0].say_to_mic(test_data_path("play_everywhere.wav"))

        app_states[0].wait_standalone_music_playing()
        ms0_music1 = multiroom_states[0].wait_for(
            lambda m: (
                m.mode == P.MultiroomState.Mode.MASTER
                and m.multiroom_broadcast.state == P.MultiroomBroadcast.State.PLAYING
            )
        )
        logger.info(
            f"Master play URL {ms0_music1.multiroom_broadcast.multiroom_params.url}, track ID {MultiroomState.music_track_id(ms0_music1)}"
        )

        app_states[1].wait_multiroom_slave_playing()
        multiroom_states[1].wait_for(
            lambda m: (
                m.mode == P.MultiroomState.Mode.SLAVE
                and m.multiroom_broadcast.state == P.MultiroomBroadcast.State.PLAYING
                and m.multiroom_broadcast.multiroom_params.url == ms0_music1.multiroom_broadcast.multiroom_params.url
            )
        )

        devices[device_index].start_conversation()
        devices[device_index].say_to_mic(test_data_path("next.wav"))

        ms0_music2 = multiroom_states[0].wait_for(
            lambda m: (
                m.mode == P.MultiroomState.Mode.MASTER
                and m.multiroom_broadcast.state == P.MultiroomBroadcast.State.PLAYING
                and m.multiroom_broadcast.multiroom_params.url != ms0_music1.multiroom_broadcast.multiroom_params.url
            )
        )
        logger.info(
            f"New master URL {ms0_music2.multiroom_broadcast.multiroom_params.url}, track ID {MultiroomState.music_track_id(ms0_music2)}"
        )

        multiroom_states[0].wait_for(
            lambda m: (
                m.mode == P.MultiroomState.Mode.MASTER
                and m.multiroom_broadcast.state == P.MultiroomBroadcast.State.PLAYING
                and m.multiroom_broadcast.multiroom_params.url == ms0_music2.multiroom_broadcast.multiroom_params.url
            )
        )

        devices[device_index].start_conversation()
        devices[device_index].say_to_mic(test_data_path("prev.wav"))

        ms0_music3 = multiroom_states[0].wait_for(
            lambda m: (
                m.mode == P.MultiroomState.Mode.MASTER
                and m.multiroom_broadcast.state == P.MultiroomBroadcast.State.PLAYING
                and m.multiroom_broadcast.multiroom_params.url != ms0_music2.multiroom_broadcast.multiroom_params.url
            )
        )
        logger.info(
            f"Prev master URL {ms0_music3.multiroom_broadcast.multiroom_params.url}, track ID {MultiroomState.music_track_id(ms0_music3)}"
        )
        devices.failer.assert_fail(
            MultiroomState.music_track_id(ms0_music1) == MultiroomState.music_track_id(ms0_music3),
            "Prev track id missmatch",
        )

        multiroom_states[0].wait_for(
            lambda m: (
                m.mode == P.MultiroomState.Mode.MASTER
                and m.multiroom_broadcast.state == P.MultiroomBroadcast.State.PLAYING
                and m.multiroom_broadcast.multiroom_params.url == ms0_music3.multiroom_broadcast.multiroom_params.url
            )
        )
