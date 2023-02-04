import pytest
from freezegun import freeze_time

from maps_adv.geosmb.doorman.proto import clients_pb2, common_pb2
from maps_adv.geosmb.doorman.server.lib.enums import SegmentType
from maps_adv.geosmb.doorman.server.tests.utils import ENUM_MAPS_TO_PB, extract_ids

pytestmark = [pytest.mark.asyncio]


url = "/v1/list_clients/"


@pytest.mark.real_db
@pytest.mark.parametrize(
    "filter_ids, expected_ids",
    [(None, [222, 111]), ([222, 333, 12345], [222]), ([12345], [])],
)
async def test_returns_filtered_by_client_ids(factory, api, filter_ids, expected_ids):
    await factory.create_empty_client(client_id=111)
    await factory.create_empty_client(client_id=222)
    await factory.create_empty_client(biz_id=999, client_id=333)

    got = await api.post(
        url,
        proto=clients_pb2.ClientsListInput(
            biz_id=123,
            client_ids=filter_ids,
            pagination=common_pb2.Pagination(limit=100500, offset=0),
        ),
        decode_as=clients_pb2.ClientsListOutput,
        expected_status=200,
    )

    assert extract_ids(got.clients) == expected_ids


async def test_filters_by_search_and_client_ids(factory, api):
    id_1 = await factory.create_empty_client(first_name="Иван")
    await factory.create_empty_client(first_name="Иван")

    got = await api.post(
        url,
        proto=clients_pb2.ClientsListInput(
            biz_id=123,
            client_ids=[id_1],
            search_string="Иван",
            pagination=common_pb2.Pagination(limit=100500, offset=0),
        ),
        decode_as=clients_pb2.ClientsListOutput,
        expected_status=200,
    )

    assert extract_ids(got.clients) == [id_1]


async def test_filters_by_label_and_client_ids(factory, api):
    id_1 = await factory.create_empty_client(labels=["orange"])
    id_2 = await factory.create_empty_client(labels=["lemon"])
    await factory.create_empty_client(labels=["orange"])

    got = await api.post(
        url,
        proto=clients_pb2.ClientsListInput(
            biz_id=123,
            client_ids=[id_1, id_2],
            label="orange",
            pagination=common_pb2.Pagination(limit=100500, offset=0),
        ),
        decode_as=clients_pb2.ClientsListOutput,
        expected_status=200,
    )

    assert extract_ids(got.clients) == [id_1]


@freeze_time("2020-01-01 00:00:01", tick=True)
async def test_filters_by_segment_and_client_ids(factory, api):
    id_1 = await factory.create_empty_client(segments={SegmentType.ACTIVE})
    await factory.create_empty_client(segments={SegmentType.ACTIVE})

    got = await api.post(
        url,
        proto=clients_pb2.ClientsListInput(
            biz_id=123,
            client_ids=[id_1],
            segment=ENUM_MAPS_TO_PB["segment_type"][SegmentType.ACTIVE],
            pagination=common_pb2.Pagination(limit=100500, offset=0),
        ),
        decode_as=clients_pb2.ClientsListOutput,
        expected_status=200,
    )

    assert extract_ids(got.clients) == [id_1]


@freeze_time("2020-01-01 00:00:01", tick=True)
async def test_filters_by_search_and_segment_and_client_ids(factory, api):
    id_1 = await factory.create_empty_client(
        first_name="Иван", segments={SegmentType.ACTIVE}
    )
    await factory.create_empty_client(first_name="Иван", segments={SegmentType.ACTIVE})

    got = await api.post(
        url,
        proto=clients_pb2.ClientsListInput(
            biz_id=123,
            client_ids=[id_1],
            segment=ENUM_MAPS_TO_PB["segment_type"][SegmentType.ACTIVE],
            search_string="Иван",
            pagination=common_pb2.Pagination(limit=100500, offset=0),
        ),
        decode_as=clients_pb2.ClientsListOutput,
        expected_status=200,
    )

    assert extract_ids(got.clients) == [id_1]
