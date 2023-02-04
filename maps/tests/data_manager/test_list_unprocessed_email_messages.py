from datetime import datetime, timezone

import pytest
from smb.common.testing_utils import dt

from maps_adv.geosmb.scenarist.server.lib.enums import MessageType

pytestmark = [pytest.mark.asyncio]


def _message_ids(result):
    return sorted(
        message["id"]
        for message_group in result
        for message in message_group["messages"]
    )


async def test_returns_unprocessed_email_messages(factory, dm):
    message1 = await factory.create_message(message_type=MessageType.EMAIL)
    message2 = await factory.create_message(message_type=MessageType.EMAIL)

    result = await dm.list_unprocessed_email_messages()

    assert _message_ids(result) == [message1["id"], message2["id"]]


async def test_not_returns_processed_email_messages(factory, dm):
    await factory.create_message(
        message_type=MessageType.EMAIL,
        processed_at=datetime(2020, 1, 2, tzinfo=timezone.utc),
    )
    await factory.create_message(
        message_type=MessageType.EMAIL,
        processed_at=datetime(2020, 1, 2, tzinfo=timezone.utc),
        processed_meta={"some": "metadata"},
    )
    await factory.create_message(
        message_type=MessageType.EMAIL,
        processed_at=datetime(2020, 1, 2, tzinfo=timezone.utc),
        error="I tried, but I failed",
    )

    result = await dm.list_unprocessed_email_messages()

    assert _message_ids(result) == []


@pytest.mark.parametrize("message_type", [MessageType.PUSH, MessageType.SMS])
async def test_does_not_return_not_other_types_of_messages(factory, dm, message_type):
    await factory.create_message(message_type=message_type)

    result = await dm.list_unprocessed_email_messages()

    assert _message_ids(result) == []


async def test_return_data(factory, dm):
    message1 = await factory.create_message(
        time_to_send=dt("2020-02-02 12:30:00"),
        message_type=MessageType.EMAIL,
        message_anchor="message_1",
        message_meta={
            "recipient": "example1@yandex.ru",
            "subject": "Тема письма",
            "template_name": "template_1",
            "template_vars": {},
        },
    )
    message2 = await factory.create_message(
        time_to_send=dt("2020-03-04 14:50:00"),
        message_type=MessageType.EMAIL,
        message_anchor="message_2",
        message_meta={
            "recipient": "example2@yandex.ru",
            "subject": "Другая тема письма",
            "template_name": "template_2",
            "template_vars": {"first_name": "Not", "last_name": "Sure"},
        },
    )

    result = await dm.list_unprocessed_email_messages()

    assert result == [
        {
            "time_to_send": dt("2020-02-02 12:30:00"),
            "subject": "Тема письма",
            "template_name": "template_1",
            "messages": [
                {
                    "id": message1["id"],
                    "message_anchor": "message_1",
                    "recipient": "example1@yandex.ru",
                    "template_vars": {},
                }
            ],
        },
        {
            "time_to_send": dt("2020-03-04 14:50:00"),
            "subject": "Другая тема письма",
            "template_name": "template_2",
            "messages": [
                {
                    "id": message2["id"],
                    "message_anchor": "message_2",
                    "recipient": "example2@yandex.ru",
                    "template_vars": {"first_name": "Not", "last_name": "Sure"},
                }
            ],
        },
    ]


async def test_replaces_missing_fields_in_metadata_with_default_values(factory, dm):
    message = await factory.create_message(
        time_to_send=dt("2020-02-02 12:30:00"),
        message_type=MessageType.EMAIL,
        message_anchor="message_1",
        message_meta={},
    )

    result = await dm.list_unprocessed_email_messages()

    assert result == [
        {
            "time_to_send": dt("2020-02-02 12:30:00"),
            "subject": None,
            "template_name": None,
            "messages": [
                {
                    "id": message["id"],
                    "message_anchor": "message_1",
                    "recipient": None,
                    "template_vars": {},
                }
            ],
        }
    ]


async def test_returns_empty_list_if_no_data(dm):
    result = await dm.list_unprocessed_email_messages()

    assert result == []


async def test_returns_messages_grouped_by_certain_fields(factory, dm):
    message1 = await factory.create_message(
        time_to_send=dt("2020-02-02 12:30:00"),
        message_type=MessageType.EMAIL,
        message_anchor="message_1",
        message_meta={
            "recipient": "example1@yandex.ru",
            "subject": "Тема письма",
            "template_name": "template_1",
            "template_vars": {},
        },
    )
    message2 = await factory.create_message(
        time_to_send=dt("2020-02-02 12:30:00"),
        message_type=MessageType.EMAIL,
        message_anchor="message_2",
        message_meta={
            "recipient": "example2@yandex.ru",
            "subject": "Тема письма",
            "template_name": "template_1",
            "template_vars": {"first_name": "Not", "last_name": "Sure"},
        },
    )
    message3 = await factory.create_message(
        time_to_send=dt("2020-02-02 14:50:00"),
        message_type=MessageType.EMAIL,
        message_anchor="message_3",
        message_meta={
            "recipient": "example3@yandex.ru",
            "subject": "Другая тема письма",
            "template_name": "template_2",
            "template_vars": {"first_name": "Not", "last_name": "Sure"},
        },
    )
    message4 = await factory.create_message(
        time_to_send=dt("2020-02-02 14:50:00"),
        message_type=MessageType.EMAIL,
        message_anchor="message_4",
        message_meta={
            "recipient": "example4@yandex.ru",
            "subject": "Другая тема письма",
            "template_name": "template_2",
            "template_vars": {"first_name": "Yes", "last_name": "Of course"},
        },
    )

    result = await dm.list_unprocessed_email_messages()

    assert result == [
        {
            "time_to_send": dt("2020-02-02 12:30:00"),
            "subject": "Тема письма",
            "template_name": "template_1",
            "messages": [
                {
                    "id": message1["id"],
                    "message_anchor": "message_1",
                    "recipient": "example1@yandex.ru",
                    "template_vars": {},
                },
                {
                    "id": message2["id"],
                    "message_anchor": "message_2",
                    "recipient": "example2@yandex.ru",
                    "template_vars": {"first_name": "Not", "last_name": "Sure"},
                },
            ],
        },
        {
            "time_to_send": dt("2020-02-02 14:50:00"),
            "subject": "Другая тема письма",
            "template_name": "template_2",
            "messages": [
                {
                    "id": message3["id"],
                    "message_anchor": "message_3",
                    "recipient": "example3@yandex.ru",
                    "template_vars": {"first_name": "Not", "last_name": "Sure"},
                },
                {
                    "id": message4["id"],
                    "message_anchor": "message_4",
                    "recipient": "example4@yandex.ru",
                    "template_vars": {"first_name": "Yes", "last_name": "Of course"},
                },
            ],
        },
    ]


async def test_does_not_group_messages_with_different_fields_values(factory, dm):
    message1 = await factory.create_message(
        time_to_send=dt("2020-02-02 12:30:00"),
        message_type=MessageType.EMAIL,
        message_anchor="message_1",
        message_meta={
            "recipient": "example1@yandex.ru",
            "subject": "Тема письма",
            "template_name": "template_1",
            "template_vars": {},
        },
    )
    # Different template
    message2 = await factory.create_message(
        time_to_send=dt("2020-02-02 12:30:00"),
        message_type=MessageType.EMAIL,
        message_anchor="message_1",
        message_meta={
            "recipient": "example1@yandex.ru",
            "subject": "Тема письма",
            "template_name": "template_2",
            "template_vars": {},
        },
    )
    # Different time to send
    message3 = await factory.create_message(
        time_to_send=dt("2020-03-04 14:50:00"),
        message_type=MessageType.EMAIL,
        message_anchor="message_1",
        message_meta={
            "recipient": "example1@yandex.ru",
            "subject": "Тема письма",
            "template_name": "template_1",
            "template_vars": {},
        },
    )
    # Different subject
    message4 = await factory.create_message(
        time_to_send=dt("2020-02-02 12:30:00"),
        message_type=MessageType.EMAIL,
        message_anchor="message_1",
        message_meta={
            "recipient": "example1@yandex.ru",
            "subject": "Дргуая тема письма",
            "template_name": "template_1",
            "template_vars": {},
        },
    )

    result = await dm.list_unprocessed_email_messages()

    assert _message_ids(result) == [
        message1["id"],
        message2["id"],
        message3["id"],
        message4["id"],
    ]
    assert len(result) == 4
