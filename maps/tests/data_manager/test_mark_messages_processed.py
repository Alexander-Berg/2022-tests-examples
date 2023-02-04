import pytest
from smb.common.testing_utils import dt

from maps_adv.geosmb.scenarist.server.lib.enums import MessageType

pytestmark = [pytest.mark.asyncio]


async def test_sets_passed_values(factory, dm):
    message1 = await factory.create_message(message_type=MessageType.EMAIL)
    message2 = await factory.create_message(message_type=MessageType.EMAIL)
    message3 = await factory.create_message(message_type=MessageType.EMAIL)

    await dm.mark_messages_processed(
        {
            message1["id"]: "Error info",
            message2["id"]: None,
            message3["id"]: "Error info",
        },
        dt("2020-02-03 04:05:06"),
        {"some": "metadata"},
    )

    message1_updated = await factory.retrieve_message_by_id(
        message1["id"], ["processed_at", "error", "processed_meta"]
    )
    message2_updated = await factory.retrieve_message_by_id(
        message2["id"], ["processed_at", "error", "processed_meta"]
    )
    message3_updated = await factory.retrieve_message_by_id(
        message3["id"], ["processed_at", "error", "processed_meta"]
    )
    assert message1_updated == {
        "processed_at": dt("2020-02-03 04:05:06"),
        "error": "Error info",
        "processed_meta": {"some": "metadata"},
    }
    assert message2_updated == {
        "processed_at": dt("2020-02-03 04:05:06"),
        "error": None,
        "processed_meta": {"some": "metadata"},
    }
    assert message3_updated == {
        "processed_at": dt("2020-02-03 04:05:06"),
        "error": "Error info",
        "processed_meta": {"some": "metadata"},
    }


async def test_ignores_unknown_messages(factory, dm):
    message1 = await factory.create_message(message_type=MessageType.EMAIL)
    message2 = await factory.create_message(message_type=MessageType.EMAIL)
    unknown_message_id = message1["id"] + message2["id"] + 1

    await dm.mark_messages_processed(
        {
            message1["id"]: None,
            unknown_message_id: "Something",
            message2["id"]: "Error info",
        },
        dt("2020-02-03 04:05:06"),
        {"some": "metadata"},
    )

    message1_updated = await factory.retrieve_message_by_id(
        message1["id"], ["processed_at", "error", "processed_meta"]
    )
    message2_updated = await factory.retrieve_message_by_id(
        message2["id"], ["processed_at", "error", "processed_meta"]
    )
    assert message1_updated == {
        "processed_at": dt("2020-02-03 04:05:06"),
        "error": None,
        "processed_meta": {"some": "metadata"},
    }
    assert message2_updated == {
        "processed_at": dt("2020-02-03 04:05:06"),
        "error": "Error info",
        "processed_meta": {"some": "metadata"},
    }


async def test_not_modifies_other_messages(factory, dm):
    message1 = await factory.create_message(message_type=MessageType.EMAIL)
    message2 = await factory.create_message(message_type=MessageType.EMAIL)
    message3 = await factory.create_message(
        message_type=MessageType.EMAIL,
        processed_at=dt("2020-05-06 00:20:00"),
        error="Something went wrong",
        processed_meta={"meta": "data"},
    )

    await dm.mark_messages_processed(
        {message1["id"]: None}, dt("2020-02-03 04:05:06"), {"some": "metadata"}
    )

    message2_updated = await factory.retrieve_message_by_id(
        message2["id"], ["processed_at", "error", "processed_meta"]
    )
    message3_updated = await factory.retrieve_message_by_id(
        message3["id"], ["processed_at", "error", "processed_meta"]
    )
    assert message2_updated == {
        "processed_at": None,
        "error": None,
        "processed_meta": None,
    }
    assert message3_updated == {
        "processed_at": dt("2020-05-06 00:20:00"),
        "error": "Something went wrong",
        "processed_meta": {"meta": "data"},
    }


async def test_returns_nothing(factory, dm):
    message1 = await factory.create_message(message_type=MessageType.EMAIL)

    result = await dm.mark_messages_processed(
        {message1["id"]: None}, dt("2020-02-03 04:05:06"), {"some": "metadata"}
    )

    assert result is None
