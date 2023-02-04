import pytest
from smb.common.testing_utils import Any

from maps_adv.common.helpers import AsyncIterator

pytestmark = [pytest.mark.asyncio]


input_data = [
    [
        (
            100,
            7665456,
            "announcement1",
            "description1",
            "date_from1",
            "date_to1",
            None,
            None,
        ),
        (
            101,
            124345,
            "announcement2",
            "description2",
            "date_from2",
            "date_to2",
            "image_url2",
            None,
        ),
    ],
    [
        (
            102,
            654643,
            "announcement3",
            "description3",
            "date_from3",
            "date_to3",
            "image_url3",
            "link3",
        )
    ],
]


async def test_writes_lines(factory, dm):
    await dm.import_promos_from_yt(AsyncIterator(input_data))

    assert await factory.retrieve_all_promos() == [
        {
            "id": Any(int),
            "promotion_id": 100,
            "biz_id": 7665456,
            "announcement": "announcement1",
            "banner_img": None,
            "date_from": "date_from1",
            "date_to": "date_to1",
            "description": "description1",
            "link": None,
        },
        {
            "id": Any(int),
            "promotion_id": 101,
            "biz_id": 124345,
            "announcement": "announcement2",
            "banner_img": "image_url2",
            "date_from": "date_from2",
            "date_to": "date_to2",
            "description": "description2",
            "link": None,
        },
        {
            "id": Any(int),
            "promotion_id": 102,
            "biz_id": 654643,
            "announcement": "announcement3",
            "banner_img": "image_url3",
            "date_from": "date_from3",
            "date_to": "date_to3",
            "description": "description3",
            "link": "link3",
        },
    ]


async def test_removes_existing_data(factory, dm):
    await factory.create_promotion()

    await dm.import_promos_from_yt(AsyncIterator(input_data))

    assert await factory.retrieve_all_promos() == [
        {
            "id": Any(int),
            "promotion_id": 100,
            "biz_id": 7665456,
            "announcement": "announcement1",
            "banner_img": None,
            "date_from": "date_from1",
            "date_to": "date_to1",
            "description": "description1",
            "link": None,
        },
        {
            "id": Any(int),
            "promotion_id": 101,
            "biz_id": 124345,
            "announcement": "announcement2",
            "banner_img": "image_url2",
            "date_from": "date_from2",
            "date_to": "date_to2",
            "description": "description2",
            "link": None,
        },
        {
            "id": Any(int),
            "promotion_id": 102,
            "biz_id": 654643,
            "announcement": "announcement3",
            "banner_img": "image_url3",
            "date_from": "date_from3",
            "date_to": "date_to3",
            "description": "description3",
            "link": "link3",
        },
    ]


async def test_clears_existing_data_if_empty_generator(factory, dm):
    await factory.create_promotion()

    try:
        await dm.import_promos_from_yt(AsyncIterator([]))
    except:  # noqa
        pytest.fail("Should not raise")

    assert await factory.retrieve_all_promos() == []


async def test_returns_nothing(dm):
    got = await dm.import_promos_from_yt(AsyncIterator(input_data))

    assert got is None
