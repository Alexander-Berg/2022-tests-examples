import pytest
from aiohttp.web import Response
from smb.common.http_client import UnknownResponse

from maps_adv.geosmb.clients.bvm import BvmBadProto, BvmNotFound, BvmUnknownError
from maps_adv.geosmb.clients.bvm.proto.bvm_pb2 import (
    ClientError,
    ClientErrorCode,
    FetchPermalinksByBizIdsInput,
    FetchPermalinksByBizIdsOutput,
)

pytestmark = [pytest.mark.asyncio]


async def test_sends_correct_request(bvm_client, mock_bvm_fetch_permalinks_by_biz_ids):
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
            body=FetchPermalinksByBizIdsOutput(
                items=[
                    FetchPermalinksByBizIdsOutput.BizIdData(
                        biz_id=123, permalinks=[111111]
                    ),
                    FetchPermalinksByBizIdsOutput.BizIdData(
                        biz_id=456, permalinks=[222222]
                    ),
                ]
            ).SerializeToString(),
        )

    mock_bvm_fetch_permalinks_by_biz_ids(_handler)

    await bvm_client.fetch_permalinks_by_biz_ids(biz_ids=[123, 456])

    assert (
        request_url == "http://bvm.server/v1/fetch_approximate_permalinks_for_biz_ids"
    )
    assert request_headers["Content-Type"] == "application/x-protobuf"
    proto_body = FetchPermalinksByBizIdsInput.FromString(request_body)
    assert proto_body == FetchPermalinksByBizIdsInput(biz_ids=[123, 456])


async def test_returns_permalinks_by_biz_ids(
    bvm_client, mock_bvm_fetch_permalinks_by_biz_ids
):
    mock_bvm_fetch_permalinks_by_biz_ids(
        Response(
            status=200,
            body=FetchPermalinksByBizIdsOutput(
                items=[
                    FetchPermalinksByBizIdsOutput.BizIdData(
                        biz_id=123, permalinks=[111111]
                    ),
                    FetchPermalinksByBizIdsOutput.BizIdData(
                        biz_id=456, permalinks=[222222]
                    ),
                ]
            ).SerializeToString(),
        )
    )

    got = await bvm_client.fetch_permalinks_by_biz_ids(biz_ids=[123, 456])

    assert got == {123: [111111], 456: [222222]}


async def test_raises_for_unknown_response(
    bvm_client, mock_bvm_fetch_permalinks_by_biz_ids
):
    mock_bvm_fetch_permalinks_by_biz_ids(Response(status=450))

    with pytest.raises(UnknownResponse):
        await bvm_client.fetch_permalinks_by_biz_ids(biz_ids=[123, 456])


@pytest.mark.parametrize(
    "code, exception",
    [
        (ClientErrorCode.UNKNOWN, BvmUnknownError),
        (ClientErrorCode.NOT_FOUND, BvmNotFound),
        (ClientErrorCode.BAD_PROTO, BvmBadProto),
    ],
)
async def test_raises_for_known_error_response(
    code, exception, bvm_client, mock_bvm_fetch_permalinks_by_biz_ids
):
    mock_bvm_fetch_permalinks_by_biz_ids(
        Response(
            status=400,
            body=ClientError(code=code, message="some message").SerializeToString(),
        )
    )

    with pytest.raises(exception) as exc:
        await bvm_client.fetch_permalinks_by_biz_ids(biz_ids=[123, 456])

    assert exc.value.args == ("some message",)
