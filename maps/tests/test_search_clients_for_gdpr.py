import pytest
from aiohttp.web import Response

from maps_adv.geosmb.doorman.proto import clients_pb2

pytestmark = [pytest.mark.asyncio]


async def test_sends_correct_request(client, mock_search_clients_gdpr):
    request_path = None
    request_body = None

    async def _handler(request):
        nonlocal request_path, request_body
        request_path = request.path
        request_body = await request.read()
        return Response(
            status=200,
            body=clients_pb2.SearchClientsForGdprOutput(
                clients_exist=True
            ).SerializeToString(),
        )

    mock_search_clients_gdpr(_handler)

    await client.search_clients_for_gdpr(passport_uid=123)

    assert request_path == "/internal/v1/search_clients_for_gdpr/"
    assert (
        request_body
        == clients_pb2.SearchClientsForGdprInput(passport_uid=123).SerializeToString()
    )


@pytest.mark.parametrize("clients_exists", [True, False])
async def test_parses_response_correctly(
    client, mock_search_clients_gdpr, clients_exists
):
    mock_search_clients_gdpr(
        lambda _: Response(
            status=200,
            body=clients_pb2.SearchClientsForGdprOutput(
                clients_exist=clients_exists
            ).SerializeToString(),
        )
    )

    got = await client.search_clients_for_gdpr(passport_uid=123)

    assert got is clients_exists
