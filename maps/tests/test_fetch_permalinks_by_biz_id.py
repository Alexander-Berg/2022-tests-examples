import pytest
from aiohttp.web import Response
from smb.common.http_client import UnknownResponse

from maps_adv.geosmb.clients.bvm import BvmBadProto, BvmNotFound, BvmUnknownError
from maps_adv.geosmb.clients.bvm.proto.bvm_pb2 import (
    ClientError,
    ClientErrorCode,
    FetchPermalinksByBizIdInput,
    FetchPermalinksByBizIdOutput,
)

pytestmark = [pytest.mark.asyncio]


async def test_sends_correct_request(bvm_client, mock_bvm_fetch_permalinks_by_biz_id):
    request_url = None
    request_body = None
    request_headers = None

    async def _handler(request):
        nonlocal request_url, request_body, request_headers
        request_url = str(request.url)
        request_body = await request.read()
        request_headers = request.headers
        return Response(
            status=200,
            body=FetchPermalinksByBizIdOutput(permalinks=[7654321]).SerializeToString(),
        )

    mock_bvm_fetch_permalinks_by_biz_id(_handler)

    await bvm_client.fetch_permalinks_by_biz_id(biz_id=123)

    assert request_url == "http://bvm.server/v1/fetch_permalinks"
    assert request_headers["Content-Type"] == "application/x-protobuf"
    proto_body = FetchPermalinksByBizIdInput.FromString(request_body)
    assert proto_body == FetchPermalinksByBizIdInput(biz_id=123)


async def test_returns_permalinks_by_biz_id(
    bvm_client, mock_bvm_fetch_permalinks_by_biz_id
):
    mock_bvm_fetch_permalinks_by_biz_id(
        Response(
            status=200,
            body=FetchPermalinksByBizIdOutput(
                permalinks=[888888, 999999]
            ).SerializeToString(),
        )
    )

    got = await bvm_client.fetch_permalinks_by_biz_id(biz_id=123)

    assert got == [888888, 999999]


async def test_raises_for_unknown_response(
    bvm_client, mock_bvm_fetch_permalinks_by_biz_id
):
    mock_bvm_fetch_permalinks_by_biz_id(Response(status=450))

    with pytest.raises(UnknownResponse):
        await bvm_client.fetch_permalinks_by_biz_id(biz_id=123)


@pytest.mark.parametrize(
    "code, exception",
    [
        (ClientErrorCode.UNKNOWN, BvmUnknownError),
        (ClientErrorCode.NOT_FOUND, BvmNotFound),
        (ClientErrorCode.BAD_PROTO, BvmBadProto),
    ],
)
async def test_raises_for_known_error_response(
    code, exception, bvm_client, mock_bvm_fetch_permalinks_by_biz_id
):
    mock_bvm_fetch_permalinks_by_biz_id(
        Response(
            status=400,
            body=ClientError(code=code, message="some message").SerializeToString(),
        )
    )

    with pytest.raises(exception) as exc:
        await bvm_client.fetch_permalinks_by_biz_id(biz_id=123)

    assert exc.value.args == ("some message",)
