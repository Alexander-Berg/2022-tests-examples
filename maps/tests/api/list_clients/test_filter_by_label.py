import pytest

from maps_adv.geosmb.doorman.proto import clients_pb2, common_pb2
from maps_adv.geosmb.doorman.server.tests.utils import extract_ids

pytestmark = [pytest.mark.asyncio]


url = "/v1/list_clients/"


@pytest.mark.parametrize(
    "filter_label, expected_ids",
    [(None, [444, 333, 222, 111]), ("orange", [333, 222]), ("kiwi", [])],
)
async def test_returns_filtered_by_label(factory, api, filter_label, expected_ids):
    await factory.create_empty_client(client_id=111, labels=[])
    await factory.create_empty_client(client_id=222, labels=["orange"])
    await factory.create_empty_client(client_id=333, labels=["orange", "lemon"])
    await factory.create_empty_client(client_id=444, labels=["lemon"])
    await factory.create_empty_client(biz_id=999, client_id=555, labels=["orange"])

    got = await api.post(
        url,
        proto=clients_pb2.ClientsListInput(
            biz_id=123,
            label=filter_label,
            pagination=common_pb2.Pagination(limit=100500, offset=0),
        ),
        decode_as=clients_pb2.ClientsListOutput,
        expected_status=200,
    )

    assert extract_ids(got.clients) == expected_ids


async def test_filters_by_client_ids_and_label(factory, api):
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


async def test_filters_by_search_and_label(factory, api):
    id_1 = await factory.create_empty_client(labels=["orange"], first_name="Иван")
    await factory.create_empty_client(labels=["lemon"], first_name="Иван")
    await factory.create_empty_client(labels=["orange"], first_name="Фёдор")

    got = await api.post(
        url,
        proto=clients_pb2.ClientsListInput(
            biz_id=123,
            search_string="Иван",
            label="orange",
            pagination=common_pb2.Pagination(limit=100500, offset=0),
        ),
        decode_as=clients_pb2.ClientsListOutput,
        expected_status=200,
    )

    assert extract_ids(got.clients) == [id_1]
