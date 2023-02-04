from yandex_io.pylibs.functional_tests.utils import test_data_path


def wait_timers(device, alarmd, count, matcher=lambda _: True):
    timers = device.wait_for_message(
        alarmd,
        lambda m: m.HasField("timers_state") and len(m.timers_state.timers) == count and matcher(m),
        "Failed to set target timers. Target: {}".format(count),
    ).timers_state.timers
    if count == 0:
        device.stage_logger.test_stage("No timers are active")
    else:
        device.stage_logger.test_stage("Timer successfuly set. Active timers: {}".format(count))
    return timers


def get_pause_timestamp(device, alarmd):
    return (
        device.wait_for_message(
            alarmd,
            lambda m: m.HasField("timers_state")
            and len(m.timers_state.timers) == 1
            and m.timers_state.timers[0].HasField("pause_timestamp_sec"),
            "Failed to pause timer",
        )
        .timers_state.timers[0]
        .pause_timestamp_sec
    )


def get_pause_duration(device, alarmd):
    return (
        device.wait_for_message(
            alarmd,
            lambda m: m.HasField("timers_state")
            and len(m.timers_state.timers) == 1
            and m.timers_state.timers[0].HasField("paused_seconds"),
            "Failed to resume paused timer",
        )
        .timers_state.timers[0]
        .paused_seconds
    )


def disable_timers(device, alarmd):
    device.start_conversation()
    device.say_to_mic(test_data_path("cancel_all_timers.wav"))
    device.wait_for_listening_start()
    wait_timers(device, alarmd, 0)
    device.stage_logger.test_stage("Timers disabled")


def disable_timers_if_any(device):
    with device.get_service_connector("alarmd") as alarmd:
        initial_alarmd_message = device.wait_for_message(
            alarmd, lambda m: m.HasField("timers_state"), "Failed to recieve timer state"
        )
        if len(initial_alarmd_message.timers_state.timers) != 0:
            disable_timers(device, alarmd)
            return
