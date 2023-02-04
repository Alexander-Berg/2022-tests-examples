from decimal import Decimal

import pytest

from maps_adv.geosmb.clients.geosearch import AddressComponent
from maps_adv.geosmb.landlord.server.lib.enums import LandingVersion
from maps_adv.geosmb.landlord.server.lib.exceptions import (
    NoDataForBizId,
    NoOrginfo,
    VersionDoesNotExist,
)

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


@pytest.mark.parametrize("version", list(LandingVersion))
async def test_returns_enriched_landing_data(domain, dm, version):
    dm.fetch_landing_data_for_crm.coro.return_value = {
        "some": "data",
        "landing_details": {
            "contacts": {"geo": {"permalink": "54321"}},
            "permalink": "54321",
        },
    }

    result = await domain.fetch_landing_data_for_crm(biz_id=15, version=version)

    assert result == {
        "some": "data",
        "landing_details": {
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
                }
            },
            "permalink": "54321",
        },
    }


@pytest.mark.parametrize("version", list(LandingVersion))
async def test_returns_enriched_landing_data_without_geo(geosearch, domain, dm, version):
    geosearch.resolve_org.coro.return_value = None
    dm.fetch_landing_data_for_crm.coro.return_value = {
        "some": "data",
        "landing_details": {
            "contacts": {"geo": {"permalink": "54321"}},
            "permalink": "54321",
            "is_updated_from_geosearch": True,
        },
    }

    result = await domain.fetch_landing_data_for_crm(biz_id=15, version=version)

    assert result == {
        "some": "data",
        "landing_details": {
            "contacts": {"geo": {"permalink": "54321"}},
            "permalink": "54321",
        },
    }


@pytest.mark.parametrize(
    "address_component", [ac for ac in AddressComponent if ac != AddressComponent.HOUSE]
)
async def test_considers_address_inaccurate_if_house_not_in_address_components(
    address_component, geosearch, domain, dm
):
    geosearch.resolve_org.coro.return_value.address_components = {
        address_component: "value"
    }
    dm.fetch_landing_data_for_crm.coro.return_value = {
        "some": "data",
        "landing_details": {
            "contacts": {"geo": {"permalink": "54321"}},
            "permalink": "54321",
        },
    }

    result = await domain.fetch_landing_data_for_crm(
        biz_id=15, version=LandingVersion.STABLE
    )

    landing_details = result["landing_details"]
    assert landing_details["contacts"]["geo"]["address_is_accurate"] is False


async def test_considers_address_accurate_if_house_in_address_components(
    geosearch, domain, dm
):
    geosearch.resolve_org.coro.return_value.address_components = {
        AddressComponent.HOUSE: "value"
    }
    dm.fetch_landing_data_for_crm.coro.return_value = {
        "some": "data",
        "landing_details": {
            "contacts": {"geo": {"permalink": "54321"}},
            "permalink": "54321",
        },
    }

    result = await domain.fetch_landing_data_for_crm(
        biz_id=15, version=LandingVersion.STABLE
    )

    landing_details = result["landing_details"]
    assert landing_details["contacts"]["geo"]["address_is_accurate"] is True


async def test_returns_locality_if_locality_in_geosearch_response(
    geosearch, domain, dm
):
    geosearch.resolve_org.coro.return_value.address_components = {
        AddressComponent.LOCALITY: "Москва",
    }
    dm.fetch_landing_data_for_crm.coro.return_value = {
        "some": "data",
        "landing_details": {
            "contacts": {"geo": {"permalink": "54321"}},
            "permalink": "54321",
        },
    }

    result = await domain.fetch_landing_data_for_crm(
        biz_id=15, version=LandingVersion.STABLE
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
    geosearch.resolve_org.coro.return_value.address_components = {
        address_component: "value"
    }
    dm.fetch_landing_data_for_crm.coro.return_value = {
        "some": "data",
        "landing_details": {
            "contacts": {"geo": {"permalink": "54321"}},
            "permalink": "54321",
        },
    }

    result = await domain.fetch_landing_data_for_crm(
        biz_id=15, version=LandingVersion.STABLE
    )

    landing_details = result["landing_details"]
    assert "locality" not in landing_details["contacts"]["geo"]


@pytest.mark.parametrize("version", LandingVersion)
async def test_calls_geoseach_for_geo_data(version, domain, dm, geosearch):
    dm.fetch_landing_data_for_crm.coro.return_value = {
        "some": "data",
        "landing_details": {
            "contacts": {"geo": {"permalink": "54321"}},
            "permalink": "54321",
        },
    }

    await domain.fetch_landing_data_for_crm(biz_id=15, version=version)

    geosearch.resolve_org.assert_called_with(permalink=54321)


@pytest.mark.parametrize("version", LandingVersion)
async def test_calls_dm_as_expected(geosearch, domain, dm, version):
    geosearch.resolve_org.coro.return_value.permalink = "54321"
    dm.fetch_landing_data_for_crm.coro.return_value = {
        "some": "data",
        "landing_details": {
            "contacts": {"geo": {"permalink": "75653"}},
            "permalink": "75653",
        },
    }

    await domain.fetch_landing_data_for_crm(biz_id=15, version=version)

    dm.fetch_landing_data_for_crm.assert_called_with(biz_id=15, version=version)


@pytest.mark.parametrize("exc_cls", [NoDataForBizId, VersionDoesNotExist])
async def test_propagates_exceptions(domain, dm, exc_cls):
    dm.fetch_landing_data_for_crm.coro.side_effect = exc_cls

    with pytest.raises(exc_cls):
        await domain.fetch_landing_data_for_crm(
            biz_id=15, version=LandingVersion.STABLE
        )


@pytest.mark.parametrize("version", LandingVersion)
async def test_calls_if_no_org_info(geosearch, dm, domain, version):
    geosearch.resolve_org.coro.return_value = None
    dm.fetch_landing_data_for_crm.coro.return_value = {
        "some": "data",
        "landing_details": {
            "contacts": {"geo": {"permalink": "75653"}},
            "permalink": "75653",
            "is_updated_from_geosearch": True,
        },
    }

    await domain.fetch_landing_data_for_crm(biz_id=15, version=version)

    dm.fetch_landing_data_for_crm.assert_called_with(biz_id=15, version=version)


@pytest.mark.parametrize("version", LandingVersion)
async def test_raises_if_no_org_info(geosearch, dm, domain, version):
    geosearch.resolve_org.coro.return_value = None
    dm.fetch_landing_data_for_crm.coro.return_value = {
        "some": "data",
        "landing_details": {
            "contacts": {"geo": {"permalink": "54321"}},
            "permalink": "54321",
            "is_updated_from_geosearch": False,
        },
    }

    with pytest.raises(NoOrginfo):
        await domain.fetch_landing_data_for_crm(
            biz_id=15, version=LandingVersion.STABLE
        )
