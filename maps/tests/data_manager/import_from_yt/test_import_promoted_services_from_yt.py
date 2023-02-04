from decimal import Decimal
from itertools import chain

import pytest
from smb.common.testing_utils import Any

from maps_adv.common.helpers import AsyncIterator

pytestmark = [pytest.mark.asyncio]


input_data = [
    [
        (100, 9870, "title1", Decimal("100500"), "link1", "url1", "desc1"),
        (101, 9870, "title2", None, None, None, None),
    ],
    [
        (102, 9871, "title3", Decimal("100500"), None, None, None),
        (103, 9872, "title4", None, "link4", None, None),
        (104, 9873, "title5", None, None, "url5", None),
        (105, 9874, "title6", None, None, None, "desc6"),
    ],
]


async def test_writes_lines(factory, dm):
    await dm.import_promoted_services_from_yt(AsyncIterator(input_data))

    assert await factory.retrieve_all_promoted_services() == [
        {
            "id": Any(int),
            "biz_id": biz_id,
            "service_id": service_id,
            "title": title,
            "cost": cost,
            "image": image,
            "url": url,
            "description": description,
        }
        for service_id, biz_id, title, cost, image, url, description in chain(
            *input_data
        )
    ]


async def test_removes_existing_data(factory, dm):
    await factory.create_promoted_service(service_id=7645)

    await dm.import_promoted_services_from_yt(AsyncIterator(input_data))

    assert not any(
        service["service_id"] == 7645
        for service in await factory.retrieve_all_promoted_services()
    )


async def test_clears_existing_data_if_empty_generator(factory, dm):
    await factory.create_promoted_service(service_id=7645)

    try:
        await dm.import_promoted_services_from_yt(AsyncIterator([]))
    except:  # noqa
        pytest.fail("Should not raise")

    assert await factory.retrieve_all_promoted_services() == []


async def test_returns_nothing(dm):
    got = await dm.import_promoted_services_from_yt(AsyncIterator(input_data))

    assert got is None
