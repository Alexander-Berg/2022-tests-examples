import pytest
from aiohttp.web import Response
from smb.common.http_client import UnknownResponse

from maps_adv.geosmb.clients.market import (
    MarketIntBadProto,
    MarketIntConflict,
    MarketIntNotFound,
    MarketIntUnknownError,
    ServiceStatus,
)
from maps_adv.geosmb.clients.market.lib.enums import (
    ClientAction,
    FilterServiceOrderingFields,
    OrderType,
    PriceType,
)
from maps_adv.geosmb.clients.market.proto.common_pb2 import (
    Error,
    Money,
    OrderType as OrderTypeProto,
    PagingInputParameters,
    PagingOutputParameters,
)
from maps_adv.geosmb.clients.market.proto.services_pb2 import (
    ClientAction as ClientActionProto,
    FilterServiceOrderingFields as FilterServiceOrderingFieldsProto,
    FilterServiceOrderingParameters,
    FilterServicesFilteringParameters,
    FilterServicesParameters,
    PriceType as PriceTypeProto,
    ServiceLevelPrice,
    ServiceList,
    ServiceListItem,
    ServicePrice,
    ServiceSchedulePrices,
    ServiceStatus as ServiceStatusProto,
)

pytestmark = [pytest.mark.asyncio]


@pytest.mark.parametrize(
    "ordering, proto_ordering",
    [
        (None, []),
        (
            [
                dict(type=OrderType.ASC, field=FilterServiceOrderingFields.STATUS),
                dict(type=OrderType.DESC, field=FilterServiceOrderingFields.PRICE_MIN),
                dict(type=OrderType.ASC, field=FilterServiceOrderingFields.UPDATED_AT),
                dict(
                    type=OrderType.DESC,
                    field=FilterServiceOrderingFields.DURATION_MINUTES,
                ),
                dict(type=OrderType.ASC, field=FilterServiceOrderingFields.NAME),
            ],
            [
                FilterServiceOrderingParameters(
                    type=OrderTypeProto.ASC,
                    field=FilterServiceOrderingFieldsProto.STATUS,
                ),
                FilterServiceOrderingParameters(
                    type=OrderTypeProto.DESC,
                    field=FilterServiceOrderingFieldsProto.PRICE_MIN,
                ),
                FilterServiceOrderingParameters(
                    type=OrderTypeProto.ASC,
                    field=FilterServiceOrderingFieldsProto.UPDATED_AT,
                ),
                FilterServiceOrderingParameters(
                    type=OrderTypeProto.DESC,
                    field=FilterServiceOrderingFieldsProto.DURATION_MINUTES,
                ),
                FilterServiceOrderingParameters(
                    type=OrderTypeProto.ASC, field=FilterServiceOrderingFieldsProto.NAME
                ),
            ],
        ),
    ],
)
@pytest.mark.parametrize(
    "filtering, proto_filtering",
    [
        (None, None),
        (
            dict(
                query="something",
                category_ids=[1500, 1600, 1700],
                statuses=[ServiceStatus.DELETED, ServiceStatus.PUBLISHED],
                include_without_categories=True,
            ),
            FilterServicesFilteringParameters(
                query="something",
                category_ids=[1500, 1600, 1700],
                statuses=[ServiceStatusProto.DELETED, ServiceStatusProto.PUBLISHED],
                include_without_categories=True,
            ),
        ),
        (
            dict(
                category_ids=[],
                statuses=[],
            ),
            FilterServicesFilteringParameters(
                category_ids=[],
                statuses=[],
            ),
        ),
    ],
)
async def test_sends_correct_bytes_request(
    ordering,
    proto_ordering,
    filtering,
    proto_filtering,
    market_client,
    mock_filter_services,
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
            body=ServiceList(
                paging=PagingOutputParameters(limit=0, offset=0, total=0),
                ordering=[],
                items=[],
            ).SerializeToString(),
        )

    mock_filter_services(_handler)

    await market_client.fetch_services(
        biz_id=123, ordering=ordering, filtering=filtering
    )

    assert request_url == "http://market.server/v1/filter_services"
    assert request_headers["Content-Type"] == "application/x-protobuf"
    proto_body = FilterServicesParameters.FromString(request_body)
    assert proto_body == FilterServicesParameters(
        biz_id=123,
        paging=PagingInputParameters(limit=500, offset=0),
        ordering=proto_ordering,
        filtering=proto_filtering,
    )


async def test_returns_services_details(market_client, mock_filter_services):
    # with optional fields
    mock_filter_services(
        Response(
            status=200,
            body=ServiceList(
                paging=PagingOutputParameters(limit=500, offset=0, total=600),
                ordering=[
                    FilterServiceOrderingParameters(
                        type=OrderTypeProto.ASC,
                        field=FilterServiceOrderingFieldsProto.STATUS,
                    ),
                ],
                items=[
                    ServiceListItem(
                        id=i,
                        biz_id=123,
                        name="any_name",
                        status=ServiceStatusProto.DELETED,
                        category_ids=[1200, 1300],
                        min_price=Money(value="15.99", currency="RUB"),
                        duration_minutes=60,
                        main_image_url_template="any_template/%s",
                        updated_at="2020-08-20T16:30:00",
                        description="any description",
                        service_prices=[
                            ServicePrice(
                                service_id=200,
                                price=Money(value="25.99", currency="RUB"),
                                duration_minutes=600,
                                schedule=ServiceSchedulePrices(schedule_id=12),
                            )
                        ],
                        price_type=PriceTypeProto.UNKNOWN,
                        client_action=ClientActionProto.LINK,
                        client_action_settings=dict(
                            link=dict(
                                link="https://ya.ru",
                                button_text="ссылка"
                            )
                        ),
                    )
                    for i in (100, 200)
                ],
            ).SerializeToString(),
        )
    )
    # without optional fields
    mock_filter_services(
        Response(
            status=200,
            body=ServiceList(
                paging=PagingOutputParameters(limit=500, offset=500, total=600),
                ordering=[
                    FilterServiceOrderingParameters(
                        type=OrderTypeProto.ASC,
                        field=FilterServiceOrderingFieldsProto.STATUS,
                    ),
                ],
                items=[
                    ServiceListItem(
                        id=300,
                        biz_id=124,
                        name="any_other_name",
                        status=ServiceStatusProto.PUBLISHED,
                        category_ids=[1201, 1301],
                        duration_minutes=15,
                        updated_at="2020-08-20T15:30:00",
                        service_prices=[
                            ServicePrice(
                                service_id=300,
                                price=Money(value="6.99", currency="USD"),
                                price_level=ServiceLevelPrice(level=12),
                            )
                        ],
                        price_type=PriceTypeProto.UNKNOWN,
                    )
                ],
            ).SerializeToString(),
        )
    )

    result = await market_client.fetch_services(biz_id=123)

    assert result == [
        dict(
            item_id=100,
            biz_id=123,
            name="any_name",
            status=ServiceStatus.DELETED,
            category_ids=[1200, 1300],
            min_price=dict(value="15.99", currency="RUB"),
            duration_minutes=60,
            main_image_url_template="any_template/%s",
            updated_at="2020-08-20T16:30:00",
            description="any description",
            service_prices=[
                dict(
                    service_id=200,
                    price=dict(value="25.99", currency="RUB"),
                    duration_minutes=600,
                    schedule=dict(schedule_id=12),
                ),
            ],
            price_type=PriceType.UNKNOWN,
            client_action=ClientAction.LINK,
            client_action_settings=dict(
                link=dict(
                    link="https://ya.ru",
                    button_text="ссылка"
                )
            ),
        ),
        dict(
            item_id=200,
            biz_id=123,
            name="any_name",
            status=ServiceStatus.DELETED,
            category_ids=[1200, 1300],
            min_price=dict(value="15.99", currency="RUB"),
            duration_minutes=60,
            main_image_url_template="any_template/%s",
            updated_at="2020-08-20T16:30:00",
            description="any description",
            service_prices=[
                dict(
                    service_id=200,
                    price=dict(value="25.99", currency="RUB"),
                    duration_minutes=600,
                    schedule=dict(schedule_id=12),
                ),
            ],
            price_type=PriceType.UNKNOWN,
            client_action=ClientAction.LINK,
            client_action_settings=dict(
                link=dict(
                    link="https://ya.ru",
                    button_text="ссылка"
                )
            ),
        ),
        dict(
            item_id=300,
            biz_id=124,
            name="any_other_name",
            status=ServiceStatus.PUBLISHED,
            category_ids=[1201, 1301],
            duration_minutes=15,
            updated_at="2020-08-20T15:30:00",
            service_prices=[
                dict(
                    service_id=300,
                    price=dict(value="6.99", currency="USD"),
                    price_level=dict(level=12),
                ),
            ],
            price_type=PriceType.UNKNOWN,
        ),
    ]


async def test_minimal_price_without_value_treated_as_empty(
    market_client, mock_filter_services
):
    mock_filter_services(
        Response(
            status=200,
            body=ServiceList(
                paging=PagingOutputParameters(limit=1, offset=0, total=1),
                ordering=[
                    FilterServiceOrderingParameters(
                        type=OrderTypeProto.ASC,
                        field=FilterServiceOrderingFieldsProto.STATUS,
                    ),
                ],
                items=[
                    ServiceListItem(
                        id=100,
                        biz_id=123,
                        name="any_name",
                        status=ServiceStatusProto.DELETED,
                        category_ids=[1200, 1300],
                        min_price=Money(value="", currency=""),
                        duration_minutes=60,
                        main_image_url_template="any_template/%s",
                        updated_at="2020-08-20T16:30:00",
                        description="any description",
                        service_prices=[
                            ServicePrice(
                                service_id=200,
                                price=Money(value="25.99", currency="RUB"),
                                duration_minutes=600,
                                schedule=ServiceSchedulePrices(schedule_id=12),
                            )
                        ],
                        price_type=PriceTypeProto.UNKNOWN,
                    )
                ],
            ).SerializeToString(),
        )
    )

    result = await market_client.fetch_services(biz_id=123)

    assert result == [
        dict(
            item_id=100,
            biz_id=123,
            name="any_name",
            status=ServiceStatus.DELETED,
            category_ids=[1200, 1300],
            duration_minutes=60,
            main_image_url_template="any_template/%s",
            updated_at="2020-08-20T16:30:00",
            description="any description",
            service_prices=[
                dict(
                    service_id=200,
                    price=dict(value="25.99", currency="RUB"),
                    duration_minutes=600,
                    schedule=dict(schedule_id=12),
                ),
            ],
            price_type=PriceType.UNKNOWN,
        ),
    ]


async def test_may_return_service_without_prices(market_client, mock_filter_services):
    mock_filter_services(
        Response(
            status=200,
            body=ServiceList(
                paging=PagingOutputParameters(limit=1, offset=0, total=1),
                ordering=[
                    FilterServiceOrderingParameters(
                        type=OrderTypeProto.ASC,
                        field=FilterServiceOrderingFieldsProto.STATUS,
                    ),
                ],
                items=[
                    ServiceListItem(
                        id=100,
                        biz_id=123,
                        name="any_name",
                        status=ServiceStatusProto.DELETED,
                        category_ids=[1200, 1300],
                        main_image_url_template="any_template/%s",
                        updated_at="2020-08-20T16:30:00",
                        description="any description",
                        service_prices=[],
                        price_type=PriceTypeProto.UNKNOWN,
                    )
                ],
            ).SerializeToString(),
        )
    )

    result = await market_client.fetch_services(biz_id=123)

    assert result == [
        dict(
            item_id=100,
            biz_id=123,
            name="any_name",
            status=ServiceStatus.DELETED,
            category_ids=[1200, 1300],
            main_image_url_template="any_template/%s",
            updated_at="2020-08-20T16:30:00",
            description="any description",
            service_prices=[],
            price_type=PriceType.UNKNOWN,
        ),
    ]


async def test_returns_empty_list_if_no_services(market_client, mock_filter_services):
    mock_filter_services(
        Response(
            status=200,
            body=ServiceList(
                paging=PagingOutputParameters(limit=0, offset=0, total=0),
                ordering=[],
                items=[],
            ).SerializeToString(),
        )
    )

    result = await market_client.fetch_services(biz_id=123)

    assert result == []


async def test_paginates_in_request_correctly(market_client, mock_filter_services):
    request_bodies = []

    async def _handler(request):
        nonlocal request_bodies
        request_bodies.append(await request.read())
        return Response(
            status=200,
            body=ServiceList(
                paging=PagingOutputParameters(limit=500, offset=0, total=1500),
                ordering=[],
                items=[
                    ServiceListItem(
                        id=300,
                        biz_id=124,
                        name="any_other_name",
                        status=ServiceStatusProto.PUBLISHED,
                        category_ids=[1201, 1301],
                        min_price=Money(value="5.99", currency="USD"),
                        duration_minutes=15,
                        updated_at="2020-08-20T15:30:00",
                        service_prices=[],
                        price_type=PriceTypeProto.UNKNOWN,
                    )
                ],
            ).SerializeToString(),
        )

    for _ in range(3):
        mock_filter_services(_handler)

    await market_client.fetch_services(biz_id=123)

    proto_bodies = [
        FilterServicesParameters.FromString(request_body)
        for request_body in request_bodies
    ]
    assert [pb.paging for pb in proto_bodies] == [
        PagingInputParameters(limit=500, offset=0),
        PagingInputParameters(limit=500, offset=500),
        PagingInputParameters(limit=500, offset=1000),
    ]


@pytest.mark.parametrize(
    "main_image_url_template",
    [
        "http://avatars.server/get-crm/322/abc",
        "http://avatars.server/get-crm/322/abc/%s",
    ],
)
async def test_appends_postfix_to_image_urls_if_omitted(
    market_client, mock_filter_services, main_image_url_template
):
    mock_filter_services(
        Response(
            status=200,
            body=ServiceList(
                paging=PagingOutputParameters(limit=1, offset=0, total=1),
                ordering=[
                    FilterServiceOrderingParameters(
                        type=OrderTypeProto.ASC,
                        field=FilterServiceOrderingFieldsProto.STATUS,
                    ),
                ],
                items=[
                    ServiceListItem(
                        id=i,
                        biz_id=123,
                        name="any_name",
                        status=ServiceStatusProto.DELETED,
                        category_ids=[1200, 1300],
                        min_price=Money(value="15.99", currency="RUB"),
                        duration_minutes=60,
                        main_image_url_template=main_image_url_template,
                        updated_at="2020-08-20T16:30:00",
                        description="any description",
                        service_prices=[
                            ServicePrice(
                                service_id=200,
                                price=Money(value="25.99", currency="RUB"),
                                duration_minutes=600,
                                schedule=ServiceSchedulePrices(schedule_id=12),
                            )
                        ],
                        price_type=PriceTypeProto.UNKNOWN,
                    )
                    for i in (100, 200)
                ],
            ).SerializeToString(),
        )
    )

    result = await market_client.fetch_services(biz_id=123)

    assert (
        result[0]["main_image_url_template"]
        == "http://avatars.server/get-crm/322/abc/%s"
    )


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
    code, exception, market_client, mock_filter_services
):
    mock_filter_services(
        Response(
            status=400,
            body=Error(code=code, message="some message").SerializeToString(),
        )
    )

    with pytest.raises(exception, match="some message"):
        await market_client.fetch_services(biz_id=123)


async def test_raises_for_unknown_error_response(market_client, mock_filter_services):
    mock_filter_services(Response(status=409))

    with pytest.raises(UnknownResponse):
        await market_client.fetch_services(biz_id=123)
