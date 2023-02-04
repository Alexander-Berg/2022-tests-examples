from yandex_io.pylibs.functional_tests.utils import (
    test_data_path,
    assert_alice_volume,
    regression,
    station_presmoke,
    presmoke,
)
from yandex_io.pylibs.functional_tests.matchers import has_output_speech
from testlib import (
    cancel_all_alarms,
    set_alarm,
    check_alarm_fired,
    check_alarm_set,
    check_alarm_started,
    check_alarm_stopped,
    convert_ical_to_list,
)
import re
import datetime as dt
import pytz
import logging
import pytest


logger = logging.getLogger(__name__)

INITIAL_VOLUME = 0
EXPECTED_VOLUME = 5


@pytest.mark.testpalm("https://testpalm.yandex-team.ru/alice/testsuite/5d09de6f3d42cb5e76b4cf06?testcase=1341")
@presmoke
@station_presmoke
def test_alarm(device):
    device.wait_for_listening_start()
    device.wait_for_authenticate_completion()

    with device.get_service_connector("alarmd") as alarmd, device.get_service_connector("testpoint") as testpoint:
        alarm_state = device.wait_for_message(
            alarmd,
            lambda m: m.HasField("alarms_state") and m.alarms_state.HasField("alarms_settings"),
            "Failed to recieve alarm settings",
        ).alarms_state
        start_alarm_volume = alarm_state.alarms_settings.min_volume_level
        finish_alarm_volume = alarm_state.alarms_settings.max_volume_level

        if alarm_state.HasField("icalendar_state") and convert_ical_to_list(alarm_state.icalendar_state):
            cancel_all_alarms(device, alarmd)

        # Set volume to zero
        device.start_conversation()
        device.say_to_mic(test_data_path("volume_0.wav"))

        assert_alice_volume(device, testpoint, INITIAL_VOLUME, False)

        device.stage_logger.test_stage("Alice volume was set to 0")

        alarm, _ = set_alarm(device, alarmd, "alarm_in_20sec.wav", 20)

        check_alarm_fired(alarmd, alarm, device)

        # Check that volume is gradually increasing for alarm
        for volume in range(start_alarm_volume, finish_alarm_volume + 1):
            assert_alice_volume(device, testpoint, volume, False)
            device.stage_logger.test_stage("Volume went up to {}".format(volume))

        # Check that volume was reset when alarm is off
        device.start_conversation()
        assert_alice_volume(device, testpoint, INITIAL_VOLUME, False)

        device.stage_logger.test_stage("Volume reset to {} after alarm stopped".format(INITIAL_VOLUME))


@pytest.mark.testpalm("https://testpalm.yandex-team.ru/alice/testsuite/5d09de6f3d42cb5e76b4cf06?testcase=2418")
@pytest.mark.testpalm("https://testpalm.yandex-team.ru/alice/testsuite/5d09de6f3d42cb5e76b4cf06?testcase=2722")
@regression
def test_change_volume(device):
    device.wait_for_listening_start()
    device.wait_for_authenticate_completion()

    with device.get_service_connector("alarmd") as alarmd, device.get_service_connector("aliced") as aliced:

        # Set volume to 5
        device.start_conversation()
        device.say_to_mic(test_data_path("set_alarm_volume.wav"))

        device.wait_for_message(
            alarmd,
            lambda m: m.HasField("alarms_state")
            and m.alarms_state.HasField("alarms_settings")
            and m.alarms_state.alarms_settings.max_volume_level == EXPECTED_VOLUME,
            "Alarm volume was not set to expected value: {}".format(EXPECTED_VOLUME),
        )

        device.stage_logger.test_stage("Alarm volume changed to {}".format(EXPECTED_VOLUME))

        response = device.wait_for_message(
            aliced, has_output_speech, "Alice did not respond"
        ).alice_state.vins_response.output_speech

        device.failer.assert_fail(
            re.search(r"[гГ]ромкость будильника", response) and re.search(rf"{EXPECTED_VOLUME}", response),
            "Alice responce to settings alarm volume command is incorrect",
        )

        device.stage_logger.test_stage("Alice confirmed alarm volume change")

        aliced.clear_message_queue()

        # ask volume
        device.start_conversation()
        device.say_to_mic(test_data_path("ask_alarm_volume.wav"))

        device.stage_logger.test_stage("Ask Alice about alarm volume")

        response = device.wait_for_message(
            aliced, has_output_speech, "Alice did not responde"
        ).alice_state.vins_response.output_speech

        device.failer.assert_fail(
            re.search(r"[гГ]ромкость будильника", response) and re.search(rf"{EXPECTED_VOLUME}", response),
            "Alice responce to asking alarm volume is incorrect",
        )

        device.stage_logger.test_stage("Alice correctly respond")


def check_alarm(device, aliced, response_regexp_list):
    aliced.clear_message_queue()

    device.start_conversation()
    device.say_to_mic(test_data_path("check_alarm.wav"))

    device.stage_logger.test_stage("Ask Alice about set alarms")

    device.wait_for_message(
        aliced,
        lambda m: has_output_speech(m)
        and any(map(lambda regexp: re.search(regexp, m.alice_state.vins_response.output_speech), response_regexp_list)),
        "Alice didn't respond or response to question \"My alarms?\" was incorrect. Used regexps: {}".format(
            ",".join(response_regexp_list)
        ),
    )


@pytest.mark.testpalm("https://testpalm.yandex-team.ru/alice/testsuite/5d09de6f3d42cb5e76b4cf06?testcase=2253")
@regression
def test_time_to_alarm_response(device):
    device.wait_for_listening_start()
    device.wait_for_authenticate_completion()

    with device.get_service_connector("alarmd") as alarmd, device.get_service_connector("aliced") as aliced:

        cancel_all_alarms(device, alarmd)

        check_alarm(device, aliced, [r"[Вв]ключенных будильников не обнаружено"])

        device.stage_logger.test_stage("Alice correctly respond that none alarms are set")

        set_alarm(device, alarmd, "alarm_in_1hour.wav", 3600)

        check_alarm(device, aliced, [r"59 минут", r"1 час"])

        device.stage_logger.test_stage("Alice correctly respond that 1 hour left before alarm activation")


@pytest.mark.testpalm("https://testpalm.yandex-team.ru/alice/testsuite/5d09de6f3d42cb5e76b4cf06?testcase=2197")
@regression
def test_alarm_snooze(device):
    device.wait_for_listening_start()
    device.wait_for_authenticate_completion()

    with device.get_service_connector("alarmd") as alarmd:

        cancel_all_alarms(device, alarmd)

        alarm, _ = set_alarm(device, alarmd, "alarm_in_20sec.wav", 20)

        check_alarm_fired(alarmd, alarm, device)

        check_alarm_started(alarmd, alarm, device)

        # "Еще 5 минут"

        device.start_conversation()
        device.say_to_mic(test_data_path("snooze_alarm.wav"))

        alarm_set_time = dt.datetime.utcnow().replace(tzinfo=pytz.utc)

        check_alarm_stopped(alarmd, device)

        check_alarm_set(alarmd, alarm_set_time, 5 * 60, device)

        device.stage_logger.test_stage("Alarm was snoozed to 5 minutes")


@pytest.mark.testpalm("https://testpalm.yandex-team.ru/alice/testsuite/5d09de6f3d42cb5e76b4cf06?testcase=2248")
@regression
def test_alarm_cancel_after_timeout(device):
    device.wait_for_listening_start()
    device.wait_for_authenticate_completion()

    EPS = 10

    alarm_timeout = device.config["alarmd"].get("alarmTimerTimeoutSec", 600)
    logger.info("Alarm timeout: {}".format(alarm_timeout))

    with device.get_service_connector("alarmd") as alarmd:

        cancel_all_alarms(device, alarmd)

        alarm, _ = set_alarm(device, alarmd, "alarm_in_20sec.wav", 20)

        check_alarm_fired(alarmd, alarm, device)

        check_alarm_started(alarmd, alarm, device)

        check_alarm_stopped(alarmd, device, timeout=alarm_timeout + EPS)

        device.stage_logger.test_stage(
            "Alarm stopped without explicitly cancelling after timeout {}".format(alarm_timeout)
        )
