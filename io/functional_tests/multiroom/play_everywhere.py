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
def test_play_everywhere(devices):
    devices.wait_devices_ready()

    with AppStates(devices) as app_states, MultiroomStates(devices) as multiroom_states:
        multiroom_states.wait_any_peers()
        devices[0].start_conversation()
        devices[0].say_to_mic(test_data_path("play_everywhere.wav"))
        app_states[0].wait_standalone_music_playing()
        ms0 = multiroom_states[0].wait_for(
            lambda m: (
                m.mode == P.MultiroomState.Mode.MASTER
                and m.multiroom_broadcast.state == P.MultiroomBroadcast.State.PLAYING
            )
        )

        app_states[1].wait_multiroom_slave_playing()
        ms1 = multiroom_states[1].wait_for(
            lambda m: (
                m.mode == P.MultiroomState.Mode.SLAVE
                and m.multiroom_broadcast.state == P.MultiroomBroadcast.State.PLAYING
            )
        )

        if (
            ms0.multiroom_broadcast.multiroom_params.url != ms1.multiroom_broadcast.multiroom_params.url
            or ms0.multiroom_broadcast.multiroom_params.basetime_ns
            != ms1.multiroom_broadcast.multiroom_params.basetime_ns
            or ms0.multiroom_broadcast.multiroom_params.position_ns
            != ms1.multiroom_broadcast.multiroom_params.position_ns
        ):
            logger.info(f"Multiroom state 0:\n{ms0}")
            logger.info(f"Multiroom state 1:\n{ms1}")
            devices.failer.fail("Missmatched multiroom params for master and slave")
