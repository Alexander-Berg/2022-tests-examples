import pytz
import datetime as dt
import icalendar as ical
from yandex_io.pylibs.functional_tests.utils import test_data_path
import yandex_io.protos.model_objects_pb2 as P

ALARM_TO_FIRE_EPS = 20


def convert_ical_to_list(icalendar):
    if not icalendar:
        return []
    return list(filter(lambda item: type(item) == ical.cal.Alarm, ical.Calendar.from_ical(icalendar).walk()))


def has_incoming_alarms(icalendar):
    if not icalendar:
        return False
    def is_incoming_alarm(alarm):
        if not isinstance(alarm, ical.cal.Alarm):
            return False
        trigger_time = alarm['TRIGGER'].dt
        if isinstance(trigger_time, dt.timedelta):
            # mark of regular alarm
            return True
        elif isinstance(trigger_time, dt.datetime):
            now = dt.datetime.utcnow().replace(tzinfo=pytz.utc)
            return trigger_time > now
        else:
            raise RuntimeError("Unknown trigger time type: " + str(type(trigger_time)))
    return len(list(filter(is_incoming_alarm, ical.Calendar.from_ical(icalendar).walk()))) > 0


def cancel_all_alarms(device, alarmd, clear_media_settings=True):
    device.start_conversation()
    device.say_to_mic(test_data_path("cancel_alarm.wav"))

    alarms_state = device.wait_for_message(
        alarmd,
        lambda m: m.HasField("alarms_state")
        and m.alarms_state.HasField("icalendar_state")
        and not has_incoming_alarms(m.alarms_state.icalendar_state),
        "Some alarms were not canceled",
    ).alarms_state
    if alarms_state.HasField("media_alarm_setting") and clear_media_settings:

        def set_default_alarmd_sound(device, alarmd):
            device.start_conversation()
            device.say_to_mic(test_data_path("set_default.wav"))
            device.wait_for_message(
                alarmd,
                lambda m: m.HasField("alarms_state") and not m.alarms_state.HasField("media_alarm_setting"),
                "Alarm tone was not set to dafault",
            )

        set_default_alarmd_sound(device, alarmd)


def set_alarm(device, alarmd, wav, seconds_to_fire):
    alarmd.clear_message_queue()

    alarm_set_time = dt.datetime.utcnow().replace(tzinfo=pytz.utc)

    device.start_conversation()
    device.say_to_mic(test_data_path(wav))

    alarm, alarmd_message = check_alarm_set(alarmd, alarm_set_time, seconds_to_fire, device)
    return alarm, alarmd_message


def check_alarm_set(alarmd, alarm_set_time, seconds_to_fire, device):
    def check_alarm_list_min_size(alarm_state, size):
        return len(convert_ical_to_list(alarm_state.icalendar_state)) >= size

    new_alarmd_message = device.wait_for_message(
        alarmd,
        lambda m: m.HasField("alarms_state")
        and m.alarms_state.HasField("icalendar_state")
        and m.alarms_state.icalendar_state
        and check_alarm_list_min_size(m.alarms_state, 1),
        "Failed to set alarm",
    )

    alarms = convert_ical_to_list(new_alarmd_message.alarms_state.icalendar_state)

    device.failer.assert_fail(
        len(alarms) == 1, "More than one alarms is set. Assume that your test clears alarm state before"
    )

    alarm = alarms[0]
    if (
        not dt.timedelta(seconds=0)
        < alarm['TRIGGER'].dt - alarm_set_time
        < dt.timedelta(seconds=seconds_to_fire + ALARM_TO_FIRE_EPS)
    ):
        new_alarmd_message = device.wait_for_message(
            alarmd,
            lambda m: m.HasField("alarms_state")
            and m.alarms_state.HasField("icalendar_state")
            and len(m.alarms_state.icalendar_state) > 0
            and convert_ical_to_list(m.alarms_state.icalendar_state)[0]["TRIGGER"].dt - alarm_set_time
            < dt.timedelta(seconds=seconds_to_fire + ALARM_TO_FIRE_EPS),
            "Alarm was set in wrong time. Expected time to alarm: {}, Actual: {}, Max diff: {}".format(
                seconds_to_fire, alarm['TRIGGER'].dt - alarm_set_time, ALARM_TO_FIRE_EPS
            ),
        )

    device.stage_logger.test_stage("Set alarm in {} seconds".format(seconds_to_fire))

    return alarm, new_alarmd_message


def check_alarm_fired(alarmd, alarm, device):
    alarm_event = device.wait_for_message(
        alarmd, lambda m: m.HasField("alarm_event") and m.alarm_event.HasField("alarm_fired"), "No alarm fired", 60
    ).alarm_event
    device.failer.assert_fail(
        alarm_event.alarm_fired.alarm_type == P.Alarm.AlarmType.ALARM
        or alarm_event.alarm_fired.alarm_type == P.Alarm.AlarmType.MEDIA_ALARM,
        "Undefined alarm type fired. Was waiting for ALARM",
    )
    device.failer.assert_fail(
        alarm_event.alarm_fired.start_timestamp_ms
        == (alarm['TRIGGER'].dt.timestamp() - alarm_event.alarm_fired.duration_seconds) * 1000,
        "Wrong alarm fired or alarm fired in wrong time",
    )
    device.stage_logger.test_stage("Alarm fired")


def check_alarm_started(alarmd, alarm, device):
    alarm_event = device.wait_for_message(
        alarmd, lambda m: m.HasField("alarm_event") and m.alarm_event.HasField("alarm_started"), "No alarm started"
    ).alarm_event.alarm_started
    device.stage_logger.test_stage("Alarm started")
    return alarm_event


def check_alarm_stopped(alarmd, device, timeout=30):
    alarm_event = device.wait_for_message(
        alarmd,
        lambda m: m.HasField("alarm_event")
        and m.alarm_event.HasField("alarm_stopped")
        and m.alarm_event.alarm_stopped.HasField("alarm"),
        "No alarm stopped",
        timeout,
    ).alarm_event.alarm_stopped.alarm
    device.stage_logger.test_stage("Alarm stopped")
    return alarm_event


def has_media_state(m):
    return m.HasField("alarms_state") and m.alarms_state.HasField("media_alarm_setting")
