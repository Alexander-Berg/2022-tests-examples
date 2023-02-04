import yandex_io.protos.model_objects_pb2 as P
from yandex_io.pylibs.functional_tests.utils import test_data_path, ANY_IDLING_ALICE_STATE, presmoke, station_presmoke
import pytest


@pytest.mark.testpalm("https://testpalm.yandex-team.ru/alice/testsuite/5d09de6f3d42cb5e76b4cf06?testcase=495")
@presmoke
@station_presmoke
def test_saying_wakeword(device, backend_client):
    device.wait_for_authenticate_completion()

    config = backend_client.getAccountConfig()
    config["spotter"] = "alisa"
    backend_client.setAccountConfig(config)

    device.wait_for_listening_start('alisa')

    device.stage_logger.test_stage("Wakeword is now \"Alisa\"")

    with device.get_service_connector("aliced") as aliced_connector:

        initial_aliced_state_message = device.wait_for_message(
            aliced_connector, lambda m: m.HasField("alice_state"), "Failed to get alice state"
        )
        device.failer.assert_fail(
            initial_aliced_state_message.alice_state.state in ANY_IDLING_ALICE_STATE, "Alice should be in IDLE state"
        )

        device.say_alisa_wake_word()

        device.wait_for_message(
            aliced_connector,
            lambda m: m.HasField("alice_state") and m.alice_state.state == P.AliceState.State.LISTENING,
            "Alice should start listening",
        )

        device.press_alice_button(target_state=ANY_IDLING_ALICE_STATE)

        config["spotter"] = "yandex"
        device.failer.assert_fail(backend_client.setAccountConfig(config)["status"] == "ok", "Spotter change failed")

        device.wait_for_listening_start('-yandex-')

        device.stage_logger.test_stage("Wakeword is now \"Yandex\"")

        device.say_to_mic(test_data_path("yandex.wav"))

        device.wait_for_message(
            aliced_connector,
            lambda m: m.HasField("alice_state") and m.alice_state.state == P.AliceState.State.LISTENING,
            "Alice should start listening",
        )

        device.press_alice_button(target_state=ANY_IDLING_ALICE_STATE)
