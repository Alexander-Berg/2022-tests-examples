import pytest
from aiohttp.web import Response
from smb.common.testing_utils import dt

from maps_adv.geosmb.doorman.client.lib.enums import ClientGender, Source
from maps_adv.geosmb.doorman.proto import clients_pb2, common_pb2

pytestmark = [pytest.mark.asyncio]

update_kwargs = dict(
    client_id=111,
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
    id=111,
    biz_id=123,
    phone=1234567890123,
    email="email@yandex.ru",
    passport_uid=456,
    first_name="client_first_name",
    last_name="client_last_name",
    gender=common_pb2.ClientGender.MALE,
    comment="this is comment",
    labels=[],
    segments=[],
    source=common_pb2.Source.GEOADV_PHONE_CALL,
    registration_timestamp=dt("2020-01-01 13:10:20", as_proto=True),
)


async def test_sends_correct_request(client, mock_update_client):
    request_path = None
    request_body = None

    async def _handler(request):
        nonlocal request_path, request_body
        request_path = request.path
        request_body = await request.read()
        return Response(status=200, body=output_pb.SerializeToString())

    mock_update_client(_handler)

    await client.update_client(**update_kwargs)

    assert request_path == "/v1/update_client/"
    assert (
        request_body
        == clients_pb2.ClientUpdateData(
            id=111,
            data=clients_pb2.ClientSetupData(
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
            ),
        ).SerializeToString()
    )


async def test_sends_correct_request_with_skipped_params(client, mock_update_client):
    request_body = None

    async def _handler(request):
        nonlocal request_body
        request_body = await request.read()
        return Response(status=200, body=output_pb.SerializeToString())

    mock_update_client(_handler)

    await client.update_client(client_id=111, biz_id=123, source=Source.CRM_INTERFACE)

    assert (
        request_body
        == clients_pb2.ClientUpdateData(
            id=111,
            data=clients_pb2.ClientSetupData(
                biz_id=123,
                metadata=clients_pb2.SourceMetadata(
                    source=common_pb2.Source.CRM_INTERFACE
                ),
            ),
        ).SerializeToString()
    )


async def test_parses_response_correctly(client, mock_update_client):
    mock_update_client(
        lambda _: Response(status=200, body=output_pb.SerializeToString())
    )

    got = await client.update_client(**update_kwargs)

    assert got == dict(
        id=111,
        biz_id=123,
        phone=1234567890123,
        email="email@yandex.ru",
        passport_uid=456,
        first_name="client_first_name",
        last_name="client_last_name",
        gender=ClientGender.MALE,
        comment="this is comment",
        labels=[],
        segments=[],
        source=Source.GEOADV_PHONE_CALL,
        registration_timestamp=dt("2020-01-01 13:10:20"),
    )
