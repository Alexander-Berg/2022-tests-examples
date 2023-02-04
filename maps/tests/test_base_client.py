import pytest
from aiohttp.web import Response
from smb.common.http_client import UnknownResponse

from maps_adv.geosmb.doorman.client.lib.enums import ClientGender, Source
from maps_adv.geosmb.doorman.client.lib.exceptions import BadRequest, Conflict, NotFound
from maps_adv.geosmb.doorman.proto import errors_pb2

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


@pytest.mark.parametrize(
    "response_status, expected_exception",
    [(400, BadRequest), (404, NotFound), (409, Conflict)],
)
async def test_raises_if_response_with_proto_error(
    client, mock_create_client, response_status, expected_exception
):
    async def _handler(_):
        error = errors_pb2.Error(
            code=errors_pb2.Error.VALIDATION_ERROR, description="error-description"
        )
        return Response(status=response_status, body=error.SerializeToString())

    mock_create_client(_handler)

    with pytest.raises(expected_exception) as exc:
        await client.create_client(**creation_kwargs)

    assert exc.value.args == ("error-description",)


async def test_raises_if_bad_response_status(client, mock_create_client):
    mock_create_client(lambda _: Response(status=499))

    with pytest.raises(UnknownResponse):
        await client.create_client(**creation_kwargs)
