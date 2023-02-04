import yandex_io.protos.model_objects_pb2 as model_objects
from yandex_io.pylibs.functional_tests.utils import ANY_IDLING_ALICE_STATE, presmoke, station_presmoke


@presmoke
@station_presmoke
def test_pressing_button(device):
    device.wait_for_authenticate_completion()
    device.wait_for_listening_start()

    with device.get_service_connector("aliced") as aliced:
        device.wait_for_message(
            aliced,
            lambda m: m.HasField('alice_state')
            and m.alice_state.has_startup_requirements
            and m.alice_state.state in ANY_IDLING_ALICE_STATE,
            "Alice must be idle",
        )
        device.press_alice_button(target_state=None)
        device.wait_for_message(
            aliced,
            lambda m: m.HasField('alice_state')
            and m.alice_state.has_startup_requirements
            and m.alice_state.state == model_objects.AliceState.LISTENING,
            "Alice should be in listening state",
        )
        device.press_alice_button(target_state=None)
        device.wait_for_message(
            aliced,
            lambda m: m.HasField('alice_state')
            and m.alice_state.has_startup_requirements
            and m.alice_state.state in ANY_IDLING_ALICE_STATE,
            "Alice state must be idle after second toggle",
        )
