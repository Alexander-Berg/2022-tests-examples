import re
import pytest
from yandex_io.pylibs.functional_tests.utils import test_data_path, ANY_IDLING_ALICE_STATE, presmoke, station_presmoke
from yandex_io.pylibs.functional_tests.matchers import has_output_speech, is_alice_listening


class City:
    MOSCOW = "[Мм]оскве"
    KALININGRAD = "[Кк]алининграде"


def get_nowcast_pattern(city):
    """
    Creates a regular expression that matches a prefix of Alice' nowcast message.

    """
    temperature = r"((\+|\-|\−)\d{1,2}|0)"
    return [
        rf"^[Сс]ейчас в {city}\s*,?\s*{temperature}.*$",
        rf"^[Сс]егодня в {city}.*температура от {temperature} до {temperature}.*$",
    ]


@pytest.mark.testpalm("https://testpalm.yandex-team.ru/alice/testsuite/5d09de6f3d42cb5e76b4cf06?testcase=669")
@presmoke
@station_presmoke
def test_weather(device):

    device.wait_for_authenticate_completion()
    device.wait_for_listening_start()

    with device.get_service_connector("aliced") as aliced:

        device.start_conversation()
        device.say_to_mic(test_data_path("ask_weather_in_moscow.wav"))

        alice_state = device.wait_for_message(aliced, has_output_speech, "Alice did not respond").alice_state
        device.failer.assert_fail(alice_state.vins_response.text_only, "Response contains not only text response")
        device.failer.assert_fail(
            any(
                map(
                    lambda regexp: re.match(regexp, alice_state.vins_response.output_speech),
                    get_nowcast_pattern(City.MOSCOW),
                )
            ),
            "Failed to match regesps with Alice response. Response: {}, Regexps: {}".format(
                alice_state.vins_response.output_speech, get_nowcast_pattern(City.MOSCOW)
            ),
        )

        device.wait_for_message(aliced, is_alice_listening, "Alice should listen after telling weather forecast")
        device.say_to_mic(test_data_path("ask_weather_in_kaliningrad.wav"))

        alice_state = device.wait_for_message(aliced, has_output_speech, "Alice did not respond").alice_state
        device.failer.assert_fail(alice_state.vins_response.text_only, "Response contains not only text response")
        device.failer.assert_fail(
            any(
                map(
                    lambda regexp: re.match(regexp, alice_state.vins_response.output_speech),
                    get_nowcast_pattern(City.KALININGRAD),
                )
            ),
            "Failed to match regesps with Alice response. Response: {}, Regexps: {}".format(
                alice_state.vins_response.output_speech, get_nowcast_pattern(City.KALININGRAD)
            ),
        )

        # Check that pressing button interrupts speaking
        device.press_alice_button(target_state=ANY_IDLING_ALICE_STATE)
