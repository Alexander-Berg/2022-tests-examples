import pytest
from aiohttp.web import Response
from smb.common.testing_utils import dt

from maps_adv.geosmb.clients.facade import ValidationException
from maps_adv.geosmb.clients.facade.proto.facade_pb2 import (
    LoyaltyItemsListForSnapshotRequest,
    LoyaltyItemsListForSnapshotResponse,
    Paging,
    PagingOutput,
)

pytestmark = [pytest.mark.asyncio]


async def test_sends_correct_request(
    facade_client, mock_get_loyalty_items_list_for_snapshot
):
    request_url = None
    request_headers = None
    request_body = None

    async def facade_handler(request):
        nonlocal request_url, request_headers, request_body
        request_url = str(request.url)
        request_headers = request.headers
        request_body = await request.read()
        return Response(
            status=200,
            body=LoyaltyItemsListForSnapshotResponse(
                items_list=[], paging=PagingOutput(limit=1000, offset=0, total=20)
            ).SerializeToString(),
        )

    mock_get_loyalty_items_list_for_snapshot(facade_handler)

    async for _ in facade_client.get_loyalty_items_list_for_snapshot():
        pass

    assert request_url == "http://facade.server/v1/get_loyalty_items_list_for_snapshot"
    assert request_headers["X-Ya-Service-Ticket"] == "KEK_FROM_AIOTVM_PYTEST_PLUGIN"
    assert request_headers["Content-Type"] == "application/x-protobuf"
    proto_body = LoyaltyItemsListForSnapshotRequest.FromString(request_body)
    assert proto_body == LoyaltyItemsListForSnapshotRequest(
        paging=Paging(limit=500, offset=0)
    )


async def test_paginates_in_request_correctly(
    facade_client, mock_get_loyalty_items_list_for_snapshot
):
    request_bodies = []

    async def facade_handler(request):
        nonlocal request_bodies
        request_body = await request.read()
        request_bodies.append(
            LoyaltyItemsListForSnapshotRequest.FromString(request_body)
        )
        return Response(
            status=200,
            body=LoyaltyItemsListForSnapshotResponse(
                items_list=[
                    LoyaltyItemsListForSnapshotResponse.LoyaltyItemForSnapshot(
                        client_id=111,
                        issued_at=dt("2020-01-01 00:00:00", as_proto=True),
                        id=1,
                        type="COUPON",
                        data='{"key1": "value1"}',
                    )
                ],
                paging=PagingOutput(limit=1000, offset=0, total=20),
            ).SerializeToString(),
        )

    mock_get_loyalty_items_list_for_snapshot(facade_handler)
    mock_get_loyalty_items_list_for_snapshot(facade_handler)
    mock_get_loyalty_items_list_for_snapshot(facade_handler)
    mock_get_loyalty_items_list_for_snapshot(
        Response(
            status=200,
            body=LoyaltyItemsListForSnapshotResponse(
                items_list=[], paging=PagingOutput(limit=1000, offset=0, total=20)
            ).SerializeToString(),
        )
    )

    async for _ in facade_client.get_loyalty_items_list_for_snapshot():
        pass

    assert request_bodies == [
        LoyaltyItemsListForSnapshotRequest(paging=Paging(limit=500, offset=0)),
        LoyaltyItemsListForSnapshotRequest(paging=Paging(limit=500, offset=500)),
        LoyaltyItemsListForSnapshotRequest(paging=Paging(limit=500, offset=1000)),
    ]


async def test_returns_coupon_items_list(
    facade_client, mock_get_loyalty_items_list_for_snapshot
):
    mock_get_loyalty_items_list_for_snapshot(
        Response(
            status=200,
            body=LoyaltyItemsListForSnapshotResponse(
                items_list=[
                    LoyaltyItemsListForSnapshotResponse.LoyaltyItemForSnapshot(
                        client_id=111,
                        issued_at=dt("2020-01-01 00:00:00", as_proto=True),
                        id=1,
                        type="COUPON",
                        data='{"key1": "value1"}',
                    ),
                    LoyaltyItemsListForSnapshotResponse.LoyaltyItemForSnapshot(
                        client_id=222,
                        issued_at=dt("2020-02-02 00:00:00", as_proto=True),
                        id=2,
                        type="COUPON",
                        data='{"key2": "value2"}',
                    ),
                ],
                paging=PagingOutput(limit=1000, offset=0, total=20),
            ).SerializeToString(),
        )
    )
    mock_get_loyalty_items_list_for_snapshot(
        Response(
            status=200,
            body=LoyaltyItemsListForSnapshotResponse(
                items_list=[
                    LoyaltyItemsListForSnapshotResponse.LoyaltyItemForSnapshot(
                        client_id=333,
                        issued_at=dt("2020-03-03 00:00:00", as_proto=True),
                        id=3,
                        type="COUPON",
                        data='{"key3": "value3"}',
                    )
                ],
                paging=PagingOutput(limit=1000, offset=0, total=20),
            ).SerializeToString(),
        )
    )
    mock_get_loyalty_items_list_for_snapshot(
        Response(
            status=200,
            body=LoyaltyItemsListForSnapshotResponse(
                items_list=[], paging=PagingOutput(limit=1000, offset=0, total=20)
            ).SerializeToString(),
        )
    )

    result = []
    async for coupon_items in facade_client.get_loyalty_items_list_for_snapshot():
        result.append(coupon_items)

    assert result == [
        [
            dict(
                client_id=111,
                issued_at=dt("2020-01-01 00:00:00"),
                id=1,
                type="COUPON",
                data={"key1": "value1"},
            ),
            dict(
                client_id=222,
                issued_at=dt("2020-02-02 00:00:00"),
                id=2,
                type="COUPON",
                data={"key2": "value2"},
            ),
        ],
        [
            dict(
                client_id=333,
                issued_at=dt("2020-03-03 00:00:00"),
                id=3,
                type="COUPON",
                data={"key3": "value3"},
            )
        ],
    ]


async def test_returns_empty_list_if_got_nothing(
    facade_client, mock_get_loyalty_items_list_for_snapshot
):
    mock_get_loyalty_items_list_for_snapshot(
        Response(
            status=200,
            body=LoyaltyItemsListForSnapshotResponse(
                items_list=[], paging=PagingOutput(limit=1000, offset=0, total=20)
            ).SerializeToString(),
        )
    )

    async for orgs_with_coupons in facade_client.get_loyalty_items_list_for_snapshot():
        assert orgs_with_coupons == []


async def test_raises_if_bad_data_field(
    facade_client, mock_get_loyalty_items_list_for_snapshot
):
    mock_get_loyalty_items_list_for_snapshot(
        Response(
            status=200,
            body=LoyaltyItemsListForSnapshotResponse(
                items_list=[
                    LoyaltyItemsListForSnapshotResponse.LoyaltyItemForSnapshot(
                        client_id=333,
                        issued_at=dt("2020-03-03 00:00:00", as_proto=True),
                        id=3,
                        type="COUPON",
                        data="bad-json",
                    )
                ],
                paging=PagingOutput(limit=1000, offset=0, total=20),
            ).SerializeToString(),
        )
    )

    with pytest.raises(ValidationException, match="data field must be json-string"):
        async for _ in facade_client.get_loyalty_items_list_for_snapshot():
            pass
