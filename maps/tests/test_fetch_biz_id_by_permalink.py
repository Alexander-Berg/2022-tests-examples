import pytest
from aiohttp.web import Response
from smb.common.http_client import UnknownResponse

from maps_adv.geosmb.clients.bvm import BvmBadProto, BvmNotFound, BvmUnknownError
from maps_adv.geosmb.clients.bvm.proto.bvm_pb2 import (
    ClientError,
    ClientErrorCode,
    FetchBizIdInput,
    FetchBizIdOutput,
)

pytestmark = [pytest.mark.asyncio]


async def test_sends_correct_bytes_request(bvm_client, mock_bvm_fetch_biz_id):
    request_url = None
    request_body = None
    request_headers = None

    async def _handler(request):
        nonlocal request_url, request_body, request_headers
        request_url = str(request.url)
        request_body = await request.read()
        request_headers = request.headers
        return Response(
            status=200, body=FetchBizIdOutput(biz_id=123).SerializeToString()
        )

    mock_bvm_fetch_biz_id(_handler)

    await bvm_client.fetch_biz_id_by_permalink(permalink=7654321)

    assert request_url == "http://bvm.server/v1/fetch_biz_id"
    assert request_headers["Content-Type"] == "application/x-protobuf"
    proto_body = FetchBizIdInput.FromString(request_body)
    assert proto_body == FetchBizIdInput(permalink=7654321)


async def test_returns_biz_id_by_permalink(bvm_client, mock_bvm_fetch_biz_id):
    mock_bvm_fetch_biz_id(
        Response(status=200, body=FetchBizIdOutput(biz_id=123).SerializeToString())
    )

    got = await bvm_client.fetch_biz_id_by_permalink(permalink=7654321)

    assert got == 123


async def test_raises_for_unknown_response(bvm_client, mock_bvm_fetch_biz_id):
    mock_bvm_fetch_biz_id(Response(status=450))

    with pytest.raises(UnknownResponse):
        await bvm_client.fetch_biz_id_by_permalink(permalink=7654321)


@pytest.mark.parametrize(
    "code, exception",
    [
        (ClientErrorCode.UNKNOWN, BvmUnknownError),
        (ClientErrorCode.NOT_FOUND, BvmNotFound),
        (ClientErrorCode.BAD_PROTO, BvmBadProto),
    ],
)
async def test_raises_for_known_error_response(
    code, exception, bvm_client, mock_bvm_fetch_biz_id
):
    mock_bvm_fetch_biz_id(
        Response(
            status=400,
            body=ClientError(code=code, message="some message").SerializeToString(),
        )
    )

    with pytest.raises(exception) as exc:
        await bvm_client.fetch_biz_id_by_permalink(permalink=7654321)

    assert exc.value.args == ("some message",)
