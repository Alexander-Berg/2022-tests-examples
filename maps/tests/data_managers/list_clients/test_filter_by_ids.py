import pytest
from freezegun import freeze_time

from maps_adv.geosmb.doorman.server.lib.enums import SegmentType
from maps_adv.geosmb.doorman.server.tests.utils import extract_ids

pytestmark = [pytest.mark.asyncio]


@pytest.mark.parametrize(
    "filter_ids, expected_ids",
    [(None, [222, 111]), ([222, 333, 12345], [222]), ([12345], [])],
)
async def test_returns_filtered_by_client_ids(factory, dm, filter_ids, expected_ids):
    await factory.create_empty_client(client_id=111)
    await factory.create_empty_client(client_id=222)
    await factory.create_empty_client(biz_id=999, client_id=333)

    _, got = await dm.list_clients(
        biz_id=123, client_ids=filter_ids, limit=100500, offset=0
    )

    assert extract_ids(got) == expected_ids


async def test_filters_by_search_and_client_ids(factory, dm):
    id_1 = await factory.create_empty_client(first_name="Иван")
    await factory.create_empty_client(first_name="Иван")

    _, got = await dm.list_clients(
        biz_id=123, client_ids=[id_1], search_string="Иван", limit=100500, offset=0
    )

    assert extract_ids(got) == [id_1]


async def test_filters_by_label_and_client_ids(factory, dm):
    id_1 = await factory.create_empty_client(labels=["orange"])
    id_2 = await factory.create_empty_client(labels=["lemon"])
    await factory.create_empty_client(labels=["orange"])

    _, got = await dm.list_clients(
        biz_id=123, client_ids=[id_1, id_2], label="orange", limit=100500, offset=0
    )

    assert extract_ids(got) == [id_1]


@freeze_time("2020-01-01 00:00:01", tick=True)
async def test_filters_by_segment_and_client_ids(factory, dm):
    id_1 = await factory.create_empty_client(segments={SegmentType.ACTIVE})
    await factory.create_empty_client(segments={SegmentType.ACTIVE})

    _, got = await dm.list_clients(
        biz_id=123,
        client_ids=[id_1],
        segment_type=SegmentType.ACTIVE,
        limit=100500,
        offset=0,
    )

    assert extract_ids(got) == [id_1]


@freeze_time("2020-01-01 00:00:01", tick=True)
async def test_filters_by_search_and_segment_and_client_ids(factory, dm):
    id_1 = await factory.create_empty_client(
        first_name="Иван", segments={SegmentType.ACTIVE}
    )
    await factory.create_empty_client(first_name="Иван", segments={SegmentType.ACTIVE})

    _, got = await dm.list_clients(
        biz_id=123,
        client_ids=[id_1],
        search_string="Иван",
        segment_type=SegmentType.ACTIVE,
        limit=100500,
        offset=0,
    )

    assert extract_ids(got) == [id_1]
