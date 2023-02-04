import pytest

from maps_adv.geosmb.doorman.proto import (
    clients_pb2,
    common_pb2,
    errors_pb2,
    statistics_pb2,
)
from maps_adv.geosmb.doorman.proto.segments_pb2 import SegmentType as SegmentTypePb
from maps_adv.geosmb.doorman.server.lib.enums import Source
from maps_adv.geosmb.doorman.server.tests.utils import extract_ids

pytestmark = [pytest.mark.asyncio]

url = "/v1/list_clients/"


@pytest.mark.parametrize(
    "search_string, expected",
    [
        ("", "search_string: ['Length must be between 3 and 512.']"),
        ("zz", "search_string: ['Length must be between 3 and 512.']"),
        ("z" * 513, "search_string: ['Length must be between 3 and 512.']"),
    ],
)
async def test_returns_error_for_wrong_search_string(api, search_string, expected):
    input_proto = clients_pb2.ClientsListInput(
        biz_id=123,
        search_string=search_string,
        pagination=common_pb2.Pagination(limit=100500, offset=0),
    )
    got = await api.post(
        url, proto=input_proto, decode_as=errors_pb2.Error, expected_status=400
    )

    assert got == errors_pb2.Error(
        code=errors_pb2.Error.VALIDATION_ERROR, description=expected
    )


async def test_returns_clients_details(api, factory):
    client_id = await factory.create_client(first_name="Иван")

    input_proto = clients_pb2.ClientsListInput(
        biz_id=123,
        search_string="Иван",
        pagination=common_pb2.Pagination(limit=100500, offset=0),
    )
    got = await api.post(
        url,
        proto=input_proto,
        decode_as=clients_pb2.ClientsListOutput,
        expected_status=200,
    )

    assert list(got.clients) == [
        clients_pb2.ClientData(
            id=client_id,
            biz_id=123,
            phone=1234567890123,
            email="email@yandex.ru",
            passport_uid=456,
            first_name="Иван",
            last_name="client_last_name",
            gender=common_pb2.ClientGender.MALE,
            comment="this is comment",
            cleared_for_gdpr=False,
            labels=["mark-2021"],
            segments=[SegmentTypePb.NO_ORDERS],
            statistics=statistics_pb2.ClientStatistics(
                orders=statistics_pb2.OrderStatistics(
                    total=0, successful=0, unsuccessful=0
                )
            ),
            source=common_pb2.Source.CRM_INTERFACE,
            registration_timestamp=got.clients[0].registration_timestamp,
        )
    ]


async def test_returns_source_from_first_revision(factory, api):
    client_id = await factory.create_client(
        source=Source.CRM_INTERFACE, first_name="Иван"
    )
    await factory.create_revision(client_id, source=Source.GEOADV_PHONE_CALL)

    got = await api.post(
        url,
        proto=clients_pb2.ClientsListInput(
            biz_id=123,
            search_string="Иван",
            pagination=common_pb2.Pagination(limit=100500, offset=0),
        ),
        decode_as=clients_pb2.ClientsListOutput,
        expected_status=200,
    )

    assert got.clients[0].source == common_pb2.Source.CRM_INTERFACE


async def test_filters_by_biz_id(api, factory):
    id_1 = await factory.create_empty_client(
        biz_id=123, comment="работает таксистом в Москве"
    )
    await factory.create_empty_client(biz_id=999, comment="работает таксистом в Москве")

    input_proto = clients_pb2.ClientsListInput(
        biz_id=123,
        search_string="работает",
        pagination=common_pb2.Pagination(limit=100500, offset=0),
    )
    got = await api.post(
        url,
        proto=input_proto,
        decode_as=clients_pb2.ClientsListOutput,
        expected_status=200,
    )

    assert extract_ids(got.clients) == [id_1]


@pytest.mark.parametrize(
    "search_string",
    (
        # each column engage in search
        "Васи",
        "Иванова",
        "234567",
        "unknown@yandex.ru",
        "рЫботает",
        # ignore not matched search words
        "арбуз 123456789 ананас Иванов",
        # short patterns by strict matching
        "Вас",
        "123",
        "456",
        "789",
        # ignore not matched search words
        "арбуз 123456789 ананас Иванов",
    ),
)
async def test_finds_by_search_string(api, factory, search_string):
    client_id = await factory.create_empty_client(
        biz_id=123,
        first_name="Вася",
        last_name="Иванов",
        phone=123456789,
        email="email@yandex.ru",
        comment="работает таксистом в Москве",
    )
    await factory.create_empty_client(
        biz_id=123,
        first_name="Петя",
        last_name="Петров",
        phone=99999,
        email="petrov@mail.ru",
        comment="повар в Питере",
    )

    input_proto = clients_pb2.ClientsListInput(
        biz_id=123,
        search_string=search_string,
        pagination=common_pb2.Pagination(limit=100500, offset=0),
    )
    got = await api.post(
        url,
        proto=input_proto,
        decode_as=clients_pb2.ClientsListOutput,
        expected_status=200,
    )

    assert extract_ids(got.clients) == [client_id]


async def test_returns_sorted_by_fts_rank(api, factory):
    # 1 match
    id_1 = await factory.create_empty_client(first_name="Вася")
    # 3 matches
    id_2 = await factory.create_empty_client(comment="123456789 Иванов Вася")
    # 2 matches
    id_3 = await factory.create_empty_client(first_name="Вася", last_name="Иванов")
    # 1 match
    id_4 = await factory.create_empty_client(first_name="Иванов")

    input_pb = clients_pb2.ClientsListInput(
        biz_id=123,
        search_string="Вася Иванов 123456789 email@yandex.ru работает",
        pagination=common_pb2.Pagination(limit=100500, offset=0),
    )
    got = await api.post(
        url,
        proto=input_pb,
        decode_as=clients_pb2.ClientsListOutput,
        expected_status=200,
    )

    assert extract_ids(got.clients) == [id_2, id_3, id_4, id_1]


async def test_returns_with_right_sorted_order(api, factory):
    """
    Searched result is sorted by priority:
    * strict matches by priority columns
    * fuzzy matched by email column
    * other matches
    """
    # FTS
    id_1 = await factory.create_empty_client(comment="emaiN")
    id_2 = await factory.create_empty_client(comment="emaiN")
    # Priority column search (strict)
    id_3 = await factory.create_empty_client(first_name="emaiN@yandex.ru")
    id_4 = await factory.create_empty_client(first_name="emaiN@yandex.ru")
    # Priority column search (fuzzy)
    id_5 = await factory.create_empty_client(email="email_1@yandex.ru")
    id_6 = await factory.create_empty_client(email="email_2@yandex.ru")

    input_pb = clients_pb2.ClientsListInput(
        biz_id=123,
        search_string="emaiN",
        pagination=common_pb2.Pagination(limit=100500, offset=0),
    )
    got = await api.post(
        url,
        proto=input_pb,
        decode_as=clients_pb2.ClientsListOutput,
        expected_status=200,
    )

    assert extract_ids(got.clients) == [id_4, id_3, id_6, id_5, id_2, id_1]


async def test_returns_nothing_if_nothing_found(api, factory):
    await factory.create_empty_client(first_name="Петр", last_name="Каховский")

    input_proto = clients_pb2.ClientsListInput(
        biz_id=123,
        search_string="Вася Иванов",
        pagination=common_pb2.Pagination(limit=100500, offset=0),
    )
    got = await api.post(
        url,
        proto=input_proto,
        decode_as=clients_pb2.ClientsListOutput,
        expected_status=200,
    )

    assert list(got.clients) == []
