import icalendar as ical
from yandex_io.pylibs.functional_tests.utils import test_data_path, presmoke
from yandex_io.pylibs.functional_tests.matchers import has_output_speech
from testlib import cancel_all_alarms
import time
import re
import pytest

UTC_OFFSET = time.localtime().tm_gmtoff / 3600


@pytest.mark.testpalm("https://testpalm.yandex-team.ru/alice/testsuite/5d09de6f3d42cb5e76b4cf06?testcase=1341")
@presmoke
def test_regular_alarm(device):
    device.wait_for_listening_start()
    device.wait_for_authenticate_completion()
    with device.get_service_connector("alarmd") as alarmd, device.get_service_connector("aliced") as aliced:

        cancel_all_alarms(device, alarmd, False)

        alarmd.clear_message_queue()

        device.start_conversation()
        device.say_to_mic(test_data_path("regular_alarm.wav"))

        def event_mather(item):
            return (
                type(item) == ical.cal.Event
                and item["RRULE"]["FREQ"][0] == "WEEKLY"
                and item["RRULE"]["BYDAY"][0] == "TU"
                and item["DTSTART"].dt.hour == 7 - UTC_OFFSET
            )

        device.wait_for_message(
            alarmd,
            lambda m: m.HasField("alarms_state")
            and m.alarms_state.HasField("icalendar_state")
            and len(m.alarms_state.icalendar_state) > 0
            and any(map(event_mather, ical.Calendar.from_ical(m.alarms_state.icalendar_state).walk())),
            "Was waiting for regular alarm",
        )

        device.stage_logger.test_stage("Set regular alarm on 7h Tuesday")

        aliced.clear_message_queue()

        device.start_conversation()
        device.say_to_mic(test_data_path("all_alarms.wav"))
        alice_state = device.wait_for_message(aliced, has_output_speech, "Alice did not respond").alice_state

        device.failer.assert_fail(
            re.search(r"по вторникам в 7 часов утра", alice_state.vins_response.output_speech),
            "Alice should mention regular alarm on vins responce",
        )

        device.stage_logger.test_stage("Alice respond that regular alarm is set")

        cancel_all_alarms(device, alarmd, False)
