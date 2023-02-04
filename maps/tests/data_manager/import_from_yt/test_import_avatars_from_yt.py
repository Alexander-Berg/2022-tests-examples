import pytest

from maps_adv.common.helpers import AsyncIterator

pytestmark = [pytest.mark.asyncio]


input_data = [
    [("source-1", 10, "name-1")],
    [("source-2", 20, "name-2"), ("source-3", 30, "name-3")],
]

expected_data = [
    {
        "source_url": "source-1",
        "avatars_group_id": 10,
        "avatars_name": "name-1",
    },
    {
        "source_url": "source-2",
        "avatars_group_id": 20,
        "avatars_name": "name-2",
    },
    {
        "source_url": "source-3",
        "avatars_group_id": 30,
        "avatars_name": "name-3",
    },
]


async def test_writes_lines(factory, dm):
    await dm.import_avatars_from_yt(AsyncIterator(input_data))
    assert await factory.retrieve_all_avatars() == expected_data


async def test_removes_existing_data(factory, dm):
    await factory.create_avatars()
    await dm.import_avatars_from_yt(AsyncIterator(input_data))
    assert await factory.retrieve_all_avatars() == expected_data


async def test_clears_existing_data_if_empty_generator(factory, dm):
    await factory.create_avatars()
    await dm.import_avatars_from_yt(AsyncIterator([]))
    assert await factory.retrieve_all_avatars() == []
