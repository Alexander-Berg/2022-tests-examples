import pytest

from maps_adv.geosmb.clients.geosearch import AddressComponent
from maps_adv.geosmb.landlord.proto import common_pb2, preferences_pb2
from maps_adv.geosmb.landlord.proto.internal import suggests_pb2

pytestmark = [pytest.mark.asyncio]

url = "/v1/suggests/cta_button/"

make_route = preferences_pb2.CTAButton(
    predefined=preferences_pb2.CTAButton.PredefinedType.MAKE_ROUTE,
    value="http://maps-url?ll=22.33%2C11.22&rtext=~11.22%2C22.33&mode=route&ruri=ymapsbm1%3A%2F%2Forg%3Foid%3D54321",  # noqa
)
call = preferences_pb2.CTAButton(
    predefined=preferences_pb2.CTAButton.PredefinedType.CALL,
    value="+7 (495) 739-70-00",
)
request = preferences_pb2.CTAButton(
    predefined=preferences_pb2.CTAButton.PredefinedType.REQUEST,
    value="http://widget_url/54321",
)
book_maps = preferences_pb2.CTAButton(
    predefined=preferences_pb2.CTAButton.PredefinedType.BOOK_MAPS,
    value="/web-maps/webview?mode=booking&booking[permalink]=1085365923&booking[standalone]=true&source=partner-cta"
)


@pytest.fixture
def make_landing(factory):
    async def _make(categories, phone="+7 (495) 739-70-00"):
        data_id = await factory.insert_landing_data(
            categories=categories, contacts={"phone": phone}
        )
        await factory.insert_biz_state(
            biz_id=15, slug="kek", permalink="54321", unstable_version=data_id
        )

    return _make


@pytest.mark.parametrize(
    "phone, address, expected",
    (
        [None, None, [request]],
        [None, "Улица 1", [make_route, request]],
        ["+7 (495) 739-70-00", None, [call, request]],
        ["+7 (495) 739-70-00", "Улица 1", [call, make_route, request]],
    ),
)
async def test_suggests_buttons_for_unknown_category_group(
    phone, address, expected, api, make_landing, geosearch
):
    geosearch.resolve_org.coro.return_value.formatted_address = address
    await make_landing(["Хз что это"], phone)

    got = await api.post(
        url,
        proto=suggests_pb2.SuggestInput(biz_id=15),
        decode_as=suggests_pb2.CTAButtonSuggest,
        expected_status=200,
    )

    assert list(got.available_buttons) == expected


async def test_does_not_suggest_make_route_for_online_orgs(
    api, make_landing, geosearch
):
    await make_landing(["Хз что это", "Барбершоп"])
    geosearch.resolve_org.coro.return_value.is_online = True

    got = await api.post(
        url,
        proto=suggests_pb2.SuggestInput(biz_id=15),
        decode_as=suggests_pb2.CTAButtonSuggest,
        expected_status=200,
    )

    assert not any(
        button.predefined == preferences_pb2.CTAButton.PredefinedType.MAKE_ROUTE
        for button in got.available_buttons
    )


async def test_does_not_suggest_make_route_for_orgs_with_inaccurate_address(
    api, make_landing, geosearch
):
    await make_landing(["Хз что это", "Барбершоп"])
    geosearch.resolve_org.coro.return_value.address_components = {
        AddressComponent.DISTRICT: "На районе",
        AddressComponent.STREET: "Улица",
    }

    got = await api.post(
        url,
        proto=suggests_pb2.SuggestInput(biz_id=15),
        decode_as=suggests_pb2.CTAButtonSuggest,
        expected_status=200,
    )

    assert not any(
        button.predefined == preferences_pb2.CTAButton.PredefinedType.MAKE_ROUTE
        for button in got.available_buttons
    )


async def test_returns_buttons_in_order_of_priority(api, make_landing):
    await make_landing(["Хз что это", "Барбершоп", "Кофейня"])

    got = await api.post(
        url,
        proto=suggests_pb2.SuggestInput(biz_id=15),
        decode_as=suggests_pb2.CTAButtonSuggest,
        expected_status=200,
    )

    assert list(got.available_buttons) == [call, make_route, request]


@pytest.mark.parametrize(
    "phone, address",
    (
        [None, None],
        [
            None,
            "Улица 1",
        ],
        [
            "+7 (495) 739-70-00",
            None,
        ],
        [
            "+7 (495) 739-70-00",
            "Улица 1",
        ],
    ),
)
async def test_suggests_types_for_unknown_category_group(
    phone, address, api, make_landing, geosearch
):
    geosearch.resolve_org.coro.return_value.formatted_address = address
    await make_landing(["Хз что это"], phone)

    got = await api.post(
        url,
        proto=suggests_pb2.SuggestInput(biz_id=15),
        decode_as=suggests_pb2.CTAButtonSuggest,
        expected_status=200,
    )

    assert list(got.available_types) == [
        preferences_pb2.CTAButton.PredefinedType.CALL,
        preferences_pb2.CTAButton.PredefinedType.MAKE_ROUTE,
        preferences_pb2.CTAButton.PredefinedType.REQUEST,
    ]


async def test_does_not_suggest_make_route_type_for_online_orgs(
    api, make_landing, geosearch
):
    await make_landing(["Хз что это", "Барбершоп"])
    geosearch.resolve_org.coro.return_value.is_online = True

    got = await api.post(
        url,
        proto=suggests_pb2.SuggestInput(biz_id=15),
        decode_as=suggests_pb2.CTAButtonSuggest,
        expected_status=200,
    )

    assert (
        preferences_pb2.CTAButton.PredefinedType.MAKE_ROUTE not in got.available_types
    )


async def test_suggests_booking_if_bookings_exist_in_geosearch(
    api, make_landing, geosearch
):
    await make_landing(["Хз что это", "Барбершоп"])
    geosearch.resolve_org.coro.return_value.bookings = [{
        "standaloneWidgetPath": "/web-maps/webview?mode=booking&booking[permalink]=1085365923&booking[standalone]=true&source=partner-cta"
    }]

    got = await api.post(
        url,
        proto=suggests_pb2.SuggestInput(biz_id=15),
        decode_as=suggests_pb2.CTAButtonSuggest,
        expected_status=200,
    )

    assert preferences_pb2.CTAButton.PredefinedType.BOOK_MAPS in got.available_types
    assert book_maps in list(got.available_buttons)


async def test_does_not_suggest_booking_if_no_bookings_in_geosearch(
    api, make_landing, geosearch
):
    await make_landing(["Хз что это", "Барбершоп"])
    geosearch.resolve_org.coro.return_value.bookings = []

    got = await api.post(
        url,
        proto=suggests_pb2.SuggestInput(biz_id=15),
        decode_as=suggests_pb2.CTAButtonSuggest,
        expected_status=200,
    )

    assert preferences_pb2.CTAButton.PredefinedType.BOOK_MAPS not in got.available_types
    assert book_maps not in list(got.available_buttons)


async def test_does_not_suggest_make_route_type_for_orgs_with_inaccurate_address(
    api, make_landing, geosearch
):
    await make_landing(["Хз что это", "Барбершоп"])
    geosearch.resolve_org.coro.return_value.address_components = {
        AddressComponent.DISTRICT: "На районе",
        AddressComponent.STREET: "Улица",
    }

    got = await api.post(
        url,
        proto=suggests_pb2.SuggestInput(biz_id=15),
        decode_as=suggests_pb2.CTAButtonSuggest,
        expected_status=200,
    )

    assert (
        preferences_pb2.CTAButton.PredefinedType.MAKE_ROUTE not in got.available_types
    )


async def test_returns_types_in_order_of_priority(api, make_landing):
    await make_landing(["Хз что это", "Кофейня", "Барбершоп"])

    got = await api.post(
        url,
        proto=suggests_pb2.SuggestInput(biz_id=15),
        decode_as=suggests_pb2.CTAButtonSuggest,
        expected_status=200,
    )

    assert list(got.available_types) == [
        preferences_pb2.CTAButton.PredefinedType.CALL,
        preferences_pb2.CTAButton.PredefinedType.MAKE_ROUTE,
        preferences_pb2.CTAButton.PredefinedType.REQUEST,
    ]


async def test_returns_error_for_unknown_biz_id(api):
    got = await api.post(
        url,
        proto=suggests_pb2.SuggestInput(biz_id=15),
        decode_as=common_pb2.Error,
        expected_status=400,
    )

    assert got == common_pb2.Error(code=common_pb2.Error.BIZ_ID_UNKNOWN)
