from yandex_io.pylibs.functional_tests.utils import test_data_path, regression
from yandex_io.pylibs.functional_tests.matchers import has_output_speech

import datetime
import pytest


def prepare_regexps():
    cur_time = datetime.datetime.now().time()
    regexps = [
        (str(cur_time.hour), str(cur_time.minute)),
        (str(cur_time.hour + int(cur_time.minute == 59)), str((cur_time.minute + 1) % 60)),
    ]
    return regexps


@pytest.mark.testpalm("https://testpalm.yandex-team.ru/alice/testsuite/5d09de6f3d42cb5e76b4cf06?testcase=2055")
@regression
def test_whats_the_time(device):

    device.wait_for_authenticate_completion()
    device.wait_for_listening_start()

    with device.get_service_connector("aliced") as aliced:

        regexps = prepare_regexps()

        device.start_conversation()
        device.say_to_mic(test_data_path("whats_the_time.wav"))

        response = device.wait_for_message(
            aliced, has_output_speech, "Alice did not respond"
        ).alice_state.vins_response.output_speech

        assert any(map(lambda regexp: all(time in response for time in regexp), regexps))
        device.failer.assert_fail(
            any(map(lambda regexp: all(time in response for time in regexp), regexps)), "Alice respond invalid time"
        )
