import pytest
from aiohttp.web import Response

from maps_adv.geosmb.clients.facade.proto.facade_pb2 import (
    OrganizationsWithCouponsRequest,
    OrganizationsWithCouponsResponse,
    Paging,
)

pytestmark = [pytest.mark.asyncio]


async def test_sends_correct_request(facade_client, mock_get_coupons_for_export):
    request_url = None
    request_headers = None
    request_body = None

    async def facade_handler(request):
        nonlocal request_url, request_headers, request_body
        request_url = str(request.url)
        request_headers = request.headers
        request_body = await request.read()
        return Response(
            status=200, body=OrganizationsWithCouponsResponse().SerializeToString()
        )

    mock_get_coupons_for_export(facade_handler)

    async for _ in facade_client.get_organizations_with_coupons():
        pass

    assert request_url == "http://facade.server/v1/get_organizations_with_coupons"
    assert request_headers["X-Ya-Service-Ticket"] == "KEK_FROM_AIOTVM_PYTEST_PLUGIN"
    assert request_headers["Content-Type"] == "application/x-protobuf"
    proto_body = OrganizationsWithCouponsRequest.FromString(request_body)
    assert proto_body == OrganizationsWithCouponsRequest(
        paging=Paging(limit=500, offset=0)
    )


async def test_paginates_in_request_correctly(
    facade_client, mock_get_coupons_for_export
):
    request_bodies = []

    async def facade_handler(request):
        nonlocal request_bodies
        request_body = await request.read()
        request_bodies.append(OrganizationsWithCouponsRequest.FromString(request_body))
        return Response(
            status=200,
            body=OrganizationsWithCouponsResponse(
                organizations_list=[
                    OrganizationsWithCouponsResponse.OrganizationWithCoupons(
                        biz_id="biz_id", permalink="permalink", showcase_url="url"
                    )
                ]
            ).SerializeToString(),
        )

    mock_get_coupons_for_export(facade_handler)
    mock_get_coupons_for_export(facade_handler)
    mock_get_coupons_for_export(facade_handler)
    mock_get_coupons_for_export(
        Response(
            status=200,
            body=OrganizationsWithCouponsResponse(
                organizations_list=[]
            ).SerializeToString(),
        )
    )

    async for _ in facade_client.get_organizations_with_coupons():
        pass

    assert request_bodies == [
        OrganizationsWithCouponsRequest(paging=Paging(limit=500, offset=0)),
        OrganizationsWithCouponsRequest(paging=Paging(limit=500, offset=500)),
        OrganizationsWithCouponsRequest(paging=Paging(limit=500, offset=1000)),
    ]


async def test_returns_list_of_orgs_with_coupons(
    facade_client, mock_get_coupons_for_export
):
    mock_get_coupons_for_export(
        Response(
            status=200,
            body=OrganizationsWithCouponsResponse(
                organizations_list=[
                    OrganizationsWithCouponsResponse.OrganizationWithCoupons(
                        biz_id="biz_id_0", permalink="1111111111", showcase_url="url_0"
                    ),
                    OrganizationsWithCouponsResponse.OrganizationWithCoupons(
                        biz_id="biz_id_1", permalink="2222222222", showcase_url="url_1"
                    ),
                ]
            ).SerializeToString(),
        )
    )
    mock_get_coupons_for_export(
        Response(
            status=200,
            body=OrganizationsWithCouponsResponse(
                organizations_list=[
                    OrganizationsWithCouponsResponse.OrganizationWithCoupons(
                        biz_id="biz_id_2", permalink="3333333333", showcase_url="url_2"
                    )
                ]
            ).SerializeToString(),
        )
    )
    mock_get_coupons_for_export(
        Response(
            status=200,
            body=OrganizationsWithCouponsResponse(
                organizations_list=[]
            ).SerializeToString(),
        )
    )

    result = []
    async for orgs_with_coupons in facade_client.get_organizations_with_coupons():
        result.append(orgs_with_coupons)

    assert result[0] == [
        dict(biz_id="biz_id_0", permalink="1111111111", showcase_url="url_0"),
        dict(biz_id="biz_id_1", permalink="2222222222", showcase_url="url_1"),
    ]
    assert result[1] == [
        dict(biz_id="biz_id_2", permalink="3333333333", showcase_url="url_2")
    ]


async def test_returns_empty_list_if_got_nothing(
    facade_client, mock_get_coupons_for_export
):
    mock_get_coupons_for_export(
        Response(
            status=200,
            body=OrganizationsWithCouponsResponse(
                organizations_list=[]
            ).SerializeToString(),
        )
    )

    async for orgs_with_coupons in facade_client.get_organizations_with_coupons():
        assert orgs_with_coupons == []
