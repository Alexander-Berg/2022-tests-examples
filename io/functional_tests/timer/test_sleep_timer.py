import pytest
import yandex_io.protos.model_objects_pb2 as P
from yandex_io.pylibs.functional_tests.utils import test_data_path, presmoke, regression
from yandex_io.pylibs.functional_tests.matchers import is_music_stop, is_music_playing

from testlib import disable_timers, disable_timers_if_any, wait_timers


@pytest.mark.testpalm("https://testpalm.yandex-team.ru/alice/testsuite/5d09de6f3d42cb5e76b4cf06?testcase=1747")
@presmoke
@pytest.mark.with_yandex_plus
def test_sleep_timer_triggered(device):
    device.wait_for_authenticate_completion()
    device.wait_for_listening_start()

    with device.get_service_connector("alarmd") as alarmd, device.get_service_connector("aliced") as aliced:
        disable_timers_if_any(device)

        # Start music
        device.start_conversation()
        device.say_to_mic(test_data_path("music_play.wav"))
        device.wait_for_listening_start()

        # Wait music
        device.wait_for_message(aliced, is_music_playing, "Music did not start to play")

        # Set sleep timer for 10 seconds
        device.start_conversation()
        device.say_to_mic(test_data_path("sleep_timer_enable_10sec.wav"))
        device.wait_for_listening_start()

        timer_msg = wait_timers(device, alarmd, 1)[0]
        device.failer.assert_fail(
            timer_msg.alarm_type == P.Alarm.AlarmType.COMMAND_TIMER, "Whrong timer type. Expected to be COMMAND_TIMER"
        )
        device.failer.assert_fail(
            timer_msg.duration_seconds == 10,
            "Invalid timer duration. Expected: {}, Actual: {}".format(10, timer_msg.duration_seconds == 10),
        )

        device.stage_logger.test_stage("Timer was set")

        # Wait until sleep timer is triggered
        wait_timers(device, alarmd, 0)

        device.stage_logger.test_stage("Timer fired")

        # Wait music pause
        device.wait_for_message(aliced, is_music_stop, "Music did not stop after sleep timer fired")

        device.stage_logger.test_stage("Music stopped after timer fired")

        # TODO (SK-4624): Add assertions for to_home_screen and hdmi_off messages


@pytest.mark.testpalm("https://testpalm.yandex-team.ru/alice/testsuite/5d09de6f3d42cb5e76b4cf06?testcase=1758")
@regression
def test_multiple_sleep_timers(device):
    device.wait_for_authenticate_completion()
    device.wait_for_listening_start()

    with device.get_service_connector("alarmd") as alarmd:
        disable_timers_if_any(device)

        # Set sleep timer for 10 minutes
        device.start_conversation()
        device.say_to_mic(test_data_path("sleep_timer_enable_10min.wav"))

        # Wait until timer is set
        timer_msg = wait_timers(device, alarmd, 1)[0]
        device.failer.assert_fail(
            timer_msg.alarm_type == P.Alarm.AlarmType.COMMAND_TIMER, "Whrong timer type. Expected to be COMMAND_TIMER"
        )
        device.failer.assert_fail(
            timer_msg.duration_seconds == 10 * 60,
            "Invalid timer duration. Expected: {}, Actual: {}".format(10 * 60, timer_msg.duration_seconds == 10),
        )

        device.stage_logger.test_stage("First timer was set")

        # Overwrite previous timer with another one
        device.start_conversation()
        device.say_to_mic(test_data_path("sleep_timer_enable_2min.wav"))

        # Wait until second timer is set
        def predicate(m):
            return (
                m.timers_state.timers[0].alarm_type == P.Alarm.AlarmType.COMMAND_TIMER
                and m.timers_state.timers[0].duration_seconds == 2 * 60
            )

        wait_timers(device, alarmd, 1, predicate)

        device.stage_logger.test_stage("Second timer was set. First cancelled")

        disable_timers(device, alarmd)


@pytest.mark.testpalm("https://testpalm.yandex-team.ru/alice/testsuite/5d09de6f3d42cb5e76b4cf06?testcase=1759")
@presmoke
def test_sleep_timer_cancel(device):
    device.wait_for_authenticate_completion()
    device.wait_for_listening_start()

    with device.get_service_connector("alarmd") as alarmd:

        disable_timers_if_any(device)

        # Set sleep timer for 10 minutes
        device.start_conversation()
        device.say_to_mic(test_data_path("sleep_timer_enable_10min.wav"))

        # Wait until timer is set
        timer_msg = wait_timers(device, alarmd, 1)[0]
        device.failer.assert_fail(
            timer_msg.alarm_type == P.Alarm.AlarmType.COMMAND_TIMER, "Whrong timer type. Expected to be COMMAND_TIMER"
        )
        device.failer.assert_fail(
            timer_msg.duration_seconds == 10 * 60,
            "Invalid timer duration. Expected: {}, Actual: {}".format(10 * 60, timer_msg.duration_seconds == 10),
        )

        device.stage_logger.test_stage("Sleep timer was set")

        device.start_conversation()
        device.say_to_mic(test_data_path("timer_10m.wav"))

        wait_timers(device, alarmd, 2)

        device.stage_logger.test_stage("Timer was set")

        device.start_conversation()
        device.say_to_mic(test_data_path("sleep_timer_disable.wav"))

        # check that only normal timer in queue
        timer_msg = wait_timers(device, alarmd, 1)[0]
        device.failer.assert_fail(
            timer_msg.alarm_type == P.Alarm.AlarmType.TIMER,
            "Whrong timer was deleted. Expected TIMER to be still active",
        )

        device.stage_logger.test_stage("Sleep timer was successfully deleted")

        disable_timers(device, alarmd)
