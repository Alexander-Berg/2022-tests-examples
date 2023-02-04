import pytest

from maps_adv.geosmb.clients.geosearch import AddressComponent
from maps_adv.geosmb.landlord.server.lib import BaseDataManager

pytestmark = [
    pytest.mark.asyncio,
    pytest.mark.mock_dm,
    pytest.mark.usefixtures("logging_warning"),
]


def fetch_saved_cta_button(dm: BaseDataManager) -> dict:
    return dm.save_landing_data_for_biz_id.call_args.kwargs["landing_data"][
        "preferences"
    ].get("cta_button")


@pytest.mark.parametrize("params", [{"biz_id": 15}, {"permalink": 54321}])
async def test_saves_call_cta_button_for_unknown_category_group(
    domain, dm, geosearch, params
):
    geosearch.resolve_org.coro.return_value.categories_names = [
        "Неизвестная услуга"
    ]
    geosearch.resolve_org.coro.return_value.formatted_callable_phones = [
        "+7 (495) 739-70-00",
        "+7 (495) 739-70-11",
    ]

    await domain.generate_data_for_biz_id(**params)

    assert fetch_saved_cta_button(dm) == {
        "predefined": "CALL",
        "value": "+7 (495) 739-70-00",
    }


@pytest.mark.parametrize("params", [{"biz_id": 15}, {"permalink": 54321}])
async def test_saves_make_route_cta_button_for_unknown_category_group_without_phone(
    domain, dm, geosearch, params
):
    geosearch.resolve_org.coro.return_value.categories_names = [
        "Неизвестная услуга"
    ]
    geosearch.resolve_org.coro.return_value.formatted_callable_phones = []
    geosearch.resolve_org.coro.return_value.formatted_address = "Улица 1"

    await domain.generate_data_for_biz_id(**params)

    assert fetch_saved_cta_button(dm) == {
        "predefined": "MAKE_ROUTE",
        "value": "http://maps-url?ll=22.33%2C11.22&rtext=~11.22%2C22.33&mode=route&ruri=ymapsbm1%3A%2F%2Forg%3Foid%3D54321",  # noqa
    }


@pytest.mark.parametrize("params", [{"biz_id": 15}, {"permalink": 54321}])
async def test_does_not_save_cta_button_for_unknown_category_group_without_phone_and_address(  # noqa: E501
    domain, dm, geosearch, params
):
    geosearch.resolve_org.coro.return_value.categories_names = [
        "Неизвестная услуга"
    ]
    geosearch.resolve_org.coro.return_value.formatted_callable_phones = []
    geosearch.resolve_org.coro.return_value.formatted_address = None

    await domain.generate_data_for_biz_id(**params)

    assert fetch_saved_cta_button(dm) == {
        "predefined": "REQUEST",
        "value": "http://widget_url/54321"
    }


@pytest.mark.parametrize("params", [{"biz_id": 15}, {"permalink": 54321}])
async def test_does_not_save_cta_button_for_unknown_category_group_with_inaccurate_address(  # noqa: E501
    domain, dm, geosearch, params
):
    geosearch.resolve_org.coro.return_value.categories_names = [
        "Неизвестная услуга"
    ]
    geosearch.resolve_org.coro.return_value.formatted_callable_phones = []
    geosearch.resolve_org.coro.return_value.address_components = {
        AddressComponent.DISTRICT: "На районе",
        AddressComponent.STREET: "Улица",
    }

    await domain.generate_data_for_biz_id(**params)

    assert fetch_saved_cta_button(dm) == {
        "predefined": "REQUEST",
        "value": "http://widget_url/54321"
    }


@pytest.mark.parametrize("params", [{"biz_id": 15}, {"permalink": 54321}])
async def test_does_not_save_cta_button_for_unknown_category_group_online_org(
    domain, dm, geosearch, params
):
    geosearch.resolve_org.coro.return_value.categories_names = [
        "Неизвестная услуга"
    ]
    geosearch.resolve_org.coro.return_value.formatted_callable_phones = []
    geosearch.resolve_org.coro.return_value.is_online = True

    await domain.generate_data_for_biz_id(**params)

    assert fetch_saved_cta_button(dm) == {
        "predefined": "REQUEST",
        "value": "http://widget_url/54321"
    }
