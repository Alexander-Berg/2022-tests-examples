from datetime import datetime

import pytest
from smb.common.testing_utils import Any

pytestmark = [pytest.mark.asyncio]


async def test_creates_business(factory, dm):
    await dm.add_business(biz_id=123, permalink=556677, counter_id=444)

    assert await factory.list_businesses() == [
        {
            "id": Any(int),
            "biz_id": 123,
            "permalink": 556677,
            "counter_id": 444,
            "created_at": Any(datetime),
        }
    ]


async def test_does_not_update_existing_business(factory, dm):
    await factory.create_business(biz_id=123, permalink=99999, counter_id=999)

    await dm.add_business(biz_id=123, permalink=556677, counter_id=444)

    assert await factory.fetch_business(123) == {
        "id": Any(int),
        "biz_id": 123,
        "permalink": 99999,
        "counter_id": 999,
        "created_at": Any(datetime),
    }


async def test_does_not_affect_other_businesses(factory, dm):
    await factory.create_business(biz_id=345, permalink=99999, counter_id=999)

    await dm.add_business(biz_id=123, permalink=556677, counter_id=444)

    assert await factory.list_businesses() == [
        {
            "id": Any(int),
            "biz_id": 345,
            "permalink": 99999,
            "counter_id": 999,
            "created_at": Any(datetime),
        },
        {
            "id": Any(int),
            "biz_id": 123,
            "permalink": 556677,
            "counter_id": 444,
            "created_at": Any(datetime),
        },
    ]


async def test_returns_none(factory, dm):
    result = await dm.add_business(biz_id=123, permalink=556677, counter_id=444)

    assert result is None
