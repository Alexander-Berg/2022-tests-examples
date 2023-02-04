from ads.bsyeti.big_rt.lib.events.proto.event_pb2 import TEventMessage
from ads.bsyeti.big_rt.lib.events.proto.enum_options_pb2 import Schema
from ads.bsyeti.libs.events.proto.types_pb2 import EMessageType
from grut.libs.bigrt.events.watchlog.proto.types_pb2 import EMessageType as EWatchlogMessageType

_MESSAGE_TYPES = {}


def _build_message_types():
    message_types = {}
    for eventTypeEnum in [EMessageType, EWatchlogMessageType]:
        for k, v in eventTypeEnum.items():
            descriptor = eventTypeEnum.DESCRIPTOR.values_by_name[k]
            schema = descriptor.GetOptions().Extensions[Schema]
            if schema in message_types:
                continue
            message_types[schema] = v
    return message_types


def get_message_type(message):
    global _MESSAGE_TYPES
    if not _MESSAGE_TYPES:
        _MESSAGE_TYPES = _build_message_types()

    fname = message.DESCRIPTOR.full_name.replace(".", "::")
    if fname not in _MESSAGE_TYPES:
        return _MESSAGE_TYPES[message.DESCRIPTOR.name]
    return _MESSAGE_TYPES[fname]


def make_event(profile_id, timestamp, body, source=None, message_type=None):
    event = TEventMessage()
    event.ProfileID = str(profile_id).encode("utf-8")
    event.TimeStamp = timestamp
    event.Type = message_type or get_message_type(body)
    event.Body = body.SerializeToString()
    if source is not None:
        event.Source = source
    return event
