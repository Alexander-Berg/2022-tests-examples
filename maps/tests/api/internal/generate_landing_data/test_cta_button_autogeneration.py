import pytest

from maps_adv.geosmb.clients.geosearch import AddressComponent
from maps_adv.geosmb.landlord.proto import generate_pb2

pytestmark = [
    pytest.mark.asyncio,
    pytest.mark.usefixtures("logging_warning"),
]

URL = "/v1/generate_landing_data/"


@pytest.mark.parametrize("params", [{"biz_id": 15}, {"permalink": 54321}])
async def test_saves_call_cta_button_for_unknown_category_group(
    api, factory, geosearch, params
):
    geosearch.resolve_org.coro.return_value.categories_names = [
        "Неизвестная услуга"
    ]
    geosearch.resolve_org.coro.return_value.formatted_callable_phones = [
        "+7 (495) 739-70-00",
        "+7 (495) 739-70-11",
    ]

    await api.post(
        URL,
        proto=generate_pb2.GenerateDataInput(**params),
        decode_as=generate_pb2.GenerateDataOutput,
        expected_status=201,
    )

    landing_datas = await factory.list_all_landing_data()
    assert landing_datas[0]["preferences"]["cta_button"] == {
        "predefined": "CALL",
        "value": "+7 (495) 739-70-00",
    }


@pytest.mark.parametrize("params", [{"biz_id": 15}, {"permalink": 54321}])
async def test_saves_make_route_cta_button_for_unknown_category_group_without_phone(
    api, factory, geosearch, params
):
    geosearch.resolve_org.coro.return_value.categories_names = [
        "Неизвестная услуга"
    ]
    geosearch.resolve_org.coro.return_value.formatted_callable_phones = []
    geosearch.resolve_org.coro.return_value.formatted_address = "Улица 1"

    await api.post(
        URL,
        proto=generate_pb2.GenerateDataInput(**params),
        decode_as=generate_pb2.GenerateDataOutput,
        expected_status=201,
    )

    landing_datas = await factory.list_all_landing_data()
    assert landing_datas[0]["preferences"]["cta_button"] == {
        "predefined": "MAKE_ROUTE",
        "value": "http://maps-url?ll=22.33%2C11.22&rtext=~11.22%2C22.33&mode=route&ruri=ymapsbm1%3A%2F%2Forg%3Foid%3D54321",  # noqa
    }


@pytest.mark.parametrize("params", [{"biz_id": 15}, {"permalink": 54321}])
async def test_does_not_save_cta_button_for_unknown_category_group_without_phone_and_address(  # noqa: E501
    api, factory, geosearch, params
):
    geosearch.resolve_org.coro.return_value.categories_names = [
        "Неизвестная услуга"
    ]
    geosearch.resolve_org.coro.return_value.formatted_callable_phones = []
    geosearch.resolve_org.coro.return_value.formatted_address = None

    await api.post(
        URL,
        proto=generate_pb2.GenerateDataInput(**params),
        decode_as=generate_pb2.GenerateDataOutput,
        expected_status=201,
    )

    landing_datas = await factory.list_all_landing_data()
    assert "cta_button" in landing_datas[0]["preferences"]


@pytest.mark.parametrize("params", [{"biz_id": 15}, {"permalink": 54321}])
async def test_does_not_save_cta_button_for_unknown_category_group_with_inaccurate_address(  # noqa: E501
    api, factory, geosearch, params
):
    geosearch.resolve_org.coro.return_value.categories_names = [
        "Неизвестная услуга"
    ]
    geosearch.resolve_org.coro.return_value.formatted_callable_phones = []
    geosearch.resolve_org.coro.return_value.address_components = {
        AddressComponent.DISTRICT: "На районе",
        AddressComponent.STREET: "Улица",
    }

    await api.post(
        URL,
        proto=generate_pb2.GenerateDataInput(**params),
        decode_as=generate_pb2.GenerateDataOutput,
        expected_status=201,
    )

    landing_datas = await factory.list_all_landing_data()
    assert "cta_button" in landing_datas[0]["preferences"]


@pytest.mark.parametrize("params", [{"biz_id": 15}, {"permalink": 54321}])
async def test_does_not_save_cta_button_for_unknown_category_group_online_org(
    api, factory, geosearch, params
):
    geosearch.resolve_org.coro.return_value.categories_names = [
        "Неизвестная услуга"
    ]
    geosearch.resolve_org.coro.return_value.formatted_callable_phones = []
    geosearch.resolve_org.coro.return_value.is_online = True

    await api.post(
        URL,
        proto=generate_pb2.GenerateDataInput(**params),
        decode_as=generate_pb2.GenerateDataOutput,
        expected_status=201,
    )

    landing_datas = await factory.list_all_landing_data()
    assert "cta_button" in landing_datas[0]["preferences"]
