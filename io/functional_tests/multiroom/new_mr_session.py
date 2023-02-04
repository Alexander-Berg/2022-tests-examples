import yandex_io.protos.model_objects_pb2 as P
import logging
import pytest
from yandex_io.pylibs.functional_tests.multiroom import MultiroomStates
from yandex_io.pylibs.functional_tests.multiroom import AppStates
from yandex_io.pylibs.functional_tests.utils import test_data_path

logger = logging.getLogger(__name__)


#
# If the speaker is not playing at the moment, then it does not matter whether it is a master
# or not, then on "Alice, Play <Music | Metalica | Hit Parade> everywhere" a new MR session
# should begin.
# URL: https://st.yandex-team.ru/SK-5720, point 1
#
@pytest.mark.skip(reason="Mulriroom tests are broken. TICKET: SK-6160")
@pytest.mark.experiments("hw_music_multiroom_redirect")
@pytest.mark.experiments("hw_music_thin_client_multiroom")
@pytest.mark.experiments("commands_multiroom_redirect")
@pytest.mark.experiments("hw_music_thin_client_disable_mp3_get_alice")
@pytest.mark.with_yandex_plus
@pytest.mark.multidevice(2)
def test_new_multiroom_session(devices):
    devices.wait_devices_ready()

    with AppStates(devices) as app_states, MultiroomStates(devices) as multiroom_states:
        #
        # Init. Start multiroom on any device and stop after success
        #
        multiroom_states.wait_any_peers()
        logger.info("*** Init. Start multiroom on any device and stop after success ***")
        devices[0].start_conversation()
        devices[0].say_to_mic(test_data_path("play_everywhere.wav"))

        app_states[0].wait_standalone_music_playing()
        ms_init_0 = multiroom_states[0].wait_for(
            lambda m: (
                m.mode == P.MultiroomState.Mode.MASTER
                and m.multiroom_broadcast.state == P.MultiroomBroadcast.State.PLAYING
            )
        )

        app_states[1].wait_multiroom_slave_playing()
        ms_init_1 = multiroom_states[1].wait_for(
            lambda m: (
                m.mode == P.MultiroomState.Mode.SLAVE
                and m.multiroom_broadcast.state == P.MultiroomBroadcast.State.PLAYING
            )
        )

        devices.failer.assert_fail(ms_init_0.multiroom_broadcast.session_timestamp_ms > 0, "Invalid initial MR session")

        devices.failer.assert_fail(
            ms_init_0.multiroom_broadcast.session_timestamp_ms == ms_init_1.multiroom_broadcast.session_timestamp_ms,
            "MR \"init\" sessions missmatch",
        )

        # Stop multiroom session
        logger.info("*** Init. Stop all devices ***")
        devices[0].start_conversation()
        devices[0].say_to_mic(test_data_path("stop.wav"))
        app_states[0].wait_media_stopped()
        app_states[1].wait_media_stopped()
        multiroom_states[0].wait_for(lambda m: m.multiroom_broadcast.state != P.MultiroomBroadcast.State.PLAYING)
        multiroom_states[1].wait_for(lambda m: m.multiroom_broadcast.state != P.MultiroomBroadcast.State.PLAYING)

        #
        # V1. Start multiroom on first master again and make sure to start a new multiroom session
        #
        logger.info("*** V1. Start multiroom on first master again and make sure to start a new multiroom session ***")
        devices[0].start_conversation()
        devices[0].say_to_mic(test_data_path("play_everywhere.wav"))

        app_states[0].wait_standalone_music_playing()
        ms_v1_0 = multiroom_states[0].wait_for(
            lambda m: (
                m.mode == P.MultiroomState.Mode.MASTER
                and m.multiroom_broadcast.state == P.MultiroomBroadcast.State.PLAYING
            )
        )

        app_states[1].wait_multiroom_slave_playing()
        ms_v1_1 = multiroom_states[1].wait_for(
            lambda m: (
                m.mode == P.MultiroomState.Mode.SLAVE
                and m.multiroom_broadcast.state == P.MultiroomBroadcast.State.PLAYING
            )
        )

        devices.failer.assert_fail(ms_v1_0.multiroom_broadcast.session_timestamp_ms > 0, "Invalid v1 MR session")
        devices.failer.assert_fail(
            ms_v1_0.multiroom_broadcast.session_timestamp_ms == ms_v1_1.multiroom_broadcast.session_timestamp_ms,
            "MR \"v1\" sessions missmatch",
        )
        devices.failer.assert_fail(
            ms_init_0.multiroom_broadcast.session_timestamp_ms != ms_v1_0.multiroom_broadcast.session_timestamp_ms,
            "Same MR session (init == v1)",
        )

        # Stop multiroom session
        logger.info("*** V1. Stop all devices ***")
        devices[0].start_conversation()
        devices[0].say_to_mic(test_data_path("stop.wav"))
        app_states[0].wait_media_stopped()
        app_states[1].wait_media_stopped()
        multiroom_states[0].wait_for(lambda m: m.multiroom_broadcast.state != P.MultiroomBroadcast.State.PLAYING)
        multiroom_states[1].wait_for(lambda m: m.multiroom_broadcast.state != P.MultiroomBroadcast.State.PLAYING)

        #
        # V2. Start multiroom on old slave device
        #
        logger.info("*** V2. Start multiroom on first master again and make sure to start a new multiroom session ***")
        devices[1].start_conversation()
        devices[1].say_to_mic(test_data_path("play_everywhere.wav"))

        app_states[1].wait_standalone_music_playing()
        ms_v2_1 = multiroom_states[1].wait_for(
            lambda m: (
                m.mode == P.MultiroomState.Mode.MASTER
                and m.multiroom_broadcast.state == P.MultiroomBroadcast.State.PLAYING
            )
        )

        app_states[0].wait_multiroom_slave_playing()
        ms_v2_0 = multiroom_states[0].wait_for(
            lambda m: (
                m.mode == P.MultiroomState.Mode.SLAVE
                and m.multiroom_broadcast.state == P.MultiroomBroadcast.State.PLAYING
            )
        )

        devices.failer.assert_fail(ms_v2_1.multiroom_broadcast.session_timestamp_ms > 0, "Invalid v2 MR session")
        devices.failer.assert_fail(
            ms_v2_0.multiroom_broadcast.session_timestamp_ms == ms_v2_1.multiroom_broadcast.session_timestamp_ms,
            "MR \"v2\" sessions missmatch",
        )
        devices.failer.assert_fail(
            ms_init_1.multiroom_broadcast.session_timestamp_ms != ms_v2_1.multiroom_broadcast.session_timestamp_ms,
            "Same MR session (init == v2)",
        )
        devices.failer.assert_fail(
            ms_v1_1.multiroom_broadcast.session_timestamp_ms != ms_v2_1.multiroom_broadcast.session_timestamp_ms,
            "Same MR session (v1 == v2)",
        )
        logger.info("*** Fin ***")
