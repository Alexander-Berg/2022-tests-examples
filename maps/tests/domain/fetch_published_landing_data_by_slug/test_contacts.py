import logging
from unittest import mock

import pytest

from maps_adv.geosmb.clients.geosearch import AddressComponent
from maps_adv.geosmb.landlord.server.lib.enums import LandingVersion

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


@pytest.fixture(autouse=True)
def caplog_set_level_error(caplog):
    caplog.set_level(logging.ERROR)


@pytest.mark.parametrize(
    "address_component", [ac for ac in AddressComponent if ac != AddressComponent.HOUSE]
)
async def test_considers_address_inaccurate_if_house_not_in_address_components(
    address_component, geosearch, domain
):
    geosearch.resolve_org.coro.return_value.address_components = {
        address_component: "value"
    }

    result = await domain.fetch_published_landing_data_by_slug(
        slug="cafe", token="fetch_data_token", version=LandingVersion.STABLE
    )

    assert result["contacts"]["geo"]["address_is_accurate"] is False


async def test_considers_address_accurate_if_house_in_address_components(
    geosearch, domain
):
    geosearch.resolve_org.coro.return_value.address_components = {
        AddressComponent.HOUSE: "value"
    }

    result = await domain.fetch_published_landing_data_by_slug(
        slug="cafe", token="fetch_data_token", version=LandingVersion.STABLE
    )

    assert result["contacts"]["geo"]["address_is_accurate"] is True


async def test_returns_locality_if_locality_in_geosearch_response(geosearch, domain):
    geosearch.resolve_org.coro.return_value.address_components = {
        AddressComponent.LOCALITY: "Москва",
    }

    result = await domain.fetch_published_landing_data_by_slug(
        slug="cafe", token="fetch_data_token", version=LandingVersion.STABLE
    )

    assert result["contacts"]["geo"]["locality"] == "Москва"


@pytest.mark.parametrize(
    "address_component",
    [ac for ac in AddressComponent if ac != AddressComponent.LOCALITY],
)
async def test_does_not_return_locality_if_no_locality_in_geosearch_response(
    address_component, geosearch, domain
):
    geosearch.resolve_org.coro.return_value.address_components = {
        address_component: "value"
    }

    result = await domain.fetch_published_landing_data_by_slug(
        slug="cafe", token="fetch_data_token", version=LandingVersion.STABLE
    )

    assert "locality" not in result["contacts"]["geo"]


async def test_returns_sub_phone_in_contacts_if_has_one(dm, domain):
    dm.fetch_substitution_phone.coro.return_value = "+7 (800) 200-06-00"

    result = await domain.fetch_published_landing_data_by_slug(
        slug="cafe", token="fetch_data_token", version=LandingVersion.STABLE
    )

    assert result["contacts"]["phone"] == "+7 (800) 200-06-00"
    assert result["contacts"]["is_substitution_phone"] is True


async def test_returns_saved_phone_in_contacts_if_has_no_sub_phone(dm, domain):
    dm.fetch_substitution_phone.coro.return_value = None

    result = await domain.fetch_published_landing_data_by_slug(
        slug="cafe", token="fetch_data_token", version=LandingVersion.STABLE
    )

    assert result["contacts"]["phone"] == "+7 (495) 739-70-00"
    assert result["contacts"]["is_substitution_phone"] is False


async def test_does_not_return_service_area_if_no_service_area_in_geosearch_response(
    geosearch, domain
):
    geosearch.resolve_org.coro.return_value.service_area = None

    result = await domain.fetch_published_landing_data_by_slug(
        slug="cafe", token="fetch_data_token", version=LandingVersion.STABLE
    )

    assert "service_area" not in result["contacts"]["geo"]


async def test_returns_service_radius_if_service_area_is_circle(
    geosearch, domain
):
    geosearch.resolve_org.coro.return_value.service_area = {
        "service_radius_km": 700
    }

    result = await domain.fetch_published_landing_data_by_slug(
        slug="cafe", token="fetch_data_token", version=LandingVersion.STABLE
    )

    assert result["contacts"]["geo"]["service_area"] == dict(service_radius_km=700)


async def test_does_not_request_geobase_if_service_area_is_circle(
    geosearch, geobase_client, domain
):
    geosearch.resolve_org.coro.return_value.service_area = {
        "service_radius_km": 700
    }

    await domain.fetch_published_landing_data_by_slug(
        slug="cafe", token="fetch_data_token", version=LandingVersion.STABLE
    )

    geobase_client.fetch_linguistics_for_region.assert_not_called()


async def test_requests_geobase_for_details_if_service_area_is_regions(
    geosearch, domain, geobase_client
):
    geosearch.resolve_org.coro.return_value.service_area = {
        "geo_ids": [39, 11029]
    }

    await domain.fetch_published_landing_data_by_slug(
        slug="cafe", token="fetch_data_token", version=LandingVersion.STABLE
    )

    geobase_client.fetch_linguistics_for_region.assert_has_calls(
        [mock.call(geo_id=39), mock.call(geo_id=11029)],
        any_order=True,
    )


async def test_returns_service_regions_if_service_area_is_regions(
    geosearch, domain, geobase_client
):
    geosearch.resolve_org.coro.return_value.service_area = {
        "geo_ids": [39, 11029]
    }

    result = await domain.fetch_published_landing_data_by_slug(
        slug="cafe", token="fetch_data_token", version=LandingVersion.STABLE
    )

    assert result["contacts"]["geo"]["service_area"] == dict(
        area=dict(
            regions=[
                dict(preposition="в", prepositional_case="Ростове-на-Дону"),
                dict(preposition="в", prepositional_case="Ростовской области"),
            ]
        )
    )


async def test_does_not_return_failed_linguistics(
    geosearch, domain, geobase_client
):
    geosearch.resolve_org.coro.return_value.service_area = {
        "geo_ids": [39, 11029]
    }
    geobase_client.fetch_linguistics_for_region.coro.side_effect = [
        Exception,
        {"preposition": "в", "prepositional_case": "Ростове-на-Дону"},
    ]

    result = await domain.fetch_published_landing_data_by_slug(
        slug="cafe", token="fetch_data_token", version=LandingVersion.STABLE
    )

    assert result["contacts"]["geo"]["service_area"] == dict(
        area=dict(regions=[dict(preposition="в", prepositional_case="Ростове-на-Дону")])
    )


async def test_logs_geobase_exception(geosearch, domain, geobase_client, caplog):
    geosearch.resolve_org.coro.return_value.service_area = {
        "geo_ids": [39, 11029]
    }
    geobase_client.fetch_linguistics_for_region.side_effect = Exception

    await domain.fetch_published_landing_data_by_slug(
        slug="cafe", token="fetch_data_token", version=LandingVersion.STABLE
    )

    assert (
        "Failed to fetch region name from GeoBase for geo_id 39 (permalink 54321)"
        in caplog.messages
    )
    assert (
        "Failed to fetch region name from GeoBase for geo_id 11029 (permalink 54321)"
        in caplog.messages
    )
