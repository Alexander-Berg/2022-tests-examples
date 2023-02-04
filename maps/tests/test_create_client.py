import pytest
from aiohttp.web import Response
from smb.common.testing_utils import dt

from maps_adv.geosmb.doorman.client.lib.enums import ClientGender, Source
from maps_adv.geosmb.doorman.proto import clients_pb2, common_pb2

pytestmark = [pytest.mark.asyncio]

creation_kwargs = dict(
    biz_id=123,
    source=Source.CRM_INTERFACE,
    metadata=dict(
        extra='{"test_field": 1}',
        url="http://test_widget.ru",
        device_id="test-device-id",
        uuid="test-uuid",
    ),
    phone=1234567890123,
    email="email@yandex.ru",
    passport_uid=456,
    first_name="client_first_name",
    last_name="client_last_name",
    gender=ClientGender.MALE,
    comment="this is comment",
    initiator_id=112233,
)


output_pb = clients_pb2.ClientData(
    id=1,
    biz_id=123,
    phone=1234567890123,
    email="email@yandex.ru",
    passport_uid=456,
    first_name="client_first_name",
    last_name="client_last_name",
    gender=common_pb2.ClientGender.MALE,
    comment="this is comment",
    segments=[],
    labels=[],
    source=common_pb2.Source.CRM_INTERFACE,
    registration_timestamp=dt("2020-01-01 13:10:20", as_proto=True),
)


async def test_sends_correct_request(client, mock_create_client):
    request_path = None
    request_body = None

    async def _handler(request):
        nonlocal request_path, request_body
        request_path = request.path
        request_body = await request.read()
        return Response(status=201, body=output_pb.SerializeToString())

    mock_create_client(_handler)

    await client.create_client(**creation_kwargs)

    assert request_path == "/v1/create_client/"
    assert (
        request_body
        == clients_pb2.ClientSetupData(
            biz_id=123,
            metadata=clients_pb2.SourceMetadata(
                source=common_pb2.Source.CRM_INTERFACE,
                extra='{"test_field": 1}',
                url="http://test_widget.ru",
                device_id="test-device-id",
                uuid="test-uuid",
            ),
            phone=1234567890123,
            email="email@yandex.ru",
            passport_uid=456,
            first_name="client_first_name",
            last_name="client_last_name",
            gender=common_pb2.ClientGender.MALE,
            comment="this is comment",
            initiator_id=112233,
        ).SerializeToString()
    )


async def test_sends_correct_request_with_skipped_params(client, mock_create_client):
    request_body = None

    async def _handler(request):
        nonlocal request_body
        request_body = await request.read()
        return Response(status=201, body=output_pb.SerializeToString())

    mock_create_client(_handler)

    await client.create_client(biz_id=123, source=Source.CRM_INTERFACE)

    assert (
        request_body
        == clients_pb2.ClientSetupData(
            biz_id=123,
            metadata=clients_pb2.SourceMetadata(source=common_pb2.Source.CRM_INTERFACE),
        ).SerializeToString()
    )


async def test_parses_response_correctly(client, mock_create_client):
    mock_create_client(
        lambda _: Response(status=201, body=output_pb.SerializeToString())
    )

    got = await client.create_client(**creation_kwargs)

    assert got == dict(
        id=1,
        biz_id=123,
        phone=1234567890123,
        email="email@yandex.ru",
        passport_uid=456,
        first_name="client_first_name",
        last_name="client_last_name",
        gender=ClientGender.MALE,
        comment="this is comment",
        segments=[],
        labels=[],
        source=Source.CRM_INTERFACE,
        registration_timestamp=dt("2020-01-01 13:10:20"),
    )
