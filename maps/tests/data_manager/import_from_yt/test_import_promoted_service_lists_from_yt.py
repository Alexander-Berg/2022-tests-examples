import pytest
from smb.common.testing_utils import Any

from maps_adv.common.helpers import AsyncIterator

pytestmark = [pytest.mark.asyncio]


input_data = [
    [(100, 7665456, [4, 8, 15, 16, 23, 42]), (101, 124345, [1, 2])],
    [(102, 654643, [3])],
]


async def test_writes_lines(factory, dm):
    await dm.import_promoted_service_lists_from_yt(AsyncIterator(input_data))

    assert await factory.retrieve_all_promoted_service_lists() == [
        {
            "id": Any(int),
            "list_id": 100,
            "biz_id": 7665456,
            "services": [4, 8, 15, 16, 23, 42],
        },
        {"id": Any(int), "list_id": 101, "biz_id": 124345, "services": [1, 2]},
        {"id": Any(int), "list_id": 102, "biz_id": 654643, "services": [3]},
    ]


async def test_removes_existing_data(factory, dm):
    await factory.create_promoted_service_list()

    await dm.import_promoted_service_lists_from_yt(AsyncIterator(input_data))

    assert await factory.retrieve_all_promoted_service_lists() == [
        {
            "id": Any(int),
            "list_id": 100,
            "biz_id": 7665456,
            "services": [4, 8, 15, 16, 23, 42],
        },
        {"id": Any(int), "list_id": 101, "biz_id": 124345, "services": [1, 2]},
        {"id": Any(int), "list_id": 102, "biz_id": 654643, "services": [3]},
    ]


async def test_clears_existing_data_if_empty_generator(factory, dm):
    await factory.create_promoted_service_list()

    try:
        await dm.import_promoted_service_lists_from_yt(AsyncIterator([]))
    except:  # noqa
        pytest.fail("Should not raise")

    assert await factory.retrieve_all_promoted_service_lists() == []


async def test_returns_nothing(dm):
    got = await dm.import_promoted_service_lists_from_yt(AsyncIterator(input_data))

    assert got is None
