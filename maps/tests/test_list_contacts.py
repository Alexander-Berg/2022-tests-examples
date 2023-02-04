import pytest
from aiohttp.web import Response

from maps_adv.geosmb.doorman.proto.clients_pb2 import (
    ClientContacts,
    ClientContactsList,
    ListContactsInput,
)

pytestmark = [pytest.mark.asyncio]


output_pb = ClientContactsList(
    clients=[
        ClientContacts(
            id=111,
            biz_id=123,
            phone=1234567890123,
            email="email@yandex.ru",
            passport_uid=456,
            first_name="client_first_name",
            last_name="client_last_name",
            cleared_for_gdpr=True,
        ),
        ClientContacts(
            id=222,
            biz_id=999,
            phone=987654,
            email="email_2@yandex.ru",
            passport_uid=888,
            first_name="client_first_name_2",
            last_name="client_last_name_2",
            cleared_for_gdpr=False,
        ),
    ]
)


async def test_sends_correct_request(client, mock_list_contacts):
    request_path = None
    request_body = None

    async def _handler(request):
        nonlocal request_path, request_body
        request_path = request.path
        request_body = await request.read()
        return Response(status=200, body=output_pb.SerializeToString())

    mock_list_contacts(_handler)

    await client.list_contacts(client_ids=[111, 222])

    assert request_path == "/v1/list_contacts/"
    proto_body = ListContactsInput.FromString(request_body)
    assert proto_body == ListContactsInput(client_ids=[111, 222])


async def test_parses_response_correctly(client, mock_list_contacts):
    mock_list_contacts(Response(status=200, body=output_pb.SerializeToString()))

    got = await client.list_contacts(client_ids=[111, 222])

    assert got == {
        111: dict(
            biz_id=123,
            phone=1234567890123,
            email="email@yandex.ru",
            passport_uid=456,
            first_name="client_first_name",
            last_name="client_last_name",
            cleared_for_gdpr=True,
        ),
        222: dict(
            biz_id=999,
            phone=987654,
            email="email_2@yandex.ru",
            passport_uid=888,
            first_name="client_first_name_2",
            last_name="client_last_name_2",
            cleared_for_gdpr=False,
        ),
    }
