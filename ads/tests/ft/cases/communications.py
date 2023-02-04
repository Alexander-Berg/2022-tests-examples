from ads.bsyeti.caesar.libs.profiles.proto.communications_channel_pb2 import TMessageData
from ads.bsyeti.libs.communications.proto.common_pb2 import (
    TEventSource,
    EEventType,
    ECommunicationType,
    ESourceType,
    EChannel,
    EMessageStatus,
)
from ads.bsyeti.libs.events.proto.communications_pb2 import TCommunicationEventData

from ads.bsyeti.caesar.tests.ft.common.event import make_event


class TestCaseCommunicationsPopupNew:
    table = "Communications"
    assert_empty_profiles = False

    extra_profiles = {
        "CommunicationsChannel": [],
    }

    def __init__(self):
        self.expected = {}

    def make_events(self, time, shard, profile_id):
        event_id = 1
        body = TCommunicationEventData()
        body.Id = event_id
        body.Type = EEventType.SEND_MESSAGE
        body.CommunicationType = ECommunicationType.INFORMATION
        body.Source.Type = ESourceType.DIRECT_ADMIN
        body.Source.Id = 1
        body.SendMessage.Channels.extend([EChannel.DIRECT_WEB_UI])
        body.SendMessage.Expired = 123
        body.SendMessage.DirectWebData.MessageText = "TEXT"

        yield make_event(profile_id, time, body)

        message_id = (profile_id << 32) + event_id
        self.expected[message_id] = [EMessageStatus.NEW]

    def check_profiles(self, profiles):
        pass

    def check_communications_channel_table(self, rows):
        assert len(rows) == len(self.expected)
        for row in rows:
            data = TMessageData()
            data.ParseFromString(row[b"Data"])
            assert self.expected.get(row[b"MessageId"], []) == data.Status


class TestCaseCommunicationsPopupClear:
    table = "Communications"
    assert_empty_profiles = False

    extra_profiles = {
        "CommunicationsChannel": [],
    }

    def __init__(self):
        self.expected = {}

    def make_events(self, time, shard, profile_id):
        event_id = 1
        body = TCommunicationEventData()
        body.Id = event_id
        body.Type = EEventType.SEND_MESSAGE
        body.CommunicationType = ECommunicationType.INFORMATION
        body.Source.Type = ESourceType.DIRECT_ADMIN
        body.Source.Id = 1
        body.SendMessage.Channels.extend([EChannel.DIRECT_WEB_UI])
        body.SendMessage.Expired = 123
        body.SendMessage.DirectWebData.MessageText = "TEXT"

        yield make_event(profile_id, time, body)

        body = TCommunicationEventData()
        body.Id = 2
        body.Type = EEventType.CLEAR_MESSAGES
        body.CommunicationType = ECommunicationType.INFORMATION
        body.Source.Type = ESourceType.DIRECT_ADMIN
        body.Source.Id = 1
        body.ClearMessages.TargetEventIds.extend([event_id])
        body.ClearMessages.TargetChannels.extend([EChannel.DIRECT_WEB_UI])

        yield make_event(profile_id, time, body)

    def check_profiles(self, profiles):
        pass

    def check_communications_channel_table(self, rows):
        assert len(rows) == 0


def _make_message_data(statuses=[EMessageStatus.NEW]):
    data = TMessageData()
    data.Status.extend(statuses)
    return data.SerializeToString()


class TestCaseCommunicationsPopupStatus:
    table = "Communications"
    assert_empty_profiles = False
    SHARDS_COUNT = 1
    EVENT_ID = 1
    MESSAGE_ID = 1

    extra_profiles = {
        "CommunicationsChannel": [
            {
                "Channel": EChannel.DIRECT_WEB_UI,
                "MessageId": MESSAGE_ID,
                "Data": _make_message_data(),
            }
        ]
    }

    def __init__(self):
        self.expected = {}

    def make_events(self, time, shard, profile_id):
        event_id = 2

        body = TCommunicationEventData()
        body.Id = event_id
        body.Type = EEventType.MODIFY_MESSAGES
        body.CommunicationType = ECommunicationType.INFORMATION
        body.Source.Type = ESourceType.DIRECT_ADMIN
        body.Source.Id = 1
        body.ModifyMessages.TargetChannels.extend([EChannel.DIRECT_WEB_UI])
        body.ModifyMessages.MessageId = self.MESSAGE_ID
        body.ModifyMessages.Status.extend([EMessageStatus.DELIVERED])

        yield make_event(profile_id, time, body)

        self.expected[self.MESSAGE_ID] = [EMessageStatus.DELIVERED]

    def check_profiles(self, profiles):
        pass

    def check_communications_channel_table(self, rows):
        assert len(rows) == len(self.expected)
        for row in rows:
            data = TMessageData()
            data.ParseFromString(row[b"Data"])
            assert self.expected.get(row[b"MessageId"], []) == data.Status


def make_event_create_or_update(
    event_id=1,
    entity_id=1,
    source_type=ESourceType.DIRECT_OFFLINE_REGULAR,
    source_id=1,
    expired=0,
    next_time_show=0,
    slot_types=None,
    major_version=1,
    view_data=None,
    default_add_statuses=None,
    default_remove_statuses=None,
    minor_add_statuses=None,
    minor_remove_statuses=None,
    major_add_statuses=None,
    major_remove_statuses=None,
    channel=None,
):
    body = TCommunicationEventData()
    body.Id = event_id
    body.TargetEntityId = entity_id
    body.Type = EEventType.CREATE_OR_UPDATE
    body.Source.Type = source_type
    body.Source.Id = source_id
    if channel is not None:
        body.Channel = channel
    body.CreateOrUpdate.Expired = expired
    body.CreateOrUpdate.NextShowTime = next_time_show
    body.CreateOrUpdate.SlotTypes.extend(slot_types or [1])
    body.CreateOrUpdate.MajorViewDataVersion = major_version
    if view_data is not None:
        body.CreateOrUpdate.ViewData.Key = view_data
    body.CreateOrUpdate.DefaultAction.AddStatuses.extend(default_add_statuses)
    body.CreateOrUpdate.DefaultAction.RemoveStatuses.extend(default_remove_statuses)
    body.CreateOrUpdate.OnMinorVersionChange.AddStatuses.extend(minor_add_statuses)
    body.CreateOrUpdate.OnMinorVersionChange.RemoveStatuses.extend(minor_remove_statuses)
    body.CreateOrUpdate.OnMajorVersionChange.AddStatuses.extend(major_add_statuses)
    body.CreateOrUpdate.OnMajorVersionChange.RemoveStatuses.extend(major_remove_statuses)
    body.CreateOrUpdate.OnMajorVersionChange.UpdateNextShowTime = next_time_show
    return body


def make_conditiona_and_update(
    major_version=None,
    minor_version=None,
    add_statuses=None,
    remove_statuses=None,
    next_time_show=None,
):
    result = TCommunicationEventData.TConditionalUpdate.TConditionAndUpdate()
    if major_version is not None:
        result.ConditionMajorVersion = major_version
    if minor_version is not None:
        result.ConditionMinorVersion = minor_version
    result.Action.AddStatuses.extend(add_statuses)
    result.Action.RemoveStatuses.extend(remove_statuses)
    if next_time_show is not None:
        result.Action.UpdateNextShowTime = next_time_show
    return result


def make_event_conditional_update(
    event_id=1,
    entity_id=1,
    source_type=ESourceType.DIRECT_OFFLINE_REGULAR,
    source_id=1,
    condition_and_update_list=None,
    major_version=None,
    minor_version=None,
    add_statuses=None,
    remove_statuses=None,
    next_time_show=None,
    channel=None,
):
    if add_statuses or remove_statuses or next_time_show is not None:
        condition_and_update_list = [
            make_conditiona_and_update(
                major_version=major_version,
                minor_version=minor_version,
                add_statuses=add_statuses,
                remove_statuses=remove_statuses,
                next_time_show=next_time_show,
            )
        ]
    body = TCommunicationEventData()
    body.Id = event_id
    body.TargetEntityId = entity_id
    body.Type = EEventType.CONDITIONAL_UPDATE
    body.Source.Type = source_type
    body.Source.Id = source_id
    if channel is not None:
        body.Channel = channel
    body.ConditionalUpdate.ConditionAndUpdate.extend(condition_and_update_list)
    return body


class TestCaseCommunicationsRecommend:
    table = "Communications"
    assert_empty_profiles = False
    SHARDS_COUNT = 1

    extra_profiles = {"CommunicationsChannel": []}

    def generate_profile_ids(self, shard_count):
        return {0: list(range(len(self.test_cases)))}

    def __init__(self):
        self.expected = {}
        self.case_description = {}
        self.test_cases = [
            {
                "Description": "Create new recommendation",
                "Events": [
                    make_event_create_or_update(
                        event_id=123,
                        entity_id=321,
                        source_type=ESourceType.DIRECT_OFFLINE_REGULAR,
                        source_id=42,
                        expired=111,
                        next_time_show=222,
                        slot_types=[0, 1, 2],
                        major_version=3,
                        default_add_statuses=[EMessageStatus.NEW],
                        default_remove_statuses=[EMessageStatus.MUTE],
                        minor_remove_statuses=[
                            EMessageStatus.MUTE,
                            EMessageStatus.DELIVERED,
                            EMessageStatus.APPLY,
                            EMessageStatus.REJECT,
                        ],
                        major_remove_statuses=[
                            EMessageStatus.MUTE,
                            EMessageStatus.DELIVERED,
                            EMessageStatus.APPLY,
                            EMessageStatus.REJECT,
                        ],
                    )
                ],
                "Messages": [
                    {
                        "EventId": 123,
                        "TargetEntityId": 321,
                        "NextShowTime": 222,
                        "Statuses": 2,
                        "Slots": 7,
                        "SourceId": 42,
                        "SourceType": ESourceType.DIRECT_OFFLINE_REGULAR,
                        "MajorVersion": 3,
                        "MinorVersion": 0,
                    }
                ],
            },
            {
                "Description": "Recreate new recommendation",
                "Events": [
                    make_event_create_or_update(
                        expired=111,
                        next_time_show=222,
                        slot_types=[0, 1, 2],
                        major_version=3,
                        default_add_statuses=[EMessageStatus.NEW],
                        default_remove_statuses=[EMessageStatus.MUTE],
                        minor_remove_statuses=[
                            EMessageStatus.MUTE,
                            EMessageStatus.DELIVERED,
                            EMessageStatus.APPLY,
                            EMessageStatus.REJECT,
                        ],
                        major_remove_statuses=[
                            EMessageStatus.MUTE,
                            EMessageStatus.DELIVERED,
                            EMessageStatus.APPLY,
                            EMessageStatus.REJECT,
                        ],
                    ),
                    make_event_create_or_update(
                        source_id=24,
                        expired=11,
                        next_time_show=22,
                        slot_types=[1, 2],
                        major_version=3,
                        default_add_statuses=[EMessageStatus.NEW],
                        default_remove_statuses=[EMessageStatus.MUTE],
                        minor_remove_statuses=[
                            EMessageStatus.MUTE,
                            EMessageStatus.DELIVERED,
                            EMessageStatus.APPLY,
                            EMessageStatus.REJECT,
                        ],
                        major_remove_statuses=[
                            EMessageStatus.MUTE,
                            EMessageStatus.DELIVERED,
                            EMessageStatus.APPLY,
                            EMessageStatus.REJECT,
                        ],
                    ),
                ],
                "Messages": [
                    {
                        "NextShowTime": 222,
                        "Statuses": 2,
                        "Slots": 6,
                        "SourceId": 24,
                        "MajorVersion": 3,
                        "MinorVersion": 0,
                    }
                ],
            },
            {
                "Description": "Change recommendation data",
                "Events": [
                    make_event_create_or_update(
                        major_version=1,
                        view_data="1",
                        default_add_statuses=[
                            EMessageStatus.NEW,
                            EMessageStatus.DELIVERED,
                            EMessageStatus.APPLY,
                            EMessageStatus.REJECT,
                        ],
                    ),
                    make_event_create_or_update(
                        major_version=1,
                        view_data="2",
                        default_remove_statuses=[EMessageStatus.DELIVERED],
                        minor_remove_statuses=[EMessageStatus.APPLY],
                        major_remove_statuses=[EMessageStatus.REJECT],
                    ),
                ],
                "Messages": [
                    {
                        "Statuses": 2 + 8 + 128,
                        "MajorVersion": 1,
                        "MinorVersion": 1,
                    }
                ],
            },
            {
                "Description": "Change major version",
                "Events": [
                    make_event_create_or_update(
                        major_version=1,
                        view_data="1",
                        default_add_statuses=[
                            EMessageStatus.NEW,
                            EMessageStatus.DELIVERED,
                            EMessageStatus.APPLY,
                            EMessageStatus.REJECT,
                        ],
                    ),
                    make_event_create_or_update(
                        major_version=2,
                        view_data="2",
                        default_remove_statuses=[EMessageStatus.DELIVERED],
                        minor_remove_statuses=[EMessageStatus.APPLY],
                        major_remove_statuses=[EMessageStatus.REJECT],
                    ),
                ],
                "Messages": [
                    {
                        "Statuses": 2 + 8 + 64,
                        "MajorVersion": 2,
                        "MinorVersion": 0,
                    }
                ],
            },
            {
                "Description": "Mute recommendation",
                "Events": [
                    make_event_create_or_update(
                        slot_types=[2],
                        major_version=3,
                        default_add_statuses=[EMessageStatus.NEW],
                        default_remove_statuses=[EMessageStatus.MUTE],
                        minor_remove_statuses=[
                            EMessageStatus.MUTE,
                            EMessageStatus.DELIVERED,
                            EMessageStatus.APPLY,
                            EMessageStatus.REJECT,
                        ],
                        major_remove_statuses=[
                            EMessageStatus.MUTE,
                            EMessageStatus.DELIVERED,
                            EMessageStatus.APPLY,
                            EMessageStatus.REJECT,
                        ],
                    ),
                    make_event_conditional_update(add_statuses=[EMessageStatus.MUTE]),
                ],
                "Messages": [
                    {
                        "Statuses": 2 + 32,
                        "Slots": 4,
                    }
                ],
            },
            {
                "Description": "Recreate muted recommendation",
                "Events": [
                    make_event_create_or_update(
                        major_version=3,
                        default_add_statuses=[EMessageStatus.NEW],
                        default_remove_statuses=[EMessageStatus.MUTE],
                        minor_remove_statuses=[
                            EMessageStatus.MUTE,
                            EMessageStatus.DELIVERED,
                            EMessageStatus.APPLY,
                            EMessageStatus.REJECT,
                        ],
                        major_remove_statuses=[
                            EMessageStatus.MUTE,
                            EMessageStatus.DELIVERED,
                            EMessageStatus.APPLY,
                            EMessageStatus.REJECT,
                        ],
                    ),
                    make_event_conditional_update(add_statuses=[EMessageStatus.MUTE]),
                    make_event_create_or_update(
                        expired=111,
                        next_time_show=222,
                        slot_types=[0, 1, 2],
                        major_version=3,
                        default_add_statuses=[EMessageStatus.NEW],
                        default_remove_statuses=[EMessageStatus.MUTE],
                        minor_remove_statuses=[
                            EMessageStatus.MUTE,
                            EMessageStatus.DELIVERED,
                            EMessageStatus.APPLY,
                            EMessageStatus.REJECT,
                        ],
                        major_remove_statuses=[
                            EMessageStatus.MUTE,
                            EMessageStatus.DELIVERED,
                            EMessageStatus.APPLY,
                            EMessageStatus.REJECT,
                        ],
                    ),
                ],
                "Messages": [
                    {
                        "Statuses": 2,
                        "Slots": 7,
                    }
                ],
            },
            {
                "Description": "User actions",
                "Events": [
                    make_event_create_or_update(
                        next_time_show=222,
                        major_version=3,
                        default_add_statuses=[EMessageStatus.NEW],
                    ),
                    make_event_conditional_update(
                        condition_and_update_list=[
                            make_conditiona_and_update(major_version=3, add_statuses=[EMessageStatus.REJECT]),
                            make_conditiona_and_update(
                                major_version=3,
                                minor_version=1,
                                add_statuses=[EMessageStatus.APPLY],
                            ),
                            make_conditiona_and_update(
                                major_version=0,
                                minor_version=0,
                                add_statuses=[EMessageStatus.APPLY],
                            ),
                            make_conditiona_and_update(major_version=3, minor_version=0, next_time_show=777),
                        ]
                    ),
                ],
                "Messages": [
                    {
                        "NextShowTime": 777,
                        "Statuses": 2 + 128,
                    }
                ],
            },
            {
                "Description": "Create in two channels",
                "Events": [
                    make_event_create_or_update(
                        next_time_show=111,
                    ),
                    make_event_create_or_update(
                        channel=2,
                        next_time_show=222,
                    ),
                ],
                "Messages": [
                    {
                        "Channel": 1,
                        "NextShowTime": 111,
                    },
                    {
                        "Channel": 2,
                        "NextShowTime": 222,
                    }
                ],
            },
            {
                "Description": "User action in two channels",
                "Events": [
                    make_event_create_or_update(
                        next_time_show=111,
                    ),
                    make_event_create_or_update(
                        channel=3,
                        next_time_show=222,
                    ),
                    make_event_conditional_update(next_time_show=333),
                    make_event_conditional_update(next_time_show=444, channel=3),
                ],
                "Messages": [
                    {
                        "Channel": 1,
                        "NextShowTime": 333,
                    },
                    {
                        "Channel": 3,
                        "NextShowTime": 444,
                    }
                ],
            },
        ]
        self.iter_case = iter(self.test_cases)

    def make_events(self, time, shard, profile_id):
        test_case = next(self.iter_case)

        for event in test_case["Events"]:
            yield make_event(profile_id, time, event)

        for message in test_case["Messages"]:
            entity_id = message["TargetEntityId"] if "TargetEntityId" in message else 1
            event_id = message["EventId"] if "EventId" in message else 1
            message_id = (profile_id << 36) + (entity_id << 16) + event_id
            channel = message["Channel"] if "Channel" in message else 1
            if channel not in self.expected:
                self.expected[channel] = {}
            self.expected[channel][message_id] = message
            self.case_description[message_id] = test_case["Description"]

    def check_profiles(self, profiles):
        pass

    def check_communications_channel_table(self, rows):
        assert len(rows) == sum([len(self.expected[c]) for c in self.expected]), "%s: number of output messages"
        for row in rows:
            message_id = row[b"MessageId"]
            channel = row[b"Channel"]
            assert channel in self.expected, "%s : %d - unexpected channel" % (
                self.case_description.get(message_id, []),
                channel,
            )
            exp_channel = self.expected.get(channel)
            assert message_id in exp_channel, "%s : %d : %d - unexpected message_id" % (
                self.case_description.get(message_id, []),
                channel,
                message_id,
            )
            exp = exp_channel.get(message_id)
            for field in ["Statuses", "Slots", "NextShowTime", "EventId", "TargetEntityId"]:
                if field in exp:
                    assert exp[field] == row[bytes(field, "utf-8")], "%s : %d : %d : %s - invalid value" % (
                        self.case_description.get(message_id, []),
                        channel,
                        message_id,
                        field,
                    )
            if any(f in exp for f in ["SourceId", "SourceType"]):
                source = TEventSource()
                source.ParseFromString(row[b"Source"])
                if "SourceId" in exp:
                    assert exp["SourceId"] == source.Id, "%s : %d : %s - invalid value" % (
                        self.case_description.get(message_id, []),
                        message_id,
                        "SourceId",
                    )
                if "SourceType" in exp:
                    assert exp["SourceType"] == source.Type, "%s : %d : %s - invalid value" % (
                        self.case_description.get(message_id, []),
                        message_id,
                        "SourceType",
                    )
            if any(f in exp for f in ["MajorVersion", "MinorVersion", "ViewData"]):
                data = TMessageData()
                data.ParseFromString(row[b"Data"])
                if "MajorVersion" in exp:
                    assert (
                        exp["MajorVersion"] == data.DirectWebData.MajorViewDataVersion
                    ), "%s : %d : %s - invalid value" % (
                        self.case_description.get(message_id, []),
                        message_id,
                        "MajorVersion",
                    )
                if "MinorVersion" in exp:
                    assert (
                        exp["MinorVersion"] == data.DirectWebData.MinorViewDataVersion
                    ), "%s : %d : %s - invalid value" % (
                        self.case_description.get(message_id, []),
                        message_id,
                        "MinorVersion",
                    )
                if "ViewData" in exp:
                    assert exp["ViewData"] == data.DirectWebData.ViewData, "%s : %d : %s - invalid value" % (
                        self.case_description.get(message_id, []),
                        message_id,
                        "ViewData",
                    )
