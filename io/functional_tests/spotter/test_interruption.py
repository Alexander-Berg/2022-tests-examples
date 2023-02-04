import pytest
import time
import yandex_io.protos.model_objects_pb2 as P
from yandex_io.pylibs.functional_tests.utils import test_data_path, ANY_IDLING_ALICE_STATE, presmoke, station_presmoke
from yandex_io.pylibs.functional_tests.matchers import is_music_playing


@pytest.mark.testpalm("https://testpalm.yandex-team.ru/alice/testsuite/5d09de6f3d42cb5e76b4cf06?testcase=1554")
@presmoke
@pytest.mark.with_yandex_plus
def test_music_interruption(device, backend_client):
    device.wait_for_authenticate_completion()

    config = backend_client.getAccountConfig()
    config["spotter"] = "alisa"
    backend_client.setAccountConfig(config)

    device.wait_for_listening_start('alisa')

    with device.get_service_connector("aliced") as aliced:

        initial_aliced_state_message = device.wait_for_message(
            aliced, lambda m: m.HasField("alice_state"), "Failed to get Alice state"
        )
        device.failer.assert_fail(
            initial_aliced_state_message.alice_state.state in ANY_IDLING_ALICE_STATE, "Alice should be in IDLE state"
        )

        device.say_alisa_wake_word()
        device.wait_for_message(
            aliced,
            lambda m: m.HasField("alice_state") and m.alice_state.state == P.AliceState.State.LISTENING,
            "Alice should be in LISTENING state after phrase spotted",
        )
        device.say_to_mic(test_data_path("play_genre_1.wav"))

        device.wait_for_message(aliced, is_music_playing, "Music failed to start")
        device.say_alisa_wake_word()

        # TODO: Add a proper check for music's volume is being reduced
        device.wait_for_message(
            aliced,
            lambda m: m.HasField("alice_state") and m.alice_state.state == P.AliceState.State.LISTENING,
            "Alice should be in LISTENING state after phrase spotted",
        )
        device.wait_for_message(aliced, is_music_playing, "Music should continue playing in background")

        device.press_alice_button(target_state=ANY_IDLING_ALICE_STATE)


@pytest.mark.testpalm("https://testpalm.yandex-team.ru/alice/testsuite/5d09de6f3d42cb5e76b4cf06?testcase=1554")
@presmoke
@station_presmoke
@pytest.mark.with_yandex_plus
def test_news_interruption(device, backend_client):
    device.wait_for_authenticate_completion()

    config = backend_client.getAccountConfig()
    config["spotter"] = "alisa"
    backend_client.setAccountConfig(config)

    device.wait_for_listening_start('alisa')

    with device.get_service_connector("aliced") as aliced:

        initial_aliced_state_message = device.wait_for_message(
            aliced, lambda m: m.HasField("alice_state"), "Failed to get Alice state"
        )
        device.failer.assert_fail(
            initial_aliced_state_message.alice_state.state in ANY_IDLING_ALICE_STATE, "Alice should be in IDLE state"
        )

        device.say_alisa_wake_word()
        device.wait_for_message(
            aliced,
            lambda m: m.HasField("alice_state") and m.alice_state.state == P.AliceState.State.LISTENING,
            "Alice should be in LISTENING state after phrase spotted",
        )
        device.say_to_mic(test_data_path("tell_news.wav"))

        device.wait_for_message(
            aliced,
            lambda m: m.HasField("alice_state")
            and m.alice_state.state == P.AliceState.State.SPEAKING
            and m.alice_state.vins_response.HasField("output_speech")
            and m.alice_state.vins_response.output_speech != "",
            "Alice failed to tell news",
        )

        # FIXME(slavashel): interruption spotter starts with some delay. Saying wakeword in time with tts start can not wotk.
        time.sleep(1)

        device.say_alisa_wake_word()
        device.wait_for_message(
            aliced,
            lambda m: m.HasField("alice_state") and m.alice_state.state == P.AliceState.State.LISTENING,
            "Alice should be in LISTENING state after phrase spotted",
        )

        device.press_alice_button(target_state=ANY_IDLING_ALICE_STATE)
