from yandex_io.pylibs.functional_tests.utils import (
    test_data_path,
    presmoke,
    station_presmoke,
    assert_alice_volume,
    regression,
)
from yandex_io.pylibs.functional_tests.matchers import has_output_speech
import re
import pytest

reminder_string = "поспать"


def reminder_matcher(m, regexps):
    return (
        has_output_speech(m)
        and reminder_string in m.alice_state.vins_response.output_speech
        and any(re.search(regexp, m.alice_state.vins_response.output_speech) for regexp in regexps)
    )


@pytest.mark.testpalm("https://testpalm.yandex-team.ru/alice/testsuite/5d09de6f3d42cb5e76b4cf06?testcase=670")
@pytest.mark.testpalm("https://testpalm.yandex-team.ru/alice/testsuite/5d09de6f3d42cb5e76b4cf06?testcase=2616")
@presmoke
@station_presmoke
def test_reminder_30s(device):
    device.wait_for_listening_start()
    device.wait_for_authenticate_completion()

    reminder_volume = device.config["alarmd"]["minimumReminderUserVolume"]

    with device.get_service_connector("aliced") as aliced, device.get_service_connector("testpoint") as testpoint:

        device.start_conversation()
        device.say_to_mic(test_data_path("volume_1.wav"))

        assert_alice_volume(device, testpoint, 1, False)

        device.start_conversation()
        device.say_to_mic(test_data_path("reminder_30s.wav"))

        device.wait_for_message(
            aliced,
            lambda m: reminder_matcher(m, [r"[Пп]оставила"]),
            "Alice should confirm that reminder was set",
        )

        device.wait_for_message(
            aliced, lambda m: reminder_matcher(m, [r"[Хх]отели", r"[Пп]росили", r"[Нн]апомнить"]), "Reminder did not fired", 120
        )

        assert_alice_volume(device, testpoint, reminder_volume, False)
        assert_alice_volume(device, testpoint, 1, False)


@pytest.mark.testpalm("https://testpalm.yandex-team.ru/alice/testsuite/5d09de6f3d42cb5e76b4cf06?testcase=670")
@regression
def test_delete_reminder(device):
    device.wait_for_listening_start()
    device.wait_for_authenticate_completion()

    with device.get_service_connector("aliced") as aliced:

        device.start_conversation()
        device.say_to_mic(test_data_path("reminder_5min.wav"))

        device.wait_for_message(
            aliced, lambda m: reminder_matcher(m, [r"[Пп]оставила"]), "Alice should confirm that reminder was set"
        )

        device.start_conversation()
        device.say_to_mic(test_data_path("my_reminders.wav"))

        device.wait_for_message(
            aliced, lambda m: reminder_matcher(m, [r"[Уу]становлено"]), "Alice should list all set reminders"
        )

        device.start_conversation()
        device.say_to_mic(test_data_path("delete_this_reminder.wav"))

        device.wait_for_message(
            aliced, lambda m: reminder_matcher(m, [r"[Уу]далила"]), "Alice should confirm that reminder was deleted"
        )

        device.start_conversation()
        device.say_to_mic(test_data_path("my_reminders.wav"))

        device.wait_for_message(aliced, has_output_speech, "Alice should answer on my reminders request")
