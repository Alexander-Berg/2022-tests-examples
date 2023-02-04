import pytest
from freezegun import freeze_time

from maps_adv.geosmb.doorman.proto import clients_pb2, common_pb2
from maps_adv.geosmb.doorman.proto.segments_pb2 import SegmentType as SegmentTypePb
from maps_adv.geosmb.doorman.server.lib.enums import SegmentType
from maps_adv.geosmb.doorman.server.tests.utils import extract_ids

pytestmark = [pytest.mark.asyncio]

url = "/v1/list_clients/"


@pytest.mark.parametrize("limit", range(1, 4))
@pytest.mark.parametrize("offset", range(1, 4))
async def test_returns_nothing_if_there_are_no_clients(limit, offset, api):
    input_proto = clients_pb2.ClientsListInput(
        biz_id=123, pagination=common_pb2.Pagination(limit=limit, offset=offset)
    )
    got = await api.post(
        url,
        proto=input_proto,
        decode_as=clients_pb2.ClientsListOutput,
        expected_status=200,
    )

    assert got == clients_pb2.ClientsListOutput(clients=[], total_count=0)


@pytest.mark.parametrize("limit", range(1, 4))
@pytest.mark.real_db
async def test_respects_limit(limit, api, factory):
    client_ids = list(reversed([await factory.create_empty_client() for _ in range(3)]))

    input_proto = clients_pb2.ClientsListInput(
        biz_id=123, pagination=common_pb2.Pagination(limit=limit, offset=0)
    )
    got = await api.post(
        url,
        proto=input_proto,
        decode_as=clients_pb2.ClientsListOutput,
        expected_status=200,
    )

    assert extract_ids(got.clients) == client_ids[:limit]
    assert got.total_count == 3


@pytest.mark.parametrize("offset", range(0, 4))
@pytest.mark.real_db
async def test_respects_offset(offset, api, factory):
    client_ids = list(reversed([await factory.create_empty_client() for _ in range(3)]))

    input_proto = clients_pb2.ClientsListInput(
        biz_id=123, pagination=common_pb2.Pagination(limit=2, offset=offset)
    )
    got = await api.post(
        url,
        proto=input_proto,
        decode_as=clients_pb2.ClientsListOutput,
        expected_status=200,
    )

    assert extract_ids(got.clients) == client_ids[offset : offset + 2]  # noqa
    assert got.total_count == 3


@pytest.mark.parametrize("limit", range(1, 4))
@pytest.mark.real_db
async def test_respects_limit_with_search(limit, api, factory):
    [await factory.create_empty_client() for _ in range(3)]
    client_ids = list(
        reversed([await factory.create_empty_client(comment="Иван") for _ in range(3)])
    )

    input_proto = clients_pb2.ClientsListInput(
        biz_id=123,
        search_string="Иван",
        pagination=common_pb2.Pagination(limit=limit, offset=0),
    )
    got = await api.post(
        url,
        proto=input_proto,
        decode_as=clients_pb2.ClientsListOutput,
        expected_status=200,
    )

    assert extract_ids(got.clients) == client_ids[:limit]
    assert got.total_count == 3


@pytest.mark.parametrize("offset", range(0, 4))
@pytest.mark.real_db
async def test_respects_offset_with_search(offset, api, factory):
    [await factory.create_empty_client() for _ in range(3)]
    client_ids = list(
        reversed([await factory.create_empty_client(comment="Иван") for _ in range(3)])
    )

    input_proto = clients_pb2.ClientsListInput(
        biz_id=123,
        search_string="Иван",
        pagination=common_pb2.Pagination(limit=2, offset=offset),
    )
    got = await api.post(
        url,
        proto=input_proto,
        decode_as=clients_pb2.ClientsListOutput,
        expected_status=200,
    )

    assert extract_ids(got.clients) == client_ids[offset : offset + 2]  # noqa
    assert got.total_count == 3


@pytest.mark.parametrize("offset", range(0, 4))
async def test_respects_pagination_with_search(api, factory, offset):
    # FTS
    id_1 = await factory.create_empty_client(comment="emaiN")
    id_2 = await factory.create_empty_client(comment="emaiN")
    # Priority column search (strict)
    id_3 = await factory.create_empty_client(first_name="emaiN@yandex.ru")
    id_4 = await factory.create_empty_client(first_name="emaiN@yandex.ru")
    # Priority column search (fuzzy)
    id_5 = await factory.create_empty_client(email="email_1@yandex.ru")
    id_6 = await factory.create_empty_client(email="email_2@yandex.ru")
    full_expected_ids = [id_4, id_3, id_6, id_5, id_2, id_1]

    input_proto = clients_pb2.ClientsListInput(
        biz_id=123,
        search_string="emaiN",
        pagination=common_pb2.Pagination(limit=2, offset=offset),
    )
    got = await api.post(
        url,
        proto=input_proto,
        decode_as=clients_pb2.ClientsListOutput,
        expected_status=200,
    )

    assert extract_ids(got.clients) == full_expected_ids[offset : offset + 2]  # noqa
    assert got.total_count == 6


@freeze_time("2020-01-01 00:00:01", tick=True)
@pytest.mark.parametrize("limit", range(1, 4))
@pytest.mark.real_db
async def test_respects_limit_with_segment_filter(limit, api, factory):
    client_ids = list(
        reversed(
            [
                await factory.create_empty_client(segments={SegmentType.ACTIVE})
                for _ in range(3)
            ]
        )
    )

    got = await api.post(
        url,
        proto=clients_pb2.ClientsListInput(
            biz_id=123,
            segment=SegmentTypePb.ACTIVE,
            pagination=common_pb2.Pagination(limit=limit, offset=0),
        ),
        decode_as=clients_pb2.ClientsListOutput,
        expected_status=200,
    )

    assert extract_ids(got.clients) == client_ids[:limit]
    assert got.total_count == 3


@freeze_time("2020-01-01 00:00:01", tick=True)
@pytest.mark.parametrize("offset", range(0, 3))
@pytest.mark.real_db
async def test_respects_offset_with_segment_filter(offset, api, factory):
    client_ids = list(
        reversed(
            [
                await factory.create_empty_client(segments={SegmentType.ACTIVE})
                for _ in range(3)
            ]
        )
    )

    got = await api.post(
        url,
        proto=clients_pb2.ClientsListInput(
            biz_id=123,
            segment=SegmentTypePb.ACTIVE,
            pagination=common_pb2.Pagination(limit=2, offset=offset),
        ),
        decode_as=clients_pb2.ClientsListOutput,
        expected_status=200,
    )

    assert extract_ids(got.clients) == client_ids[offset : offset + 2]  # noqa
    assert got.total_count == 3


@pytest.mark.parametrize("limit", range(1, 4))
@pytest.mark.real_db
async def test_respects_limit_with_filter_by_ids(limit, api, factory):
    client_ids = list(reversed([await factory.create_empty_client() for _ in range(3)]))

    got = await api.post(
        url,
        proto=clients_pb2.ClientsListInput(
            biz_id=123,
            client_ids=client_ids,
            pagination=common_pb2.Pagination(limit=limit, offset=0),
        ),
        decode_as=clients_pb2.ClientsListOutput,
        expected_status=200,
    )

    assert extract_ids(got.clients) == client_ids[:limit]
    assert got.total_count == 3


@pytest.mark.parametrize("offset", range(0, 3))
@pytest.mark.real_db
async def test_respects_offset_with_filter_by_ids(offset, api, factory):
    client_ids = list(reversed([await factory.create_empty_client() for _ in range(3)]))

    got = await api.post(
        url,
        proto=clients_pb2.ClientsListInput(
            biz_id=123,
            client_ids=client_ids,
            pagination=common_pb2.Pagination(limit=2, offset=offset),
        ),
        decode_as=clients_pb2.ClientsListOutput,
        expected_status=200,
    )

    assert extract_ids(got.clients) == client_ids[offset : offset + 2]  # noqa
    assert got.total_count == 3
