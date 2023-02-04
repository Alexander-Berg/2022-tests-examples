import pytest

from maps_adv.geosmb.clients.geosearch import AddressComponent
from maps_adv.geosmb.landlord.proto import contacts_pb2, organization_details_pb2

pytestmark = [pytest.mark.asyncio]

URL = "/v1/fetch_landing_data/"


async def test_returns_sub_phone_in_contacts_if_has_one(api, factory, landing_data):
    await factory.create_substitution_phone(biz_id=22)
    data_id = await factory.insert_landing_data(**landing_data)

    await factory.insert_biz_state(
        biz_id=22, slug="cafe", published=True, stable_version=data_id
    )

    got = await api.post(
        URL,
        proto=organization_details_pb2.OrganizationDetailsInput(
            slug="cafe", token="fetch_data_token"
        ),
        decode_as=organization_details_pb2.OrganizationDetails,
        expected_status=200,
    )

    assert got.contacts.phone == "+7 (800) 200-06-00"
    assert got.contacts.is_substitution_phone is True


async def test_returns_saved_phone_in_contacts_if_has_no_sub_phone(
    api, factory, landing_data
):
    await factory.create_substitution_phone()
    data_id = await factory.insert_landing_data(**landing_data)
    await factory.insert_biz_state(
        biz_id=22, slug="cafe", published=True, stable_version=data_id
    )

    got = await api.post(
        URL,
        proto=organization_details_pb2.OrganizationDetailsInput(
            slug="cafe", token="fetch_data_token"
        ),
        decode_as=organization_details_pb2.OrganizationDetails,
        expected_status=200,
    )

    assert got.contacts.phone == "+7 (495) 739-70-00"
    assert got.contacts.is_substitution_phone is False


@pytest.mark.parametrize(
    "address_component", [ac for ac in AddressComponent if ac != AddressComponent.HOUSE]
)
async def test_considers_address_inaccurate_if_house_not_in_address_components(
    address_component, geosearch, api, factory, landing_data
):
    geosearch.resolve_org.coro.return_value.address_components = {
        address_component: "value"
    }
    data_id = await factory.insert_landing_data(**landing_data)

    await factory.insert_biz_state(
        biz_id=22, slug="cafe", published=True, stable_version=data_id
    )

    got = await api.post(
        URL,
        proto=organization_details_pb2.OrganizationDetailsInput(
            slug="cafe", token="fetch_data_token"
        ),
        decode_as=organization_details_pb2.OrganizationDetails,
        expected_status=200,
    )

    assert got.contacts.geo.address_is_accurate is False


async def test_considers_address_accurate_if_house_in_address_components(
    geosearch, api, factory, landing_data
):
    geosearch.resolve_org.coro.return_value.address_components = {
        AddressComponent.HOUSE: "value"
    }
    data_id = await factory.insert_landing_data(**landing_data)

    await factory.insert_biz_state(
        biz_id=22, slug="cafe", published=True, stable_version=data_id
    )

    got = await api.post(
        URL,
        proto=organization_details_pb2.OrganizationDetailsInput(
            slug="cafe", token="fetch_data_token"
        ),
        decode_as=organization_details_pb2.OrganizationDetails,
        expected_status=200,
    )

    assert got.contacts.geo.address_is_accurate is True


async def test_returns_locality_if_locality_in_geosearch_response(
    geosearch, api, factory, landing_data
):
    geosearch.resolve_org.coro.return_value.address_components = {
        AddressComponent.LOCALITY: "Москва"
    }
    data_id = await factory.insert_landing_data(**landing_data)

    await factory.insert_biz_state(
        biz_id=22, slug="cafe", published=True, stable_version=data_id
    )

    got = await api.post(
        URL,
        proto=organization_details_pb2.OrganizationDetailsInput(
            slug="cafe", token="fetch_data_token"
        ),
        decode_as=organization_details_pb2.OrganizationDetails,
        expected_status=200,
    )

    assert got.contacts.geo.locality == "Москва"


@pytest.mark.parametrize(
    "address_component",
    [ac for ac in AddressComponent if ac != AddressComponent.LOCALITY],
)
async def test_does_not_return_locality_if_no_locality_in_geosearch_response(
    address_component, geosearch, api, factory, landing_data
):
    geosearch.resolve_org.coro.return_value.address_components = {
        address_component: "value"
    }
    data_id = await factory.insert_landing_data(**landing_data)

    await factory.insert_biz_state(
        biz_id=22, slug="cafe", published=True, stable_version=data_id
    )

    got = await api.post(
        URL,
        proto=organization_details_pb2.OrganizationDetailsInput(
            slug="cafe", token="fetch_data_token"
        ),
        decode_as=organization_details_pb2.OrganizationDetails,
        expected_status=200,
    )

    assert not got.contacts.geo.HasField("locality")


async def test_does_not_return_service_area_if_no_service_area_in_geosearch_response(
    geosearch, api, factory, landing_data
):
    geosearch.resolve_org.coro.return_value.service_area = None
    data_id = await factory.insert_landing_data(**landing_data)

    await factory.insert_biz_state(
        biz_id=22, slug="cafe", published=True, stable_version=data_id
    )

    got = await api.post(
        URL,
        proto=organization_details_pb2.OrganizationDetailsInput(
            slug="cafe", token="fetch_data_token"
        ),
        decode_as=organization_details_pb2.OrganizationDetails,
        expected_status=200,
    )

    assert not got.contacts.geo.HasField("service_area")


async def test_returns_service_radius_if_service_area_is_circle(
    geosearch, api, factory, landing_data
):
    geosearch.resolve_org.coro.return_value.service_area = {
        "service_radius_km": 700
    }
    data_id = await factory.insert_landing_data(**landing_data)

    await factory.insert_biz_state(
        biz_id=22, slug="cafe", published=True, stable_version=data_id
    )

    got = await api.post(
        URL,
        proto=organization_details_pb2.OrganizationDetailsInput(
            slug="cafe", token="fetch_data_token"
        ),
        decode_as=organization_details_pb2.OrganizationDetails,
        expected_status=200,
    )

    assert got.contacts.geo.service_area == contacts_pb2.ServiceArea(
        service_radius_km=700
    )


async def test_returns_service_regions_if_service_area_is_regions(
    geosearch, api, factory, landing_data, geobase_client
):
    geosearch.resolve_org.coro.return_value.service_area = {
        "geo_ids": [39, 11029]
    }
    data_id = await factory.insert_landing_data(**landing_data)

    await factory.insert_biz_state(
        biz_id=22, slug="cafe", published=True, stable_version=data_id
    )

    got = await api.post(
        URL,
        proto=organization_details_pb2.OrganizationDetailsInput(
            slug="cafe", token="fetch_data_token"
        ),
        decode_as=organization_details_pb2.OrganizationDetails,
        expected_status=200,
    )

    assert got.contacts.geo.service_area == contacts_pb2.ServiceArea(
        area=contacts_pb2.Area(
            regions=[
                contacts_pb2.Region(
                    preposition="в", prepositional_case="Ростове-на-Дону"
                ),
                contacts_pb2.Region(
                    preposition="в", prepositional_case="Ростовской области"
                ),
            ]
        )
    )
