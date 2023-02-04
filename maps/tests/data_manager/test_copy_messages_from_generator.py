from datetime import datetime, timezone
from operator import itemgetter

import pytest
from smb.common.testing_utils import Any

from maps_adv.common.helpers import AsyncIterator
from maps_adv.geosmb.scenarist.server.lib.enums import MessageType, ScenarioName

pytestmark = [pytest.mark.asyncio]


input_data = [
    [
        {
            "time_to_send": "2020-02-03 11:22:33+0200",
            "message_anchor": "message_1",
            "message_type": "EMAIL",
            "message_meta": '{"some": "metadata"}',
            "doorman_ids": [1, 2, 3],
            "promoter_ids": [3, 4, 5],
            "scenario_codes": ["DISCOUNT_FOR_LOST", "DISCOUNT_FOR_DISLOYAL"],
        },
        {
            "time_to_send": "2020-03-02 20:00:44+0300",
            "message_anchor": "message_2",
            "message_type": "SMS",
            "message_meta": '{"some_other": "metadata_value"}',
            "doorman_ids": [22],
            "promoter_ids": [],
            "scenario_codes": ["DISCOUNT_FOR_DISLOYAL"],
        },
    ],
    [
        {
            "time_to_send": "2020-11-11 11:11:11+0000",
            "message_anchor": "message_33",
            "message_type": "PUSH",
            "message_meta": '{"some_more": "metadata"}',
            "doorman_ids": [],
            "promoter_ids": [33],
            "scenario_codes": ["ENGAGE_PROSPECTIVE"],
        }
    ],
]


async def test_writes_lines(factory, dm):
    await dm.copy_messages_from_generator(AsyncIterator(input_data))

    assert sorted(
        await factory.retrieve_all_messages(), key=itemgetter("message_anchor")
    ) == [
        {
            "id": Any(int),
            "time_to_send": datetime(2020, 2, 3, 9, 22, 33, tzinfo=timezone.utc),
            "message_anchor": "message_1",
            "message_type": MessageType.EMAIL,
            "message_meta": {"some": "metadata"},
            "doorman_ids": [1, 2, 3],
            "promoter_ids": [3, 4, 5],
            "scenario_names": [
                ScenarioName.DISCOUNT_FOR_LOST,
                ScenarioName.DISCOUNT_FOR_DISLOYAL,
            ],
            "created_at": Any(datetime),
            "processed_at": None,
            "processed_meta": None,
            "error": None,
        },
        {
            "id": Any(int),
            "time_to_send": datetime(2020, 3, 2, 17, 0, 44, tzinfo=timezone.utc),
            "message_anchor": "message_2",
            "message_type": MessageType.SMS,
            "message_meta": {"some_other": "metadata_value"},
            "doorman_ids": [22],
            "promoter_ids": [],
            "scenario_names": [ScenarioName.DISCOUNT_FOR_DISLOYAL],
            "created_at": Any(datetime),
            "processed_at": None,
            "processed_meta": None,
            "error": None,
        },
        {
            "id": Any(int),
            "time_to_send": datetime(2020, 11, 11, 11, 11, 11, tzinfo=timezone.utc),
            "message_anchor": "message_33",
            "message_type": MessageType.PUSH,
            "message_meta": {"some_more": "metadata"},
            "doorman_ids": [],
            "promoter_ids": [33],
            "scenario_names": [ScenarioName.ENGAGE_PROSPECTIVE],
            "created_at": Any(datetime),
            "processed_at": None,
            "processed_meta": None,
            "error": None,
        },
    ]


@pytest.mark.parametrize(
    "processed_at", [None, datetime(2020, 6, 6, 6, 6, 6, tzinfo=timezone.utc)]
)
@pytest.mark.parametrize("error", [None, "I failed so much"])
async def test_not_removes_existing_data(factory, dm, processed_at, error):
    await factory.create_message(
        time_to_send=datetime(2020, 4, 5, 22, 33, 44, tzinfo=timezone.utc),
        message_anchor="message_0",
        message_type=MessageType.PUSH,
        message_meta={"existing": "meta"},
        doorman_ids=[333, 444],
        promoter_ids=[555, 666],
        scenario_names=[ScenarioName.THANK_THE_LOYAL],
        created_at=datetime(2020, 2, 2, 3, 3, 3, tzinfo=timezone.utc),
        processed_at=processed_at,
        error=error,
    )

    await dm.copy_messages_from_generator(AsyncIterator(input_data))

    assert sorted(
        await factory.retrieve_all_messages(), key=itemgetter("message_anchor")
    ) == [
        {
            "id": Any(int),
            "time_to_send": datetime(2020, 4, 5, 22, 33, 44, tzinfo=timezone.utc),
            "message_anchor": "message_0",
            "message_type": MessageType.PUSH,
            "message_meta": {"existing": "meta"},
            "doorman_ids": [333, 444],
            "promoter_ids": [555, 666],
            "scenario_names": [ScenarioName.THANK_THE_LOYAL],
            "created_at": Any(datetime),
            "processed_at": processed_at,
            "processed_meta": None,
            "error": error,
        },
        {
            "id": Any(int),
            "time_to_send": datetime(2020, 2, 3, 9, 22, 33, tzinfo=timezone.utc),
            "message_anchor": "message_1",
            "message_type": MessageType.EMAIL,
            "message_meta": {"some": "metadata"},
            "doorman_ids": [1, 2, 3],
            "promoter_ids": [3, 4, 5],
            "scenario_names": [
                ScenarioName.DISCOUNT_FOR_LOST,
                ScenarioName.DISCOUNT_FOR_DISLOYAL,
            ],
            "created_at": Any(datetime),
            "processed_at": None,
            "processed_meta": None,
            "error": None,
        },
        {
            "id": Any(int),
            "time_to_send": datetime(2020, 3, 2, 17, 0, 44, tzinfo=timezone.utc),
            "message_anchor": "message_2",
            "message_type": MessageType.SMS,
            "message_meta": {"some_other": "metadata_value"},
            "doorman_ids": [22],
            "promoter_ids": [],
            "scenario_names": [ScenarioName.DISCOUNT_FOR_DISLOYAL],
            "created_at": Any(datetime),
            "processed_at": None,
            "processed_meta": None,
            "error": None,
        },
        {
            "id": Any(int),
            "time_to_send": datetime(2020, 11, 11, 11, 11, 11, tzinfo=timezone.utc),
            "message_anchor": "message_33",
            "message_type": MessageType.PUSH,
            "message_meta": {"some_more": "metadata"},
            "doorman_ids": [],
            "promoter_ids": [33],
            "scenario_names": [ScenarioName.ENGAGE_PROSPECTIVE],
            "created_at": Any(datetime),
            "processed_at": None,
            "processed_meta": None,
            "error": None,
        },
    ]


async def test_works_ok_with_empty_generator(factory, dm):
    await factory.create_message(
        time_to_send=datetime(2020, 4, 5, 22, 33, 44, tzinfo=timezone.utc),
        message_anchor="message_0",
        message_type=MessageType.PUSH,
        message_meta={"existing": "meta"},
        doorman_ids=[333, 444],
        promoter_ids=[555, 666],
        scenario_names=[ScenarioName.THANK_THE_LOYAL],
        created_at=datetime(2020, 2, 2, 3, 3, 3, tzinfo=timezone.utc),
        processed_at=None,
        error=None,
    )

    try:
        await dm.copy_messages_from_generator(AsyncIterator([]))
    except:  # noqa
        pytest.fail("Should not raise")

    assert await factory.retrieve_all_messages() == [
        {
            "id": Any(int),
            "time_to_send": datetime(2020, 4, 5, 22, 33, 44, tzinfo=timezone.utc),
            "message_anchor": "message_0",
            "message_type": MessageType.PUSH,
            "message_meta": {"existing": "meta"},
            "doorman_ids": [333, 444],
            "promoter_ids": [555, 666],
            "scenario_names": [ScenarioName.THANK_THE_LOYAL],
            "created_at": Any(datetime),
            "processed_at": None,
            "processed_meta": None,
            "error": None,
        }
    ]


async def test_returns_nothing(dm):
    got = await dm.copy_messages_from_generator(AsyncIterator(input_data))

    assert got is None
