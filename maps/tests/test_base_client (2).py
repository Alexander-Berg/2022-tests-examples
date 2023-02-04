import pytest
from aiohttp.web import Response
from smb.common.http_client import UnknownResponse

from maps_adv.geosmb.promoter.client import BadRequest
from maps_adv.geosmb.promoter.proto import errors_pb2

pytestmark = [pytest.mark.asyncio]


async def test_raises_if_response_with_proto_error(client, mock_remove_leads_for_gdpr):
    mock_remove_leads_for_gdpr(
        Response(
            status=400,
            body=errors_pb2.Error(
                code=errors_pb2.Error.VALIDATION_ERROR, description="error-description"
            ).SerializeToString(),
        )
    )

    with pytest.raises(BadRequest, match="error-description"):
        await client.remove_leads_for_gdpr(passport_uid=54321)


async def test_raises_if_bad_response_status(client, mock_remove_leads_for_gdpr):
    mock_remove_leads_for_gdpr(lambda _: Response(status=499))

    with pytest.raises(UnknownResponse):
        await client.remove_leads_for_gdpr(passport_uid=54321)
