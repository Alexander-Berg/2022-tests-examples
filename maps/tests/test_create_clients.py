import pytest
from aiohttp.web import Response

from maps_adv.geosmb.doorman.client.lib.enums import Source
from maps_adv.geosmb.doorman.proto import clients_pb2, common_pb2

pytestmark = [pytest.mark.asyncio]

creation_kwargs = dict(
    biz_id=123,
    source=Source.CRM_INTERFACE,
    label="orange",
    clients=[
        dict(
            first_name="Иван",
            last_name="Волков",
            phone=1111111111,
            email="ivan@yandex.ru",
            comment="ivan comment",
        ),
        dict(
            first_name="Алекс",
            last_name="Зайцев",
            phone=2222222222,
            email="alex@yandex.ru",
            comment="alex comment",
        ),
    ],
)


output_pb = clients_pb2.BulkCreateClientsOutput(total_created=0, total_merged=2)


async def test_sends_correct_request(client, mock_create_clients):
    request_path = None
    request_body = None

    async def _handler(request):
        nonlocal request_path, request_body
        request_path = request.path
        request_body = await request.read()
        return Response(status=201, body=output_pb.SerializeToString())

    mock_create_clients(_handler)

    await client.create_clients(**creation_kwargs)

    assert request_path == "/v1/create_clients/"
    assert (
        request_body
        == clients_pb2.BulkCreateClientsInput(
            biz_id=123,
            source=common_pb2.Source.CRM_INTERFACE,
            label="orange",
            clients=[
                clients_pb2.BulkCreateClientsInput.BulkClient(
                    first_name="Иван",
                    last_name="Волков",
                    phone=1111111111,
                    email="ivan@yandex.ru",
                    comment="ivan comment",
                ),
                clients_pb2.BulkCreateClientsInput.BulkClient(
                    first_name="Алекс",
                    last_name="Зайцев",
                    phone=2222222222,
                    email="alex@yandex.ru",
                    comment="alex comment",
                ),
            ],
        ).SerializeToString()
    )


async def test_parses_response_correctly(client, mock_create_clients):
    mock_create_clients(
        lambda _: Response(status=201, body=output_pb.SerializeToString())
    )

    got = await client.create_clients(**creation_kwargs)

    assert got == (0, 2)
