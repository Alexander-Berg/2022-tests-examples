import yandex_io.protos.model_objects_pb2 as P
import logging
import pytest
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
def test_stop(devices, device_index):
    devices.wait_devices_ready()

    stop_device_index = device_index
    stop_device_name = "MASTER" if stop_device_index == 0 else "SLAVE"
    logger.info(f"****\nAnnotation:\n1. Play music at MASTER device\n2. Stop music at {stop_device_name} device")

    with AppStates(devices) as app_states, MultiroomStates(devices) as multiroom_states:
        multiroom_states.wait_any_peers()
        devices[0].start_conversation()
        devices[0].say_to_mic(test_data_path("play_everywhere.wav"))

        app_states[0].wait_standalone_music_playing()
        multiroom_states[0].wait_for(
            lambda m: (
                m.mode == P.MultiroomState.Mode.MASTER
                and m.multiroom_broadcast.state == P.MultiroomBroadcast.State.PLAYING
            )
        )

        app_states[1].wait_multiroom_slave_playing()
        multiroom_states[1].wait_for(
            lambda m: (
                m.mode == P.MultiroomState.Mode.SLAVE
                and m.multiroom_broadcast.state == P.MultiroomBroadcast.State.PLAYING
            )
        )

        devices[stop_device_index].start_conversation()
        devices[stop_device_index].say_to_mic(test_data_path("stop.wav"))
        app_states[0].wait_media_stopped()
        app_states[1].wait_media_stopped()
