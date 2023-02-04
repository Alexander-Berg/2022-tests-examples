import pytest
from smb.common.testing_utils import Any

from maps_adv.common.helpers import AsyncIterator

pytestmark = [pytest.mark.asyncio]


input_data = [
    [(100, 7665456, "title1", "link1"), (101, 124345, "title2", "link2")],
    [(102, 654643, "title3", "link3")],
]


async def test_writes_lines(factory, dm):
    await dm.import_promoted_cta_from_yt(AsyncIterator(input_data))

    assert await factory.retrieve_all_promoted_cta() == [
        {
            "id": Any(int),
            "cta_id": 100,
            "biz_id": 7665456,
            "title": "title1",
            "link": "link1",
        },
        {
            "id": Any(int),
            "cta_id": 101,
            "biz_id": 124345,
            "title": "title2",
            "link": "link2",
        },
        {
            "id": Any(int),
            "cta_id": 102,
            "biz_id": 654643,
            "title": "title3",
            "link": "link3",
        },
    ]


async def test_removes_existing_data(factory, dm):
    await factory.create_promoted_cta()

    await dm.import_promoted_cta_from_yt(AsyncIterator(input_data))

    assert await factory.retrieve_all_promoted_cta() == [
        {
            "id": Any(int),
            "cta_id": 100,
            "biz_id": 7665456,
            "title": "title1",
            "link": "link1",
        },
        {
            "id": Any(int),
            "cta_id": 101,
            "biz_id": 124345,
            "title": "title2",
            "link": "link2",
        },
        {
            "id": Any(int),
            "cta_id": 102,
            "biz_id": 654643,
            "title": "title3",
            "link": "link3",
        },
    ]


async def test_clears_existing_data_if_empty_generator(factory, dm):
    await factory.create_promoted_cta()

    try:
        await dm.import_promoted_cta_from_yt(AsyncIterator([]))
    except:  # noqa
        pytest.fail("Should not raise")

    assert await factory.retrieve_all_promoted_cta() == []


async def test_returns_nothing(dm):
    got = await dm.import_promoted_cta_from_yt(AsyncIterator(input_data))

    assert got is None
