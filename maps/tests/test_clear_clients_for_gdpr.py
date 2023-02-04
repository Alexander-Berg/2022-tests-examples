import pytest
from aiohttp.web import Response

from maps_adv.geosmb.doorman.proto import clients_pb2

pytestmark = [pytest.mark.asyncio]


output_pb = clients_pb2.ClearClientsForGdprOutput(
    cleared_clients=[
        clients_pb2.ClearClientsForGdprOutput.ClearedClient(client_id=1, biz_id=111),
        clients_pb2.ClearClientsForGdprOutput.ClearedClient(client_id=2, biz_id=222),
    ]
)


async def test_sends_correct_request(client, mock_clear_clients_gdpr):
    request_path = None
    request_body = None

    async def _handler(request):
        nonlocal request_path, request_body
        request_path = request.path
        request_body = await request.read()
        return Response(status=200, body=output_pb.SerializeToString())

    mock_clear_clients_gdpr(_handler)

    await client.clear_clients_for_gdpr(passport_uid=123)

    assert request_path == "/internal/v1/clear_clients_for_gdpr/"
    assert (
        request_body
        == clients_pb2.ClearClientsForGdprInput(passport_uid=123).SerializeToString()
    )


async def test_parses_response_correctly(client, mock_clear_clients_gdpr):
    mock_clear_clients_gdpr(
        lambda _: Response(status=200, body=output_pb.SerializeToString())
    )

    got = await client.clear_clients_for_gdpr(passport_uid=123)

    assert got == [{"client_id": 1, "biz_id": 111}, {"client_id": 2, "biz_id": 222}]
