import pytest

from maps_adv.geosmb.doorman.server.lib.enums import SegmentType
from maps_adv.geosmb.doorman.server.tests.utils import extract_ids

pytestmark = [
    pytest.mark.asyncio,
    pytest.mark.freeze_time("2020-01-01 00:00:01", tick=True),
]


@pytest.mark.parametrize(
    "filter_type, expected_ids",
    [
        (None, [101010, 777, 666, 555, 444, 333, 222, 111]),
        (SegmentType.REGULAR, [111]),
        (SegmentType.ACTIVE, [555, 222, 111]),
        (SegmentType.LOST, [777, 666, 333]),
        (SegmentType.UNPROCESSED_ORDERS, [444, 111]),
        (SegmentType.NO_ORDERS, [101010, 777, 666, 555]),
        (SegmentType.SHORT_LAST_CALL, [666]),
        (SegmentType.MISSED_LAST_CALL, [777]),
    ],
)
async def test_returns_filtered_by_segment(factory, dm, filter_type, expected_ids):
    # also UNPROCESSED_ORDERS because side effect of factory REGULAR creation
    await factory.create_empty_client(
        client_id=111, segments={SegmentType.REGULAR, SegmentType.ACTIVE}
    )
    await factory.create_empty_client(client_id=222, segments={SegmentType.ACTIVE})
    await factory.create_empty_client(client_id=333, segments={SegmentType.LOST})
    await factory.create_empty_client(
        client_id=444, segments={SegmentType.UNPROCESSED_ORDERS}
    )
    # also ACTIVE because side effect of factory NO_ORDERS creation
    await factory.create_empty_client(client_id=555, segments={SegmentType.NO_ORDERS})
    await factory.create_empty_client(
        client_id=666, segments={SegmentType.SHORT_LAST_CALL}
    )
    await factory.create_empty_client(
        client_id=777, segments={SegmentType.MISSED_LAST_CALL}
    )
    # has segment, but other biz_id
    await factory.create_empty_client(
        biz_id=999, client_id=999, segments={SegmentType.ACTIVE}
    )
    await factory.create_empty_client(client_id=101010)

    _, got = await dm.list_clients(
        biz_id=123, segment_type=filter_type, limit=100500, offset=0
    )

    assert extract_ids(got) == expected_ids


@pytest.mark.parametrize(
    "filter_type",
    [t for t in SegmentType if t != SegmentType.NO_ORDERS],
)
async def test_returns_search_result_filtered_by_segment(factory, dm, filter_type):
    client_id_1 = await factory.create_empty_client(
        first_name="Иван", segments={filter_type}
    )
    await factory.create_empty_client(first_name="Вася")

    _, got = await dm.list_clients(
        biz_id=123,
        search_string="Иван",
        segment_type=filter_type,
        limit=100500,
        offset=0,
    )

    assert extract_ids(got) == [client_id_1]


async def test_returns_search_result_filtered_by_no_order_segment(factory, dm):
    client_id_1 = await factory.create_empty_client(
        first_name="Иван", segments={SegmentType.NO_ORDERS}
    )
    await factory.create_empty_client(first_name="Вася", segments={SegmentType.ACTIVE})

    _, got = await dm.list_clients(
        biz_id=123,
        search_string="Иван",
        segment_type=SegmentType.NO_ORDERS,
        limit=100500,
        offset=0,
    )

    assert extract_ids(got) == [client_id_1]


async def test_returns_nothing_if_no_clients_in_segment(factory, dm):
    await factory.create_empty_client(segments={SegmentType.ACTIVE})

    total_count, clients = await dm.list_clients(
        biz_id=123, segment_type=SegmentType.NO_ORDERS, limit=100500, offset=0
    )

    assert total_count == 0
    assert clients == []
