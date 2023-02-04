import pytest

from maps_adv.geosmb.doorman.proto import clients_pb2
from maps_adv.geosmb.doorman.server.tests.utils import extract_ids

pytestmark = [pytest.mark.asyncio]


url = "v1/list_contacts/"


async def test_returns_contact_details(factory, api):
    client_id = await factory.create_client(
        biz_id=123,
        passport_uid=456,
        phone=1234567890123,
        email="email@yandex.ru",
        first_name="client_first_name",
        last_name="client_last_name",
    )

    got = await api.post(
        url,
        proto=clients_pb2.ListContactsInput(client_ids=[client_id]),
        decode_as=clients_pb2.ClientContactsList,
        expected_status=200,
    )

    assert got == clients_pb2.ClientContactsList(
        clients=[
            dict(
                id=client_id,
                biz_id=123,
                passport_uid=456,
                phone=1234567890123,
                email="email@yandex.ru",
                first_name="client_first_name",
                last_name="client_last_name",
                cleared_for_gdpr=False,
            )
        ]
    )


async def test_returns_only_contacts_of_matched_clients(api, factory):
    await factory.create_client(client_id=1, passport_uid=123)
    await factory.create_client(client_id=2, passport_uid=456)

    got = await api.post(
        url,
        proto=clients_pb2.ListContactsInput(client_ids=[1, 999]),
        decode_as=clients_pb2.ClientContactsList,
        expected_status=200,
    )

    assert extract_ids(got.clients) == [1]


async def test_returns_nothing_if_no_clients(api):
    got = await api.post(
        url,
        proto=clients_pb2.ListContactsInput(client_ids=[111, 222]),
        decode_as=clients_pb2.ClientContactsList,
        expected_status=200,
    )

    assert got == clients_pb2.ClientContactsList()


async def test_sorts_by_creation_time(api, factory):
    id_1 = await factory.create_client(passport_uid=123)
    id_2 = await factory.create_client(passport_uid=456)
    id_3 = await factory.create_client(passport_uid=789)

    got = await api.post(
        url,
        proto=clients_pb2.ListContactsInput(client_ids=[id_2, id_1, id_3]),
        decode_as=clients_pb2.ClientContactsList,
        expected_status=200,
    )

    assert extract_ids(got.clients) == [id_1, id_2, id_3]
