import pytest

from maps_adv.geosmb.clients.facade import FacadeIntClient

pytest_plugins = ["aiohttp.pytest_plugin", "smb.common.aiotvm.pytest.plugin"]


@pytest.fixture
def mock_get_coupons_for_export(aresponses):
    return lambda h: aresponses.add(
        "facade.server", "/v1/get_organizations_with_coupons", "POST", h
    )


@pytest.fixture
def mock_get_org_with_booking_for_export(aresponses):
    return lambda h: aresponses.add(
        "facade.server", "/v1/get_organizations_with_booking", "POST", h
    )


@pytest.fixture
def mock_list_coupons(aresponses):
    return lambda *a: aresponses.add(
        "facade.server", "/v1/get_coupons_statuses_list", "POST", *a
    )


@pytest.fixture
def mock_get_loyalty_items_list_for_snapshot(aresponses):
    return lambda *a: aresponses.add(
        "facade.server", "/v1/get_loyalty_items_list_for_snapshot", "POST", *a
    )


@pytest.fixture
def mock_get_business_coupons_for_snapshot(aresponses):
    return lambda *a: aresponses.add(
        "facade.server", "/v1/get_business_coupons_for_snapshot", "POST", *a
    )


@pytest.fixture
def mock_fetch_coupon_promotions(aresponses):
    return lambda *a: aresponses.add(
        "facade.server", "/v1/fetch_coupon_promotions", "POST", *a
    )


@pytest.fixture
def mock_list_coupons_with_segments(aresponses):
    return lambda *a: aresponses.add(
        "facade.server", "/v1/get_coupons_with_segments", "POST", *a
    )


@pytest.fixture
async def facade_client(aiotvm):
    async with FacadeIntClient(
        url="http://facade.server", tvm=aiotvm, tvm_destination="facade-tvm-dest"
    ) as client:
        yield client
