import pytest

from maps_adv.geosmb.clients.geosearch import AddressComponent
from maps_adv.geosmb.landlord.server.lib.exceptions import NoDataForBizId

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


make_route = dict(
    predefined="MAKE_ROUTE",
    value="http://maps-url?ll=22.33%2C11.22&rtext=~11.22%2C22.33&mode=route&ruri=ymapsbm1%3A%2F%2Forg%3Foid%3D54321",  # noqa
)
call = dict(
    predefined="CALL",
    value="+7 (495) 739-70-00",
)
request = dict(
    predefined="REQUEST",
    value="http://widget_url/54321",
)
book_maps = dict(
    predefined="BOOK_MAPS",
    value="/web-maps/webview?mode=booking&booking[permalink]=1085365923&booking[standalone]=true&source=partner-cta"
)


@pytest.fixture
def mock_landing(dm):
    async def _make(categories, phone="+7 (495) 739-70-00"):
        dm.fetch_landing_data_for_crm.coro.return_value = {
            "landing_details": {
                "contacts": {"geo": {"permalink": "54321"}, "phone": phone},
                "categories": categories,
            }
        }

    return _make


async def test_returns_expected_keys(domain, mock_landing):
    await mock_landing(["Хз что это"])

    got = await domain.suggest_field_values(biz_id=15, field="cta_button")

    assert list(got.keys()) == ["available_buttons", "available_types"]


@pytest.mark.parametrize(
    "phone, address, expected",
    (
        [None, None, [request]],
        [
            None,
            "Улица 1",
            [make_route, request],
        ],
        [
            "+7 (495) 739-70-00",
            None,
            [call, request],
        ],
        [
            "+7 (495) 739-70-00",
            "Улица 1",
            [call, make_route, request],
        ],
    ),
)
async def test_suggests_buttons_for_unknown_category_group(
    phone, address, expected, domain, mock_landing, geosearch
):
    geosearch.resolve_org.coro.return_value.formatted_address = address
    await mock_landing(["Хз что это"], phone)

    got = await domain.suggest_field_values(biz_id=15, field="cta_button")

    assert got["available_buttons"] == expected


@pytest.mark.parametrize(
    "phone, address, expected",
    (
        [None, None, [request]],
        [
            None,
            "Улица 1",
            [request],
        ],
        [
            "+7 (495) 739-70-00",
            None,
            [call, request],
        ],
        [
            "+7 (495) 739-70-00",
            "Улица 1",
            [call, request],
        ],
    ),
)
async def test_suggests_buttons_for_unknown_category_group_without_geo(
    phone, address, expected, domain, mock_landing, geosearch
):
    geosearch.resolve_org.coro.return_value = None
    await mock_landing(["Хз что это"], phone)

    got = await domain.suggest_field_values(biz_id=15, field="cta_button")

    assert got["available_buttons"] == expected


async def test_does_not_suggest_make_route_for_online_orgs(
    domain, mock_landing, geosearch
):
    await mock_landing(["Хз что это", "Барбершоп"])
    geosearch.resolve_org.coro.return_value.is_online = True

    got = await domain.suggest_field_values(biz_id=15, field="cta_button")

    assert make_route not in got["available_buttons"]


async def test_does_not_suggest_make_route_for_orgs_with_inaccurate_address(
    domain, mock_landing, geosearch
):
    await mock_landing(["Хз что это", "Барбершоп"])
    geosearch.resolve_org.coro.return_value.address_components = {
        AddressComponent.DISTRICT: "На районе",
        AddressComponent.STREET: "Улица",
    }

    got = await domain.suggest_field_values(biz_id=15, field="cta_button")

    assert make_route not in got["available_buttons"]


async def test_returns_buttons_in_order_of_priority(domain, mock_landing):
    await mock_landing(["Хз что это", "Барбершоп", "Кофейня"])

    got = await domain.suggest_field_values(biz_id=15, field="cta_button")

    assert got["available_buttons"] == [call, make_route, request]


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
    phone, address, domain, mock_landing, geosearch
):
    geosearch.resolve_org.coro.return_value.formatted_address = address
    await mock_landing(["Хз что это"], phone)

    got = await domain.suggest_field_values(biz_id=15, field="cta_button")

    assert got["available_types"] == ["CALL", "MAKE_ROUTE", "REQUEST"]


async def test_does_not_suggest_make_route_type_for_online_orgs(
    domain, mock_landing, geosearch
):
    await mock_landing(["Хз что это", "Барбершоп"])
    geosearch.resolve_org.coro.return_value.is_online = True

    got = await domain.suggest_field_values(biz_id=15, field="cta_button")

    assert "MAKE_ROUTE" not in got["available_types"]


async def test_suggests_booking_if_bookings_exist_in_geosearch(
    domain, mock_landing, geosearch
):
    await mock_landing(["Хз что это", "Барбершоп"])
    geosearch.resolve_org.coro.return_value.bookings = [{
        "standaloneWidgetPath": "/web-maps/webview?mode=booking&booking[permalink]=1085365923&booking[standalone]=true&source=partner-cta"
    }]

    got = await domain.suggest_field_values(biz_id=15, field="cta_button")

    assert "BOOK_MAPS" in got["available_types"]
    assert book_maps in got["available_buttons"]


async def test_does_not_suggest_booking_if_no_bookings_in_geosearch(
    domain, mock_landing, geosearch
):
    await mock_landing(["Хз что это", "Барбершоп"])
    geosearch.resolve_org.coro.return_value.bookings = []

    got = await domain.suggest_field_values(biz_id=15, field="cta_button")

    assert "BOOK_MAPS" not in got["available_types"]
    assert book_maps not in got["available_buttons"]


async def test_does_not_suggest_make_route_type_for_orgs_with_inaccurate_address(
    domain, mock_landing, geosearch
):
    await mock_landing(["Хз что это", "Барбершоп"])
    geosearch.resolve_org.coro.return_value.address_components = {
        AddressComponent.DISTRICT: "На районе",
        AddressComponent.STREET: "Улица",
    }

    got = await domain.suggest_field_values(biz_id=15, field="cta_button")

    assert "MAKE_ROUTE" not in got["available_types"]


async def test_returns_types_in_order_of_priority(domain, mock_landing):
    await mock_landing(["Хз что это", "Кофейня", "Барбершоп"])

    got = await domain.suggest_field_values(biz_id=15, field="cta_button")

    assert got["available_types"] == ["CALL", "MAKE_ROUTE", "REQUEST"]


async def test_raises_for_unknown_biz_id(domain, dm):
    dm.fetch_landing_data_for_crm.coro.side_effect = NoDataForBizId

    with pytest.raises(NoDataForBizId):
        await domain.suggest_field_values(biz_id=15, field="cta_button")
