import json

import pytest

from maps_adv.common.helpers import AsyncIterator

pytestmark = [pytest.mark.asyncio]


input_data = [
    [
        (
            100,
            json.dumps({"categories": [{"name": "QQQ"}], "source_name": "SSS 1"}),
        ),
        (
            101,
            json.dumps({"categories": [{"name": "WWW 1"}, {"name": "WWW 2"}], "source_name": "SSS 1"}),
        ),
    ],
    [
        (
            102,
            json.dumps({"categories": [{"name": "__$OTHERS$__"}], "source_name": "SSS 1"}),
        )
    ],
]


async def test_writes_lines(factory, dm):
    await dm.import_goods_data_from_yt(AsyncIterator(input_data))

    assert await factory.retrieve_all_goods_data() == [
        {"permalink": 100, "categories": {"source_name": "SSS 1", "categories": [{"name": "QQQ"}]}},
        {
            "permalink": 101,
            "categories": {"source_name": "SSS 1", "categories": [{"name": "WWW 1"}, {"name": "WWW 2"}]},
        },
        {"permalink": 102, "categories": {"source_name": "SSS 1", "categories": [{"name": "__$OTHERS$__"}]}},
    ]


async def test_removes_existing_data(factory, dm):
    await factory.create_goods_data()

    await dm.import_goods_data_from_yt(AsyncIterator(input_data))

    assert await factory.retrieve_all_goods_data() == [
        {"permalink": 100, "categories": {"source_name": "SSS 1", "categories": [{"name": "QQQ"}]}},
        {
            "permalink": 101,
            "categories": {"source_name": "SSS 1", "categories": [{"name": "WWW 1"}, {"name": "WWW 2"}]},
        },
        {"permalink": 102, "categories": {"source_name": "SSS 1", "categories": [{"name": "__$OTHERS$__"}]}},
    ]


async def test_clears_existing_data_if_empty_generator(factory, dm):
    await factory.create_goods_data()

    try:
        await dm.import_goods_data_from_yt(AsyncIterator([]))
    except:  # noqa
        pytest.fail("Should not raise")

    assert await factory.retrieve_all_goods_data() == []


async def test_returns_nothing(dm):
    got = await dm.import_goods_data_from_yt(AsyncIterator(input_data))

    assert got is None
