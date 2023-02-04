from decimal import Decimal

import pytest

from maps_adv.geosmb.clients.geosearch import AddressComponent
from maps_adv.geosmb.landlord.server.lib.enums import LandingVersion
from maps_adv.geosmb.landlord.server.lib.exceptions import (
    NoDataForBizId,
    NoOrginfo,
    UnknownColorPreset,
)

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


@pytest.mark.parametrize("version", LandingVersion)
async def test_uses_dm(domain, dm, version):
    dm.fetch_biz_state.coro.return_value = {"slug": "fake", "published": True}

    dm.save_landing_data_for_biz_id.coro.return_value = {
        "landing_details": {
            "preferences": {"color_theme": {"preset": "RED"}, "cart_enabled": True},
            "contacts": {"geo": {"permalink": "54321"}},
            "permalink": "54321",
        }
    }

    result = await domain.update_landing_data_from_crm(
        biz_id=15,
        version=version,
        landing_details={
            "preferences": {"color_theme": {"preset": "RED"}, "cart_enabled": True},
            "contacts": {},
        },
    )

    dm.save_landing_data_for_biz_id.assert_called_with(
        biz_id=15,
        version=version,
        landing_data={
            "preferences": {"color_theme": {"preset": "RED"}, "cart_enabled": True},
            "contacts": {},
        },
    )
    assert result == {
        "landing_details": {
            "preferences": {"color_theme": {"preset": "RED"}, "cart_enabled": True},
            "contacts": {
                "geo": {
                    "address": "Город, Улица, 1",
                    "lat": Decimal("11.22"),
                    "lon": Decimal("22.33"),
                    "permalink": "54321",
                    "address_is_accurate": True,
                    "locality": "Город",
                    "postal_code": "1234567",
                    "country_code": "RU",
                    "address_region": "Область",
                    "street_address": "Улица, 1",
                },
            },
            "permalink": "54321",
        }
    }


async def test_requests_for_geosearch_data(domain, dm, geosearch):
    dm.fetch_biz_state.coro.return_value = {"slug": "fake", "published": True}

    dm.save_landing_data_for_biz_id.coro.return_value = {
        "landing_details": {
            "preferences": {"color_theme": {"preset": "RED"}},
            "contacts": {"geo": {"permalink": "54321"}},
            "permalink": "54321",
        }
    }

    await domain.update_landing_data_from_crm(
        biz_id=15,
        version=LandingVersion.STABLE,
        landing_details={
            "preferences": {"color_theme": {"preset": "RED"}},
            "contacts": {},
        },
    )

    geosearch.resolve_org.assert_called_with(permalink=54321)


async def test_updates_permalink_if_has_new_one(domain, dm, geosearch):
    dm.fetch_biz_state.coro.return_value = {"slug": "fake", "published": True}

    dm.save_landing_data_for_biz_id.coro.return_value = {
        "landing_details": {
            "preferences": {"color_theme": {"preset": "RED"}},
            "contacts": {"geo": {"permalink": "67893"}},
            "permalink": "54321",
        }
    }
    geosearch.resolve_org.coro.return_value.permalink = "124345"

    await domain.update_landing_data_from_crm(
        biz_id=15,
        version=LandingVersion.STABLE,
        landing_details={
            "preferences": {"color_theme": {"preset": "RED"}},
            "contacts": {},
        },
    )

    dm.update_biz_state_permalink.assert_called_with(biz_id=15, permalink="124345")


async def test_enriches_result_with_fresh_geosearch_data(domain, dm, geosearch):
    dm.fetch_biz_state.coro.return_value = {"slug": "fake", "published": True}

    dm.save_landing_data_for_biz_id.coro.return_value = {
        "landing_details": {
            "preferences": {"color_theme": {"preset": "RED"}},
            "contacts": {"geo": {"permalink": "54321"}},
            "permalink": "54321",
        }
    }
    geosearch.resolve_org.coro.return_value.latitude = Decimal("5")
    geosearch.resolve_org.coro.return_value.longitude = Decimal("6")
    geosearch.resolve_org.coro.return_value.formatted_address = "Сама классная улица, 5"
    geosearch.resolve_org.coro.return_value.permalink = "124345"

    result = await domain.update_landing_data_from_crm(
        biz_id=15,
        version=LandingVersion.STABLE,
        landing_details={
            "preferences": {"color_theme": {"preset": "RED"}},
            "contacts": {},
        },
    )

    assert result == {
        "landing_details": {
            "preferences": {"color_theme": {"preset": "RED"}},
            "contacts": {
                "geo": {
                    "address": "Сама классная улица, 5",
                    "lat": Decimal("5"),
                    "lon": Decimal("6"),
                    "permalink": "124345",
                    "address_is_accurate": True,
                    "locality": "Город",
                    "postal_code": "1234567",
                    "country_code": "RU",
                    "address_region": "Область",
                    "street_address": "Улица, 1",
                }
            },
            "permalink": "124345",
        }
    }


@pytest.mark.parametrize(
    "address_component", [ac for ac in AddressComponent if ac != AddressComponent.HOUSE]
)
async def test_considers_address_inaccurate_if_house_not_in_address_components(
    address_component, geosearch, domain, dm
):
    dm.fetch_biz_state.coro.return_value = {"slug": "fake", "published": True}

    geosearch.resolve_org.coro.return_value.address_components = {
        address_component: "value"
    }
    dm.save_landing_data_for_biz_id.coro.return_value = {
        "landing_details": {
            "preferences": {"color_theme": {"preset": "RED"}},
            "contacts": {"geo": {"permalink": "54321"}},
            "permalink": "54321",
        }
    }

    result = await domain.update_landing_data_from_crm(
        biz_id=15,
        version=LandingVersion.STABLE,
        landing_details={
            "preferences": {"color_theme": {"preset": "RED"}},
            "contacts": {},
        },
    )

    landing_details = result["landing_details"]
    assert landing_details["contacts"]["geo"]["address_is_accurate"] is False


async def test_considers_address_accurate_if_house_in_address_components(
    geosearch, domain, dm
):
    dm.fetch_biz_state.coro.return_value = {"slug": "fake", "published": True}

    geosearch.resolve_org.coro.return_value.address_components = {
        AddressComponent.HOUSE: "value"
    }
    dm.save_landing_data_for_biz_id.coro.return_value = {
        "landing_details": {
            "preferences": {"color_theme": {"preset": "RED"}},
            "contacts": {"geo": {"permalink": "54321"}},
            "permalink": "54321",
        }
    }

    result = await domain.update_landing_data_from_crm(
        biz_id=15,
        version=LandingVersion.STABLE,
        landing_details={
            "preferences": {"color_theme": {"preset": "RED"}},
            "contacts": {},
        },
    )

    landing_details = result["landing_details"]
    assert landing_details["contacts"]["geo"]["address_is_accurate"] is True


async def test_returns_locality_if_locality_in_geosearch_response(
    geosearch, domain, dm
):
    dm.fetch_biz_state.coro.return_value = {"slug": "fake", "published": True}

    geosearch.resolve_org.coro.return_value.address_components = {
        AddressComponent.LOCALITY: "Москва",
    }
    dm.save_landing_data_for_biz_id.coro.return_value = {
        "landing_details": {
            "preferences": {"color_theme": {"preset": "RED"}},
            "contacts": {"geo": {"permalink": "54321"}},
            "permalink": "54321",
        }
    }

    result = await domain.update_landing_data_from_crm(
        biz_id=15,
        version=LandingVersion.STABLE,
        landing_details={
            "preferences": {"color_theme": {"preset": "RED"}},
            "contacts": {},
        },
    )

    landing_details = result["landing_details"]
    assert landing_details["contacts"]["geo"]["locality"] == "Москва"


@pytest.mark.parametrize(
    "address_component",
    [ac for ac in AddressComponent if ac != AddressComponent.LOCALITY],
)
async def test_does_not_return_locality_if_no_locality_in_geosearch_response(
    address_component, geosearch, domain, dm
):
    dm.fetch_biz_state.coro.return_value = {"slug": "fake", "published": True}

    geosearch.resolve_org.coro.return_value.address_components = {
        address_component: "value"
    }
    dm.save_landing_data_for_biz_id.coro.return_value = {
        "landing_details": {
            "preferences": {"color_theme": {"preset": "RED"}},
            "contacts": {"geo": {"permalink": "54321"}},
            "permalink": "54321",
        }
    }

    result = await domain.update_landing_data_from_crm(
        biz_id=15,
        version=LandingVersion.STABLE,
        landing_details={
            "preferences": {"color_theme": {"preset": "RED"}},
            "contacts": {},
        },
    )

    landing_details = result["landing_details"]
    assert "locality" not in landing_details["contacts"]["geo"]


@pytest.mark.parametrize("version", list(LandingVersion))
async def test_propagates_exception(domain, dm, version):
    dm.save_landing_data_for_biz_id.coro.side_effect = NoDataForBizId

    with pytest.raises(NoDataForBizId):
        await domain.update_landing_data_from_crm(
            biz_id=15,
            version=version,
            landing_details={
                "preferences": {"color_theme": {"preset": "RED"}},
                "contacts": {},
            },
        )


async def test_raises_for_unknown_color_preset(domain):
    with pytest.raises(UnknownColorPreset, match="GOVENIY"):
        await domain.update_landing_data_from_crm(
            biz_id=15,
            version=LandingVersion.STABLE,
            landing_details={
                "preferences": {"color_theme": {"preset": "GOVENIY"}},
                "contacts": {},
            },
        )


async def test_raises_if_no_org_info(geosearch, domain, dm):
    dm.save_landing_data_for_biz_id.coro.return_value = {
        "landing_details": {
            "preferences": {"color_theme": {"preset": "RED"}},
            "contacts": {"geo": {"permalink": "54321"}},
            "permalink": "54321",
        }
    }
    geosearch.resolve_org.coro.return_value = None

    with pytest.raises(NoOrginfo):
        await domain.update_landing_data_from_crm(
            biz_id=15,
            version=LandingVersion.STABLE,
            landing_details={
                "preferences": {"color_theme": {"preset": "RED"}},
                "contacts": {},
            },
        )
