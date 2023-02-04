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
    "device_indexes",
    [
        (0, 0),
        (0, 1),
        (1, 0),
        (1, 1),
    ],
)
def test_play_stop_continue(devices, device_indexes):
    devices.wait_devices_ready()

    stop_device_index, continue_device_index = device_indexes

    stop_device_name = "MASTER" if stop_device_index == 0 else "SLAVE"
    continue_device_name = "MASTER" if continue_device_index == 0 else "SLAVE"
    logger.info(
        f"****\nAnnotation:\n1. Play music at MASTER device\n2. Stop music at {stop_device_name} device\nContinue music at {continue_device_name} device"
    )

    with AppStates(devices) as app_states, MultiroomStates(devices) as multiroom_states:
        multiroom_states.wait_any_peers()
        logger.info("Before \"включи музыку везде\" command....")
        devices[0].start_conversation()
        devices[0].say_to_mic(test_data_path("play_everywhere.wav"))
        logger.info("Command \"включи музыку везде\" sent, awaint mediad playing...")
        app0 = app_states[0].wait_standalone_music_playing()
        logger.info("Media from device 0 playing, awating multiroom state from this device...")
        ms0 = multiroom_states[0].wait_for(
            lambda m: (
                m.mode == P.MultiroomState.Mode.MASTER
                and m.multiroom_broadcast.state == P.MultiroomBroadcast.State.PLAYING
            )
        )
        logger.info("Multiroom state from device 0 received, awating media playing on device 1...")

        app_states[1].wait_multiroom_slave_playing()
        logger.info("Media from device 1 playing, awating multiroom state from this device...")
        ms1 = multiroom_states[1].wait_for(
            lambda m: (
                m.mode == P.MultiroomState.Mode.SLAVE
                and m.multiroom_broadcast.state == P.MultiroomBroadcast.State.PLAYING
            )
        )

        logger.info(f"Multiroom state 0:\n{ms0}")
        logger.info(f"Multiroom state 1:\n{ms1}")
        devices.failer.assert_fail(
            MultiroomState.is_same_playback(ms0, ms1), "Missmatched multiroom params for master and slave"
        )

        logger.info("Before \"стоп\" command....")
        devices[stop_device_index].start_conversation()
        devices[stop_device_index].say_to_mic(test_data_path("stop.wav"))
        logger.info("Command \"стоп\" sent, awaint mediad stopping...")
        app_states[0].wait_media_stopped()
        logger.info("Media from device 0 stopped, awating device 1...")
        app_states[1].wait_media_stopped()
        logger.info("Media from device 1 stopped.")

        multiroom_states[0].wait_for(lambda m: (m.multiroom_broadcast.state != P.MultiroomBroadcast.State.PLAYING))

        logger.info("Before \"продолжи\" command....")
        devices[continue_device_index].start_conversation()
        devices[continue_device_index].say_to_mic(test_data_path("continue.wav"))
        logger.info("Command \"продолжи\" sent, awaint mediad playing...")

        app0_cont = app_states[0].wait_standalone_music_playing()
        logger.info("Media from device 0 playing, awating multiroom state from this device...")
        ms0_cont = multiroom_states[0].wait_for(
            lambda m: (
                m.mode == P.MultiroomState.Mode.MASTER
                and m.multiroom_broadcast.state == P.MultiroomBroadcast.State.PLAYING
            )
        )
        logger.info("Multiroom state from device 0 received, awating media playing on device 1...")

        app1_cont = app_states[1].wait_multiroom_slave_playing()
        logger.info("Media from device 1 playing, awating multiroom state from this device...")
        ms1_cont = multiroom_states[1].wait_for(
            lambda m: (
                m.mode == P.MultiroomState.Mode.SLAVE
                and m.multiroom_broadcast.state == P.MultiroomBroadcast.State.PLAYING
            )
        )
        logger.info("Multiroom state from device 1 received, check all asserts...")
        logger.info(f"AppState (master) initial  0:\n{app0}")
        logger.info(f"AppState (master) continue 0:\n{app0_cont}")
        logger.info(f"AppState (slave) continue  1:\n{app1_cont}")
        logger.info(f"Multiroom state (master) continue 0:\n{ms0_cont}")
        logger.info(f"Multiroom state (slave)  continue 1:\n{ms1_cont}")

        is_multiroom_session_attributes = (
            ms0.multiroom_broadcast.device_id == ms0_cont.multiroom_broadcast.device_id
            and ms0.multiroom_broadcast.room_id == ms0_cont.multiroom_broadcast.room_id
            and ms0.multiroom_broadcast.room_device_ids == ms0_cont.multiroom_broadcast.room_device_ids
        )
        logger.info(f"CHECK: After continue master must have session attributes: {is_multiroom_session_attributes}")
        devices.failer.assert_fail(is_multiroom_session_attributes, "Multiroom master session attributes missmatched")

        is_same_playback = MultiroomState.is_same_playback(ms0_cont, ms1_cont)
        logger.info(f"CHECK: Master and salve have same playback: {is_same_playback}")
        devices.failer.assert_fail(is_same_playback, "Multiroom master and slave state missmatched")

        logger.info(f"Multiroom state  initial:\n{ms0}")
        logger.info(f"Multiroom state continue:\n{ms0_cont}")

        master_has_same_music_track = MultiroomState.music_track_id(ms0) == MultiroomState.music_track_id(ms0_cont)
        logger.info(f"CHECK: Master play same music after continue: {master_has_same_music_track}")
        devices.failer.assert_fail(master_has_same_music_track, "Master must play same music")

        master_has_not_same_playback = not MultiroomState.is_same_playback(ms0, ms0_cont)
        logger.info(f"CHECK: After continue multiroom playback changed: {master_has_not_same_playback}")
        devices.failer.assert_fail(master_has_not_same_playback, "Master must change multiroom playback parameters")

        master_increase_position = MultiroomState.position(ms0) < MultiroomState.position(ms0_cont)
        logger.info(f"CHECK: After continue position must greate then before: {master_increase_position}")
        devices.failer.assert_fail(master_increase_position, "Continue position must be greate then initial")

        logger.info("Test passed")
