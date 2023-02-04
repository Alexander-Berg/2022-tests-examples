import pytest

from maps_adv.geosmb.landlord.server.lib.exceptions import (
    NoDataForBizId,
    UnsupportedFieldForSuggest,
)

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


@pytest.fixture(autouse=True)
def common_mocks(dm):
    dm.fetch_landing_data_for_crm.coro.return_value = {
        "landing_details": {"contacts": {"geo": {"permalink": "2233"}}}
    }


@pytest.mark.parametrize(
    "categories_names",
    [
        ["Общепит", "Ресторан"],
        ["Карусель"],
        [],
    ],
)
async def test_suggests_categories_from_geosearch(
    domain, geosearch, categories_names
):
    geosearch.resolve_org.coro.return_value.categories_names = categories_names

    result = await domain.suggest_field_values(biz_id=15, field="categories")

    assert result == categories_names


@pytest.mark.parametrize(
    "categories_names",
    [
        ["Общепит", "Ресторан"],
        ["Карусель"],
        [],
    ],
)
async def test_suggests_categories_without_geosearch(
    domain, geosearch, categories_names, dm
):
    geosearch.resolve_org.coro.return_value = None
    dm.fetch_landing_data_for_crm.coro.return_value['landing_details']['categories'] = categories_names

    result = await domain.suggest_field_values(biz_id=15, field="categories")

    assert result == categories_names


@pytest.mark.parametrize(
    ("features", "expected_extras"),
    [
        ([{"id": "wifi", "name": "Wi-fi", "value": True}], ["Wi-fi"]),
        # Capitalize name
        ([{"id": "wifi", "name": "wi-fi", "value": True}], ["Wi-fi"]),
        (
            # False value ignored
            [{"id": "wifi", "name": "Wi-fi", "value": False}],
            [],
        ),
        (
            # Text value ignored
            [{"id": "wifi", "name": "Wi-fi", "value": ["2.4G"]}],
            [],
        ),
        (
            # Enum value ignored
            [
                {
                    "id": "wifi",
                    "name": "Wi-fi",
                    "value": {"id": "enum_value1", "name": "Значение 1"},
                }
            ],
            [],
        ),
        (
            # Nameless feature ignored
            [{"id": "wifi", "value": True}],
            [],
        ),
        (
            # Combined case
            [
                {"id": "wifi", "name": "Wi-fi", "value": True},
                {"id": "wifi", "name": "Wi-fi", "value": ["2.4G"]},
                {"id": "card", "name": "Оплата картой", "value": True},
            ],
            ["Wi-fi", "Оплата картой"],
        ),
    ],
)
async def test_suggests_extras_from_geosearch(
    domain, geosearch, features, expected_extras
):
    geosearch.resolve_org.coro.return_value.features = features

    result = await domain.suggest_field_values(biz_id=15, field="plain_extras")

    assert result == expected_extras


@pytest.mark.parametrize(
    "expected_extras",
    [
        [],
        ["Wi-fi"],
        ["Wi-fi", "Оплата картой"],
    ],
)
async def test_suggests_extras_without_geosearch(
    domain, geosearch, expected_extras, dm
):
    geosearch.resolve_org.coro.return_value = None
    dm.fetch_landing_data_for_crm.coro.return_value['landing_details']['extras'] = {
        'plain_extras': expected_extras
    }

    result = await domain.suggest_field_values(biz_id=15, field="plain_extras")

    assert result == expected_extras


@pytest.mark.parametrize("field", ["categories", "plain_extras"])
async def test_uses_geosearch_client_if_needed(domain, geosearch, field):
    await domain.suggest_field_values(biz_id=15, field=field)

    geosearch.resolve_org.assert_called_with(permalink=2233)


async def test_raises_for_unknown_biz_id(domain, dm):
    dm.fetch_landing_data_for_crm.coro.side_effect = NoDataForBizId

    with pytest.raises(NoDataForBizId):
        await domain.suggest_field_values(biz_id=15, field="plain_extras")


async def test_raises_for_unknown_field(domain):
    with pytest.raises(UnsupportedFieldForSuggest) as exc:
        await domain.suggest_field_values(biz_id=15, field="something_we_cant")

    assert exc.value.args == ("something_we_cant",)
