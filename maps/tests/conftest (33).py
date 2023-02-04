import pytest

from maps_adv.geosmb.clients.loyalty import LoyaltyIntClient

pytest_plugins = ["aiohttp.pytest_plugin", "smb.common.aiotvm.pytest.plugin"]


@pytest.fixture
def mock_confirm_coupons_sent_to_review(aresponses):
    return lambda *a: aresponses.add(
        "loyalty.server", "/v0/confirm_coupons_sent_to_moderation", "POST", *a
    )


@pytest.fixture
def mock_submit_coupons_reviews_list(aresponses):
    return lambda h: aresponses.add(
        "loyalty.server", "/v0/submit_coupons_reviews_list", "POST", h
    )


@pytest.fixture
def mock_get_coupons_list_for_review(aresponses):
    return lambda *a: aresponses.add(
        "loyalty.server", "/v0/get_coupons_list_for_review", "POST", *a
    )


@pytest.fixture
async def loyalty_client(aiotvm):
    async with LoyaltyIntClient(
        url="http://loyalty.server", tvm=aiotvm, tvm_destination="loyalty-tvm-dest"
    ) as client:
        yield client
