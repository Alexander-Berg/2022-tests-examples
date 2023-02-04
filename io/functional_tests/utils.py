import collections
import os.path as op
import yatest.common as yc
import yandex_io.protos.model_objects_pb2 as P

from _pytest.mark.structures import get_unpacked_marks
import pytest


# On yandexmini default idling state is AliceState.IDLE.
# but if current screen does not support long listening or long listening is disabled
# in quasar.cfg by flag, idling state is AliceState.IDLE.
ANY_IDLING_ALICE_STATE = (P.AliceState.IDLE,)


def alice_volume_matcher(volume, muted):
    def matcher(msg):
        return (
            msg.HasField("testpoint_message")
            and msg.testpoint_message.HasField("alice_volume_state")
            and (volume is None or msg.testpoint_message.alice_volume_state.cur_volume == volume)
            and (muted is None or msg.testpoint_message.alice_volume_state.muted == muted)
        )

    return matcher


def platform_volume_matcher(volume, muted):
    def matcher(msg):
        return (
            msg.HasField("testpoint_message")
            and msg.testpoint_message.HasField("platform_volume_state")
            and (volume is None or msg.testpoint_message.platform_volume_state.cur_volume == volume)
            and (muted is None or msg.testpoint_message.platform_volume_state.muted == muted)
        )

    return matcher


def assert_alice_volume(device, testpoint, volume, muted):
    device.wait_for_message(
        testpoint,
        alice_volume_matcher(volume, muted),
        "Failed to set target alice volume: Volume={}, Muted={}".format(volume, muted),
    )
    device.stage_logger.test_stage("Alice volume {} and muted {} are successfully set".format(volume, muted))


def assert_platform_volume(device, testpoint, volume, muted):
    device.wait_for_message(
        testpoint,
        platform_volume_matcher(volume, muted),
        "Failed to set target platform volume: Volume={}, Muted={}".format(volume, muted),
    )
    device.stage_logger.test_stage("Platfotm volume {} and muted {} are successfully set".format(volume, muted))


@yc.not_test
def test_data_path(file=""):
    return yc.test_source_path(op.join("data", file))


def common_data_path(file=""):
    return yc.source_path(op.join("yandex_io/functional_tests/data_common", file))


def collect_pytest_marks(request):
    return [mark for context in [request.module, request.cls, request.function] for mark in get_unpacked_marks(context)]


regression = pytest.mark.regression
presmoke = pytest.mark.presmoke
station_presmoke = pytest.mark.station_presmoke


def tus_mark():
    return pytest.mark.skipif(yc.get_param("session_id") is not None, reason="Test can work only with TUS")


def sample_app_mark():
    return pytest.mark.skipif(
        yc.get_param("remote_test") is not None and yc.get_param("remote_test") == "true",
        reason="Test does not support remote testing.",
    )


class FakeExecution:
    def __init__(self):
        Process = collections.namedtuple("Process", ("pid",))
        self.running = True
        self.command = "Remote instance"
        self.process = Process("999999")

    def kill(self):
        return True
