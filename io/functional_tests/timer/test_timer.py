from yandex_io.pylibs.functional_tests.utils import (
    test_data_path,
    assert_alice_volume,
    presmoke,
    station_presmoke,
    regression,
)
from yandex_io.pylibs.functional_tests.matchers import has_output_speech
import logging
import yandex_io.protos.model_objects_pb2 as P
import datetime
import pytest

from testlib import disable_timers, wait_timers, get_pause_timestamp, get_pause_duration

logger = logging.getLogger(__name__)


@pytest.mark.testpalm("https://testpalm.yandex-team.ru/alice/testsuite/5d09de6f3d42cb5e76b4cf06?testcase=1331")
@presmoke
@station_presmoke
def test_timer_10s(device):
    device.wait_for_listening_start()
    device.wait_for_authenticate_completion()

    with device.get_service_connector("alarmd") as alarmd, device.get_service_connector("testpoint") as testpoint:

        wait_timers(device, alarmd, 0)

        device.start_conversation()
        device.say_to_mic(test_data_path("volume_0.wav"))
        assert_alice_volume(device, testpoint, 0, False)

        device.start_conversation()
        device.say_to_mic(test_data_path("timer_10s.wav"))

        timer = wait_timers(device, alarmd, 1)[0]
        device.failer.assert_fail(
            timer.duration_seconds == 10,
            "Invalid timer time was set. Expected: {}, Actual: {}".format(10, timer.duration_seconds),
        )
        device.failer.assert_fail(
            timer.alarm_type == P.Alarm.AlarmType.TIMER, "Invalid alarm time. Expected to be TIMER"
        )

        device.stage_logger.test_stage("10 seconds timer was set")

        alarm_fired = device.wait_for_message(
            alarmd,
            lambda m: m.HasField("alarm_event") and m.alarm_event.HasField("alarm_fired"),
            "Timer did not fired",
            12,
        ).alarm_event.alarm_fired
        device.failer.assert_fail(timer == alarm_fired, "Wrong alarm fired")

        alarm_started = device.wait_for_message(
            alarmd,
            lambda m: m.HasField("alarm_event") and m.alarm_event.HasField("alarm_started"),
            "No alarm started",
        ).alarm_event.alarm_started
        device.failer.assert_fail(timer == alarm_started, "Wrong alarm started")

        device.stage_logger.test_stage("Timer fired")

        assert_alice_volume(device, testpoint, 5, False)

        device.start_conversation()
        device.say_to_mic(test_data_path("volume_3.wav"))
        assert_alice_volume(device, testpoint, 3, False)


@pytest.mark.testpalm("https://testpalm.yandex-team.ru/alice/testsuite/5d09de6f3d42cb5e76b4cf06?testcase=1331")
@presmoke
@station_presmoke
def test_cancel_all_timers(device):
    device.wait_for_listening_start()
    device.wait_for_authenticate_completion()

    with device.get_service_connector("alarmd") as alarmd:

        disable_timers(device, alarmd)

        device.start_conversation()
        device.say_to_mic(test_data_path("timer_10m.wav"))
        wait_timers(device, alarmd, 1)

        device.start_conversation()
        device.say_to_mic(test_data_path("timer_10m.wav"))
        wait_timers(device, alarmd, 2)

        disable_timers(device, alarmd)


@pytest.mark.testpalm("https://testpalm.yandex-team.ru/alice/testsuite/5d09de6f3d42cb5e76b4cf06?testcase=2419")
@regression
def test_timer_pause(device):
    device.wait_for_listening_start()
    device.wait_for_authenticate_completion()

    with device.get_service_connector("alarmd") as alarmd:

        disable_timers(device, alarmd)

        device.start_conversation()
        device.say_to_mic(test_data_path("timer_10m.wav"))

        wait_timers(device, alarmd, 1)

        device.start_conversation()
        pause_ts = datetime.datetime.now().timestamp()
        device.say_to_mic(test_data_path("pause_timer.wav"))

        pause_timestamp = get_pause_timestamp(device, alarmd)
        device.failer.assert_fail(
            pause_ts < pause_timestamp < pause_ts + 10,
            "Invalid pause timestamp. Expected to be in range [{}-{}], Actual: {}".format(
                pause_ts, pause_ts + 10, pause_timestamp
            ),
        )

        device.stage_logger.test_stage("Timer paused")

        device.start_conversation()
        device.say_to_mic(test_data_path("resume_timer.wav"))

        pause_duration = get_pause_duration(device, alarmd)
        device.failer.assert_fail(pause_duration > 0, "Pause diration must be non zero")

        device.stage_logger.test_stage("Timer resumed")

        disable_timers(device, alarmd)


@pytest.mark.testpalm("https://testpalm.yandex-team.ru/alice/testsuite/5d09de6f3d42cb5e76b4cf06?testcase=1755")
@regression
def test_my_timers(device):
    device.wait_for_listening_start()
    device.wait_for_authenticate_completion()

    with device.get_service_connector("alarmd") as alarmd, device.get_service_connector("aliced") as aliced:

        device.start_conversation()
        device.say_to_mic(test_data_path("timer_10m.wav"))

        wait_timers(device, alarmd, 1)

        device.start_conversation()
        device.say_to_mic(test_data_path("sleep_timer_enable_10min.wav"))

        wait_timers(device, alarmd, 2)

        device.start_conversation()
        device.say_to_mic(test_data_path("my_timers.wav"))

        device.wait_for_message(
            aliced,
            lambda m: has_output_speech(m) and "таймер сна" in m.alice_state.vins_response.output_speech,
            "Alice should list all timers",
        )

        disable_timers(device, alarmd)
