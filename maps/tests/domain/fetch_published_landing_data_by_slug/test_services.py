import pytest

from maps_adv.geosmb.clients.market import (
    BaseHttpClientException,
    ClientAction,
    MarketIntBadProto,
    MarketIntConflict,
    MarketIntNotFound,
    MarketIntUnknownError,
)
from maps_adv.geosmb.landlord.server.lib.enums import LandingVersion, ServiceItemType

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


# services are fully tested in service provider
async def test_returns_services_as_expected(domain):
    result = await domain.fetch_published_landing_data_by_slug(
        slug="cafe", token="fetch_data_token", version=LandingVersion.STABLE
    )

    assert result["services"] == dict(
        items=[
            dict(
                type=ServiceItemType.MARKET,
                id=200,
                name="Стрижка",
                image="https://strizhka.url/pic",
                description="Подстрижём под горшок профессионально",
                categories=["Классно выйдет", "Вообще топовая услуга"],
                min_cost="15.99",
                min_duration=30,
                action_type=ClientAction.LINK,
                service_action_type_settings=dict(
                    link=dict(
                        link="https://ya.ru",
                        button_text="ссылка"
                    )
                ),
            ),
            dict(
                type=ServiceItemType.MARKET,
                id=300,
                name="Стрижка собаки",
                description="Ты чо пёс?",
                categories=["Вообще топовая услуга", "Популярное"],
            ),
            dict(
                type=ServiceItemType.MARKET,
                id=400,
                name="Стрижка кота",
                categories=["Популярное", "Классно выйдет"],
                cost="25.99",
                duration=None,
            ),
        ]
    )


# services are fully tested in service provider
async def test_does_not_return_services_if_no_services(domain, market_int):
    market_int.fetch_services.coro.return_value = []

    result = await domain.fetch_published_landing_data_by_slug(
        slug="cafe", token="fetch_data_token", version=LandingVersion.STABLE
    )

    assert "services" not in result


@pytest.mark.parametrize(
    "exception",
    [
        MarketIntNotFound,
        MarketIntUnknownError,
        MarketIntBadProto,
        MarketIntConflict,
        BaseHttpClientException,
    ],
)
# services are fully tested in service provider
async def test_does_not_return_services_if_market_errored_on_fetching_services(
    exception, domain, market_int
):
    market_int.fetch_services.coro.side_effect = exception

    try:
        result = await domain.fetch_published_landing_data_by_slug(
            slug="cafe", token="fetch_data_token", version=LandingVersion.STABLE
        )
    except Exception:
        pytest.fail("Should not propagate exception from market_int client")

    assert "services" not in result
