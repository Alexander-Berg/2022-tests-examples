import pytest
from aiohttp.web import Response
from smb.common.http_client import UnknownResponse

from maps_adv.geosmb.clients.market import (
    MarketIntBadProto,
    MarketIntConflict,
    MarketIntNotFound,
    MarketIntUnknownError,
)
from maps_adv.geosmb.clients.market.lib.enums import (
    FilterServiceCategoriesOrderingFields,
    OrderType,
)
from maps_adv.geosmb.clients.market.proto.categories_pb2 import (
    FilterServiceCategoriesOrderingFields as FilterServiceCategoriesOrderingFieldsProto,
    FilterServiceCategoriesOrderingParameters,
    FilterServiceCategoriesParameters,
    ServiceCategory,
    ServiceCategoryList,
)
from maps_adv.geosmb.clients.market.proto.common_pb2 import (
    Error,
    OrderType as OrderTypeProto,
    PagingInputParameters,
    PagingOutputParameters,
)

pytestmark = [pytest.mark.asyncio]


@pytest.mark.parametrize(
    "ordering, proto_ordering",
    [
        (None, []),
        (
            [
                dict(
                    type=OrderType.ASC,
                    field=FilterServiceCategoriesOrderingFields.NAME,
                ),
            ],
            [
                dict(
                    type=OrderTypeProto.ASC,
                    field=FilterServiceCategoriesOrderingFieldsProto.NAME,
                ),
            ],
        ),
        (
            [
                dict(
                    type=OrderType.DESC,
                    field=FilterServiceCategoriesOrderingFields.NAME,
                ),
            ],
            [
                dict(
                    type=OrderTypeProto.DESC,
                    field=FilterServiceCategoriesOrderingFieldsProto.NAME,
                ),
            ],
        ),
    ],
)
@pytest.mark.parametrize("query", (None, "some query"))
async def test_sends_correct_bytes_request(
    ordering,
    proto_ordering,
    query,
    market_client,
    mock_filter_service_categories,
):
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
            body=ServiceCategoryList(
                paging=PagingOutputParameters(limit=0, offset=0, total=0),
                ordering=[],
                items=[],
            ).SerializeToString(),
        )

    mock_filter_service_categories(_handler)

    await market_client.fetch_service_categories(
        biz_id=123, ordering=ordering, query=query
    )

    assert request_url == "http://market.server/v1/filter_service_categories"
    assert request_headers["Content-Type"] == "application/x-protobuf"
    proto_body = FilterServiceCategoriesParameters.FromString(request_body)
    assert proto_body == FilterServiceCategoriesParameters(
        biz_id=123,
        paging=PagingInputParameters(limit=500, offset=0),
        query=query,
        ordering=proto_ordering,
    )


async def test_returns_service_categories_names(
    market_client, mock_filter_service_categories
):
    mock_filter_service_categories(
        Response(
            status=200,
            body=ServiceCategoryList(
                paging=PagingOutputParameters(limit=500, offset=0, total=600),
                ordering=[
                    FilterServiceCategoriesOrderingParameters(
                        type=OrderTypeProto.ASC,
                        field=FilterServiceCategoriesOrderingFieldsProto.NAME,
                    ),
                ],
                items=[
                    ServiceCategory(id=15, biz_id=22, name="category 1"),
                    ServiceCategory(id=16, biz_id=22, name="category 2"),
                ],
            ).SerializeToString(),
        )
    )
    mock_filter_service_categories(
        Response(
            status=200,
            body=ServiceCategoryList(
                paging=PagingOutputParameters(limit=500, offset=500, total=600),
                ordering=[
                    FilterServiceCategoriesOrderingParameters(
                        type=OrderTypeProto.ASC,
                        field=FilterServiceCategoriesOrderingFieldsProto.NAME,
                    ),
                ],
                items=[
                    ServiceCategory(id=17, biz_id=22, name="category 3"),
                    ServiceCategory(id=18, biz_id=22, name="category 4"),
                ],
            ).SerializeToString(),
        )
    )

    result = await market_client.fetch_service_categories(biz_id=123)

    assert result == {
        15: "category 1",
        16: "category 2",
        17: "category 3",
        18: "category 4",
    }


async def test_returns_empty_dict_if_no_categories(
    market_client, mock_filter_service_categories
):
    mock_filter_service_categories(
        Response(
            status=200,
            body=ServiceCategoryList(
                paging=PagingOutputParameters(limit=0, offset=0, total=0),
                ordering=[],
                items=[],
            ).SerializeToString(),
        )
    )

    result = await market_client.fetch_service_categories(biz_id=123)

    assert result == {}


async def test_paginates_in_request_correctly(
    market_client, mock_filter_service_categories
):
    request_bodies = []

    async def _handler(request):
        nonlocal request_bodies
        request_bodies.append(await request.read())
        return Response(
            status=200,
            body=ServiceCategoryList(
                paging=PagingOutputParameters(limit=500, offset=0, total=1500),
                ordering=[
                    FilterServiceCategoriesOrderingParameters(
                        type=OrderTypeProto.ASC,
                        field=FilterServiceCategoriesOrderingFieldsProto.NAME,
                    ),
                ],
                items=[
                    ServiceCategory(id=15, biz_id=22, name="category 1"),
                ],
            ).SerializeToString(),
        )

    for _ in range(3):
        mock_filter_service_categories(_handler)

    await market_client.fetch_service_categories(biz_id=123)

    proto_bodies = [
        FilterServiceCategoriesParameters.FromString(request_body)
        for request_body in request_bodies
    ]
    assert [pb.paging for pb in proto_bodies] == [
        PagingInputParameters(limit=500, offset=0),
        PagingInputParameters(limit=500, offset=500),
        PagingInputParameters(limit=500, offset=1000),
    ]


@pytest.mark.parametrize(
    "code, exception",
    [
        (Error.UNKNOWN, MarketIntUnknownError),
        (Error.NOT_FOUND, MarketIntNotFound),
        (Error.BAD_PROTO, MarketIntBadProto),
        (Error.CONFLICT, MarketIntConflict),
    ],
)
async def test_raises_for_known_error_response(
    code, exception, market_client, mock_filter_service_categories
):
    mock_filter_service_categories(
        Response(
            status=400,
            body=Error(code=code, message="some message").SerializeToString(),
        )
    )

    with pytest.raises(exception, match="some message"):
        await market_client.fetch_service_categories(biz_id=123)


async def test_raises_for_unknown_error_response(
    market_client, mock_filter_service_categories
):
    mock_filter_service_categories(Response(status=409))

    with pytest.raises(UnknownResponse):
        await market_client.fetch_service_categories(biz_id=123)
