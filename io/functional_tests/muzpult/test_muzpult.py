from testlib import make_glagol_connector, PlayerState
from yandex_io.pylibs.functional_tests.utils import regression
import pytest


@pytest.mark.testpalm("https://testpalm.yandex-team.ru/alice/testsuite/60ca0c20e0f8730022f81a1e?testcase=3302")
@pytest.mark.testpalm("https://testpalm.yandex-team.ru/alice/testsuite/60ca0c20e0f8730022f81a1e?testcase=3320")
@pytest.mark.testpalm("https://testpalm.yandex-team.ru/alice/testsuite/60c858a0549d130022b73c72?testcase=3255")
@pytest.mark.testpalm("https://testpalm.yandex-team.ru/alice/testsuite/60c858a0549d130022b73c72?testcase=3320")
@regression
@pytest.mark.with_yandex_plus
def test_muzpult_simple_interaction(device, backend_client, authtoken, use_tus, is_remote_test):
    if use_tus and is_remote_test:
        pytest.skip("Currently broken.")

    def check_track_played(state):
        pState = state.get('playerState')
        if pState is None:
            return False
        id = pState.get('entityInfo').get('id')
        playing = state.get('playing')
        progress = pState.get('progress')
        device.stage_logger.log("id = {}, playing = {}, progress = {} ".format(id, playing, progress))
        return id == trackId and playing and progress > 0

    def check_progress(state, val):
        pState = state.get('playerState')
        return pState.get('progress') >= val

    with make_glagol_connector(device, backend_client, authtoken) as connector:
        device.stage_logger.log("Testing app version {}".format(connector.software_version()))

        device.stage_logger.test_stage("Initial stop and wait for stopped state")
        connector.stop()
        connector.wait_for_state(PlayerState.is_stopped)
        trackId = '37266989'
        answer = connector.play_music(trackId, 'track')

        device.failer.assert_fail(answer.get('status') == 'SUCCESS', 'Invalid status {}'.format(answer.get('status')))

        device.stage_logger.test_stage("Wait for music start playing")
        connector.wait_for_state(check_track_played)

        device.stage_logger.test_stage("Wait for progress reaching five seconds")
        connector.wait_for_state(lambda state: check_progress(state, 5.0))
        connector.rewind(30)
        connector.wait_for_state(lambda state: check_progress(state, 30.0))
        device.stage_logger.test_stage("Rewinded forward to 30 sec")

        connector.stop()
        connector.wait_for_state(PlayerState.is_stopped)
        device.stage_logger.test_stage("Stopped")


@pytest.mark.testpalm("https://testpalm.yandex-team.ru/alice/testsuite/60ca0c20e0f8730022f81a1e?testcase=3308")
@pytest.mark.testpalm("https://testpalm.yandex-team.ru/alice/testsuite/60c858a0549d130022b73c72?testcase=3308")
@regression
@pytest.mark.with_yandex_plus
def test_muzpult_set_volume(device, backend_client, authtoken, use_tus, is_remote_test):
    if use_tus and is_remote_test:
        pytest.skip("Currently broken.")

    def check_volume(state, v):
        volume = state.get('volume')
        device.stage_logger.log('state = {}'.format(volume))
        return volume == v

    with make_glagol_connector(device, backend_client, authtoken) as connector:
        connector.set_volume(0.2)
        connector.wait_for_state(lambda state: check_volume(state, 0.2))
        device.stage_logger.test_stage("Volume changed to 0.2")
        connector.set_volume(0.1)
        connector.wait_for_state(lambda state: check_volume(state, 0.1))
        device.stage_logger.test_stage("Volumne changed to 0.1")


@pytest.mark.testpalm("https://testpalm.yandex-team.ru/alice/testsuite/60ca0c20e0f8730022f81a1e?testcase=3306")
@pytest.mark.testpalm("https://testpalm.yandex-team.ru/alice/testsuite/60c858a0549d130022b73c72?testcase=3306")
@regression
@pytest.mark.with_yandex_plus
def test_muzpult_prev_next(device, backend_client, authtoken, use_tus, is_remote_test):
    if use_tus and is_remote_test:
        pytest.skip("Currently broken.")

    trackId = None

    def get_playing_track_id(state):
        pState = state.get('playerState', {})
        nonlocal trackId
        trackId = pState.get('id')
        playing = state.get('playing')
        progress = pState.get('progress')
        shuffled = pState.get('entityInfo', {}).get('shuffled')
        device.failer.assert_fail(not shuffled, 'started without suffling but shuffled')
        return playing and progress > 0

    with make_glagol_connector(device, backend_client, authtoken) as connector:
        connector.stop()
        connector.wait_for_state(PlayerState.is_stopped)
        connector.play_music('ann.a.yakovleva:1118', 'playlist')
        connector.wait_for_state(get_playing_track_id)
        device.stage_logger.log("playing track {}, switching to next".format(trackId))
        connector.next()

        connector.wait_for_state(lambda state: state.get('playerState', {}).get('id') != trackId)
        device.stage_logger.log("playing new track, switching back")
        connector.prev()
        connector.wait_for_state(lambda state: state.get('playerState', {}).get('id') == trackId)
        device.stage_logger.log("switching back completed")
        connector.stop()


@pytest.mark.testpalm("https://testpalm.yandex-team.ru/alice/testsuite/60c858a0549d130022b73c72?testcase=3565")
@pytest.mark.testpalm("https://testpalm.yandex-team.ru/alice/testsuite/60c858a0549d130022b73c72?testcase=3563")
@regression
@pytest.mark.with_yandex_plus
def test_muzpult_shuffle_repeat(device, backend_client, authtoken, use_tus, is_remote_test):
    if use_tus and is_remote_test:
        pytest.skip("Currently broken.")

    def is_shuffled_repeat(state):
        eInfo = state.get('playerState', {}).get('entityInfo', {})
        device.stage_logger.log("{}".format(eInfo))
        return eInfo.get('shuffled', False) and eInfo.get('repeatMode', '') == 'All'

    with make_glagol_connector(device, backend_client, authtoken) as connector:
        connector.play_music('ann.a.yakovleva:1118', 'playlist', shuffle=True, repeat='All')
        connector.wait_for_state(is_shuffled_repeat)
        connector.stop()
