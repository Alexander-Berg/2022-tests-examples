import pytest
from smb.common.testing_utils import Any

from maps_adv.common.helpers import AsyncIterator

pytestmark = [pytest.mark.asyncio]


input_data = [
    [(100, 7665456, "phone1"), (101, 124345, "phone2")],
    [(102, 654643, "phone3")],
]


async def test_writes_lines(factory, dm):
    await dm.import_call_tracking_from_yt(AsyncIterator(input_data))

    assert await factory.retrieve_all_call_tracking() == [
        {
            "id": Any(int),
            "phone_id": 100,
            "biz_id": 7665456,
            "formatted_phone": "phone1",
        },
        {
            "id": Any(int),
            "phone_id": 101,
            "biz_id": 124345,
            "formatted_phone": "phone2",
        },
        {
            "id": Any(int),
            "phone_id": 102,
            "biz_id": 654643,
            "formatted_phone": "phone3",
        },
    ]


async def test_removes_existing_data(factory, dm):
    await factory.create_substitution_phone()

    await dm.import_call_tracking_from_yt(AsyncIterator(input_data))

    assert await factory.retrieve_all_call_tracking() == [
        {
            "id": Any(int),
            "phone_id": 100,
            "biz_id": 7665456,
            "formatted_phone": "phone1",
        },
        {
            "id": Any(int),
            "phone_id": 101,
            "biz_id": 124345,
            "formatted_phone": "phone2",
        },
        {
            "id": Any(int),
            "phone_id": 102,
            "biz_id": 654643,
            "formatted_phone": "phone3",
        },
    ]


async def test_clears_existing_data_if_empty_generator(factory, dm):
    await factory.create_substitution_phone()

    try:
        await dm.import_call_tracking_from_yt(AsyncIterator([]))
    except:  # noqa
        pytest.fail("Should not raise")

    assert await factory.retrieve_all_call_tracking() == []


async def test_returns_nothing(dm):
    got = await dm.import_call_tracking_from_yt(AsyncIterator(input_data))

    assert got is None
