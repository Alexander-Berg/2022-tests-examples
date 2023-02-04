import json

import pytest

from maps_adv.common.helpers import AsyncIterator

pytestmark = [pytest.mark.asyncio]


input_data = [
    [
        (
            10,
            100,
            json.dumps(
                {
                    "id": 10,
                    "name": "service10",
                    "description": "This is service 10",
                    "categories": [],
                    "min_cost": "0.1",
                    "min_duration": "10",
                    "action_type": "action",
                    "image": "http://images,com/10",
                }
            ),
        ),
        (
            11,
            101,
            json.dumps(
                {
                    "id": 11,
                    "name": "service11",
                    "description": "This is service 11",
                    "categories": [],
                    "min_cost": "0.1",
                    "min_duration": "10",
                    "action_type": "action",
                    "image": "http://images,com/11",
                }
            ),
        ),
        (
            12,
            101,
            json.dumps(
                {
                    "id": 12,
                    "name": "service12",
                    "description": "This is service 12",
                    "categories": [],
                    "min_cost": "0.1",
                    "min_duration": "10",
                    "action_type": "action",
                    "image": "http://images,com/12",
                }
            ),
        ),
    ]
]


async def test_writes_lines(factory, dm):
    await dm.import_market_int_services_from_yt(AsyncIterator(input_data))

    assert await factory.retrieve_all_market_int_services() == [
        {
            "service_id": 10,
            "biz_id": 100,
            "service_data": {
                "id": 10,
                "name": "service10",
                "description": "This is service 10",
                "categories": [],
                "min_cost": "0.1",
                "min_duration": "10",
                "action_type": "action",
                "image": "http://images,com/10",
            },
        },
        {
            "service_id": 11,
            "biz_id": 101,
            "service_data": {
                "id": 11,
                "name": "service11",
                "description": "This is service 11",
                "categories": [],
                "min_cost": "0.1",
                "min_duration": "10",
                "action_type": "action",
                "image": "http://images,com/11",
            },
        },
        {
            "service_id": 12,
            "biz_id": 101,
            "service_data": {
                "id": 12,
                "name": "service12",
                "description": "This is service 12",
                "categories": [],
                "min_cost": "0.1",
                "min_duration": "10",
                "action_type": "action",
                "image": "http://images,com/12",
            },
        },
    ]


async def test_removes_existing_data(factory, dm):
    await factory.create_market_int_service()

    await dm.import_market_int_services_from_yt(AsyncIterator(input_data))

    assert await factory.retrieve_all_market_int_services() == [
        {
            "service_id": 10,
            "biz_id": 100,
            "service_data": {
                "id": 10,
                "name": "service10",
                "description": "This is service 10",
                "categories": [],
                "min_cost": "0.1",
                "min_duration": "10",
                "action_type": "action",
                "image": "http://images,com/10",
            },
        },
        {
            "service_id": 11,
            "biz_id": 101,
            "service_data": {
                "id": 11,
                "name": "service11",
                "description": "This is service 11",
                "categories": [],
                "min_cost": "0.1",
                "min_duration": "10",
                "action_type": "action",
                "image": "http://images,com/11",
            },
        },
        {
            "service_id": 12,
            "biz_id": 101,
            "service_data": {
                "id": 12,
                "name": "service12",
                "description": "This is service 12",
                "categories": [],
                "min_cost": "0.1",
                "min_duration": "10",
                "action_type": "action",
                "image": "http://images,com/12",
            },
        },
    ]


async def test_clears_existing_data_if_empty_generator(factory, dm):
    await factory.create_market_int_service()

    try:
        await dm.import_market_int_services_from_yt(AsyncIterator([]))
    except:  # noqa
        pytest.fail("Should not raise")

    assert await factory.retrieve_all_market_int_services() == []


async def test_returns_nothing(dm):
    got = await dm.import_market_int_services_from_yt(AsyncIterator(input_data))

    assert got is None
