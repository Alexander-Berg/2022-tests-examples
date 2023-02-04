import pytest

from maps_adv.geosmb.doorman.proto import clients_pb2, common_pb2
from maps_adv.geosmb.doorman.proto.segments_pb2 import SegmentType as SegmentTypePb
from maps_adv.geosmb.doorman.server.lib.enums import SegmentType
from maps_adv.geosmb.doorman.server.tests.utils import ENUM_MAPS_TO_PB, extract_ids

pytestmark = [
    pytest.mark.asyncio,
    pytest.mark.freeze_time("2020-01-01 00:00:01", tick=True),
]

url = "/v1/list_clients/"


@pytest.mark.parametrize(
    "filter_type, expected_ids",
    [
        (None, [101010, 777, 666, 555, 444, 333, 222, 111]),
        (SegmentTypePb.REGULAR, [111]),
        (SegmentTypePb.ACTIVE, [555, 222, 111]),
        (SegmentTypePb.LOST, [777, 666, 333]),
        (SegmentTypePb.UNPROCESSED_ORDERS, [444, 111]),
        (SegmentTypePb.NO_ORDERS, [101010, 777, 666, 555]),
        (SegmentTypePb.SHORT_LAST_CALL, [666]),
        (SegmentTypePb.MISSED_LAST_CALL, [777]),
    ],
)
async def test_returns_filtered_by_segment(factory, api, filter_type, expected_ids):
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

    got = await api.post(
        url,
        proto=clients_pb2.ClientsListInput(
            biz_id=123,
            segment=filter_type,
            pagination=common_pb2.Pagination(limit=100500, offset=0),
        ),
        decode_as=clients_pb2.ClientsListOutput,
        expected_status=200,
    )

    assert extract_ids(got.clients) == expected_ids


@pytest.mark.parametrize(
    "filter_type",
    [t for t in SegmentType if t != SegmentType.NO_ORDERS],
)
async def test_returns_search_result_filtered_by_segment(factory, api, filter_type):
    client_id_1 = await factory.create_empty_client(
        first_name="Иван", segments={filter_type}
    )
    await factory.create_empty_client(first_name="Иван")

    got = await api.post(
        url,
        proto=clients_pb2.ClientsListInput(
            biz_id=123,
            segment=ENUM_MAPS_TO_PB["segment_type"][filter_type],
            pagination=common_pb2.Pagination(limit=100500, offset=0),
        ),
        decode_as=clients_pb2.ClientsListOutput,
        expected_status=200,
    )

    assert extract_ids(got.clients) == [client_id_1]


async def test_returns_search_result_filtered_by_no_order_segment(factory, api):
    client_id_1 = await factory.create_empty_client(
        first_name="Иван", segments={SegmentType.NO_ORDERS}
    )
    await factory.create_empty_client(first_name="Иван", segments={SegmentType.ACTIVE})

    got = await api.post(
        url,
        proto=clients_pb2.ClientsListInput(
            biz_id=123,
            segment=SegmentTypePb.NO_ORDERS,
            pagination=common_pb2.Pagination(limit=100500, offset=0),
        ),
        decode_as=clients_pb2.ClientsListOutput,
        expected_status=200,
    )

    assert extract_ids(got.clients) == [client_id_1]


async def test_returns_nothing_if_no_clients_in_segment(factory, api):
    await factory.create_empty_client(segments={SegmentType.ACTIVE})

    got = await api.post(
        url,
        proto=clients_pb2.ClientsListInput(
            biz_id=123,
            segment=SegmentTypePb.NO_ORDERS,
            pagination=common_pb2.Pagination(limit=100500, offset=0),
        ),
        decode_as=clients_pb2.ClientsListOutput,
        expected_status=200,
    )

    assert got == clients_pb2.ClientsListOutput(total_count=0, clients=[])
